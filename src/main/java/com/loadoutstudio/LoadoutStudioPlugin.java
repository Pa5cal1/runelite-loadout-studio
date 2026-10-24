/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;

import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;

@PluginDescriptor(name = "Loadout Studio (Test)", description = "Build and save loadouts with Inventory Setups and Bank Tag compatibility", tags = {"inventory", "setup", "bank", "loadout", "wiki"}, enabledByDefault = false, internalName = "loadout-studio")

public class LoadoutStudioPlugin extends Plugin implements StudioPanel.Host
{
	@Inject private net.runelite.api.Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar toolbar;
	@Inject private ItemManager itemManager;
    @Inject private net.runelite.client.input.KeyManager keyManager;
    @Inject private LoadoutStudioConfig config;
    private net.runelite.client.util.HotkeyListener filterHotkey;
    private volatile List<StudioPanel.QuickLink> quickLinks=java.util.Collections.emptyList();
    @Inject private javax.inject.Provider<net.runelite.client.game.chatbox.ChatboxItemSearch> chatboxSearch;
    @Inject private net.runelite.client.game.chatbox.ChatboxPanelManager chatbox;
    private net.runelite.client.game.chatbox.ChatboxInput ownedChatbox;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private OkHttpClient http;
	@Inject private net.runelite.client.plugins.PluginManager pluginManager;
    @Inject private net.runelite.client.plugins.bank.BankSearch bankSearch;
    private BankTagsPlugin nativeOwner;
    private IndependentBankView independentBank;
    private java.util.function.IntPredicate activeBankMatches=id->false;
    private volatile Runnable bankObserver;
	private TagManager nativeTagManager;
	
	private StudioBankFilter bankFilter;
    private final String ownedBankTag="loadoutstudio_"+java.util.UUID.randomUUID().toString().replace("-","");
    private volatile Consumer<JsonArray> layoutObserver;
    private volatile Consumer<JsonObject> gearObserver;
    private volatile boolean gearDirty;
    private volatile JsonObject lastGear;
    private volatile long gearEpoch;
    private final java.util.Set<Integer> gearVarps=new java.util.HashSet<>();
    private final java.util.Map<Integer,Integer> observedBank=new java.util.LinkedHashMap<>();
    private long bankObservedAt;
	private final java.util.concurrent.atomic.AtomicLong importRevision = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong bankRevision = new java.util.concurrent.atomic.AtomicLong();
	private NavigationButton navigation;
	private StudioPanel panel;
	private WikiClient wiki;
    private java.util.concurrent.ExecutorService importWorker;
	private volatile boolean active;
	private boolean indexing;
	private int nextItem;
	private final ItemCatalogue catalogue = new ItemCatalogue();
	@Provides LoadoutStudioConfig provideConfig(ConfigManager manager) { return manager.getConfig(LoadoutStudioConfig.class); }
    @Override public LoadoutStudioConfig settings(){return config;}
    @Override public void libraryOrder(LoadoutStudioConfig.LibraryOrder order){configManager.setConfiguration(LoadoutStudioConfig.GROUP,"libraryOrder",order);}

	@Override protected void startUp()
	{
		active = true; wiki = new WikiClient(http, gson, 2000, new WikiCacheFile(this::getPluginDirectory),config::wikiFreshDays);
        independentBank=new IndependentBankView(client,clientThread,bankSearch,id->bankFilter==null?itemManager.canonicalize(id):bankFilter.layoutItemId(id));independentBank.onChanged(this::localBankLayoutChanged);
        filterHotkey=new net.runelite.client.util.HotkeyListener(config::filterHotkey){@Override public void hotkeyPressed(){clientThread.invoke(()->{net.runelite.api.widgets.Widget bank=client.getWidget(InterfaceID.Bankmain.ITEMS);if(!active||bank==null||bank.isHidden())return;String owner=recoveryOwner();SwingUtilities.invokeLater(()->{if(active&&panel!=null&&owner.equals(recoveryOwner()))panel.toggleBankFilter();});});}};keyManager.registerKeyListener(filterHotkey);
        importWorker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"loadout-local-import");t.setDaemon(true);return t;});
		bankFilter = new StudioBankFilter(new StudioBankFilter.Gateway()
		{
			public boolean bankOpen() { net.runelite.api.widgets.Widget w = client.getWidget(InterfaceID.Bankmain.ITEMS); return client.getGameState() == GameState.LOGGED_IN && w != null && !w.isHidden() && w.getChildren() != null && w.getChildren().length > 0; }
			public int capacity() { net.runelite.api.widgets.Widget w = client.getWidget(InterfaceID.Bankmain.ITEMS); return w == null || w.getChildren() == null ? 0 : w.getChildren().length; }
			public String activeTag() { return independentBank.active()?ownedTag():coreBankTags()==null?null:coreBankTags().getActiveTag(); }
            public int[] currentLayout() { if(independentBank.active())return independentBank.layout();BankTagsPlugin tags=coreBankTags();Layout layout=tags==null?null:tags.getActiveLayout();return layout==null?null:layout.getLayout().clone(); }
			public int canonicalize(int id) { return itemManager.canonicalize(id); }
			public int variation(int id) { return SetupVariationMapping.map(id); }
			public void register(String tag, java.util.function.IntPredicate matches) { activeBankMatches=matches;if(nativeTagManager!=null)nativeTagManager.unregisterTag(tag);BankTagsPlugin tags=coreBankTags();nativeTagManager=tags==null?null:tags.getInjector().getInstance(TagManager.class);if(nativeTagManager!=null)nativeTagManager.registerTag(tag,matches::test); }
			public void open(String tag, int[] layout)
			{
				BankTagsPlugin tags=coreBankTags();if(tags==null){independentBank.select(layout,activeBankMatches);nativeOwner=null;}
                else {independentBank.clear();nativeOwner=tags;tags.openTag(tag,new Layout(tag,layout),BankTagsService.OPTION_HIDE_TAG_NAME|BankTagsService.OPTION_ITEMS_NOT_IN_LAYOUT_AT_BOTTOM);}
			}
			public void title(String title) { net.runelite.api.widgets.Widget widget=client.getWidget(InterfaceID.Bankmain.TITLE); if(widget!=null) widget.setText(title); }
			public void close() { if(independentBank.active())independentBank.clear();else if(nativeOwner!=null)nativeOwner.closeBankTag(); }
			public void unregister(String tag) { if(nativeTagManager!=null){nativeTagManager.unregisterTag(tag);nativeTagManager=null;} }
			public void discardLayout(String tag) { configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP,BankTagsPlugin.TAG_LAYOUT_PREFIX+tag); }
		}, ownedBankTag);
		SwingUtilities.invokeLater(() ->
		{
			if (!active) return;
			panel = new StudioPanel(this, gson); navigation = NavigationButton.builder().tooltip("Loadout Studio (Test)").icon(icon()).panel(panel).priority(7).build(); toolbar.addNavigation(navigation);
		});
		clientThread.invokeLater(()->{indexItems();if(client.getGameState()==GameState.LOGGED_IN)indexGearVarps();});
		gearDirty=true;
	}
	@Override protected void shutDown()
	{
		active = false; importRevision.incrementAndGet(); cancelItemInput(); if (wiki != null) wiki.close(); if(importWorker!=null)importWorker.shutdownNow();
        if(filterHotkey!=null)keyManager.unregisterKeyListener(filterHotkey);filterHotkey=null;quickLinks=java.util.Collections.emptyList();
        gearObserver=null;layoutObserver=null;bankObserver=null;lastGear=null;clearBankObservation();
		bankRevision.incrementAndGet(); StudioBankFilter previousFilter = bankFilter; bankFilter = null; clientThread.invoke(() -> { if (previousFilter != null) previousFilter.clear(); });
		SwingUtilities.invokeLater(() -> { if (navigation != null) toolbar.removeNavigation(navigation); if (panel != null) panel.dispose(); panel = null; navigation = null; });
		catalogue.clear(); indexing = false; nextItem = 0;
	}
    private String ownedTag(){return ownedBankTag;}
    private BankTagsPlugin coreBankTags(){for(Plugin plugin:pluginManager.getPlugins())if(plugin instanceof BankTagsPlugin&&pluginManager.isPluginActive(plugin))return (BankTagsPlugin)plugin;return null;}
    @Override public void observeBank(Runnable observer){bankObserver=observer;}
    private void localBankLayoutChanged(){if(bankFilter==null)return;JsonArray layout=bankFilter.readChangedLayout();Consumer<JsonArray> observer=layoutObserver;long revision=bankRevision.get();if(layout!=null&&observer!=null)SwingUtilities.invokeLater(()->{if(active&&revision==bankRevision.get())observer.accept(layout);});}
    @Subscribe public void onBankScriptCallback(net.runelite.api.events.ScriptCallbackEvent event){if(active&&independentBank!=null)independentBank.callback(event);}
    @Subscribe public void onOptionalPluginChanged(net.runelite.client.events.PluginChanged event){if(event.getPlugin() instanceof BankTagsPlugin)clientThread.invokeLater(()->{if(active&&bankFilter!=null)bankFilter.apply();});}
	private static BufferedImage icon() { return StudioBrandIcon.create(); }
    @Override public void quickLinks(List<StudioPanel.QuickLink> links){quickLinks=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(links));}
    @Subscribe public void onMenuEntryAdded(net.runelite.api.events.MenuEntryAdded event)
    {
        if(!active || !config.bankMenu() || event.getActionParam1()!=InterfaceID.Bankmain.WORNITEMS_BUTTON || event.getType()!=net.runelite.api.MenuAction.CC_OP.getId() || event.getIdentifier()!=1)return;
        String owner=recoveryOwner();for(StudioPanel.QuickLink link:quickLinks)client.getMenu().createMenuEntry(-1).setOption("Loadout Studio").setTarget(link.name.replace("<","").replace(">","")).setType(net.runelite.api.MenuAction.RUNELITE).onClick(e->SwingUtilities.invokeLater(()->{if(active&&panel!=null&&owner.equals(recoveryOwner()))panel.openSaved(link.id);}));
    }

    @Override public void observeGear(Consumer<JsonObject> observer) { gearObserver=observer;gearDirty=true; }
    @Override public void requestGear() { clientThread.invoke(()->{lastGear=null;gearDirty=true;}); }
    private void indexGearVarps(){gearVarps.clear();for(int bit:GearContainers.trackedVarbits())gearVarps.add(client.getVarbit(bit).getIndex());}
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event)
    {
        if(event.getContainerId()==InventoryID.INV || event.getContainerId()==InventoryID.WORN)gearDirty=true;
        if(event.getContainerId()==InventoryID.BANK)observeBank();
    }
    @Subscribe public void onVarbitChanged(VarbitChanged event)
    {
        if(gearVarps.contains(event.getVarpId()))gearDirty=true;
        if(event.getVarpId()==VarPlayerID.COM_MODE || event.getVarpId()==VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO || event.getVarpId()==VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT)gearDirty=true;
        int bit=event.getVarbitId();if(bit==VarbitID.SPELLBOOK || bit==VarbitID.COMBAT_WEAPON_CATEGORY || bit==VarbitID.AUTOCAST_DEFMODE || bit==VarbitID.RUNE_POUCH_TYPE_1 || bit==VarbitID.RUNE_POUCH_TYPE_2 || bit==VarbitID.RUNE_POUCH_TYPE_3 || bit==VarbitID.RUNE_POUCH_TYPE_4 || bit==VarbitID.RUNE_POUCH_QUANTITY_1 || bit==VarbitID.RUNE_POUCH_QUANTITY_2 || bit==VarbitID.RUNE_POUCH_QUANTITY_3 || bit==VarbitID.RUNE_POUCH_QUANTITY_4 || GearContainers.isBoltVarbit(bit))gearDirty=true;
    }
    @Subscribe public void onClientTick(ClientTick event)
    {
        if(!active || !gearDirty || client.getGameState()!=GameState.LOGGED_IN)return;
        gearDirty=false;String owner=recoveryOwner();long epoch=gearEpoch;
        capture(snapshot->{if(!active||!owner.equals(recoveryOwner())||epoch!=gearEpoch)return;if(!snapshot.equals(lastGear)){lastGear=snapshot.deepCopy();if(gearObserver!=null)gearObserver.accept(snapshot);}},error->{});
    }
    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if(active && LoadoutStudioConfig.GROUP.equals(event.getGroup()) && LoadoutStudioConfig.isPreferenceKey(event.getKey())){SwingUtilities.invokeLater(()->{if(active&&panel!=null)panel.preferencesChanged();});return;}
        if(!active || bankFilter==null || !"banktags".equals(event.getGroup()) || coreBankTags()==null || coreBankTags().getActiveTag()==null || !event.getKey().equals("layout_"+coreBankTags().getActiveTag()) || event.getNewValue()==null)return;
        long revision=bankRevision.get();Consumer<JsonArray> observer=layoutObserver;
        clientThread.invokeLater(()->{if(!active||revision!=bankRevision.get()||bankFilter==null)return;JsonArray layout=bankFilter.readChangedLayout();if(layout!=null && observer!=null)SwingUtilities.invokeLater(()->{if(active&&revision==bankRevision.get())observer.accept(layout);});});
    }
    private void clearBankObservation(){clientThread.invoke(()->{observedBank.clear();bankObservedAt=0;});}
    private void observeBank()
    {
        net.runelite.api.widgets.Widget widget=client.getWidget(InterfaceID.Bankmain.ITEMS);ItemContainer bank=client.getItemContainer(InventoryID.BANK);
        if(client.getGameState()!=GameState.LOGGED_IN || widget==null || widget.isHidden() || bank==null)return;
        boolean firstObservation=bankObservedAt==0;java.util.Map<Integer,Integer> before=new java.util.LinkedHashMap<>(observedBank);observedBank.clear();for(Item item:bank.getItems())if(item.getId()>0 && item.getQuantity()>0 && itemManager.getItemComposition(item.getId()).getPlaceholderTemplateId()==-1)observedBank.merge(itemManager.canonicalize(item.getId()),item.getQuantity(),(a,b)->(int)Math.min(Integer.MAX_VALUE,(long)a+b));bankObservedAt=System.currentTimeMillis();if((firstObservation||!before.equals(observedBank))&&bankObserver!=null){String owner=recoveryOwner();SwingUtilities.invokeLater(()->{if(active&&owner.equals(recoveryOwner())&&bankObserver!=null)bankObserver.run();});}
    }
    @Override public void availableGear(JsonObject expected,Consumer<OwnedGear> success,Consumer<String> error)
    {
        String owner=recoveryOwner();clientThread.invoke(()->{
            if(!active||!owner.equals(recoveryOwner()))return;if(client.getGameState()!=GameState.LOGGED_IN){SwingUtilities.invokeLater(()->error.accept("Log in and open your Bank to check owned alternatives."));return;}
            observeBank();java.util.Map<Integer,Integer> amounts=new java.util.LinkedHashMap<>(observedBank);java.util.Map<Integer,StudioPanel.ItemInfo> details=new java.util.LinkedHashMap<>();
            for(int container:new int[]{InventoryID.INV,InventoryID.WORN}){ItemContainer items=client.getItemContainer(container);if(items!=null)for(Item item:items.getItems())if(item.getId()>0&&item.getQuantity()>0)amounts.merge(itemManager.canonicalize(item.getId()),item.getQuantity(),(a,b)->(int)Math.min(Integer.MAX_VALUE,(long)a+b));}
            java.util.Set<Integer> ids=new java.util.LinkedHashSet<>(amounts.keySet());for(com.google.gson.JsonElement e:BankLayouts.items(expected))if(SetupCodec.id(e)>0)ids.add(SetupCodec.id(e));
            for(int id:ids){ItemComposition item=itemManager.getItemComposition(id);ItemStats stats=itemManager.getItemStats(id);details.put(id,new StudioPanel.ItemInfo(id,item.getMembersName(),item.isStackable(),stats!=null&&stats.getEquipment()!=null?stats.getEquipment().getSlot():-1,null));}
            net.runelite.api.widgets.Widget widget=client.getWidget(InterfaceID.Bankmain.ITEMS);OwnedGear result=new OwnedGear(amounts,details,bankObservedAt,widget!=null&&!widget.isHidden());SwingUtilities.invokeLater(()->{if(active&&owner.equals(recoveryOwner()))success.accept(result);});
        });
    }

	@Subscribe public void onGameStateChanged(GameStateChanged event) { if (event.getGameState() == GameState.LOGGED_IN) {indexItems();indexGearVarps();gearDirty=true;} else if (event.getGameState() == GameState.LOGIN_SCREEN) {clearBankFilter();gearEpoch++;lastGear=null;clearBankObservation();String owner=recoveryOwner();SwingUtilities.invokeLater(()->{if(active&&owner.equals(recoveryOwner())&&gearObserver!=null)gearObserver.accept(null);});} }
	@Subscribe(priority = -1) public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_INIT)
		{
			observeBank();
			long revision = bankRevision.get(); clientThread.invokeLater(() -> { if (active && bankFilter != null && revision == bankRevision.get()) try { reportBankStatus(bankFilter.apply()); } catch (RuntimeException ex) { bankFilter.clear(); reportBankStatus(ex.getMessage()); } });
		}
	}
	@Subscribe(priority = -1) public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING && bankFilter != null)
		{
			long revision = bankRevision.get(); clientThread.invokeLater(() -> { if (active && bankFilter != null && revision == bankRevision.get()) try { if(bankFilter.waiting()) reportBankStatus(bankFilter.apply()); bankFilter.refreshTitle();if(independentBank!=null&&independentBank.active())independentBank.arrange(); } catch (RuntimeException ex) { bankFilter.clear(); reportBankStatus(ex.getMessage()); } });
		}
	}
	private void reportBankStatus(String message) { long revision = bankRevision.get(); SwingUtilities.invokeLater(() -> { if (active && panel != null && revision == bankRevision.get()) panel.bankMessage(message); }); }
	@Subscribe public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) { reload(); }
	@Subscribe public void onProfileChanged(ProfileChanged event) { reload(); }
	private void reload() { importRevision.incrementAndGet(); clearBankFilter();gearEpoch++;lastGear=null;clearBankObservation();gearDirty=true; SwingUtilities.invokeLater(() -> { if (active && panel != null) panel.reloadScope(); }); }
	@Override public void clearBankFilter()
	{
		long revision = bankRevision.incrementAndGet(); clientThread.invoke(() -> { if (bankFilter != null && revision == bankRevision.get()) bankFilter.clear(); });
	}
	@Override public void updateBankFilter(JsonObject portable, String scope, Consumer<String> callback)
	{
		updateBankFilter(portable,scope,callback,null);
	}
    @Override public void updateBankFilter(JsonObject portable,String scope,Consumer<String> callback,Consumer<JsonArray> layoutChanged)
	{
		JsonObject snapshot = portable.deepCopy(); long revision = bankRevision.incrementAndGet();
		clientThread.invoke(() ->
		{
			if (!active || bankFilter == null || revision != bankRevision.get() || !java.util.Objects.equals(scope, scope())) return;
			layoutObserver=layoutChanged;
			String result; try { result = bankFilter.select(snapshot); } catch (RuntimeException ex) { bankFilter.clear(); result = ex.getMessage(); }
			String message = result; SwingUtilities.invokeLater(() -> { if (active && revision == bankRevision.get()) callback.accept(message); });
		});
	}
    @Override public void chooseItem(Consumer<Integer> chosen,Consumer<String> error)
    {
        clientThread.invoke(()->{ if(!active) return; if(client.getGameState()!=GameState.LOGGED_IN) { SwingUtilities.invokeLater(()->error.accept("Log in to use the RuneLite chatbox Item Search.")); return; }
            net.runelite.client.game.chatbox.ChatboxItemSearch input=chatboxSearch.get(); ownedChatbox=input; input.value("");input.tooltipText("Set slot to").onItemSelected(id->{int selected=ItemCatalogue.selectionId(id,itemManager.getItemComposition(id).getMembersName());SwingUtilities.invokeLater(()->{if(active)chosen.accept(selected);});}).build();SwingUtilities.invokeLater(()->client.getCanvas().requestFocusInWindow());
        });
    }
    @Override public void enterQuantity(String initial,Consumer<String> chosen,Consumer<String> error)
    {
        clientThread.invoke(()->{ if(!active) return; if(client.getGameState()!=GameState.LOGGED_IN) { SwingUtilities.invokeLater(()->error.accept("Log in to enter a quantity in the RuneLite chatbox.")); return; }
            net.runelite.client.game.chatbox.ChatboxTextInput input=chatbox.openTextInput("Enter amount (16k = 16000; 0 = unlimited)").value(initial).onDone((Consumer<String>)(value->SwingUtilities.invokeLater(()->{if(active)chosen.accept(value);}))); ownedChatbox=input;input.build();SwingUtilities.invokeLater(()->client.getCanvas().requestFocusInWindow());
        });
    }
    @Override public void cancelItemInput() { clientThread.invoke(()->{if(ownedChatbox!=null && chatbox.getCurrentInput()==ownedChatbox) chatbox.close(); ownedChatbox=null;}); }
	@Override public void openSupportPage() { net.runelite.client.util.LinkBrowser.browse(StudioPanel.COFFEE_URL); }
	@Override public String scope() { return configManager.getRSProfileKey(); }
	@Override public String read(String scope) { return configManager.getConfiguration(LoadoutStudioConfig.GROUP, scope, "library-v1"); }
	@Override public void save(String scope, String text)
	{
		if (!java.util.Objects.equals(scope, scope())) throw new IllegalStateException("Profile changed. Reopen the setup before saving.");
		configManager.setConfiguration(LoadoutStudioConfig.GROUP, scope, "library-v1", text);
	}
	@Override public void fetchWiki(String url, java.util.function.BiConsumer<String, String> callback)
	{
		wiki.fetch(url, (text, error) -> SwingUtilities.invokeLater(() -> { if (active) callback.accept(text, error); }));
	}
	@Override public void fetchWikiPage(String url, boolean preferStrategies, java.util.function.BiConsumer<WikiClient.Page, String> callback)
	{
		wiki.fetchPage(url, preferStrategies, (page, error) -> SwingUtilities.invokeLater(() -> { if (active) callback.accept(page, error); }));
	}
    @Override public String recoveryOwner() { return String.valueOf(configManager.getProfile()==null?"guest":configManager.getProfile().getId())+":"+scope(); }
    @Override public String readRecovery(String expectedScope,String owner) { return owner.equals(recoveryOwner())?configManager.getConfiguration(LoadoutStudioConfig.GROUP,expectedScope,"draft-v1"):null; }
    @Override public void saveRecovery(String expectedScope,String owner,String text)
    {
        if(!owner.equals(recoveryOwner()) || !java.util.Objects.equals(expectedScope,scope()))return;
        if(text==null)configManager.unsetConfiguration(LoadoutStudioConfig.GROUP,expectedScope,"draft-v1");
        else configManager.setConfiguration(LoadoutStudioConfig.GROUP,expectedScope,"draft-v1",text);
    }
    @Override public void compareSetup(JsonObject expected,boolean ignoreAmmo,Consumer<java.util.Set<String>> success,Consumer<String> error)
    {
        capture(actual->clientThread.invoke(()->{if(!active)return;java.util.Set<String> result=SetupComparison.differences(expected,actual,ignoreAmmo,id->SetupVariationMapping.map(itemManager.canonicalize(id)));SwingUtilities.invokeLater(()->{if(active)success.accept(result);});}),error);
    }
    @Override public void existingSetups(Consumer<ExistingSetupReader.Result> success,Consumer<String> error)
    {
        String owner=recoveryOwner();long revision=importRevision.incrementAndGet();
        if(importWorker==null || importWorker.isShutdown())return;
        importWorker.execute(()->
        {
            try
            {
                ExistingSetupReader.Result result=new ExistingSetupReader(gson,new ExistingSetupReader.Source()
                {
                    public List<String> keys(String group,String prefix)
                    {
                        String whole=ConfigManager.getWholeKey(group,null,prefix);List<String> keys=new java.util.ArrayList<>();
                        for(String key:configManager.getConfigurationKeys(whole))keys.add(key.substring(whole.length()-prefix.length()));return keys;
                    }
                    public String get(String group,String key){return configManager.getConfiguration(group,key);}
                }).read();
                SwingUtilities.invokeLater(()->{if(active && revision==importRevision.get() && owner.equals(recoveryOwner()))success.accept(result);});
            }
            catch(RuntimeException ex){SwingUtilities.invokeLater(()->{if(active && revision==importRevision.get() && owner.equals(recoveryOwner()))error.accept("Existing setups could not be read. Their original data was not changed; portable JSON import is still available.");});}
        });
    }
    @Override public void fetchWikiPage(String url,boolean preferStrategies,boolean refresh,java.util.function.BiConsumer<WikiClient.Page,String> callback)
    {
        wiki.fetchPage(url,preferStrategies,refresh,(page,error)->SwingUtilities.invokeLater(()->{if(active)callback.accept(page,error);}));
    }
    @Override public String wikiCacheWarning(){return wiki.cacheWarning();}
    @Override public void localWikiSuggestions(String query,Consumer<List<String>> callback){wiki.suggestLocal(query,titles->SwingUtilities.invokeLater(()->{if(active)callback.accept(titles);}));}
	@Override public List<StudioPanel.ItemInfo> search(String query) { return catalogue.search(query); }
    @Override public boolean itemMatches(int id,String query) { return catalogue.matches(id,query); }
	@Override public StudioPanel.ItemInfo exactItem(String name) { return catalogue.exact(name); }
	@Override public StudioPanel.ItemInfo defaultItem(String name) { return catalogue.firstExact(name); }
	@Override public void whenItemsReady(Consumer<String> callback)
	{
		clientThread.invokeLater(() ->
		{
			if (!active) return true;
			if (client.getItemCount() < 1)
			{
				SwingUtilities.invokeLater(() -> { if (active) callback.accept("Game items are not available yet. Wait for the login screen, then choose the setup again."); });
				return true;
			}
			indexItems();
			if (indexing) return false;
			SwingUtilities.invokeLater(() -> { if (active) callback.accept(null); });
			return true;
		});
	}
	@Override public void searchWiki(String query, java.util.function.BiConsumer<List<String>, String> callback)
	{
		wiki.search(query, (titles, error) -> SwingUtilities.invokeLater(() -> { if (active) callback.accept(titles, error); }));
	}
	@Override public String catalogueStatus() { return catalogue.size() + " cached items" + (indexing ? " · indexing…" : catalogue.isEmpty() ? " · log in to load game items" : ""); }
	private void indexItems()
	{
		if (!active || indexing || client.getItemCount() < 1 || nextItem >= client.getItemCount()) return; indexing = true;
		clientThread.invokeLater(() ->
		{
			if (!active) { indexing = false; return true; }
			int limit = Math.min(client.getItemCount(), nextItem + 150);
			for (; nextItem < limit; nextItem++)
			{
				ItemComposition composition = itemManager.getItemComposition(nextItem);
				ItemStats stats=itemManager.getItemStats(nextItem);int slot=stats!=null&&stats.getEquipment()!=null?stats.getEquipment().getSlot():-1;
                catalogue.add(nextItem,composition.getMembersName(),composition.isStackable(),composition.getNote(),composition.getPlaceholderTemplateId(),slot,composition.isGeTradeable(),composition.getInventoryActions()!=null&&java.util.Arrays.stream(composition.getInventoryActions()).anyMatch(java.util.Objects::nonNull));
			}
			if (nextItem < client.getItemCount()) return false; indexing = false; return true;
		});
	}
	@Override public void describe(int id, Consumer<StudioPanel.ItemInfo> callback)
	{
		clientThread.invoke(() ->
		{
			if (!active) return; StudioPanel.ItemInfo result;
			try
			{
				if (id < 1 || id >= client.getItemCount()) throw new IllegalArgumentException();
				ItemComposition composition = itemManager.getItemComposition(id); ItemStats stats = itemManager.getItemStats(id); int slot = stats != null && stats.getEquipment() != null ? stats.getEquipment().getSlot() : -1;
				result = new StudioPanel.ItemInfo(id, composition.getMembersName(), composition.isStackable(), slot, new ImageIcon(itemManager.getImage(id)));
			}
			catch (RuntimeException ex) { result = new StudioPanel.ItemInfo(id, "Item " + id, true, -1, null); }
			StudioPanel.ItemInfo item = result; SwingUtilities.invokeLater(() -> { if (active) callback.accept(item); });
		});
	}
	@Override public void capture(Consumer<JsonObject> callback, Consumer<String> error)
	{
		clientThread.invoke(() ->
		{
			if (!active) return;
			if (client.getGameState() != GameState.LOGGED_IN) { SwingUtilities.invokeLater(() -> error.accept("Log in before capturing gear.")); return; }
			JsonObject p = SetupCodec.blank("Current gear"), s = SetupCodec.setup(p); s.add("inv", snapshot(client.getItemContainer(InventoryID.INV), 28)); s.add("eq", snapshot(client.getItemContainer(InventoryID.WORN), 14));
			boolean pouch = false, divine = false, quiver = false;
			for (String key : new String[]{"inv", "eq"}) for (com.google.gson.JsonElement e : s.getAsJsonArray(key)) if (SetupCodec.id(e) > 0)
			{
				String name = itemManager.getItemComposition(SetupCodec.id(e)).getName().toLowerCase(Locale.ROOT); pouch |= name.contains("rune pouch"); divine |= name.contains("divine rune pouch"); quiver |= name.contains("dizana") && (name.contains("quiver") || name.contains("max cape"));
			}
			if (pouch)
			{
				int[] types = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4};
				int[] amounts = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4};
				// Rune pouch enum 982 is the cache's rune-type-to-item mapping (no gameval constant is provided).
				for (int i = 0; i < (divine ? 4 : 3); i++) { int type = client.getVarbitValue(types[i]), amount = client.getVarbitValue(amounts[i]); if (type > 0 && amount > 0) s.getAsJsonArray("rp").set(i, SetupCodec.item(client.getEnum(982).getIntValue(type), amount)); }
			}
			if (quiver) { int ammo = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO), amount = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT); if (ammo > 0 && amount > 0) s.getAsJsonArray("qv").set(0, SetupCodec.item(ammo, amount)); }
			s.addProperty("sb", client.getVarbitValue(VarbitID.SPELLBOOK)); GearContainers.capture(client,s); SetupCodec.rebuildLayout(p);
			SwingUtilities.invokeLater(() -> { if (active) callback.accept(p); });
		});
	}
	private static JsonArray snapshot(ItemContainer container, int size)
	{
		JsonArray result = SetupCodec.empty(size); if (container == null) return result; Item[] items = container.getItems();
		for (int i = 0; i < Math.min(size, items.length); i++) if (items[i].getId() > 0 && items[i].getQuantity() > 0) result.set(i, SetupCodec.item(items[i].getId(), items[i].getQuantity())); return result;
	}
}
