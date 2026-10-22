/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
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
@PluginDependency(BankTagsPlugin.class)
public class LoadoutStudioPlugin extends Plugin implements StudioPanel.Host
{
	@Inject private net.runelite.api.Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar toolbar;
	@Inject private ItemManager itemManager;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private OkHttpClient http;
	@Inject private LoadoutStudioConfig config;
	@Inject private BankTagsService bankTags;
	@Inject private TagManager tagManager;
	@Inject private LayoutManager layoutManager;
	private StudioBankFilter bankFilter;
	private final java.util.concurrent.atomic.AtomicLong bankRevision = new java.util.concurrent.atomic.AtomicLong();
	private NavigationButton navigation;
	private StudioPanel panel;
	private WikiClient wiki;
	private volatile boolean active;
	private boolean indexing;
	private int nextItem;
	private final Map<Integer, StudioPanel.ItemInfo> catalogue = new ConcurrentHashMap<>();
	@Provides LoadoutStudioConfig provideConfig(ConfigManager manager) { return manager.getConfig(LoadoutStudioConfig.class); }

	@Override protected void startUp()
	{
		active = true; wiki = new WikiClient(http, gson);
		bankFilter = new StudioBankFilter(new StudioBankFilter.Gateway()
		{
			public boolean bankOpen() { net.runelite.api.widgets.Widget w = client.getWidget(InterfaceID.Bankmain.ITEMS); return client.getGameState() == GameState.LOGGED_IN && w != null && !w.isHidden() && w.getChildren() != null && w.getChildren().length > 0; }
			public int capacity() { net.runelite.api.widgets.Widget w = client.getWidget(InterfaceID.Bankmain.ITEMS); return w == null || w.getChildren() == null ? 0 : w.getChildren().length; }
			public String activeTag() { return bankTags.getActiveTag(); }
			public int canonicalize(int id) { return itemManager.canonicalize(id); }
			public int variation(int id) { return ItemVariationMapping.map(id); }
			public void register(String tag, java.util.function.IntPredicate matches) { tagManager.registerTag(tag, matches::test); }
			public void open(String tag, int[] layout)
			{
				if (!(bankTags instanceof BankTagsPlugin)) throw new IllegalStateException("The installed Bank Tags service does not support layout previews.");
				((BankTagsPlugin) bankTags).openTag(tag, new Layout(tag, layout), BankTagsService.OPTION_HIDE_TAG_NAME | BankTagsService.OPTION_ITEMS_NOT_IN_LAYOUT_AT_BOTTOM);
			}
			public void close() { bankTags.closeBankTag(); }
			public void unregister(String tag) { tagManager.unregisterTag(tag); }
			public void discardLayout(String tag) { layoutManager.removeLayout(tag); }
		}, "loadoutstudio_" + java.util.UUID.randomUUID().toString().replace("-", ""));
		SwingUtilities.invokeLater(() ->
		{
			if (!active) return;
			panel = new StudioPanel(this, gson); navigation = NavigationButton.builder().tooltip("Loadout Studio (Test)").icon(icon()).panel(panel).priority(7).build(); toolbar.addNavigation(navigation);
		});
		clientThread.invokeLater(this::indexItems);
	}
	@Override protected void shutDown()
	{
		active = false; if (wiki != null) wiki.close();
		bankRevision.incrementAndGet(); StudioBankFilter previousFilter = bankFilter; bankFilter = null; clientThread.invoke(() -> { if (previousFilter != null) previousFilter.clear(); });
		SwingUtilities.invokeLater(() -> { if (navigation != null) toolbar.removeNavigation(navigation); if (panel != null) panel.dispose(); panel = null; navigation = null; });
		catalogue.clear(); indexing = false; nextItem = 0;
	}
	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB); Graphics2D g = image.createGraphics(); g.setColor(new Color(195, 163, 82)); g.fillRoundRect(1, 1, 30, 30, 5, 5); g.setColor(new Color(39, 38, 32)); for (int y = 7; y < 28; y += 7) for (int x = 6; x < 28; x += 7) g.fillRect(x, y, 5, 5); g.dispose(); return image;
	}
	@Subscribe public void onGameStateChanged(GameStateChanged event) { if (event.getGameState() == GameState.LOGGED_IN) indexItems(); else if (event.getGameState() == GameState.LOGIN_SCREEN) clearBankFilter(); }
	@Subscribe(priority = -1) public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_INIT)
		{
			long revision = bankRevision.get(); clientThread.invokeLater(() -> { if (active && bankFilter != null && revision == bankRevision.get()) try { reportBankStatus(bankFilter.apply()); } catch (RuntimeException ex) { bankFilter.clear(); reportBankStatus(ex.getMessage()); } });
		}
	}
	@Subscribe(priority = -1) public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING && bankFilter != null && bankFilter.waiting())
		{
			long revision = bankRevision.get(); clientThread.invokeLater(() -> { if (active && bankFilter != null && bankFilter.waiting() && revision == bankRevision.get()) try { reportBankStatus(bankFilter.apply()); } catch (RuntimeException ex) { bankFilter.clear(); reportBankStatus(ex.getMessage()); } });
		}
	}
	private void reportBankStatus(String message) { long revision = bankRevision.get(); SwingUtilities.invokeLater(() -> { if (active && panel != null && revision == bankRevision.get()) panel.bankMessage(message); }); }
	@Subscribe public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) { reload(); }
	@Subscribe public void onProfileChanged(ProfileChanged event) { reload(); }
	private void reload() { clearBankFilter(); SwingUtilities.invokeLater(() -> { if (active && panel != null) panel.reloadScope(); }); }
	@Override public void clearBankFilter()
	{
		long revision = bankRevision.incrementAndGet(); clientThread.invoke(() -> { if (bankFilter != null && revision == bankRevision.get()) bankFilter.clear(); });
	}
	@Override public void updateBankFilter(JsonObject portable, String scope, Consumer<String> callback)
	{
		JsonObject snapshot = portable.deepCopy(); long revision = bankRevision.incrementAndGet();
		clientThread.invoke(() ->
		{
			if (!active || bankFilter == null || revision != bankRevision.get() || !java.util.Objects.equals(scope, scope())) return;
			String result; try { result = bankFilter.select(snapshot); } catch (RuntimeException ex) { bankFilter.clear(); result = ex.getMessage(); }
			String message = result; SwingUtilities.invokeLater(() -> { if (active && revision == bankRevision.get()) callback.accept(message); });
		});
	}
	@Override public String scope() { return configManager.getRSProfileKey(); }
	@Override public String read(String scope) { return configManager.getConfiguration(LoadoutStudioConfig.GROUP, scope, "library-v1"); }
	@Override public void save(String scope, String text)
	{
		if (!java.util.Objects.equals(scope, scope())) throw new IllegalStateException("Profile changed. Reopen the setup before saving.");
		configManager.setConfiguration(LoadoutStudioConfig.GROUP, scope, "library-v1", text);
	}
	@Override public boolean wikiEnabled() { return config.wikiRequests(); }
	@Override public void enableWiki(boolean enabled) { configManager.setConfiguration(LoadoutStudioConfig.GROUP, "wikiRequests", enabled); }
	@Override public void fetchWiki(String url, java.util.function.BiConsumer<String, String> callback)
	{
		if (!wikiEnabled()) { callback.accept(null, "Enable OSRS Wiki requests first."); return; }
		wiki.fetch(url, (text, error) -> SwingUtilities.invokeLater(() -> { if (active) callback.accept(text, error); }));
	}
	@Override public List<StudioPanel.ItemInfo> search(String query)
	{
		String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		List<StudioPanel.ItemInfo> result = new ArrayList<>();
		for (StudioPanel.ItemInfo item : catalogue.values())
		{
			boolean match = true; for (String word : words) if (!item.name.toLowerCase(Locale.ROOT).contains(word) && !String.valueOf(item.id).equals(word)) match = false;
			if (match) result.add(item);
		}
		result.sort(java.util.Comparator.comparing((StudioPanel.ItemInfo i) -> i.name).thenComparingInt(i -> i.id));
		return result.subList(0, Math.min(100, result.size()));
	}
	@Override public void searchWiki(String query, java.util.function.BiConsumer<List<String>, String> callback)
	{
		if (!wikiEnabled()) { callback.accept(null, "Enable OSRS Wiki requests first."); return; }
		wiki.search(query, (titles, error) -> SwingUtilities.invokeLater(() -> { if (active) callback.accept(titles, error); }));
	}
	@Override public String catalogueStatus() { return catalogue.size() + " cached items" + (indexing ? " · indexing…" : catalogue.isEmpty() ? " · log in to load game items" : ""); }
	private void indexItems()
	{
		if (!active || indexing || client.getItemCount() < 1) return; indexing = true;
		clientThread.invokeLater(() ->
		{
			if (!active) { indexing = false; return true; }
			int limit = Math.min(client.getItemCount(), nextItem + 150);
			for (; nextItem < limit; nextItem++)
			{
				ItemComposition composition = itemManager.getItemComposition(nextItem); String name = composition.getName();
				if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("null") && composition.getPlaceholderTemplateId() == -1) catalogue.put(nextItem, new StudioPanel.ItemInfo(nextItem, name, composition.isStackable(), -1, null));
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
				result = new StudioPanel.ItemInfo(id, composition.getName(), composition.isStackable(), slot, new ImageIcon(itemManager.getImage(id)));
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
			s.addProperty("sb", client.getVarbitValue(VarbitID.SPELLBOOK)); SetupCodec.rebuildLayout(p);
			SwingUtilities.invokeLater(() -> { if (active) callback.accept(p); });
		});
	}
	private static JsonArray snapshot(ItemContainer container, int size)
	{
		JsonArray result = SetupCodec.empty(size); if (container == null) return result; Item[] items = container.getItems();
		for (int i = 0; i < Math.min(size, items.length); i++) if (items[i].getId() > 0 && items[i].getQuantity() > 0) result.set(i, SetupCodec.item(items[i].getId(), items[i].getQuantity())); return result;
	}
}
