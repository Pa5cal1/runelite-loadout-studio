/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public final class StudioPanel extends PluginPanel
{
    static final String COFFEE_URL = "https://buymeacoffee.com/pascalvdbe9";
    static final class QuickLink{final String id,name;QuickLink(String id,String name){this.id=id;this.name=name;}}
	public interface Host
	{
		String scope();
        default LoadoutStudioConfig settings(){return new LoadoutStudioConfig(){};}
        default void libraryOrder(LoadoutStudioConfig.LibraryOrder order){}
        default void quickLinks(List<QuickLink> links) { }
        default void observeGear(Consumer<JsonObject> observer) { }
        default void requestGear() { }
        default void observeBank(Runnable observer) { }
        default boolean itemMatches(int id,String query) { for(ItemInfo item:search(query))if(item.id==id)return true;return false; }
        default void availableGear(JsonObject expected,Consumer<OwnedGear> success,Consumer<String> error) { success.accept(new OwnedGear(java.util.Collections.emptyMap(),java.util.Collections.emptyMap(),0,false)); }
        default void updateBankFilter(JsonObject p,String scope,Consumer<String> status,Consumer<JsonArray> layout){updateBankFilter(p,scope,status);}
        default String recoveryOwner(){return String.valueOf(scope());}
        default String readRecovery(String scope,String owner){return null;}
        default void saveRecovery(String scope,String owner,String text){ }
        default void existingSetups(Consumer<ExistingSetupReader.Result> success,Consumer<String> error){success.accept(new ExistingSetupReader.Result());}
        default void compareSetup(JsonObject expected,boolean ignoreAmmo,Consumer<Set<String>> success,Consumer<String> error){capture(actual->success.accept(SetupComparison.differences(expected,actual,ignoreAmmo,id->id)),error);}
        default void fetchWikiPage(String url,boolean strategies,boolean refresh,BiConsumer<WikiClient.Page,String> callback){fetchWikiPage(url,strategies,callback);}
        default String wikiCacheWarning(){return "";}
        default void localWikiSuggestions(String query,Consumer<List<String>> callback){callback.accept(java.util.Collections.emptyList());}
        default void openSupportPage() { }
        default void chooseItem(Consumer<Integer> chosen, Consumer<String> error) { error.accept("Use the running RuneLite client for chatbox Item Search."); }
        default void enterQuantity(String initial, Consumer<String> chosen, Consumer<String> error) { error.accept("Use the running RuneLite client for chatbox quantity input."); }
        default void cancelItemInput() { }
		String read(String scope);
		void save(String scope, String text);
		void capture(Consumer<JsonObject> success, Consumer<String> error);
		void describe(int id, Consumer<ItemInfo> callback);
		List<ItemInfo> search(String query);
        default ItemInfo exactItem(String name)
        {
            ItemInfo match = null;
            for (ItemInfo item : search(name)) if (item.name.equalsIgnoreCase(name.trim())) { if (match != null) return null; match = item; }
            return match;
        }
        default ItemInfo defaultItem(String name)
        {
            ItemInfo exact=exactItem(name);if(exact!=null)return exact;
            ItemInfo first=null;for(ItemInfo item:search(name))if(item.name.equalsIgnoreCase(name.trim())&&(first==null||item.id<first.id))first=item;
            return first;
        }
        default void whenItemsReady(Consumer<String> callback) { callback.accept(null); }
		String catalogueStatus();
		void fetchWiki(String url, BiConsumer<String, String> callback);
		default void fetchWikiPage(String url, boolean preferStrategies, BiConsumer<WikiClient.Page, String> callback)
		{
			fetchWiki(url, (text, error) -> callback.accept(error == null ? new WikiClient.Page(WikiClient.title(url), text) : null, error));
		}
		void searchWiki(String query, BiConsumer<List<String>, String> callback);
		void updateBankFilter(JsonObject portable, String scope, Consumer<String> callback);
		void clearBankFilter();
	}
	public static final class ItemInfo
	{
		public final int id;
		public final String name;
		public final boolean stackable;
		public final int slot;
		public final Icon icon;
		public ItemInfo(int id, String name, boolean stackable, int slot, Icon icon) { this.id = id; this.name = name == null ? "Item " + id : name; this.stackable = stackable; this.slot = slot; this.icon = icon; }
		@Override public String toString() { return name + "  [" + id + "]"; }
	}
	private static final Color GOLD = new Color(225, 166, 58), BG = new Color(37, 37, 37), CONTROL = new Color(29, 29, 29), OUTLINE = new Color(64, 64, 64), TEXT = new Color(224, 224, 224), MUTED = new Color(172, 172, 172), SLOT = new Color(29, 29, 29), RED = new Color(195, 92, 72);
	private static final Font BODY_FONT = FontManager.getRunescapeFont(), ACTION_FONT = FontManager.getRunescapeBoldFont();
	private JTextField wikiQuery;
	private final List<JButton> navigation = new ArrayList<>();
	private final Host host;
	private final Gson gson;
	private final SetupCodec codec;
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel libraryRows = vertical(), builder = vertical(), builderTools = vertical();
	private final Set<String> expandedPanels = new HashSet<>();
	private final JTextField search = new JTextField(), setupName = new JTextField(){@Override public javax.swing.JToolTip createToolTip(){javax.swing.JToolTip tip=super.createToolTip();tip.putClientProperty("html.disable",true);return tip;}};
	private final JCheckBox favorites = new JCheckBox("Favorites"), archived = new JCheckBox("Archived"), favorite = new JCheckBox("Favorite"), ignoreAmmo = new JCheckBox("Ignore Ammo quantities");
	private final JComboBox<String> spellbook = new JComboBox<>(new String[]{"Standard", "Ancient", "Lunar", "Arceuus", "None"});
	private final JCheckBox filterBank = new JCheckBox("Filter Bank"), bottomToTop = new JCheckBox("Zigzag: bottom row first");
	private final JComboBox<BankLayouts.Mode> bankLayout = new JComboBox<>(BankLayouts.Mode.values());
	private final JTextArea bankStatus = new JTextArea(3, 18);
	private BankLayouts.Mode editorLayout = BankLayouts.Mode.PRESET;
	private boolean editorBottom;
	private final JComboBox<String> section = new JComboBox<>(), format = new JComboBox<>(new String[]{"Inventory Setup JSON", "Bank Tag (RuneLite)", "Bank Tag Layouts"});
	private final JTextArea notes = new GrowingNotesArea(), code = new JTextArea(9, 18), status = new JTextArea(3, 18);
	private JButton exportCopy, exportPreview;
	private final JLabel scopeLabel = text(""), draftLabel = text("No setup open");
    private final JLabel gearReadiness=text("Gear not checked");
    private JsonObject liveActual,alternativeUndo;
    private final JPanel alternativeRows=vertical();
    private JPanel alternativesContent;private String autoShownAlternativeSession;
    private LoadoutStudioConfig.LibraryOrder libraryOrder=LoadoutStudioConfig.LibraryOrder.ALPHABETICAL;
    private JButton coffeeControl;
    private String upgradeBefore,upgradeAfter;
    private long alternativeUndoRevision;
	private final Map<Integer, ItemInfo> itemCache = new HashMap<>();
    private final JTextField catalogueQuery = new JTextField();
    private final DefaultListModel<ItemInfo> catalogueModel = new DefaultListModel<>();
    private final JList<ItemInfo> catalogueList = new JList<>(catalogueModel);
    private final Timer alternativeTimer = new Timer(200,e->refreshAlternatives());
    private final Timer catalogueTimer = new Timer(180, e -> refreshCatalogue());
    private final JLabel catalogueCount = text("Search English item names or IDs");
    private String renderedSession;

	private final Set<String> collapsed = new HashSet<>(), differences = new HashSet<>();
	private final List<String> sectionIds = new ArrayList<>();
	private SetupLibrary library = new SetupLibrary();
	private JsonObject draft;
	private String recoveryOwner;
    private JsonObject recovery;
    private final JPanel recoveryControls=vertical(), existingControls=vertical();
    private final JTextArea conversionNotice=info("",3);
    private String editingId, scope, session = UUID.randomUUID().toString();
	private String editorSection;
	private boolean editorFavorite;
	private boolean dirty, rendering, disposed, storageError;
	private long draftRevision;
	private Timer wikiSuggestTimer, wikiPageTimer;
	private long wikiSearchRevision, wikiPageRevision, wikiChoiceRevision;
	private final JPanel wikiPresetRows = vertical();
	private final JTextArea wikiPageStatus = new CompactStatusArea("Choose a Wiki page to see its setups.");
    private final JTextArea libraryFeedback = new CompactStatusArea(""), builderFeedback = new CompactStatusArea(""), exportFeedback = new CompactStatusArea("");

	public StudioPanel(Host host, Gson gson)
	{
		super(false); this.host = host; this.gson = gson; codec = new SetupCodec(gson);alternativeTimer.setRepeats(false);builderFeedback.setVisible(false);
		setLayout(new BorderLayout(0, 6)); setBackground(BG); setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6)); setPreferredSize(new Dimension(240, 900));
		tabs.setFocusable(false); tabs.setUI(new BasicTabbedPaneUI()
		{
			@Override protected int calculateTabAreaHeight(int placement, int runs, int height) { return 0; }
			@Override protected void paintTabArea(java.awt.Graphics g, int placement, int selected) { }
			@Override protected void paintContentBorder(java.awt.Graphics g, int placement, int selected) { }
			@Override protected Insets getContentBorderInsets(int placement) { return new Insets(0, 0, 0, 0); }
		});
		tabs.addTab("Library", libraryTab()); tabs.addTab("Builder", builderTab()); tabs.addTab("Wiki", scroll(wikiTab())); tabs.addTab("Export", exportTab()); add(tabs, BorderLayout.CENTER);
		JPanel heading = vertical(); JLabel title = text("Loadout Studio"); title.setName("studioTitle"); title.setToolTipText("Loadout Studio 0.8.2-test - character-specific setup library");
        JButton coffee = button("Buy a coffee", () -> attempt(host::openSupportPage)); coffeeControl=coffee;coffee.setIcon(new SidebarIcon(SidebarIcon.Kind.COFFEE)); coffee.setName("supportCoffee"); coffee.setToolTipText("Buy a coffee"); coffee.getAccessibleContext().setAccessibleName("Buy a coffee"); coffee.getAccessibleContext().setAccessibleDescription(COFFEE_URL); coffee.setPreferredSize(new Dimension(94, 28));
        JPanel titleBar = new JPanel(new BorderLayout(3, 0)); titleBar.add(title, BorderLayout.CENTER); titleBar.add(coffee, BorderLayout.EAST); heading.add(titleBar);
		JPanel menu = new JPanel(new GridLayout(2, 2, 4, 4));
		for (int i = 0; i < tabs.getTabCount(); i++)
		{
			final int index = i; JButton route = button(tabs.getTitleAt(i), () -> tabs.setSelectedIndex(index)); route.setName("nav:" + tabs.getTitleAt(i)); route.setPreferredSize(new Dimension(100, 34)); navigation.add(route); menu.add(route);
		}
		heading.add(menu); add(heading, BorderLayout.NORTH); tabs.addChangeListener(e -> { updateNavigation(); if (tabs.getSelectedIndex() == 0) search.requestFocusInWindow(); if (tabs.getSelectedIndex() == 2 && wikiQuery != null) wikiQuery.requestFocusInWindow(); if (tabs.getSelectedIndex() == 3 && draft != null) refreshCode(); });
		status.setName("studioStatus"); status.setRows(2); status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true); status.setBorder(BorderFactory.createEmptyBorder(6, 2, 2, 2)); add(status, BorderLayout.SOUTH);
		applyTheme(this); updateNavigation();
        catalogueTimer.setRepeats(false); catalogueQuery.setName("catalogueSearch"); catalogueQuery.setToolTipText("Search English item names or Item IDs"); watch(catalogueQuery, () -> catalogueTimer.restart());
        catalogueQuery.addActionListener(e -> refreshCatalogue()); catalogueList.setName("catalogueItems"); catalogueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); catalogueList.setVisibleRowCount(4);
        catalogueList.addListSelectionListener(e -> { ItemInfo item = catalogueList.getSelectedValue(); if (!e.getValueIsAdjusting() && item != null) host.describe(item.id, info -> { if (!disposed) itemCache.put(info.id, info); }); });
        catalogueList.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(javax.swing.JComponent c) { return COPY; }
            @Override protected void exportDone(javax.swing.JComponent source,Transferable data,int action){ItemDragImage.finish(this);}
            @Override protected Transferable createTransferable(javax.swing.JComponent c) { String value=catalogueDragPayload();ItemInfo selected=catalogueList.getSelectedValue();ItemInfo info=selected==null?null:itemCache.get(selected.id);ItemDragImage.prepare(this,catalogueList,info==null?null:info.icon);return value==null?null:new StringSelection(value); }
        });
        if (!java.awt.GraphicsEnvironment.isHeadless()) catalogueList.setDragEnabled(true);
        catalogueList.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) addSelectedItem(); } });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke("control S"), "saveSetup");
        getActionMap().put("saveSetup", new javax.swing.AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { saveDraft(false); } });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke("control F"), "findItems");
        getActionMap().put("findItems", new javax.swing.AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { if (tabs.getSelectedIndex() == 0) search.requestFocusInWindow(); else if (tabs.getSelectedIndex() == 2) wikiQuery.requestFocusInWindow(); else if (draft != null) { tabs.setSelectedIndex(1); expandedPanels.add("Find items"); renderBuilder(); catalogueQuery.requestFocusInWindow(); } } });
		watch(setupName, () -> { if (!rendering) { fieldsToDraft(); markDirty(); } }); reloadScope(); tabs.setSelectedIndex(0);
		filterBank.addActionListener(e -> { if (!rendering) setBankFiltering(filterBank.isSelected()); });
		bankLayout.addActionListener(e -> { if (!rendering && draft != null) { editorLayout = (BankLayouts.Mode) bankLayout.getSelectedItem(); rebuildDraftLayout(); markDirty(); renderBuilder(); } });
		bottomToTop.addActionListener(e -> { if (!rendering && draft != null) { editorBottom = bottomToTop.isSelected(); if (editorLayout == BankLayouts.Mode.ZIGZAG) rebuildDraftLayout(); markDirty(); } });
		bankStatus.setEditable(false); bankStatus.setLineWrap(true); bankStatus.setWrapStyleWord(true);
		host.observeBank(this::scheduleAlternatives);host.observeGear(this::acceptLiveGear);preferencesChanged();
	}
	public void dispose() { disposed = true;host.observeBank(null);host.observeGear(null);host.cancelItemInput(); catalogueTimer.stop();alternativeTimer.stop(); if (wikiSuggestTimer != null) wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); wikiSearchRevision++; wikiPageRevision++; }
	public void bankMessage(String value) { if (!disposed) bankStatus.setText(value == null ? "Bank filtering failed." : value); }
	private void syncBankFilter()
	{
		if (disposed || draft == null) return; String token = session; fieldsToDraft();
		host.updateBankFilter(draft, scope, value -> { if (token.equals(session)) bankMessage(value); },layout->{if(!disposed&&token.equals(session)&&draft!=null){draft.add("layout",layout.deepCopy());editorLayout=BankLayouts.Mode.CUSTOM;markDirty();renderBuilder();message("Custom Bank layout updated. Save to keep it.");}});
	}
	private void setBankFiltering(boolean enabled)
	{
		if (draft == null) return;
		filterBank.setSelected(enabled); fieldsToDraft(); markDirty(); syncBankFilter(); renderBuilder();
	}
	private void rebuildDraftLayout()
	{
		draft.add("layout", BankLayouts.generate(draft, editorLayout, editorBottom)); syncBankFilter();
	}
	private static final class Column extends JPanel implements javax.swing.Scrollable
	{
		Column() { setMinimumSize(new Dimension(0,0));setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(Component.LEFT_ALIGNMENT); setBackground(BG); }
		@Override protected void addImpl(Component child, Object constraints, int index)
		{
			if (child instanceof javax.swing.JComponent) ((javax.swing.JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);
			if (child instanceof JTextField || child instanceof JComboBox) { child.setPreferredSize(new Dimension(210, 30)); child.setMaximumSize(new Dimension(500, 30)); }
			if (child instanceof javax.swing.AbstractButton || child instanceof JLabel) child.setMaximumSize(new Dimension(500, Math.max(26, child.getPreferredSize().height)));
			if (child instanceof JPanel && !(child instanceof Column)) child.setMaximumSize(new Dimension(500, child.getPreferredSize().height));
			if (getComponentCount() > 0) super.addImpl(javax.swing.Box.createVerticalStrut(5), null, -1);
			super.addImpl(child, constraints, index);
		}
		@Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(220, 600); }
		@Override public boolean getScrollableTracksViewportWidth() { return true; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
		@Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) { return 18; }
		@Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) { return Math.max(18, r.height - 18); }
	}
	@Override public Dimension getMinimumSize(){return new Dimension(0,0);}
    private static JPanel vertical() { return new Column(); }
	private static JLabel text(String text) { JLabel label = new JLabel(text); label.putClientProperty("html.disable", true); label.setAlignmentX(Component.LEFT_ALIGNMENT); label.setFont(BODY_FONT); label.setForeground(TEXT); return label; }
	private static void themeForm(Component component)
	{
		if (component instanceof JButton && "itemSlot".equals(component.getName())) return;
		if (component instanceof JLabel) { component.setFont(BODY_FONT); }
		else if (!(component instanceof Container) || component instanceof javax.swing.text.JTextComponent || component instanceof javax.swing.AbstractButton || component instanceof JComboBox || component instanceof JList) applyTheme(component);
		else { component.setBackground(BG); for (Component child : ((Container) component).getComponents()) themeForm(child); }
	}
	private static JTextArea info(String value, int rows) { JTextArea area = new JTextArea(value, rows, 18); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setMaximumSize(new Dimension(500, rows * 18 + 8)); return area; }
	private static JPanel card(String title, Component... children) { JPanel p = vertical(); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(7, 7, 7, 7))); JLabel label = text(title); label.setName("sectionTitle"); label.setForeground(GOLD); p.add(label); for (Component child : children) p.add(child); return p; }
	private JPanel disclosure(String title, Component content)
	{
		JPanel p = vertical(); content.setVisible(expandedPanels.contains(title)); JButton toggle = button((content.isVisible() ? "- " : "+ ") + title, () -> { });
		toggle.setName("disclosure:"+title);toggle.setHorizontalAlignment(JButton.LEFT);toggle.putClientProperty("disclosure",true);
		toggle.addActionListener(e -> { boolean open = !content.isVisible(); content.setVisible(open); if (open) expandedPanels.add(title); else expandedPanels.remove(title); toggle.setText((open ? "- " : "+ ") + title); for (Container ancestor = p; ancestor != null; ancestor = ancestor.getParent()) { ancestor.invalidate(); if (ancestor == this) break; } p.revalidate(); }); p.add(toggle); p.add(content); return p;
	}
	private void updateNavigation() { for (int i = 0; i < navigation.size(); i++) { JButton b = navigation.get(i); boolean active = i == tabs.getSelectedIndex(); b.setForeground(active ? GOLD : TEXT); b.setBackground(active ? new Color(53, 48, 37) : CONTROL); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, active ? GOLD : OUTLINE), BorderFactory.createEmptyBorder(5, 6, 5, 6))); } }
    private static void themeMenu(javax.swing.JPopupMenu menu)
    {
        menu.setBackground(CONTROL);
        menu.setBorder(BorderFactory.createLineBorder(OUTLINE));
        for (Component child : menu.getComponents())
        {
            child.setFont(BODY_FONT); child.setForeground(TEXT); child.setBackground(CONTROL);
            if (child instanceof javax.swing.JMenuItem)
            {
                javax.swing.JMenuItem item = (javax.swing.JMenuItem) child;
                item.putClientProperty("html.disable", true);
                item.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            }
            if (child instanceof javax.swing.JMenu) themeMenu(((javax.swing.JMenu) child).getPopupMenu());
        }
    }
	private static void applyTheme(Component component)
	{
		component.setFont(BODY_FONT); component.setForeground(TEXT); component.setBackground(BG);
		if ("exportCode".equals(component.getName())) { component.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11)); component.setBackground(CONTROL); }
		if (component instanceof javax.swing.AbstractButton) { javax.swing.AbstractButton b = (javax.swing.AbstractButton) component; b.setBackground(CONTROL); if (!(b instanceof JCheckBox)) { b.setFont(ACTION_FONT); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(5, 6, 5, 6))); b.setFocusPainted(true); } }
        if (component instanceof JButton && component.getName() != null && component.getName().startsWith("library"))
        {
            ((JButton)component).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(2, 3, 2, 3)));
            if (component.getName().startsWith("libraryOpen:")) component.setFont(BODY_FONT);
        }
        if (component instanceof JButton && "supportCoffee".equals(component.getName()))
        {
            component.setFont(FontManager.getRunescapeSmallFont());
            ((JButton)component).setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        }
		if(component instanceof JButton && ((JButton)component).getClientProperty("disclosure")!=null){JButton b=(JButton)component;b.setBackground(new Color(44,44,44));b.setForeground(GOLD);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,OUTLINE),BorderFactory.createEmptyBorder(6,5,6,5)));}
        if(component instanceof JButton && component.getName()!=null && component.getName().startsWith("wikiPreset:")){JButton b=(JButton)component;b.setBackground(new Color(48,44,34));b.setForeground(GOLD);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,3,0,0,GOLD),BorderFactory.createEmptyBorder(7,8,7,8)));}
        if(component instanceof JButton && component.getName()!=null && component.getName().startsWith("setupToggle:")){JButton b=(JButton)component;boolean selected=Boolean.TRUE.equals(b.getClientProperty("selected"));b.setBackground(selected?new Color(58,51,33):CONTROL);b.setForeground(selected?GOLD:TEXT);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE),BorderFactory.createEmptyBorder(3,3,3,3)));}
        if (component instanceof JTextField || component instanceof JComboBox || component instanceof JList) component.setBackground(CONTROL);
		if (component instanceof JTextField) ((JTextField) component).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
		if (component instanceof JScrollPane) ((JScrollPane) component).setBorder(BorderFactory.createLineBorder(OUTLINE));
		if(component instanceof JComboBox && !Boolean.TRUE.equals(((JComboBox<?>)component).getClientProperty("preserveRenderer")))((JComboBox<?>)component).setRenderer(plainNames());
        if (component instanceof JList) { ((JList<?>)component).setCellRenderer(plainNames());((JList<?>) component).setSelectionBackground(new Color(62, 53, 35)); ((JList<?>) component).setSelectionForeground(GOLD); }
		if (component instanceof JTextArea) { JTextArea a = (JTextArea) component; a.setForeground(a.isEditable() ? TEXT : MUTED); a.setBackground(a.isEditable() ? CONTROL : BG); }
		if ("exportCode".equals(component.getName())) component.setBackground(CONTROL);
		if (component instanceof JLabel && "sectionTitle".equals(component.getName())) { component.setForeground(GOLD); component.setFont(ACTION_FONT); }
		if (component instanceof Container) for (Component child : ((Container) component).getComponents()) applyTheme(child);
	}
    private static javax.swing.DefaultListCellRenderer plainNames()
    {
        return new javax.swing.DefaultListCellRenderer()
        {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus)
            {
                putClientProperty("html.disable",true);Component rendered=super.getListCellRendererComponent(list,value,index,selected,focus);rendered.setFont(BODY_FONT);return rendered;
            }
        };
    }
	private static JButton button(String label, Runnable action) { JButton button = new JButton(label); button.putClientProperty("html.disable", true); button.setFont(ACTION_FONT); button.setForeground(TEXT); button.setBackground(CONTROL); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(5, 6, 5, 6))); button.setFocusPainted(true); button.setMargin(new Insets(5, 6, 5, 6)); button.addActionListener(e -> action.run()); return button; }
	private static JPanel row(Component... children) { JPanel p = new JPanel(new GridLayout(0, Math.max(1, Math.min(2, children.length)), 3, 3)); p.setAlignmentX(Component.LEFT_ALIGNMENT); for (Component c : children) p.add(c); p.setMaximumSize(new Dimension(500, p.getPreferredSize().height)); return p; }
	private static JScrollPane scroll(Component c) { JScrollPane pane = new JScrollPane(c);pane.setMinimumSize(new Dimension(0,0)); pane.setBorder(null); pane.getVerticalScrollBar().setUnitIncrement(18); pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return pane; }
	private static void watch(JTextField field, Runnable action) { field.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); } }); }
	private void message(String value) { if (!disposed) { for(JTextArea feedback:new JTextArea[]{libraryFeedback,builderFeedback,exportFeedback}){feedback.setVisible(false);feedback.setText("");} status.setText(value == null ? "Could not complete this action." : value); status.setToolTipText(status.getText()); } }
    private void failure(String value)
    {
        message(value); if(disposed)return;
        JTextArea feedback=tabs.getSelectedIndex()==0?libraryFeedback:tabs.getSelectedIndex()==1?builderFeedback:tabs.getSelectedIndex()==3?exportFeedback:null;
        if(feedback!=null){feedback.setText(status.getText());feedback.setToolTipText(status.getText());themeForm(feedback);feedback.setForeground(RED);feedback.setVisible(true);revalidate();repaint();}
    }
	private static String failureMessage(RuntimeException error) { String detail = error.getMessage(); return detail == null || detail.trim().isEmpty() ? "Could not complete this action (" + error.getClass().getSimpleName() + ")." : detail; }
	private void attempt(Runnable action) { try { action.run(); } catch (RuntimeException e) { failure(failureMessage(e)); } }
	public void reloadScope()
	{
		libraryOrder=host.settings().libraryOrder();ignoreAmmo.setSelected(host.settings().ignoreAmmoQuantities());liveActual=null;alternativeUndo=null;upgradeBefore=null;upgradeAfter=null;alternativeRows.removeAll();
		host.cancelItemInput(); host.clearBankFilter(); bankMessage("Bank filter off.");
		if (disposed) return; boolean recoveryFailed=false; if (wikiSuggestTimer != null) wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); wikiSearchRevision++; wikiPageRevision++; wikiPresetRows.removeAll(); wikiPageStatus.setText("Choose a Wiki page to see its setups."); scope = host.scope(); recoveryOwner=host.recoveryOwner(); recovery=null;existingControls.removeAll(); draft = null; editingId = null; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); code.setText(""); storageError = false;
		try { library = SetupLibrary.read(host.read(scope), gson); } catch (RuntimeException e) { library = new SetupLibrary(); storageError = true; failure("Library could not be read. Existing data was not overwritten. Restore a backup before saving."); }
		try{recovery=DraftRecovery.decode(host.readRecovery(scope,recoveryOwner),gson);}catch(RuntimeException ex){recoveryFailed=true;failure("Recovery data could not be read. It was not overwritten.");}
        renderRecovery();
        scopeLabel.setText(scope == null ? "Local guest library" : "Character-specific library"); renderLibrary(); renderBuilder();
		if (!storageError && !recoveryFailed) message("Ready. Choose a saved setup, capture gear or search Wiki.");
	}
    private static String pageAge(WikiClient.Page page)
    {
        if(page.fetchedAt<1)return "";
        String date=java.time.Instant.ofEpochMilli(page.fetchedAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return (page.stale?"Stale cached source · ":page.cached?"Cached source · ":"Fetched from Wiki · ")+date;
    }
    private void persistRecovery()
    {
        if(disposed || rendering || !dirty || draft==null)return;
        try{host.saveRecovery(scope,recoveryOwner,DraftRecovery.encode(draft,editingId,editorSection,editorFavorite,editorLayout,editorBottom,ignoreAmmo.isSelected(),gson));}
        catch(RuntimeException ex){message("Draft recovery could not be saved. Use Save or export a backup.");}
    }
    private void clearRecovery(){host.saveRecovery(scope,recoveryOwner,null);recovery=null;renderRecovery();}
    private void renderRecovery()
    {
        recoveryControls.removeAll();
        if(recovery!=null)
        {
            recoveryControls.add(info("An unsaved draft is available for this character/profile.",2));
            JButton restore=button("Restore draft",this::restoreRecovery);restore.setName("restoreDraft");recoveryControls.add(row(restore,button("Discard recovery",()->{if(JOptionPane.showConfirmDialog(this,"Discard this unsaved recovery? Saved setups stay intact.","Discard recovery",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)clearRecovery();})));
        }
        themeForm(recoveryControls);recoveryControls.revalidate();recoveryControls.repaint();
    }
    private void restoreRecovery()
    {
        if(recovery==null)return;JsonObject value=recovery.deepCopy();if(!discard())return;
        host.cancelItemInput();draft=value.getAsJsonObject("portable").deepCopy();editingId=DraftRecovery.optional(value,"editingId");if(editingId!=null && library.find(editingId)==null)editingId=null;
        editorSection=DraftRecovery.optional(value,"section");editorFavorite=value.get("favorite").getAsBoolean();editorLayout=BankLayouts.Mode.valueOf(value.get("layout").getAsString());editorBottom=value.get("bottom").getAsBoolean();ignoreAmmo.setSelected(value.get("ignoreAmmo").getAsBoolean());
        dirty=true;session=UUID.randomUUID().toString();differences.clear();recovery=null;renderRecovery();renderBuilder();persistRecovery();syncBankFilter();tabs.setSelectedIndex(1);message("Unsaved draft restored. Review it, then Save.");
    }
    private void showImportOptions()
    {
        existingControls.removeAll();existingControls.add(button("Paste JSON / Bank Tag",this::importDialog));existingControls.add(button("From installed plugins",this::showExisting));themeForm(existingControls);existingControls.revalidate();
    }
    private void showExisting()
    {
        String owner=recoveryOwner;existingControls.removeAll();existingControls.add(info("Reading setups from this RuneLite profile…",2));existingControls.revalidate();
        host.existingSetups(result->
        {
            if(disposed || !Objects.equals(owner,recoveryOwner))return;
            existingControls.removeAll();existingControls.add(info("Copy from installed plugins. Originals stay unchanged.",2));
            JTextField query=new JTextField();query.setName("existingSearch");existingControls.add(query);
            DefaultListModel<ExistingSetupReader.Entry> model=new DefaultListModel<>();JList<ExistingSetupReader.Entry> choices=new JList<>(model){@Override public boolean getScrollableTracksViewportWidth(){return true;}};choices.setName("existingSetups");choices.setCellRenderer(plainNames());choices.putClientProperty("html.disable",true);choices.setVisibleRowCount(4);choices.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            Runnable filter=()->{model.clear();String needle=query.getText().trim().toLowerCase(java.util.Locale.ROOT);for(ExistingSetupReader.Entry entry:result.entries)if(entry.toString().toLowerCase(java.util.Locale.ROOT).contains(needle))model.addElement(entry);};watch(query,filter);filter.run();
            JScrollPane list=scroll(choices);list.setPreferredSize(new Dimension(190,96));list.setMaximumSize(new Dimension(500,100));existingControls.add(list);
            JTextArea review=info("Select setups to review or copy.",6);review.setName("existingReview");existingControls.add(review);
            choices.addListSelectionListener(e->{ExistingSetupReader.Entry selected=choices.getSelectedValue();review.setText(selected==null?"Select setups to review or copy.":ConversionReview.imported(selected.portable,selected.source));review.setToolTipText(review.getText());});
            existingControls.add(row(button("Save copies",()->attempt(()->{List<ExistingSetupReader.Entry> selected=choices.getSelectedValuesList();if(selected.isEmpty())throw new IllegalArgumentException("Select one or more setups first.");if(JOptionPane.showConfirmDialog(this,"Copy "+selected.size()+" selected setup(s) into Loadout Studio? Existing entries and other plugins are retained.","Copy setups",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;int count=importCopies(selected);message("Copied "+count+" setup(s); duplicate names were numbered.");})),button("Open copy",()->{ExistingSetupReader.Entry selected=choices.getSelectedValue();if(selected!=null)newDraft(selected.portable);})));existingControls.add(button("Close import list",()->{existingControls.removeAll();existingControls.revalidate();}));
            if(result.entries.isEmpty())review.setText("No compatible setups found in this RuneLite profile. Portable JSON/Bank Tag import remains available.");
            if(!result.warnings.isEmpty())existingControls.add(info(String.join("\n",result.warnings),3));themeForm(existingControls);existingControls.revalidate();existingControls.repaint();
        },error->{if(Objects.equals(owner,recoveryOwner)){existingControls.removeAll();existingControls.add(info(error,3));themeForm(existingControls);existingControls.revalidate();}});
    }
    int importCopies(List<ExistingSetupReader.Entry> selected)
    {
        if(selected.size()>100)throw new IllegalArgumentException("Copy at most 100 setups at once.");int[] count={0};
        commit(next->{for(ExistingSetupReader.Entry candidate:selected)
        {
            
            SetupLibrary.Entry copy=new SetupLibrary.Entry(candidate.portable);SetupCodec.setup(copy.portable).addProperty("name",next.uniqueName(SetupCodec.name(copy.portable)));copy.favorite=candidate.favorite;
            if(candidate.section!=null)
            {
                SetupLibrary.Section target=next.sections.stream().filter(group->group.name.equals(candidate.section)).findFirst().orElse(null);
                if(target==null){target=new SetupLibrary.Section(candidate.section);next.sections.add(target);}copy.section=target.id;
            }
            copy.bankLayout=BankLayouts.infer(copy.portable);copy.zigzagBottomToTop=BankLayouts.inferBottom(copy.portable);next.setups.add(copy);count[0]++;
        }});return count[0];
    }
	private void commit(Consumer<SetupLibrary> change)
	{
		if (storageError) throw new IllegalStateException("Restore a valid library backup before saving.");
		SetupLibrary next = SetupLibrary.read(library.serialize(gson), gson); change.accept(next); String json = next.serialize(gson); host.save(scope, json); library = next; renderLibrary();
	}
	private JPanel libraryTab()
	{
		JPanel p = new JPanel(new BorderLayout(0, 8)), controls = vertical();
        JPanel toolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0)); toolbar.setName("libraryToolbar");
        toolbar.add(libraryTool("New setup", SidebarIcon.Kind.NEW, () -> createLocalDraft(SetupCodec.blank("New loadout"))));
        toolbar.add(libraryTool("Capture gear", SidebarIcon.Kind.CAPTURE, this::capture));
        toolbar.add(libraryTool("Import setup", SidebarIcon.Kind.IMPORT, this::showImportOptions));
        toolbar.add(libraryTool("Sections", SidebarIcon.Kind.SECTIONS, this::manageSections)); controls.add(toolbar); libraryFeedback.setName("libraryFeedback");libraryFeedback.setVisible(false);controls.add(libraryFeedback);
        JPanel searchBar = new JPanel(new BorderLayout(3, 0)); searchBar.setName("librarySearchBar");
        JLabel searchIcon = text(""); searchIcon.setIcon(new SidebarIcon(SidebarIcon.Kind.SEARCH)); searchBar.add(searchIcon, BorderLayout.WEST);
        search.setName("librarySearch"); search.setToolTipText("Search saved setups"); search.getAccessibleContext().setAccessibleName("Search saved setups"); searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(libraryTool("Clear search", SidebarIcon.Kind.CLEAR, () -> search.setText("")), BorderLayout.EAST); controls.add(searchBar); controls.add(row(favorites, archived));
		favorites.setToolTipText("Show favorite setups only"); archived.setToolTipText("Show archived setups"); watch(search, this::renderLibrary); favorites.addActionListener(e -> renderLibrary()); archived.addActionListener(e -> renderLibrary());
		controls.add(recoveryControls);controls.add(existingControls);
        
        p.add(controls, BorderLayout.NORTH); p.add(scroll(libraryRows), BorderLayout.CENTER);
        JPanel tools=vertical();tools.add(button("From installed plugins",this::showExisting));tools.add(button("Copy library backup",this::backup));tools.add(button("Share setups / section",this::shareSetups));tools.add(button("Upgrade item in setups",this::upgradeItems));tools.add(button("Undo last upgrade",()->attempt(this::undoUpgrade)));p.add(disclosure("Library tools",tools),BorderLayout.SOUTH);return p;
	}
    private static JButton libraryTool(String label, SidebarIcon.Kind icon, Runnable action)
    {
        JButton tool = button("", action); tool.setIcon(new SidebarIcon(icon)); tool.setName("libraryTool:" + label); tool.setPreferredSize(new Dimension(28, 28)); tool.setToolTipText(label); tool.getAccessibleContext().setAccessibleName(label); return tool;
    }
	private JPanel builderTab()
	{
		JPanel p = new JPanel(new BorderLayout(0, 8)); p.add(builderTools, BorderLayout.NORTH); p.add(scroll(builder), BorderLayout.CENTER); return p;
	}
	private void renderLibrary()
	{
		List<SetupLibrary.Entry> recent=new ArrayList<>();for(SetupLibrary.Entry e:library.setups)if(!e.archived&&(e.favorite||e.lastOpened>0))recent.add(e);recent.sort(Comparator.comparing((SetupLibrary.Entry e)->!e.favorite).thenComparing(Comparator.comparingLong((SetupLibrary.Entry e)->e.lastOpened).reversed()));List<QuickLink> shortcuts=new ArrayList<>();for(SetupLibrary.Entry e:recent.subList(0,Math.min(5,recent.size())))shortcuts.add(new QuickLink(e.id,SetupCodec.name(e.portable)));host.quickLinks(shortcuts);
		libraryRows.removeAll(); String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
		int matching = 0;
		List<SetupLibrary.Section> groups = new ArrayList<>(); groups.add(new SetupLibrary.Section("Unfiled")); groups.get(0).id = null; groups.addAll(library.sections);
		for (SetupLibrary.Section group : groups)
		{
			List<SetupLibrary.Entry> records = new ArrayList<>();
			for (SetupLibrary.Entry entry : library.setups) if (Objects.equals(entry.section, group.id) && entry.archived == archived.isSelected() && (!favorites.isSelected() || entry.favorite) && LibraryTools.matches(entry,query,host::itemMatches)) records.add(entry);
			if(libraryOrder.ordinal()==0)records.sort(Comparator.comparing(e -> SetupCodec.name(e.portable),String.CASE_INSENSITIVE_ORDER));
			if(libraryOrder.ordinal()==1)records.sort(Comparator.comparingLong((SetupLibrary.Entry e)->e.lastOpened).reversed());
			if(libraryOrder.ordinal()==2)records.sort(Comparator.comparing((SetupLibrary.Entry e)->!e.favorite).thenComparing(e->SetupCodec.name(e.portable),String.CASE_INSENSITIVE_ORDER));
			matching += records.size();
			if (records.isEmpty() && (group.id == null || !query.isEmpty() || favorites.isSelected())) continue;
			String groupId = group.id == null ? "" : group.id;
			boolean filtering = !query.isEmpty() || favorites.isSelected();
			boolean folded = collapsed.contains(groupId) && !filtering;
			JButton heading = button((folded ? "+ " : "- ") + group.name + " (" + records.size() + ")", () -> { if (!collapsed.add(groupId)) collapsed.remove(groupId); renderLibrary(); }); heading.setForeground(GOLD); heading.setAlignmentX(LEFT_ALIGNMENT); heading.setEnabled(!filtering); if (filtering) heading.setToolTipText("Matching sections stay expanded while filtering."); libraryRows.add(heading);
			if (folded) continue;
			for (SetupLibrary.Entry entry : records)
			{
                LibrarySetupRow setupRow=new LibrarySetupRow(entry.id, SetupCodec.name(entry.portable), entry.favorite, entry.archived,
                    () -> open(entry),
                    () -> attempt(() -> setLibraryFavorite(entry.id, !entry.favorite)),
                    () -> attempt(() -> commit(l -> l.find(entry.id).archived = !entry.archived)),
                    () -> confirmDeleteArchived(entry.id));
                String rowOwner=recoveryOwner;javax.swing.JPopupMenu ordering=new javax.swing.JPopupMenu();for(int direction:new int[]{-1,1}){javax.swing.JMenuItem move=new javax.swing.JMenuItem(direction<0?"Move up":"Move down");move.addActionListener(e->{if(rowOwner.equals(recoveryOwner))attempt(()->moveSetup(entry.id,direction));});ordering.add(move);}themeMenu(ordering);((JButton)setupRow.getComponent(0)).setComponentPopupMenu(ordering);libraryRows.add(setupRow);
			}
		}
		if (matching == 0)
		{
			boolean filtered = !query.isEmpty() || favorites.isSelected() || archived.isSelected();
			if (filtered)
			{
				libraryRows.add(info(!query.isEmpty() || favorites.isSelected() ? "No setups match these filters." : "No archived setups.", 2));
				libraryRows.add(button("Clear filters", () -> { favorites.setSelected(false); archived.setSelected(false); search.setText(""); renderLibrary(); }));
			}
			else libraryRows.add(card("Start a loadout", info("Choose a Wiki setup, capture gear or start blank. Review items, Save, then open your Bank.", 4), button("Find Wiki content", () -> tabs.setSelectedIndex(2)), button("Capture gear", this::capture), button("New setup", () -> createLocalDraft(SetupCodec.blank("New loadout")))));
		}
		themeForm(libraryRows); libraryRows.revalidate(); libraryRows.repaint();
	}
    private void confirmDeleteArchived(String id)
    {
        SetupLibrary.Entry entry=library.find(id);if(entry==null||!entry.archived)return;
        String capturedScope=scope,owner=recoveryOwner;
        String warning="Permanently delete \""+SetupCodec.name(entry.portable)+"\" from your saved library?\nThis cannot be undone.";
        if(id.equals(editingId))warning+="\nThe open Builder draft and its unsaved changes will also be discarded.";
        Object[] options={"Cancel","Delete permanently"};
        int chosen=JOptionPane.showOptionDialog(this,warning,"Delete archived setup",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
        if(Objects.equals(capturedScope,scope)&&Objects.equals(owner,recoveryOwner))attempt(()->deleteArchived(id,chosen==1));
    }
    boolean deleteArchived(String id,boolean confirmed)
    {
        SetupLibrary.Entry entry=library.find(id);if(!confirmed||entry==null||!entry.archived)return false;
        commit(l->{if(!l.removeArchived(id))throw new IllegalStateException("Only archived setups can be deleted.");});
        if(id.equals(editingId))
        {
            host.cancelItemInput();session=UUID.randomUUID().toString();draft=null;editingId=null;dirty=false;differences.clear();
            setupName.setText("");notes.setText("");code.setText("");clearRecovery();host.clearBankFilter();renderBuilder();tabs.setSelectedIndex(0);
        }
        else if(draft==null && recovery!=null && id.equals(DraftRecovery.optional(recovery,"editingId")))clearRecovery();
        message("Archived setup permanently deleted.");return true;
    }
	private boolean discard()
	{
		if(!dirty)return true;
        if(JOptionPane.showConfirmDialog(this,"Discard the unsaved changes in the Builder?","Unsaved setup",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return false;
        clearRecovery();return true;
	}
	void open(SetupLibrary.Entry entry)
	{
		if (!discard()) return; host.cancelItemInput();ignoreAmmo.setSelected(host.settings().ignoreAmmoQuantities()); draft = entry.portable.deepCopy(); editorLayout = entry.bankLayout == null ? BankLayouts.infer(draft) : entry.bankLayout; editorBottom = entry.bankLayout == null ? BankLayouts.inferBottom(draft) : entry.zigzagBottomToTop; editingId = entry.id; editorSection = entry.section; editorFavorite = entry.favorite; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); message("Opened saved setup. Changes are saved only when you choose Save."); alternativeRows.removeAll();alternativeUndo=null;if(library.find(entry.id)!=null)attempt(()->commit(l->l.find(entry.id).lastOpened=System.currentTimeMillis()));host.requestGear();scheduleAlternatives();resolveDraftDefaults();
	}
    private void setLibraryFavorite(String id, boolean selected)
    {
        commit(next->{SetupLibrary.Entry entry=next.find(id);if(entry==null)throw new IllegalStateException("Setup no longer exists.");entry.favorite=selected;});
        if(id.equals(editingId)) {
            editorFavorite=selected;favorite.setSelected(selected);
            if(dirty)persistRecovery();
        }
        if(recovery!=null && id.equals(DraftRecovery.optional(recovery,"editingId"))) {
            JsonObject updated=recovery.deepCopy();updated.addProperty("favorite",selected);
            host.saveRecovery(scope,recoveryOwner,gson.toJson(updated));recovery=updated;
        }
    }
    void openSaved(String id){SetupLibrary.Entry entry=library.find(id);if(entry!=null&&!entry.archived)open(entry);}
    void toggleBankFilter(){if(!disposed&&draft!=null)setBankFiltering(!filterBank.isSelected());}
	void newDraft(JsonObject p) { newDraft(p,false); }
    private void createLocalDraft(JsonObject source)
    {
        LoadoutStudioConfig settings=host.settings();String previous=session;newDraft(LoadoutDefaults.captured(source,settings));
        if(!previous.equals(session)){editorLayout=settings.defaultLayout()==LoadoutStudioConfig.DefaultLayout.ZIGZAG?BankLayouts.Mode.ZIGZAG:BankLayouts.Mode.PRESET;editorBottom=settings.defaultZigzagBottom();persistRecovery();renderBuilder();}
    }
    void preferencesChanged()
    {
        if(disposed)return;LoadoutStudioConfig settings=host.settings();libraryOrder=settings.libraryOrder();ignoreAmmo.setSelected(settings.ignoreAmmoQuantities());if(coffeeControl!=null)coffeeControl.setVisible(settings.showSupportButton());code.setText("");renderLibrary();refreshGearComparison();revalidate();repaint();
    }
    private void newDraft(JsonObject p,boolean defaultsResolved)
	{
		SetupCodec.validate(p,false);if (!discard()) return; host.cancelItemInput(); alternativeRows.removeAll();alternativeUndo=null;ignoreAmmo.setSelected(host.settings().ignoreAmmoQuantities());draft = p.deepCopy(); editorLayout = BankLayouts.infer(draft); editorBottom = BankLayouts.inferBottom(draft); editingId = null; editorSection = null; editorFavorite = false; dirty = true; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); persistRecovery();message("New draft. Review it, then Save to your library."); host.requestGear();scheduleAlternatives();if(!defaultsResolved)resolveDraftDefaults();
	}
    private boolean needsDefault(JsonElement item){return SetupCodec.id(item)==0||item!=null&&item.isJsonObject()&&item.getAsJsonObject().has("label")&&ItemCatalogue.legacyRingId(SetupCodec.id(item),item.getAsJsonObject().get("label").getAsString())!=SetupCodec.id(item);}
    private void resolveDraftDefaults()
    {
        boolean needed=false;for(String key:new String[]{"eq","inv","rp","qv","bp"})for(JsonElement e:SetupCodec.setup(draft).has(key)&&SetupCodec.setup(draft).get(key).isJsonArray()?SetupCodec.setup(draft).getAsJsonArray(key):new JsonArray())if(needsDefault(e))needed=true;
        if(!needed)return;String token=session;JsonObject current=draft;
        host.whenItemsReady(error->{
            if(disposed||error!=null||!token.equals(session)||current!=draft)return;
            int resolved=0;for(String key:new String[]{"eq","inv","rp","qv","bp"})for(JsonElement entry:SetupCodec.setup(draft).has(key)&&SetupCodec.setup(draft).get(key).isJsonArray()?SetupCodec.setup(draft).getAsJsonArray(key):new JsonArray())if(needsDefault(entry)){
                JsonObject item=entry.getAsJsonObject();int oldId=SetupCodec.id(item);ItemInfo first=host.defaultItem(WikiParser.defaultName(item));
                if(first!=null){if(oldId>0)for(int i=0;i<draft.getAsJsonArray("layout").size();i++)if(draft.getAsJsonArray("layout").get(i).getAsInt()==oldId)draft.getAsJsonArray("layout").set(i,new com.google.gson.JsonPrimitive(first.id));item.addProperty("id",first.id);item.addProperty("label",first.name);itemCache.put(first.id,first);resolved++;}
            }
            if(resolved>0){rebuildDraftLayout();markDirty();renderBuilder();syncBankFilter();scheduleAlternatives();message("Applied first choices to "+resolved+" slot(s). Review and Save to keep them.");}
        });
    }
	private void capture()
	{
		if (!discard()) return; String capturedScope = scope, token = session, owner = recoveryOwner; long revision = draftRevision;
		host.capture(p -> {
			if (disposed || !Objects.equals(scope, capturedScope) || !Objects.equals(owner, recoveryOwner) || !token.equals(session)) return;
			if (revision != draftRevision) { message("Capture cancelled because the Builder changed. Your edits were kept."); return; }
			dirty = false; createLocalDraft(p);
		}, this::failure);
	}
	private void manageSections()
	{
		String[] options = new String[library.sections.size() + 1]; options[0] = "+ New section"; for (int i = 0; i < library.sections.size(); i++) options[i + 1] = library.sections.get(i).name;
		String choice = (String) JOptionPane.showInputDialog(this, "Choose a section", "Sections", JOptionPane.PLAIN_MESSAGE, null, options, options[0]); if (choice == null) return;
		if (choice.equals(options[0])) { String name = JOptionPane.showInputDialog(this, "Section name (1–60 characters)"); if (name != null) attempt(() -> { commit(l -> l.sections.add(new SetupLibrary.Section(name.trim()))); renderBuilder(); }); }
		else
		{
			int index = java.util.Arrays.asList(options).indexOf(choice) - 1; SetupLibrary.Section chosen = library.sections.get(index);
			int action = JOptionPane.showOptionDialog(this, "Rename this section or move its setups to Unfiled.", chosen.name, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Rename", "Remove section", "Cancel"}, "Rename");
			if (action == 0) { String name = JOptionPane.showInputDialog(this, "Section name", chosen.name); if (name != null) attempt(() -> { commit(l -> l.sections.stream().filter(s -> s.id.equals(chosen.id)).findFirst().get().name = name.trim()); renderBuilder(); }); }
			else if (action == 1) attempt(() -> { commit(l -> l.removeSection(chosen.id)); renderBuilder(); });
		}
	}
	private void markDirty()
	{
		if (rendering || draft == null) return; draftRevision++; dirty = true; draftLabel.setText("Unsaved changes"); setupName.setToolTipText(setupName.getText() + " - Unsaved changes (Ctrl+S to save)"); code.setText(""); fieldsToDraft();persistRecovery();refreshGearComparison();
	}
	private void fieldsToDraft()
	{
		if (draft == null) return; JsonObject s = SetupCodec.setup(draft); s.addProperty("name", setupName.getText().trim()); s.addProperty("sb", spellbook.getSelectedIndex()); s.addProperty("notes", notes.getText()); s.addProperty("fb", filterBank.isSelected());
	}
	private void renderBuilder()
	{
		if (disposed) return;
        javax.swing.JViewport viewport = builder.getParent() instanceof javax.swing.JViewport ? (javax.swing.JViewport)builder.getParent() : null;
        java.awt.Point viewportPosition = viewport != null && session.equals(renderedSession) ? viewport.getViewPosition() : new java.awt.Point();
        String renderToken = session; renderedSession = session; rendering = true;
		try
		{
			builder.removeAll(); builderTools.removeAll(); exportCopy.setEnabled(draft != null); exportPreview.setEnabled(draft != null); if (draft == null) { builder.add(card("Build a loadout", info("Capture your gear, create a blank setup or choose a Wiki preset.", 3), button("Capture gear", this::capture), button("New setup", () -> createLocalDraft(SetupCodec.blank("New loadout"))), button("Find Wiki content", () -> tabs.setSelectedIndex(2)))); builder.revalidate(); builder.repaint(); return; }
			JsonObject s = SetupCodec.setup(draft); 
			setupName.setText(SetupCodec.name(draft)); setupName.setMaximumSize(new Dimension(500, 28)); spellbook.setSelectedIndex(s.has("sb") ? s.get("sb").getAsInt() : 4); notes.setText(s.has("notes") && !s.get("notes").isJsonNull() ? s.get("notes").getAsString() : ""); notes.setLineWrap(true); notes.setWrapStyleWord(true);
			section.setRenderer(plainNames());section.removeAllItems(); sectionIds.clear(); section.addItem("Unfiled"); sectionIds.add(null); int selectedSection = 0;
			for (SetupLibrary.Section group : library.sections) { section.addItem(group.name); sectionIds.add(group.id); if (Objects.equals(group.id, editorSection)) selectedSection = sectionIds.size() - 1; } section.setSelectedIndex(selectedSection); favorite.setSelected(editorFavorite);
            setupName.getAccessibleContext().setAccessibleName("Setup name"); setupName.setToolTipText("Setup name");
            JPanel titleBar = new JPanel(new BorderLayout(4, 0)); titleBar.add(setupName, BorderLayout.CENTER); titleBar.add(button("Save", () -> saveDraft(false)), BorderLayout.EAST); builderTools.add(titleBar);
            addResolutionGuide();
            JPanel itemSearch = vertical(); itemSearch.add(catalogueQuery); JScrollPane results = scroll(catalogueList); results.setPreferredSize(new Dimension(176, 96)); results.setMaximumSize(new Dimension(500, 100)); itemSearch.add(results); itemSearch.add(catalogueCount); itemSearch.add(button("Add to Inventory", this::addSelectedItem)); builderTools.add(disclosure("Find items", itemSearch));

            draftLabel.setText(dirty ? "Unsaved changes" : "Saved setup"); setupName.setToolTipText(SetupCodec.name(draft) + " - " + draftLabel.getText());
            filterBank.setSelected(s.has("fb") && s.get("fb").getAsBoolean()); bankLayout.setSelectedItem(editorLayout); bottomToTop.setSelected(editorBottom); bottomToTop.setEnabled(editorLayout == BankLayouts.Mode.ZIGZAG);
			            JPanel setupActions=new JPanel(new GridLayout(1,5,3,0));setupActions.setName("setupActions");
            setupActions.add(setupToggle("filter",0,"Enable bank filtering",filterBank.isSelected(),()->setBankFiltering(!filterBank.isSelected())));
            setupActions.add(setupToggle("unordered",1,"Only highlight items missing from Inventory; ignore order",s.has("uh")&&s.get("uh").getAsBoolean(),()->{s.addProperty("uh",!s.has("uh")||!s.get("uh").getAsBoolean());markDirty();renderBuilder();}));
            setupActions.add(setupToggle("highlight",2,"Enable highlighting",highlighting(),()->{s.addProperty("hd",!highlighting());markDirty();renderBuilder();}));
            setupActions.add(setupToggle("color",3,"Edit highlight color",true,()->{Color color=javax.swing.JColorChooser.showDialog(this,"Highlight color",highlightColor());if(color!=null){s.addProperty("hc",String.format("#FF%06X",color.getRGB()&0xFFFFFF));markDirty();renderBuilder();}}));
            setupActions.add(setupToggle("favorite",4,"Favorite this setup",favorite.isSelected(),()->{favorite.doClick();renderBuilder();}));builderTools.add(setupActions);
            if(host.settings().showAttackOption())builderTools.add(text("Attack option: "+(s.has("ao")&&!s.get("ao").isJsonNull()&&!s.get("ao").getAsString().isEmpty()?s.get("ao").getAsString():"not specified")));
            JButton quickCompare=button("Compare",this::compareGear);quickCompare.setName("quickCompareGear");builderTools.add(quickCompare);builderFeedback.setName("builderFeedback");builderTools.add(builderFeedback);gearReadiness.setName("gearReadiness");builderTools.add(gearReadiness);
            JPanel alternatives=vertical();alternativesContent=alternatives;if(!host.settings().autoAlternatives())alternatives.add(button("Refresh alternatives",this::refreshAlternatives));alternatives.add(alternativeRows);builder.add(disclosure("My gear alternatives",alternatives));

            JPanel inventory = itemGrid(7, 4); inventory.setName("inventoryGrid");
            for (int i = 0; i < 28; i++) inventory.add(slot("inv", i)); builder.add(loadoutSection("Inventory", inventory));
            JPanel runes = itemGrid(1, 4); for (int i = 0; i < 4; i++) runes.add(slot("rp", i)); builder.add(loadoutSection("Rune Pouch", runes));
            if(s.has("bp") && s.get("bp").isJsonArray()){JPanel bolts=itemGrid(1,4);for(int i=0;i<4;i++)bolts.add(slot("bp",i));builder.add(loadoutSection("Bolt Pouch",bolts));}

            JPanel equipment = new JPanel(new GridBagLayout()); equipment.setName("equipmentGrid");
            int[][] positions = {{0,1,0},{1,0,1},{2,1,1},{13,2,1},{3,0,2},{4,1,2},{5,2,2},{7,1,3},{9,0,4},{10,1,4},{12,2,4}};
            for (int[] position : positions) { GridBagConstraints c = new GridBagConstraints(); c.gridx = position[1]; c.gridy = position[2]; c.insets = new Insets(0, 0, 0, 0); equipment.add(slot("eq", position[0]), c); }
            equipment.setPreferredSize(new Dimension(132, 220)); builder.add(loadoutSection("Equipment", equipment));
            spellbook.setBorder(highlighting()&&differences.contains("spellbook")?BorderFactory.createLineBorder(highlightColor(),2):javax.swing.UIManager.getBorder("ComboBox.border"));spellbook.setPreferredSize(new Dimension(132, 28)); builder.add(loadoutSection("Spellbook", spellbook));
            JPanel quiver = itemGrid(1, 1); quiver.add(slot("qv", 0)); builder.add(loadoutSection("Quiver", quiver));

            int extraCount = Math.max(4, ((s.getAsJsonObject("afi").size() + 3) / 4) * 4);
            JPanel extras = itemGrid(extraCount / 4, 4); for (int i = 0; i < extraCount; i++) extras.add(slot("afi", i)); builder.add(loadoutSection("Additional filtered items", extras));
            notes.setName("loadoutNotes"); notes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); builder.add(loadoutSection("Notes", notes));

            JPanel loadoutOptions = vertical(); loadoutOptions.add(button("Max pouch runes (16k)", () -> { for (JsonElement e : s.getAsJsonArray("rp")) if (!e.isJsonNull()) e.getAsJsonObject().addProperty("q", 16000); markDirty(); renderBuilder(); })); loadoutOptions.add(button("Add Bolt Pouch slots",()->{if(!s.has("bp")||!s.get("bp").isJsonArray())s.add("bp",SetupCodec.empty(4));markDirty();renderBuilder();}));loadoutOptions.add(button("Capture attack option",()->{String token=session;host.capture(actual->{if(!disposed&&token.equals(session)){JsonObject current=SetupCodec.setup(actual);s.addProperty("ao",current.has("ao")?current.get("ao").getAsString():"");markDirty();renderBuilder();}},this::failure);}));loadoutOptions.add(button("Ignore attack option",()->{s.remove("ao");markDirty();renderBuilder();}));if(s.has("ao")&&!s.get("ao").isJsonNull()&&!s.get("ao").getAsString().isEmpty())loadoutOptions.add(info("Attack option: "+s.get("ao").getAsString(),1)); loadoutOptions.add(button("Add filtered item", () -> editSlot("afi", SetupCodec.setup(draft).getAsJsonObject("afi").size()))); builder.add(disclosure("Loadout options", loadoutOptions));
            JPanel bankOptions = vertical(); bankOptions.add(bankLayout); bankOptions.add(bottomToTop); bankStatus.setRows(2); bankStatus.setMaximumSize(new Dimension(500, 54)); bankOptions.add(bankStatus); builder.add(disclosure("Bank filtering and layout", bankOptions));
            JPanel details = vertical(); details.add(text("Section")); details.add(section);  details.add(button("Save copy", () -> saveDraft(true)));builder.add(disclosure("Setup details", details));

		}
		finally { rendering = false; refreshGearComparison();themeForm(builderTools); themeForm(builder); builderTools.revalidate(); builderTools.repaint(); builder.revalidate(); builder.repaint(); if (viewport != null) SwingUtilities.invokeLater(() -> { if (!disposed && renderToken.equals(session)) viewport.setViewPosition(new java.awt.Point(0, Math.min(viewportPosition.y, Math.max(0, builder.getPreferredSize().height - viewport.getExtentSize().height)))); }); }
	}
    private void addResolutionGuide()
    {
        int remaining=0;String targetKey=null;int targetIndex=0;JsonObject target=null;
        for(String key:new String[]{"eq","inv","rp","qv","bp"})
        {
            JsonArray items=SetupCodec.setup(draft).has(key)&&SetupCodec.setup(draft).get(key).isJsonArray()?SetupCodec.setup(draft).getAsJsonArray(key):new JsonArray();
            for(int i=0;i<items.size();i++)if(!items.get(i).isJsonNull())
            {
                JsonObject item=items.get(i).getAsJsonObject();if(SetupCodec.id(item)==0 || item.has("q")&&item.get("q").isJsonNull())
                {remaining++;if(target==null){target=item;targetKey=key;targetIndex=i;}}
            }
        }
        if(target==null)return;
        JTextArea guide=new CompactStatusArea(remaining+" slots need review. Click ? to search; right-click a slot to set its quantity.");guide.setName("resolutionGuide");builderTools.add(guide);
    }

    private JButton setupToggle(String name,int icon,String tooltip,boolean selected,Runnable action){JButton button=button("",action);button.setName("setupToggle:"+name);button.putClientProperty("selected",selected);button.setIcon(new SetupActionIcon(icon,highlightColor()));button.setToolTipText(tooltip);button.getAccessibleContext().setAccessibleName(tooltip);button.setPreferredSize(new Dimension(30,26));return button;}
    private boolean highlighting(){return draft!=null&&(!SetupCodec.setup(draft).has("hd")||SetupCodec.setup(draft).get("hd").getAsBoolean());}
    private Color highlightColor(){try{return new Color((int)Long.parseLong(SetupCodec.setup(draft).get("hc").getAsString().replace("#",""),16),true);}catch(RuntimeException ex){return RED;}}
    private static JPanel itemGrid(int rows, int columns)
    {
        JPanel grid = new JPanel(new GridLayout(rows, columns, 0, 0)); grid.setPreferredSize(new Dimension(columns * 44, rows * 44)); return grid;
    }
    private static JPanel loadoutSection(String title, Component content)
    {
        JPanel section = new JPanel(new BorderLayout(0, 6)) { @Override public Dimension getMaximumSize() { return new Dimension(500,getPreferredSize().height); } }; section.setName("loadoutSection:" + title); section.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
        JLabel label = text(title); label.setHorizontalAlignment(JLabel.CENTER); label.setForeground(MUTED); section.add(label, BorderLayout.NORTH);
        JPanel centered = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0)); centered.add(content); section.add(centered, BorderLayout.CENTER); return section;
    }
	private int firstEmpty() { JsonArray inv = SetupCodec.setup(draft).getAsJsonArray("inv"); for (int i = 0; i < 28; i++) if (inv.get(i).isJsonNull()) return i; return -1; }
	private JsonElement get(String key, int index)
	{
		JsonObject s = SetupCodec.setup(draft); if (key.equals("afi")) { List<JsonElement> entries = new ArrayList<>(); for (Map.Entry<String, JsonElement> e : s.getAsJsonObject(key).entrySet()) entries.add(e.getValue()); return index < entries.size() ? entries.get(index) : JsonNull.INSTANCE; }
		JsonArray a = s.has(key)&&s.get(key).isJsonArray()?s.getAsJsonArray(key):new JsonArray(); return index < a.size() ? a.get(index) : JsonNull.INSTANCE;
	}
	private void put(String key, int index, JsonElement value)
	{
		JsonObject s = SetupCodec.setup(draft);
		if (key.equals("afi")) { JsonObject extras = s.getAsJsonObject(key); List<String> keys = new ArrayList<>(extras.keySet()); if (index < keys.size()) extras.remove(keys.get(index)); if (value != null && !value.isJsonNull()) extras.add(String.valueOf(SetupCodec.id(value)), value); }
		else { JsonArray a = s.getAsJsonArray(key); while (a.size() <= index) a.add(JsonNull.INSTANCE); a.set(index, value == null ? JsonNull.INSTANCE : value); }
	}
	private static final String[] EQUIPMENT = {"Head", "Cape", "Neck", "Weapon", "Body", "Shield", "", "Legs", "", "Hands", "Feet", "", "Ring", "Ammo"};
	private JButton slot(String key, int index)
	{
		JsonElement entry = get(key, index); int id = SetupCodec.id(entry); String empty = ""; String container = key.equals("eq") ? EQUIPMENT[index] : key.equals("inv") ? "Inventory" : key.equals("rp") ? "Rune Pouch" : key.equals("bp") ? "Bolt Pouch" : key.equals("qv") ? "Quiver" : "Additional filtered items";
		LoadoutSlotButton button = new LoadoutSlotButton(id > 0 ? String.valueOf(id) : id == 0 ? "?" : empty); button.addActionListener(e -> editSlot(key, index)); button.setName("itemSlot"); button.putClientProperty("slotContainer", key); button.putClientProperty("slotIndex", index); button.setPreferredSize(new Dimension(44, 44)); button.setMinimumSize(new Dimension(44, 44)); button.setBackground(SLOT); button.setFont(FontManager.getRunescapeSmallFont()); button.setMargin(new Insets(1, 1, 1, 1)); button.setForeground(GOLD);
		button.setBorder(BorderFactory.createLineBorder(slotDifferent(key + ":" + index) ? highlightColor() : BG, slotDifferent(key + ":" + index) ? 2 : 1));
        button.setRolloverEnabled(true); button.setContentAreaFilled(true);
        button.getModel().addChangeListener(e -> button.setBorder(BorderFactory.createLineBorder(slotDifferent(key + ":" + index) ? highlightColor() : button.getModel().isRollover() ? OUTLINE : BG)));
        button.getAccessibleContext().setAccessibleName((container + " slot " + (index + 1)) + (id < 0 ? " - empty" : " - edit item"));
        if (id < 0) button.setToolTipText("Empty " + container + " slot - click to add an item");
		if (id == 0) button.setToolTipText((entry.getAsJsonObject().has("label")?entry.getAsJsonObject().get("label").getAsString():"Unresolved item") + " · choose an exact Item ID");
		if (id > 0) host.describe(id, info -> { if (disposed) return; itemCache.put(id, info); button.itemIcon(info.icon); button.setText(info.icon == null ? "!" : ""); button.amount(SetupCodec.unlimited(entry) ? "∞" : key.equals("rp") && SetupCodec.quantity(entry)==0 ? "?" : SetupCodec.quantity(entry) > 1 || key.equals("rp") ? compact(SetupCodec.quantity(entry)) : ""); button.setToolTipText(info.name + " · " + id + " · quantity " + (SetupCodec.unlimited(entry) ? "unlimited (quantity ignored)" : SetupCodec.quantity(entry) == 0 ? "unknown" : SetupCodec.quantity(entry))); button.putClientProperty("baseTooltip",button.getToolTipText());updateDifferenceBorders(builder); });
        button.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(javax.swing.JComponent c) { return MOVE; }
            @Override protected Transferable createTransferable(javax.swing.JComponent c) { String value = dragPayload(key, index);ItemDragImage.prepare(this,button,button.getIcon());return value == null ? null : new StringSelection(value); }
            @Override protected void exportDone(javax.swing.JComponent source,Transferable data,int action){ItemDragImage.finish(this);}
            @Override public boolean canImport(TransferSupport support)
            {
                try { if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false; return canDrop((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor), key, index); } catch (Exception ex) { return false; }
            }
            @Override public boolean importData(TransferSupport support)
            {
                try { return support.isDataFlavorSupported(DataFlavor.stringFlavor) && importItemDrop((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor), key, index); } catch (Exception ex) { message("Could not read the dragged item."); return false; }
            }
        });
        java.awt.event.MouseAdapter drag = new java.awt.event.MouseAdapter()
        {
            private java.awt.Point start; private boolean started;
            @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); started = false; }
            @Override public void mouseDragged(MouseEvent e) { if (!started && start != null && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0 && start.distance(e.getPoint()) >= 5) { started = true; button.getModel().setArmed(false); button.getTransferHandler().exportAsDrag(button, e, TransferHandler.MOVE); } }
        };
        button.addMouseListener(drag); button.addMouseMotionListener(drag); button.menuFactory=shift->slotMenu(key,index,shift);button.setComponentPopupMenu(slotMenu(key,index)); return button;
    }

    private void refreshCatalogue()
    {
        if (disposed) return; catalogueModel.clear(); String query = catalogueQuery.getText().trim();
        if (!query.isEmpty()) for (ItemInfo info : host.search(query)) catalogueModel.addElement(info);
        catalogueCount.setText(query.isEmpty() ? "Search English item names or IDs" : catalogueModel.size() + " matches"); catalogueCount.setToolTipText(host.catalogueStatus());
        if (!catalogueModel.isEmpty()) catalogueList.setSelectedIndex(0);
    }
    private void addSelectedItem()
    {
        if (draft == null) return; ItemInfo item = catalogueList.getSelectedValue(); if (item == null) { message("Choose an item in Find items first."); return; }
        int index = firstEmpty(); if (index < 0) { message("Inventory is full. Drag an item onto a slot to replace it."); return; } addCatalogueItem(item.id, "inv", index);
    }
    private void addCatalogueItem(int id, String key, int index)
    {
        if (id < 1) { message("Choose a valid Item ID first."); return; }
        String token = session; JsonElement before = get(key, index).deepCopy();
        host.describe(id, info ->
        {
            if (disposed || !token.equals(session)) return;
            if (!before.equals(get(key, index))) { message("This slot changed. Choose the destination again."); return; }
            attempt(() -> { itemCache.put(id, info); JsonObject item = SetupCodec.item(id, 1); validateSlot(key, index, item); checkExtraDuplicate(key, index, item); fieldsToDraft(); put(key, index, item); rebuildDraftLayout(); markDirty(); renderBuilder(); message("Added " + info.name + ". Review its quantity if needed."); });
        });
    }
    private boolean validSlot(String key, int index)
    {
        if (draft == null || index < 0) return false;
        if (key.equals("afi")) return index < Math.max(4, ((SetupCodec.setup(draft).getAsJsonObject("afi").size() + 3) / 4) * 4);
        int length = key.equals("eq") ? 14 : key.equals("inv") ? 28 : (key.equals("rp") || key.equals("bp")) ? 4 : key.equals("qv") ? 1 : 0;
        return index < length && !(key.equals("eq") && (index == 6 || index == 8 || index == 11));
    }
    String catalogueDragPayload() { ItemInfo item = catalogueList.getSelectedValue(); return item == null ? null : "loadout-item:" + session + ":" + item.id; }
    String dragPayload(String key, int index) { return validSlot(key, index) && !get(key, index).isJsonNull() ? "loadout-slot:" + session + ":" + key + ":" + index : null; }
    private JsonElement dropItem(String key,JsonElement item){JsonElement copy=item.deepCopy();if(key.equals("rp")&&SetupCodec.quantity(copy)>16000)copy.getAsJsonObject().addProperty("q",16000);return copy;}
    private boolean canDrop(String value, String key, int index)
    {
        if (disposed || !validSlot(key, index) || value == null) return false;
        String[] parts = value.split(":"); if (parts.length < 3 || !parts[1].equals(session)) return false;
        if (parts[0].equals("loadout-item") && parts.length == 3) { int id = Integer.parseInt(parts[2]); for (int i = 0; i < catalogueModel.size(); i++) if (catalogueModel.get(i).id == id) return true; return false; }
        if (!parts[0].equals("loadout-slot") || parts.length != 4 || !validSlot(parts[2], Integer.parseInt(parts[3]))) return false;
        JsonElement item = get(parts[2], Integer.parseInt(parts[3])); if (item.isJsonNull()) return false;
        validateSlot(key,index,dropItem(key,item));validateSlot(parts[2],Integer.parseInt(parts[3]),get(key,index));return true;
    }
    private void checkExtraDuplicate(String key, int index, JsonElement item)
    {
        if (key.equals("afi") && !item.isJsonNull() && SetupCodec.id(item) != SetupCodec.id(get(key, index)) && SetupCodec.setup(draft).getAsJsonObject("afi").has(String.valueOf(SetupCodec.id(item)))) throw new IllegalArgumentException("This item is already in Additional filtered items.");
    }
    boolean importItemDrop(String value, String key, int index)
    {
        try
        {
            if (!canDrop(value, key, index)) return false; String[] parts = value.split(":");
            if (parts[0].equals("loadout-item")) { addCatalogueItem(Integer.parseInt(parts[2]), key, index); return true; }
            String from = parts[2]; int old = Integer.parseInt(parts[3]); if (from.equals(key) && old == index) return false;
            JsonElement a = dropItem(key,get(from,old)), b = get(key,index).deepCopy();
            // Setup planning: cap the incoming stack and drop its excess rather than
            // creating a remainder slot. A different displaced rune still swaps back.
            if(from.equals("inv")&&key.equals("rp")&&SetupCodec.quantity(get(from,old))>16000&&SetupCodec.id(b)==SetupCodec.id(a))b=JsonNull.INSTANCE;
            if (from.equals("afi") && key.equals("afi"))
            {
                List<JsonElement> items = new ArrayList<>(); for (Map.Entry<String, JsonElement> entry : SetupCodec.setup(draft).getAsJsonObject("afi").entrySet()) items.add(entry.getValue()); while (items.size() <= Math.max(old, index)) items.add(JsonNull.INSTANCE); items.set(old, b); items.set(index, a);
                JsonObject extras = new JsonObject(); for (JsonElement item : items) if (!item.isJsonNull()) extras.add(String.valueOf(SetupCodec.id(item)), item); SetupCodec.setup(draft).add("afi", extras);
            }
            else { checkExtraDuplicate(key, index, a); checkExtraDuplicate(from, old, b); fieldsToDraft(); put(from,old,b);put(key,index,a); }
            rebuildDraftLayout();markDirty();renderBuilder();scheduleAlternatives();message("Moved item. Choose Save to keep your changes."); return true;
        }
        catch (RuntimeException ex) { failure(failureMessage(ex)); return false; }
    }

	private static String compact(int q) { return q >= 1000 && q % 1000 == 0 ? q / 1000 + "k" : String.valueOf(q); }
	private void validateSlot(String key, int index, JsonElement e)
	{
		if (e == null || e.isJsonNull()) return; ItemInfo info = itemCache.get(SetupCodec.id(e));
        if (!key.equals("inv") && info == null) throw new IllegalArgumentException("Item details are still loading. Choose an exact Item ID or try again.");
		if (key.equals("rp")) { if (info != null && !info.name.toLowerCase(java.util.Locale.ROOT).endsWith(" rune")) throw new IllegalArgumentException("Rune pouch accepts runes only."); if (SetupCodec.quantity(e) > 16000) throw new IllegalArgumentException("Rune pouch maximum is 16k."); }
		if (key.equals("bp") && info != null && !info.name.toLowerCase(java.util.Locale.ROOT).contains("bolt") && !info.name.equalsIgnoreCase("Bolt rack")) throw new IllegalArgumentException("Bolt pouch accepts bolts only.");
		if (key.equals("qv") && info != null && !info.name.toLowerCase(java.util.Locale.ROOT).matches(".*(arrow|bolt)s?(?: \\(.*\\))?$")) throw new IllegalArgumentException("Quiver accepts arrows or bolts.");
		if (key.equals("eq") && info != null && info.slot != index) throw new IllegalArgumentException("This item belongs in a different Equipment slot.");
	}
    private javax.swing.JPopupMenu slotMenu(String key,int index){return slotMenu(key,index,false);}
    private javax.swing.JPopupMenu slotMenu(String key,int index,boolean shift)
    {
        javax.swing.JPopupMenu menu=new javax.swing.JPopupMenu(); JsonElement current=get(key,index);
        javax.swing.JMenuItem search=new javax.swing.JMenuItem("Update Slot from Search"); search.addActionListener(e->editSlot(key,index)); menu.add(search);
        if(!key.equals("afi")) { javax.swing.JMenuItem capture=new javax.swing.JMenuItem("Update Slot from " + (key.equals("eq") ? "Equipment" : key.equals("inv") ? "Inventory" : key.equals("rp") ? "Rune Pouch" : key.equals("bp") ? "Bolt Pouch" : "Quiver")); capture.addActionListener(e->updateSlotFromContainer(key,index)); menu.add(capture); }
        javax.swing.JMenuItem quantity=new javax.swing.JMenuItem("Set quantity"); quantity.setEnabled(!current.isJsonNull() && SetupCodec.id(current)>0); quantity.addActionListener(e->editQuantity(key,index)); menu.add(quantity);
        javax.swing.JMenuItem fuzzy=new javax.swing.JMenuItem("Toggle Fuzzy"); fuzzy.setEnabled(!current.isJsonNull()); fuzzy.addActionListener(e->attempt(()->{ JsonObject item=get(key,index).getAsJsonObject(); item.addProperty("f",!item.has("f") || !item.get("f").getAsBoolean()); changedSlot(); })); menu.add(fuzzy);
        javax.swing.JMenu stack=new javax.swing.JMenu("Stack comparison"); stack.setEnabled(!current.isJsonNull() && !SetupCodec.unlimited(current));
        for(LoadoutStudioConfig.StackRule rule:LoadoutStudioConfig.StackRule.values()) { String value=rule.code; javax.swing.JMenuItem option=new javax.swing.JMenuItem(rule.label); option.addActionListener(e->attempt(()->{ get(key,index).getAsJsonObject().addProperty("sc",value); changedSlot(); })); stack.add(option); } menu.add(stack);
        javax.swing.JMenuItem remove=new javax.swing.JMenuItem("Remove Item from Slot"); remove.setEnabled(!current.isJsonNull()); remove.addActionListener(e->{ put(key,index,JsonNull.INSTANCE); changedSlot(); }); menu.add(remove);
        if(shift&&SetupCodec.id(current)>0){menu.addSeparator();javax.swing.JMenuItem allCurrent=new javax.swing.JMenuItem("Update ALL slots from "+(key.equals("eq")?"Equipment":"Inventory"));allCurrent.addActionListener(e->attempt(()->bulkFromContainer(key,index)));menu.add(allCurrent);javax.swing.JMenuItem allSearch=new javax.swing.JMenuItem("Update ALL slots from Search");allSearch.addActionListener(e->attempt(()->bulkFromSearch(key,index)));menu.add(allSearch);}themeMenu(menu);return menu;
    }
    private void bulkFromSearch(String key,int index){if(dirty)throw new IllegalStateException("Save this setup before updating all saved setups.");int old=SetupCodec.id(get(key,index));String token=session;host.chooseItem(id->{if(!disposed&&token.equals(session))host.describe(id,replacement->host.describe(old,original->{if(!disposed&&token.equals(session))attempt(()->confirmBulkUpdate(original,replacement));}));},this::failure);}
    private void bulkFromContainer(String key,int index){if(dirty)throw new IllegalStateException("Save this setup before updating all saved setups.");int old=SetupCodec.id(get(key,index));String token=session;host.capture(actual->{if(disposed||!token.equals(session))return;String source=key.equals("eq")?"eq":"inv";JsonArray slots=SetupCodec.setup(actual).getAsJsonArray(source);int id=index<slots.size()?SetupCodec.id(slots.get(index)):-1;if(id<1){failure("That live slot is empty. Choose Update ALL slots from Search instead.");return;}host.describe(id,replacement->host.describe(old,original->{if(!disposed&&token.equals(session))attempt(()->confirmBulkUpdate(original,replacement));}));},this::failure);}
    Set<String> bulkTargets(int id){Set<String> targets=new HashSet<>();for(SetupLibrary.Entry entry:library.setups)if(BankLayouts.items(entry.portable).stream().anyMatch(item->SetupCodec.id(item)==id))targets.add(entry.id);return targets;}
    private void confirmBulkUpdate(ItemInfo original,ItemInfo replacement){Set<String> targets=bulkTargets(original.id);if(targets.isEmpty())return;String owner=recoveryOwner,token=session;if(JOptionPane.showConfirmDialog(this,"Replace "+original.name+" with "+replacement.name+" in ALL "+targets.size()+" matching saved setups, including archived setups? Amounts and matching rules are retained. Undo is in Library tools.","Update all matching slots",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;if(owner.equals(recoveryOwner)&&token.equals(session))replaceLibraryItems(original,replacement,targets);}
    private void changedSlot() { fieldsToDraft(); rebuildDraftLayout(); markDirty(); renderBuilder(); scheduleAlternatives(); }
    private void editSlot(String key,int index)
    {
        if(draft==null) return; fieldsToDraft(); String token=session; JsonElement before=get(key,index).deepCopy(); message("Choose an item in the RuneLite chatbox.");
        host.chooseItem(id->{ if(disposed || !token.equals(session) || id<1 || !before.equals(get(key,index))) return;
            host.describe(id,info->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; itemCache.put(id,info);
                if(info.stackable || key.equals("rp") || key.equals("qv")) host.enterQuantity(host.settings().emptyQuantityInput()?"":amountText(before),value->{ if(token.equals(session) && before.equals(get(key,index))) applySlotItem(key,index,id,value); },this::failure);
                else applySlotItem(key,index,id,"1");
            });
        },this::failure);
    }
    private static String amountText(JsonElement item)
    {
        return item.isJsonNull()?"1":item.getAsJsonObject().has("q")&&item.getAsJsonObject().get("q").isJsonNull()?"":String.valueOf(SetupCodec.quantity(item));
    }
    private void editQuantity(String key,int index)
    {
        JsonElement before=get(key,index).deepCopy(); String token=session; if(before.isJsonNull()) return;
        host.enterQuantity(host.settings().emptyQuantityInput()?"":amountText(before),value->{ if(!disposed && token.equals(session) && before.equals(get(key,index))) applySlotItem(key,index,SetupCodec.id(before),value); },this::failure);
    }
    private void applySlotItem(String key,int index,int id,String amount)
    {
        attempt(()->{ int quantity=amount.trim().equals("0") ? 0 : SetupCodec.parseQuantity(amount);
            ItemInfo info=itemCache.get(id); if(info==null) throw new IllegalArgumentException("Item details are still loading."); if(!info.stackable && quantity>1) throw new IllegalArgumentException("Non-stackable items need separate slots.");
            JsonElement previous=get(key,index); JsonObject item=SetupCodec.item(id,quantity);
            if(!previous.isJsonNull()) for(String field:new String[]{"f","sc"}) if(previous.getAsJsonObject().has(field)) item.add(field,previous.getAsJsonObject().get(field).deepCopy());
            if(quantity==0)item.addProperty("sc","None");else if(!item.has("sc")||item.get("sc").getAsString().equals("None"))item.addProperty("sc","Standard"); validateSlot(key,index,item); checkExtraDuplicate(key,index,item); put(key,index,item); changedSlot(); message(quantity==0 ? "Unlimited: quantity matching is disabled for this slot." : "Slot updated.");
        });
    }
    private void updateSlotFromContainer(String key,int index)
    {
        String token=session; JsonElement before=get(key,index).deepCopy();
        host.capture(actual->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; JsonElement item=SetupCodec.setup(actual).getAsJsonArray(key).get(index).deepCopy();
            if(item.isJsonNull()) { put(key,index,JsonNull.INSTANCE); changedSlot(); return; }
            host.describe(SetupCodec.id(item),info->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; itemCache.put(info.id,info); attempt(()->{validateSlot(key,index,item);put(key,index,item);changedSlot();}); });
        },this::failure);
    }

	private void saveDraft(boolean copy)
	{
		if (draft == null) return;
		attempt(() -> { fieldsToDraft(); SetupCodec.validate(draft, false); String chosenSection = sectionIds.get(Math.max(0, section.getSelectedIndex())); boolean chosenFavorite = favorite.isSelected(); String id = copy || editingId == null ? UUID.randomUUID().toString() : editingId;
			commit(l -> { SetupLibrary.Entry record = l.find(id); if (record == null) { record = new SetupLibrary.Entry(draft); record.id = id; } record.portable=draft.deepCopy();if(copy||editingId==null)SetupCodec.setup(record.portable).addProperty("name",l.uniqueName(SetupCodec.name(draft)));record.section = chosenSection; record.favorite = chosenFavorite; record.bankLayout = editorLayout; record.zigzagBottomToTop = editorBottom;record.lastOpened=System.currentTimeMillis();if(l.find(id)==null)l.setups.add(record); });String storedName=SetupCodec.name(library.find(id).portable);SetupCodec.setup(draft).addProperty("name",storedName);boolean beforeRendering=rendering;rendering=true;setupName.setText(storedName);rendering=beforeRendering; editingId = id; dirty = false; clearRecovery(); draftLabel.setText("Saved setup"); setupName.setToolTipText(setupName.getText() + " - Saved setup"); message("Saved to this character/profile library."); });
	}
	private void compareGear()
	{
		if (draft == null) return; fieldsToDraft();host.requestGear(); String token = session;long revision=draftRevision;
        host.compareSetup(draft.deepCopy(),ignoreAmmo.isSelected(),result->{if(disposed || !token.equals(session)||revision!=draftRevision)return;differences.clear();differences.addAll(result);updateDifferenceBorders(builder);gearReadiness.setText(result.isEmpty()?"Gear matches setup":result.size()+" gear difference(s)");message(result.size()+" difference(s). Fuzzy variants, Stack comparison, unordered Inventory and Ammo preferences are applied.");},this::failure);
	}
    void acceptLiveGear(JsonObject actual)
    {
        if(disposed)return;liveActual=actual==null?null:actual.deepCopy();refreshGearComparison();scheduleAlternatives();
    }
    private void refreshGearComparison()
    {
        if(draft==null || liveActual==null){gearReadiness.setText("Live check: log in");gearReadiness.setToolTipText("Current gear is unknown until logged in.");if(liveActual==null){differences.clear();updateDifferenceBorders(builder);}return;}
        Set<String> next=SetupComparison.differences(draft,liveActual,ignoreAmmo.isSelected(),SetupVariationMapping::map);
        differences.clear();differences.addAll(next);gearReadiness.setText(next.isEmpty()?"Gear matches setup":next.size()+" gear difference(s)");List<String> reasons=new ArrayList<>();for(String key:next)reasons.add(differenceReason(key));gearReadiness.setToolTipText(next.isEmpty()?"Compared with your current local gear.":String.join("; ",reasons));
        updateDifferenceBorders(builder);spellbook.setBorder(highlighting()&&next.contains("spellbook")?BorderFactory.createLineBorder(highlightColor(),2):javax.swing.UIManager.getBorder("ComboBox.border"));
    }
    private void updateDifferenceBorders(Container root)
    {
        for(Component child:root.getComponents()){
            if(child instanceof JButton && "itemSlot".equals(child.getName())){JButton slot=(JButton)child;String key=slot.getClientProperty("slotContainer")+":"+slot.getClientProperty("slotIndex");slot.setBorder(BorderFactory.createLineBorder(slotDifferent(key)?highlightColor():BG,slotDifferent(key)?2:1));slot.getAccessibleContext().setAccessibleDescription(slotDifferent(key)?differenceReason(key):"Matches current gear");String base=(String)slot.getClientProperty("baseTooltip");if(base!=null)slot.setToolTipText(base+(slotDifferent(key)?" · "+differenceReason(key):""));}
            if(child instanceof Container)updateDifferenceBorders((Container)child);
        }
    }
    private boolean slotDifferent(String key){return highlighting()&&(differences.contains(key)||key.equals("eq:3")&&differences.contains("attackOption"));}
    private String differenceReason(String key)
    {
        if(key.equals("eq:3")&&!differences.contains(key)&&differences.contains("attackOption"))return "Wrong attack option";
        if(key.equals("spellbook"))return "Wrong Spellbook";if(key.equals("attackOption"))return "Wrong attack option";
        String[] parts=key.split(":");if(parts.length!=2||draft==null||liveActual==null)return key;String container=parts[0];int index=Integer.parseInt(parts[1]);JsonElement expected=get(container,index);JsonObject actual=SetupCodec.setup(liveActual);JsonArray items=actual.has(container)&&actual.get(container).isJsonArray()?actual.getAsJsonArray(container):new JsonArray();JsonElement present=index<items.size()?items.get(index):JsonNull.INSTANCE;
        String name=container.equals("eq")?EQUIPMENT[index]:container.toUpperCase(java.util.Locale.ROOT)+" "+(index+1);
        if(SetupCodec.id(expected)==0)return name+": unresolved item";if(expected.isJsonObject()&&expected.getAsJsonObject().has("q")&&expected.getAsJsonObject().get("q").isJsonNull())return name+": quantity unknown";
        if(container.equals("inv")&&SetupCodec.setup(draft).has("uh")&&SetupCodec.setup(draft).get("uh").getAsBoolean())return name+": required item or amount missing";
        if(SetupCodec.id(present)<1)return name+": missing item";if(SetupCodec.id(expected)<1)return name+": extra item";
        if(SetupCodec.id(expected)!=SetupCodec.id(present)&&!(expected.getAsJsonObject().has("f")&&expected.getAsJsonObject().get("f").getAsBoolean()&&SetupVariationMapping.map(SetupCodec.id(expected))==SetupVariationMapping.map(SetupCodec.id(present))))return name+": different item or slot";
        return name+": quantity differs";
    }
    private void scheduleAlternatives(){if(alternativeUndo!=null&&draftRevision!=alternativeUndoRevision)alternativeUndo=null;if(!disposed&&draft!=null&&host.settings().autoAlternatives()&&alternativeUndo==null)alternativeTimer.restart();}
    private void refreshAlternativeRows(){themeForm(alternativeRows);for(Container parent=alternativeRows;parent!=null;parent=parent.getParent()){parent.invalidate();if(parent==this)break;}builder.revalidate();builder.repaint();}
    private void showAlternativesOnce(boolean available){if(!available||session.equals(autoShownAlternativeSession)||!host.settings().autoShowAlternatives())return;autoShownAlternativeSession=session;expandedPanels.add("My gear alternatives");if(alternativesContent!=null){alternativesContent.setVisible(true);Container parent=alternativesContent.getParent();if(parent!=null)for(Component child:parent.getComponents())if(child instanceof JButton&&"disclosure:My gear alternatives".equals(child.getName()))((JButton)child).setText("- My gear alternatives");}}
    void refreshAlternatives()
    {
        if(draft==null)return;fieldsToDraft();String token=session;long revision=draftRevision;alternativeRows.removeAll();if(BankLayouts.items(draft).isEmpty())return;alternativeRows.add(info("Checking local gear...",1));
        host.availableGear(draft.deepCopy(),owned->{
            if(disposed||!token.equals(session)||revision!=draftRevision)return;alternativeRows.removeAll();
            if(owned.observedAt==0){alternativeRows.add(info("Open your Bank to see owned alternatives.",2));refreshAlternativeRows();return;}
            String date=java.time.Instant.ofEpochMilli(owned.observedAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            alternativeRows.add(info((owned.bankOpen?"Bank observed now":"Last Bank observation "+date)+". Other storage is not included. Review requirements before replacing.",4));
            List<OwnedGear.Proposal> proposals=owned.alternatives(draft,host::defaultItem);Map<OwnedGear.Proposal,JComboBox<ItemInfo>> choices=new java.util.LinkedHashMap<>();
            for(OwnedGear.Proposal proposal:proposals){
                int id=SetupCodec.id(proposal.before);ItemInfo original=owned.items.get(id);JLabel missing=text((proposal.container.equals("eq")?EQUIPMENT[proposal.index]:"Inventory "+(proposal.index+1))+": "+(original==null?"Item "+id:original.name));missing.setMinimumSize(new Dimension(0,18));missing.setToolTipText(missing.getText()+" · "+proposal.reason);alternativeRows.add(missing);
                JComboBox<ItemInfo> select=new JComboBox<>();select.setName("ownedChoice:"+proposal.container+":"+proposal.index);select.addItem(new ItemInfo(-1,"Keep original",false,-1,null));for(ItemInfo item:proposal.choices.subList(0,Math.min(Math.max(1,Math.min(5,host.settings().maxAlternatives())),proposal.choices.size()))){select.addItem(item);itemCache.put(item.id,item);}
                select.setRenderer(new javax.swing.DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused){JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);label.putClientProperty("html.disable",true);label.setFont(BODY_FONT);label.setText(value==null||((ItemInfo)value).id<1?"Keep original":((ItemInfo)value).name+" ("+owned.count(((ItemInfo)value).id)+" seen)");return label;}});
                select.putClientProperty("preserveRenderer",true);select.setToolTipText(proposal.reason);alternativeRows.add(select);choices.put(proposal,select);
            }
            if(proposals.isEmpty())alternativeRows.add(info("No supported owned alternatives found. Missing gear is not automatically replaced.",3));
            else alternativeRows.add(button("Use selected alternatives",()->attempt(()->{if(!token.equals(session)||revision!=draftRevision)throw new IllegalStateException("Setup changed. Check your Bank again.");Map<OwnedGear.Proposal,ItemInfo> selected=new java.util.LinkedHashMap<>();choices.forEach((proposal,select)->{ItemInfo item=(ItemInfo)select.getSelectedItem();if(item!=null&&item.id>0)selected.put(proposal,item);});host.availableGear(draft.deepCopy(),fresh->{if(disposed||!token.equals(session)||revision!=draftRevision)return;attempt(()->{for(ItemInfo item:selected.values())if(fresh.count(item.id)<1)throw new IllegalStateException("An alternative is no longer observed. Check your Bank again.");if(host.settings().checkAlternativeStock())fresh.requireReplacementStock(draft,selected);applyAlternatives(selected);});},this::failure);})));showAlternativesOnce(!proposals.isEmpty());refreshAlternativeRows();
        },error->{if(!disposed&&token.equals(session)){alternativeRows.removeAll();alternativeRows.add(info(error,2));refreshAlternativeRows();}});
    }
    void applyAlternatives(Map<OwnedGear.Proposal,ItemInfo> selected)
    {
        if(draft==null||selected.isEmpty()){message("Choose at least one alternative.");return;}
        JsonObject next=draft.deepCopy();
        for(Map.Entry<OwnedGear.Proposal,ItemInfo> e:selected.entrySet()){
            OwnedGear.Proposal proposal=e.getKey();ItemInfo item=e.getValue();if(!proposal.before.equals(get(proposal.container,proposal.index))||proposal.choices.stream().noneMatch(choice->choice.id==item.id))throw new IllegalStateException("Alternative no longer matches this slot. Check again.");
            itemCache.put(item.id,item);JsonObject replacement=proposal.before.getAsJsonObject().deepCopy();replacement.addProperty("id",item.id);replacement.addProperty("label",item.name);validateSlot(proposal.container,proposal.index,replacement);SetupCodec.setup(next).getAsJsonArray(proposal.container).set(proposal.index,replacement);
            if(!item.stackable && SetupCodec.quantity(replacement)>1)throw new IllegalArgumentException("Adjust the quantity before choosing a non-stackable replacement.");
        }
        next.add("layout",BankLayouts.generate(next,editorLayout,editorBottom));SetupCodec.validate(next,false);alternativeUndo=draft.deepCopy();draft=next;markDirty();alternativeUndoRevision=draftRevision;
        alternativeRows.removeAll();alternativeRows.add(info("Alternatives applied to this draft. Review and Save to keep them.",3));alternativeRows.add(button("Undo alternatives",()->attempt(this::undoAlternatives)));syncBankFilter();renderBuilder();
    }
    void undoAlternatives()
    {
        if(alternativeUndo==null||draftRevision!=alternativeUndoRevision)throw new IllegalStateException("The draft changed after replacement. Undo is no longer available.");draft=alternativeUndo;alternativeUndo=null;alternativeRows.removeAll();markDirty();syncBankFilter();renderBuilder();
    }
    void moveSetup(String id,int direction)
    {
        SetupLibrary.Entry entry=library.find(id);if(entry==null)return;
        List<String> peers=new ArrayList<>();for(SetupLibrary.Entry candidate:library.setups)if(Objects.equals(candidate.section,entry.section)&&candidate.archived==entry.archived)peers.add(candidate.id);
        int from=peers.indexOf(id),to=from+direction;if(to<0||to>=peers.size())return;String other=peers.get(to);
        commit(next->java.util.Collections.swap(next.setups,next.setups.indexOf(next.find(id)),next.setups.indexOf(next.find(other))));libraryOrder=LoadoutStudioConfig.LibraryOrder.MANUAL;host.libraryOrder(libraryOrder);renderLibrary();
    }
    private void shareSetups()
    {
        String owner=recoveryOwner;List<SetupLibrary.Entry> entries=new ArrayList<>(library.setups);if(entries.isEmpty()){message("Save a setup before sharing.");return;}
        JList<String> list=new JList<>(entries.stream().map(e->SetupCodec.name(e.portable)).toArray(String[]::new));list.putClientProperty("html.disable",true);list.setCellRenderer(plainNames());list.setVisibleRowCount(8);
        JComboBox<String> groups=new JComboBox<>();groups.addItem("Choose setups individually");for(SetupLibrary.Section section:library.sections)groups.addItem(section.name);groups.setRenderer(plainNames());groups.addActionListener(e->{list.clearSelection();int group=groups.getSelectedIndex()-1;if(group>=0){String id=library.sections.get(group).id;for(int i=0;i<entries.size();i++)if(Objects.equals(entries.get(i).section,id))list.addSelectionInterval(i,i);}});
        JPanel panel=vertical();panel.add(info("Select setups or a section. Only this selection is shared, including its Notes. Import adds independent copies.",4));panel.add(groups);panel.add(new JScrollPane(list));
        themeForm(panel);if(JOptionPane.showConfirmDialog(this,panel,"Share selected setups",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        if(!owner.equals(recoveryOwner))return;Set<String> ids=new HashSet<>();for(int i:list.getSelectedIndices())ids.add(entries.get(i).id);attempt(()->{String json=gson.toJson(LibraryTools.bundle(library,ids,gson));getToolkit().getSystemClipboard().setContents(new StringSelection(json),null);message("Selected setup bundle copied.");});
    }
    private void upgradeItems()
    {
        if(dirty){message("Save your current draft before upgrading saved setups.");return;}String owner=recoveryOwner,token=session;
        message("Choose the old item in the chatbox, then choose its replacement.");host.chooseItem(oldId->{if(disposed||!owner.equals(recoveryOwner)||!token.equals(session))return;host.describe(oldId,old->{
            if(disposed||!owner.equals(recoveryOwner)||!token.equals(session))return;host.chooseItem(newId->host.describe(newId,replacement->{
                if(disposed||!owner.equals(recoveryOwner)||!token.equals(session))return;
                List<SetupLibrary.Entry> entries=new ArrayList<>();for(SetupLibrary.Entry entry:library.setups)if(!entry.archived && BankLayouts.items(entry.portable).stream().anyMatch(item->SetupCodec.id(item)==oldId))entries.add(entry);
                if(entries.isEmpty()){message("No active saved setups contain that exact item.");return;}
                JList<String> list=new JList<>(entries.stream().map(e->SetupCodec.name(e.portable)).toArray(String[]::new));list.putClientProperty("html.disable",true);list.setCellRenderer(plainNames());list.setVisibleRowCount(8);JPanel preview=vertical();preview.add(info("Replace "+old.name+" with "+replacement.name+" in selected setups only. Review combat style and requirements. Exact Item IDs only.",5));preview.add(new JScrollPane(list));
                themeForm(preview);if(JOptionPane.showConfirmDialog(this,preview,"Review item upgrade",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
                if(!owner.equals(recoveryOwner)||!token.equals(session))return;Set<String> ids=new HashSet<>();for(int i:list.getSelectedIndices())ids.add(entries.get(i).id);attempt(()->replaceLibraryItems(old,replacement,ids));
            }),this::failure);
        });},this::failure);
    }
    void replaceLibraryItems(ItemInfo old,ItemInfo replacement,Set<String> ids)
    {
        if(dirty)throw new IllegalStateException("Save your draft first.");if(ids.isEmpty())throw new IllegalArgumentException("Select at least one setup.");
        if(old.slot!=replacement.slot)throw new IllegalArgumentException("Replacement must fit the same Equipment slot or item category.");
        String before=library.serialize(gson);commit(next->{for(String id:ids){SetupLibrary.Entry entry=next.find(id);if(entry==null)throw new IllegalArgumentException("Choose saved setups.");if(!replacement.stackable)for(JsonElement item:BankLayouts.items(entry.portable))if(SetupCodec.id(item)==old.id&&SetupCodec.quantity(item)>1)throw new IllegalArgumentException("A selected stack cannot be replaced by a non-stackable item.");for(String key:new String[]{"eq","inv","rp","qv","bp"})if(SetupCodec.setup(entry.portable).has(key))for(JsonElement item:SetupCodec.setup(entry.portable).getAsJsonArray(key))if(SetupCodec.id(item)==old.id){if(key.equals("rp")&&!replacement.name.toLowerCase(java.util.Locale.ROOT).endsWith(" rune"))throw new IllegalArgumentException("Rune Pouch replacements must be runes.");if(key.equals("qv")&&replacement.slot!=13)throw new IllegalArgumentException("Quiver replacements must be arrows or bolts.");if(key.equals("bp")&&!replacement.name.toLowerCase(java.util.Locale.ROOT).contains("bolt")&&!replacement.name.equalsIgnoreCase("Bolt rack"))throw new IllegalArgumentException("Bolt Pouch replacements must be bolts.");}LibraryTools.replace(entry.portable,old.id,replacement.id,replacement.name);}});
        if(editingId!=null&&ids.contains(editingId))open(library.find(editingId));upgradeBefore=before;upgradeAfter=library.serialize(gson);message("Updated "+ids.size()+" saved setup(s). Undo last upgrade is available in Library tools.");
    }
    void undoUpgrade()
    {
        if(dirty || upgradeBefore==null || !library.serialize(gson).equals(upgradeAfter))throw new IllegalStateException("Library or draft changed. This upgrade can no longer be undone safely.");SetupLibrary previous=SetupLibrary.read(upgradeBefore,gson);host.save(scope,previous.serialize(gson));library=previous;String id=editingId;upgradeBefore=null;upgradeAfter=null;if(id!=null&&library.find(id)!=null)open(library.find(id));renderLibrary();message("Last item upgrade undone.");
    }
	private JPanel exportTab()
	{
		JPanel p = new JPanel(new BorderLayout(0, 8)), controls = vertical(); controls.add(text("Export the open setup")); controls.add(format);conversionNotice.setName("conversionNotice");controls.add(conversionNotice); exportPreview = button("Show code", this::refreshCode); exportCopy = button("Copy code", () -> attempt(() -> { refreshCodeOrThrow(); getToolkit().getSystemClipboard().setContents(new StringSelection(code.getText()), null); message("Copied " + format.getSelectedItem() + "."); })); controls.add(row(exportPreview, exportCopy));exportFeedback.setName("exportFeedback");exportFeedback.setVisible(false);controls.add(exportFeedback);
		code.setName("exportCode"); code.setEditable(false); code.setLineWrap(false); p.add(controls, BorderLayout.NORTH); p.add(new JScrollPane(code), BorderLayout.CENTER);
		p.add(disclosure("Import instructions", info("Inventory Setups: import icon > Import setup > Enter setup data. Bank Tags: right-click the Bank + tab > Import tag tab. Bank Tags do not store Equipment slots, quantities, Spellbook or Notes.", 7)), BorderLayout.SOUTH);
		format.addActionListener(e -> { code.setText(""); if (draft != null) refreshCode(); });
		spellbook.addActionListener(e -> { if (!rendering) { fieldsToDraft(); markDirty(); } }); ignoreAmmo.addActionListener(e -> markDirty()); favorite.addActionListener(e -> { if (!rendering) { editorFavorite = favorite.isSelected(); markDirty(); } }); section.addActionListener(e -> { if (!rendering && section.getSelectedIndex() >= 0 && section.getSelectedIndex() < sectionIds.size()) { editorSection = sectionIds.get(section.getSelectedIndex()); markDirty(); } }); notes.getDocument().addDocumentListener(new DocumentListener() { private void update() { if(!rendering) { fieldsToDraft();markDirty();for(Container ancestor=notes.getParent();ancestor!=null;ancestor=ancestor.getParent()){ancestor.invalidate();if(ancestor==StudioPanel.this)break;}builder.revalidate(); } } public void insertUpdate(DocumentEvent e) {update();} public void removeUpdate(DocumentEvent e) {update();} public void changedUpdate(DocumentEvent e) { } }); return p;
	}
	private void refreshCode() { attempt(this::refreshCodeOrThrow); }
	private void refreshCodeOrThrow()
	{
		code.setText(""); if (draft == null) throw new IllegalStateException("Open a setup in the Builder first."); fieldsToDraft(); JsonObject p = draft.deepCopy();conversionNotice.setText(format.getSelectedIndex()==0?ConversionReview.inventory(p):ConversionReview.bank(p));conversionNotice.setRows(format.getSelectedIndex()==0?4:8);conversionNotice.setMaximumSize(new Dimension(500,conversionNotice.getRows()*18+8));conversionNotice.setToolTipText(conversionNotice.getText());
		if (ignoreAmmo.isSelected()) for (String key : new String[]{"eq", "qv"}) { JsonArray items = SetupCodec.setup(p).getAsJsonArray(key); if (items == null) continue; int start = key.equals("eq") ? 13 : 0; for (int i = start; i < items.size(); i++) if (!items.get(i).isJsonNull()) { items.get(i).getAsJsonObject().remove("q"); items.get(i).getAsJsonObject().remove("sc"); } }
		code.setText(format.getSelectedIndex() == 0 ? codec.inventory(p) : codec.bank(p, format.getSelectedIndex() == 2)); code.setCaretPosition(0);
	}
	private void importDialog()
	{
		JTextArea input = new JTextArea(12, 34); JScrollPane inputPane = new JScrollPane(input); themeForm(inputPane); int action = JOptionPane.showConfirmDialog(this, inputPane, "Paste Inventory Setup JSON, Bank Tag or library backup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE); if (action != JOptionPane.OK_OPTION) return;
		attempt(() ->
		{
			String text = input.getText().trim(); JsonElement parsed = text.startsWith("{") ? codec.parse(text) : null;
			if (parsed != null && parsed.isJsonObject() && parsed.getAsJsonObject().has("sections") && parsed.getAsJsonObject().has("setups"))
			{
				if(parsed.getAsJsonObject().has("libraryBundle")){SetupLibrary incoming=SetupLibrary.read(text,gson);if(JOptionPane.showConfirmDialog(this,"Add "+incoming.setups.size()+" shared setup copies? Existing setups stay intact.","Import shared bundle",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;commit(next->LibraryTools.merge(next,incoming,gson));message("Shared setups added as independent copies.");return;}
				SetupLibrary restored = SetupLibrary.read(text, gson); if (JOptionPane.showConfirmDialog(this, "Replace this plugin's current library with " + restored.setups.size() + " saved setups? Export a backup first if needed.", "Restore library backup", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
				host.save(scope, restored.serialize(gson)); reloadScope(); message("Library backup restored."); return;
			}
			List<JsonObject> imported=codec.importText(text);boolean bankImport=!(text.startsWith("{")||text.startsWith("[")) || parsed!=null&&parsed.isJsonObject()&&parsed.getAsJsonObject().has("bankTag");
            JTextArea review=info("Add "+imported.size()+" setup(s)? Existing saved setups are retained.\n"+ConversionReview.imported(imported.get(0),bankImport?"Bank Tag":"Inventory Setup"),9);
            themeForm(review);if(JOptionPane.showConfirmDialog(this,review,"Review import",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
			commit(l -> { for (JsonObject p : imported){JsonObject copy=p.deepCopy();SetupCodec.setup(copy).addProperty("name",l.uniqueName(SetupCodec.name(copy)));l.setups.add(new SetupLibrary.Entry(copy));} }); message("Imported " + imported.size() + " setup(s). Open them from Library."); renderLibrary();
		});
	}
	private void backup()
	{
		attempt(() -> { String text = library.serialize(gson); getToolkit().getSystemClipboard().setContents(new StringSelection(text), null); message("Complete library backup copied. Save this JSON to a file; restore with Import."); });
	}
	private JPanel wikiTab()
	{
		JPanel p = vertical(); JTextField url = new JTextField(), title = new JTextField(); url.setName("wikiPageUrl"); url.setMaximumSize(new Dimension(500, 28)); title.setMaximumSize(new Dimension(500, 28)); JTextArea raw = new JTextArea(10, 18); raw.setLineWrap(true);

		JTextField query = new JTextField(); wikiQuery = query; query.setName("wikiSearch"); query.getAccessibleContext().setAccessibleName("Search Wiki content");
		DefaultListModel<String> matches = new DefaultListModel<>(); JList<String> pages = new JList<>(matches); pages.setName("wikiSuggestions"); pages.putClientProperty("html.disable", true); pages.setVisibleRowCount(4); pages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
		p.add(text("Find content")); p.add(query);
		JTextArea searchHint = new CompactStatusArea("Press Enter or Search Wiki to search. Typing uses cached suggestions."); p.add(searchHint);
		Runnable searchPages = () -> attempt(() ->
		{
			wikiSuggestTimer.stop();
			String requested = query.getText().trim(), capturedScope = scope;
			long revision = ++wikiSearchRevision;
			if (disposed) return;
			if (requested.length() < 2 || requested.length() > 120) { searchHint.setText("Enter 2–120 characters to search."); return; }
			searchHint.setText("Searching OSRS Wiki...");
			host.searchWiki(requested, (titles, error) ->
			{
				if (disposed || revision != wikiSearchRevision || !Objects.equals(scope, capturedScope) || !query.getText().trim().equals(requested)) return;
				matches.clear();
				if (error != null) { searchHint.setText(error); searchHint.setToolTipText(error); message(error); }
				else { for (String name : WikiClient.activityTitles(titles)) matches.addElement(name); searchHint.setText(titles.isEmpty() ? "No pages found. Try another name." : "Select a page to load its setups."); }
			});
		});
		wikiSuggestTimer = new Timer(250, e ->
        {
            String term=query.getText().trim(),owner=recoveryOwner;long revision=wikiSearchRevision;
            host.localWikiSuggestions(term,titles->{if(disposed || revision!=wikiSearchRevision || !Objects.equals(owner,recoveryOwner))return;matches.clear();for(String name:WikiClient.activityTitles(titles))matches.addElement(name);searchHint.setText(titles.isEmpty()?"Press Enter or Search Wiki to load suggestions.":"Cached suggestions · Search Wiki finds more.");});
        });wikiSuggestTimer.setRepeats(false);
		JButton find = button("Search Wiki", searchPages); find.setToolTipText("Search OSRS Wiki once; repeated requests reuse cached results");
		watch(query, () ->
		{
			wikiSearchRevision++; wikiPageRevision++; wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); matches.clear(); wikiPresetRows.removeAll(); wikiPageStatus.setText("Choose a Wiki page to see its setups.");
			int length = query.getText().trim().length();
			if (length < 2 || length > 120) searchHint.setText("Enter 2–120 characters to search.");
			else{searchHint.setText("Checking cached suggestions…");wikiSuggestTimer.restart();}
		});
		tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() != 2) { wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); wikiSearchRevision++; wikiPageRevision++; } });
		query.addActionListener(e -> find.doClick()); p.add(find); JScrollPane pageList = new JScrollPane(pages); pageList.setPreferredSize(new Dimension(210, 96)); pageList.setMaximumSize(new Dimension(500, 110)); p.add(pageList);
		wikiPageStatus.setName("wikiPageStatus"); wikiPageStatus.setEditable(false); wikiPageStatus.setLineWrap(true); wikiPageStatus.setWrapStyleWord(true);
		wikiPresetRows.setName("wikiPresetChoices"); p.add(text("Available setups")); p.add(wikiPageStatus); p.add(wikiPresetRows);
		final boolean[] refreshRequested={false};
        Runnable fetchPage = () ->
		{
			boolean force=refreshRequested[0];refreshRequested[0]=false;
            wikiPageTimer.stop(); long revision = ++wikiPageRevision; if(!force){wikiPresetRows.removeAll();wikiPresetRows.revalidate();wikiPresetRows.repaint();}
			String requested = url.getText().trim(), capturedScope = scope;
			if (disposed) return;
			final String page;
			try { page = WikiClient.title(requested); } catch (RuntimeException error) { wikiPageStatus.setText(failureMessage(error)); return; }
			wikiPageStatus.setText("Loading setups from " + page + "...");
			try
			{
				host.fetchWikiPage(requested, true, force, (loaded, error) ->
				{
					if (disposed || revision != wikiPageRevision || !Objects.equals(scope, capturedScope) || !url.getText().trim().equals(requested)) return;
					if (error != null) { wikiPageStatus.setText((force?"Refresh failed; existing choices are retained. ":"")+error); message(error); return; }
					try { title.setText(loaded.title.split("(?i)/Strategies", 2)[0]); url.setToolTipText(loaded.url()); generateWiki(loaded.text, loaded.title, loaded.url());wikiPageStatus.setText(wikiPageStatus.getText()+"\n"+pageAge(loaded)+(host.wikiCacheWarning().isEmpty()?"":"\n"+host.wikiCacheWarning()));wikiPageStatus.setToolTipText(wikiPageStatus.getText()); }
					catch (RuntimeException failure) { wikiPageStatus.setText(failureMessage(failure)); message(failureMessage(failure)); }
				});
			}
			catch (RuntimeException failure) { wikiPageStatus.setText(failureMessage(failure)); message(failureMessage(failure)); }
		};
		wikiPageTimer = new Timer(300, e -> fetchPage.run()); wikiPageTimer.setRepeats(false);
		watch(url, () ->
		{
			wikiPageRevision++; wikiPageTimer.stop(); wikiPresetRows.removeAll(); wikiPresetRows.revalidate(); wikiPresetRows.repaint();
			try { WikiClient.title(url.getText().trim()); wikiPageStatus.setText("Loading page choices..."); wikiPageTimer.restart(); }
			catch (RuntimeException ignored) { wikiPageStatus.setText("Choose a page or enter a valid Wiki URL."); }
		});
		Runnable selectPage = () -> { String selected = pages.getSelectedValue(); if (selected != null) { title.setText(selected.split("(?i)/Strategies", 2)[0]); url.setText(WikiClient.articleUrl(selected)); } };
		pages.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectPage.run(); });
		url.addActionListener(e -> fetchPage.run()); p.add(row(button("Refresh Wiki",()->{if(JOptionPane.showConfirmDialog(this,"Fetch a fresh copy from OSRS Wiki? Cached pages already work without a new request.","Refresh Wiki source",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){refreshRequested[0]=true;fetchPage.run();}})));
		JPanel advanced = vertical(); advanced.setVisible(false);
		p.add(disclosure("URL / pasted wikitext",advanced));
		advanced.add(text("Wiki article URL")); advanced.add(url); advanced.add(text("Page name (pasted text)")); advanced.add(title); advanced.add(text("Wikitext (optional)")); advanced.add(new JScrollPane(raw));
		advanced.add(button("Generate from pasted text", () -> attempt(() -> { wikiPageRevision++; wikiPageTimer.stop(); generateWiki(raw.getText(), title.getText().trim(), "Pasted wikitext"); })));
		p.add(disclosure("Wiki information", info("Search text, page titles and your IP address go to OSRS Wiki. No gear, saved setups or account credentials are uploaded. Ranked recommendation tables still require manual choices.", 6))); return p;
	}
	private void generateWiki(String text, String title, String source)
	{
		List<JsonObject> presets = new WikiParser().parse(text, title, source);
		wikiPresetRows.removeAll();
		for (JsonObject preset : presets)
		{
			String variant = WikiParser.variant(preset);
			JButton choose = button(variant, () -> attempt(() -> openWikiPreset(preset)));
			choose.setName("wikiPreset:" + variant); choose.setMaximumSize(new Dimension(500, 44)); choose.setToolTipText("Open " + variant + ". First Wiki choices are a starting point; review gear, quantities and Spellbook before Save."); wikiPresetRows.add(choose);
		}
		wikiPageStatus.setText(presets.size() + " setups. Review first choices before Save.");themeForm(wikiPresetRows);wikiPresetRows.invalidate(); if (wikiPresetRows.getParent() != null) wikiPresetRows.getParent().invalidate(); wikiPresetRows.revalidate(); wikiPresetRows.repaint();
	}
	private void openWikiPreset(JsonObject original)
	{
        long choice = ++wikiChoiceRevision, page = wikiPageRevision; String token = session;
        message("Loading game items for this Wiki setup...");
        host.whenItemsReady(error ->
        {
            if (disposed || choice != wikiChoiceRevision || page != wikiPageRevision || !token.equals(session)) return;
            if (error != null) { message(error); return; }
            JsonObject preset = original.deepCopy(), setup = SetupCodec.setup(preset); int unresolved = 0;
            for (String key : new String[]{"eq", "inv", "rp", "qv"}) for (JsonElement entry : setup.getAsJsonArray(key)) if (entry != null && !entry.isJsonNull())
            {
                JsonObject item = entry.getAsJsonObject(); String label = WikiParser.defaultName(item); ItemInfo exact = host.defaultItem(label);
                if (exact != null) { item.addProperty("id", exact.id); item.addProperty("label",exact.name); itemCache.put(exact.id, exact); } else unresolved++;
            }
            SetupCodec.rebuildLayout(preset); String previousSession = session; newDraft(preset,true);
            if (!previousSession.equals(session)) message("Wiki draft loaded. " + unresolved + " unresolved slot(s): click '?' to choose an item. Review quantities and Spellbook before export.");
        });
	}
}
