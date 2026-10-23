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
	public interface Host
	{
		String scope();
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
	private final Map<Integer, ItemInfo> itemCache = new HashMap<>();
    private final JTextField catalogueQuery = new JTextField();
    private final DefaultListModel<ItemInfo> catalogueModel = new DefaultListModel<>();
    private final JList<ItemInfo> catalogueList = new JList<>(catalogueModel);
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
	private Timer wikiSuggestTimer, wikiPageTimer;
	private long wikiSearchRevision, wikiPageRevision, wikiChoiceRevision;
	private final JPanel wikiPresetRows = vertical();
	private final JTextArea wikiPageStatus = new JTextArea("Choose a Wiki page to see its setups.", 2, 18);

	public StudioPanel(Host host, Gson gson)
	{
		super(false); this.host = host; this.gson = gson; codec = new SetupCodec(gson);
		setLayout(new BorderLayout(0, 6)); setBackground(BG); setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6)); setPreferredSize(new Dimension(240, 900));
		tabs.setFocusable(false); tabs.setUI(new BasicTabbedPaneUI()
		{
			@Override protected int calculateTabAreaHeight(int placement, int runs, int height) { return 0; }
			@Override protected void paintTabArea(java.awt.Graphics g, int placement, int selected) { }
			@Override protected void paintContentBorder(java.awt.Graphics g, int placement, int selected) { }
			@Override protected Insets getContentBorderInsets(int placement) { return new Insets(0, 0, 0, 0); }
		});
		tabs.addTab("Library", libraryTab()); tabs.addTab("Builder", builderTab()); tabs.addTab("Wiki", scroll(wikiTab())); tabs.addTab("Export", exportTab()); add(tabs, BorderLayout.CENTER);
		JPanel heading = vertical(); JLabel title = text("Loadout Studio"); title.setName("studioTitle"); title.setToolTipText("Loadout Studio 0.6.1-test - character-specific setup library");
        JButton coffee = button("Buy a coffee", () -> attempt(host::openSupportPage)); coffee.setIcon(new SidebarIcon(SidebarIcon.Kind.COFFEE)); coffee.setName("supportCoffee"); coffee.setToolTipText("Buy a coffee"); coffee.getAccessibleContext().setAccessibleName("Buy a coffee"); coffee.getAccessibleContext().setAccessibleDescription(COFFEE_URL); coffee.setPreferredSize(new Dimension(94, 28));
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
            @Override protected Transferable createTransferable(javax.swing.JComponent c) { String value = catalogueDragPayload(); return value == null ? null : new StringSelection(value); }
        });
        if (!java.awt.GraphicsEnvironment.isHeadless()) catalogueList.setDragEnabled(true);
        catalogueList.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) addSelectedItem(); } });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke("control S"), "saveSetup");
        getActionMap().put("saveSetup", new javax.swing.AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { saveDraft(false); } });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke("control F"), "findItems");
        getActionMap().put("findItems", new javax.swing.AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { if (tabs.getSelectedIndex() == 0) search.requestFocusInWindow(); else if (tabs.getSelectedIndex() == 2) wikiQuery.requestFocusInWindow(); else if (draft != null) { tabs.setSelectedIndex(1); expandedPanels.add("Find items"); renderBuilder(); catalogueQuery.requestFocusInWindow(); } } });
		watch(setupName, () -> { if (!rendering) { fieldsToDraft(); markDirty(); } }); reloadScope(); tabs.setSelectedIndex(0);
		filterBank.addActionListener(e -> { if (!rendering) { fieldsToDraft(); markDirty(); syncBankFilter(); } });
		bankLayout.addActionListener(e -> { if (!rendering && draft != null) { editorLayout = (BankLayouts.Mode) bankLayout.getSelectedItem(); rebuildDraftLayout(); markDirty(); renderBuilder(); } });
		bottomToTop.addActionListener(e -> { if (!rendering && draft != null) { editorBottom = bottomToTop.isSelected(); if (editorLayout == BankLayouts.Mode.ZIGZAG) rebuildDraftLayout(); markDirty(); } });
		bankStatus.setEditable(false); bankStatus.setLineWrap(true); bankStatus.setWrapStyleWord(true);
	}
	public void dispose() { disposed = true; host.cancelItemInput(); catalogueTimer.stop(); if (wikiSuggestTimer != null) wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); wikiSearchRevision++; wikiPageRevision++; }
	public void bankMessage(String value) { if (!disposed) bankStatus.setText(value == null ? "Bank filtering failed." : value); }
	private void syncBankFilter()
	{
		if (disposed || draft == null) return; String token = session; fieldsToDraft();
		host.updateBankFilter(draft, scope, value -> { if (token.equals(session)) bankMessage(value); });
	}
	private void rebuildDraftLayout()
	{
		draft.add("layout", BankLayouts.generate(draft, editorLayout, editorBottom)); syncBankFilter();
	}
	private static final class Column extends JPanel implements javax.swing.Scrollable
	{
		Column() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(Component.LEFT_ALIGNMENT); setBackground(BG); }
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
		toggle.addActionListener(e -> { boolean open = !content.isVisible(); content.setVisible(open); if (open) expandedPanels.add(title); else expandedPanels.remove(title); toggle.setText((open ? "- " : "+ ") + title); for (Container ancestor = p; ancestor != null; ancestor = ancestor.getParent()) { ancestor.invalidate(); if (ancestor == this) break; } p.revalidate(); }); p.add(toggle); p.add(content); return p;
	}
	private void updateNavigation() { for (int i = 0; i < navigation.size(); i++) { JButton b = navigation.get(i); boolean active = i == tabs.getSelectedIndex(); b.setForeground(active ? GOLD : TEXT); b.setBackground(active ? new Color(53, 48, 37) : CONTROL); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, active ? GOLD : OUTLINE), BorderFactory.createEmptyBorder(5, 6, 5, 6))); } }
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
		if (component instanceof JTextField || component instanceof JComboBox || component instanceof JList) component.setBackground(CONTROL);
		if (component instanceof JTextField) ((JTextField) component).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
		if (component instanceof JScrollPane) ((JScrollPane) component).setBorder(BorderFactory.createLineBorder(OUTLINE));
		if(component instanceof JComboBox)((JComboBox<?>)component).setRenderer(plainNames());
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
                putClientProperty("html.disable",true);return super.getListCellRendererComponent(list,value,index,selected,focus);
            }
        };
    }
	private static JButton button(String label, Runnable action) { JButton button = new JButton(label); button.putClientProperty("html.disable", true); button.setFont(ACTION_FONT); button.setForeground(TEXT); button.setBackground(CONTROL); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(5, 6, 5, 6))); button.setFocusPainted(true); button.setMargin(new Insets(5, 6, 5, 6)); button.addActionListener(e -> action.run()); return button; }
	private static JPanel row(Component... children) { JPanel p = new JPanel(new GridLayout(0, Math.max(1, Math.min(2, children.length)), 3, 3)); p.setAlignmentX(Component.LEFT_ALIGNMENT); for (Component c : children) p.add(c); p.setMaximumSize(new Dimension(500, p.getPreferredSize().height)); return p; }
	private static JScrollPane scroll(Component c) { JScrollPane pane = new JScrollPane(c); pane.setBorder(null); pane.getVerticalScrollBar().setUnitIncrement(18); pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return pane; }
	private static void watch(JTextField field, Runnable action) { field.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); } }); }
	private void message(String value) { if (!disposed) { status.setText(value == null ? "Could not complete this action." : value); status.setToolTipText(status.getText()); } }
	private static String failureMessage(RuntimeException error) { String detail = error.getMessage(); return detail == null || detail.trim().isEmpty() ? "Could not complete this action (" + error.getClass().getSimpleName() + ")." : detail; }
	private void attempt(Runnable action) { try { action.run(); } catch (RuntimeException e) { message(failureMessage(e)); } }
	public void reloadScope()
	{
		host.cancelItemInput(); host.clearBankFilter(); bankMessage("Bank filter off.");
		if (disposed) return; if (wikiSuggestTimer != null) wikiSuggestTimer.stop(); if (wikiPageTimer != null) wikiPageTimer.stop(); wikiSearchRevision++; wikiPageRevision++; wikiPresetRows.removeAll(); wikiPageStatus.setText("Choose a Wiki page to see its setups."); scope = host.scope(); recoveryOwner=host.recoveryOwner(); recovery=null;existingControls.removeAll(); draft = null; editingId = null; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); code.setText(""); storageError = false;
		try { library = SetupLibrary.read(host.read(scope), gson); } catch (RuntimeException e) { library = new SetupLibrary(); storageError = true; message("Library could not be read. Existing data was not overwritten. Restore a backup before saving."); }
		try{recovery=DraftRecovery.decode(host.readRecovery(scope,recoveryOwner),gson);}catch(RuntimeException ex){message("Recovery data could not be read. It was not overwritten.");}
        renderRecovery();
        scopeLabel.setText(scope == null ? "Local guest library" : "Character-specific library"); renderLibrary(); renderBuilder();
		if (!storageError) message("Ready. Choose a saved setup, capture gear or search Wiki.");
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
            existingControls.add(row(button("Save copies",()->attempt(()->{List<ExistingSetupReader.Entry> selected=choices.getSelectedValuesList();if(selected.isEmpty())throw new IllegalArgumentException("Select one or more setups first.");if(JOptionPane.showConfirmDialog(this,"Copy "+selected.size()+" selected setup(s) into Loadout Studio? Existing entries and other plugins are retained.","Copy setups",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;int count=importCopies(selected);message("Copied "+count+" setup(s); identical copies were skipped.");})),button("Open copy",()->{ExistingSetupReader.Entry selected=choices.getSelectedValue();if(selected!=null)newDraft(selected.portable);})));existingControls.add(button("Close import list",()->{existingControls.removeAll();existingControls.revalidate();}));
            if(result.entries.isEmpty())review.setText("No compatible setups found in this RuneLite profile. Portable JSON/Bank Tag import remains available.");
            if(!result.warnings.isEmpty())existingControls.add(info(String.join("\n",result.warnings),3));themeForm(existingControls);existingControls.revalidate();existingControls.repaint();
        },error->{if(Objects.equals(owner,recoveryOwner)){existingControls.removeAll();existingControls.add(info(error,3));existingControls.revalidate();}});
    }
    int importCopies(List<ExistingSetupReader.Entry> selected)
    {
        if(selected.size()>100)throw new IllegalArgumentException("Copy at most 100 setups at once.");int[] count={0};
        commit(next->{for(ExistingSetupReader.Entry candidate:selected)
        {
            if(next.setups.stream().anyMatch(entry->entry.portable.equals(candidate.portable)))continue;
            SetupLibrary.Entry copy=new SetupLibrary.Entry(candidate.portable);copy.favorite=candidate.favorite;
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
        toolbar.add(libraryTool("New setup", SidebarIcon.Kind.NEW, () -> newDraft(SetupCodec.blank("New loadout"))));
        toolbar.add(libraryTool("Capture gear", SidebarIcon.Kind.CAPTURE, this::capture));
        toolbar.add(libraryTool("Import setup", SidebarIcon.Kind.IMPORT, this::showImportOptions));
        toolbar.add(libraryTool("Sections", SidebarIcon.Kind.SECTIONS, this::manageSections)); controls.add(toolbar);
        JPanel searchBar = new JPanel(new BorderLayout(3, 0)); searchBar.setName("librarySearchBar");
        JLabel searchIcon = text(""); searchIcon.setIcon(new SidebarIcon(SidebarIcon.Kind.SEARCH)); searchBar.add(searchIcon, BorderLayout.WEST);
        search.setName("librarySearch"); search.setToolTipText("Search saved setups"); search.getAccessibleContext().setAccessibleName("Search saved setups"); searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(libraryTool("Clear search", SidebarIcon.Kind.CLEAR, () -> search.setText("")), BorderLayout.EAST); controls.add(searchBar); controls.add(row(favorites, archived));
		favorites.setToolTipText("Show favorite setups only"); archived.setToolTipText("Show archived setups"); watch(search, this::renderLibrary); favorites.addActionListener(e -> renderLibrary()); archived.addActionListener(e -> renderLibrary());
		controls.add(recoveryControls);controls.add(existingControls);
        p.add(controls, BorderLayout.NORTH); p.add(scroll(libraryRows), BorderLayout.CENTER);
        JPanel tools=vertical();tools.add(button("From installed plugins",this::showExisting));tools.add(button("Copy library backup",this::backup));p.add(disclosure("Library tools",tools),BorderLayout.SOUTH);return p;
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
		libraryRows.removeAll(); String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
		List<SetupLibrary.Section> groups = new ArrayList<>(); groups.add(new SetupLibrary.Section("Unfiled")); groups.get(0).id = null; groups.addAll(library.sections);
		for (SetupLibrary.Section group : groups)
		{
			List<SetupLibrary.Entry> records = new ArrayList<>();
			for (SetupLibrary.Entry entry : library.setups) if (Objects.equals(entry.section, group.id) && entry.archived == archived.isSelected() && (!favorites.isSelected() || entry.favorite) && SetupCodec.name(entry.portable).toLowerCase(java.util.Locale.ROOT).contains(query)) records.add(entry);
			records.sort(Comparator.comparing(e -> SetupCodec.name(e.portable)));
			if (records.isEmpty() && (group.id == null || !query.isEmpty() || favorites.isSelected())) continue;
			String groupId = group.id == null ? "" : group.id;
			JButton heading = button((collapsed.contains(groupId) ? "+ " : "- ") + group.name + " (" + records.size() + ")", () -> { if (!collapsed.add(groupId)) collapsed.remove(groupId); renderLibrary(); }); heading.setForeground(GOLD); heading.setAlignmentX(LEFT_ALIGNMENT); libraryRows.add(heading);
			if (collapsed.contains(groupId)) continue;
			for (SetupLibrary.Entry entry : records)
			{
                libraryRows.add(new LibrarySetupRow(entry.id, SetupCodec.name(entry.portable), entry.favorite, entry.archived,
                    () -> open(entry),
                    () -> attempt(() -> commit(l -> l.find(entry.id).favorite = !entry.favorite)),
                    () -> attempt(() -> commit(l -> l.find(entry.id).archived = !entry.archived))));
			}
		}
		if (libraryRows.getComponentCount() == 0) libraryRows.add(info("No saved setups yet. Capture gear, create a setup or import one.", 3)); themeForm(libraryRows); libraryRows.revalidate(); libraryRows.repaint();
	}
	private boolean discard()
	{
		if(!dirty)return true;
        if(JOptionPane.showConfirmDialog(this,"Discard the unsaved changes in the Builder?","Unsaved setup",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return false;
        clearRecovery();return true;
	}
	void open(SetupLibrary.Entry entry)
	{
		if (!discard()) return; host.cancelItemInput(); draft = entry.portable.deepCopy(); editorLayout = entry.bankLayout == null ? BankLayouts.infer(draft) : entry.bankLayout; editorBottom = entry.bankLayout == null ? BankLayouts.inferBottom(draft) : entry.zigzagBottomToTop; editingId = entry.id; editorSection = entry.section; editorFavorite = entry.favorite; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); message("Opened saved setup. Changes are saved only when you choose Save."); resolveDraftDefaults();
	}
	void newDraft(JsonObject p) { newDraft(p,false); }
    private void newDraft(JsonObject p,boolean defaultsResolved)
	{
		if (!discard()) return; host.cancelItemInput(); draft = p.deepCopy(); editorLayout = BankLayouts.infer(draft); editorBottom = BankLayouts.inferBottom(draft); editingId = null; editorSection = null; editorFavorite = false; dirty = true; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); persistRecovery();message("New draft. Review it, then Save to your library."); if(!defaultsResolved)resolveDraftDefaults();
	}
    private void resolveDraftDefaults()
    {
        boolean needed=false;for(String key:new String[]{"eq","inv","rp","qv"})for(JsonElement e:SetupCodec.setup(draft).getAsJsonArray(key))if(SetupCodec.id(e)==0)needed=true;
        if(!needed)return;String token=session;JsonObject current=draft;
        host.whenItemsReady(error->{
            if(disposed||error!=null||!token.equals(session)||current!=draft)return;
            int resolved=0;for(String key:new String[]{"eq","inv","rp","qv"})for(JsonElement entry:SetupCodec.setup(draft).getAsJsonArray(key))if(SetupCodec.id(entry)==0){
                JsonObject item=entry.getAsJsonObject();ItemInfo first=host.defaultItem(WikiParser.defaultName(item));
                if(first!=null){item.addProperty("id",first.id);item.addProperty("label",first.name);itemCache.put(first.id,first);resolved++;}
            }
            if(resolved>0){SetupCodec.rebuildLayout(draft);markDirty();renderBuilder();syncBankFilter();message("Applied first choices to "+resolved+" slot(s). Review and Save to keep them.");}
        });
    }
	private void capture()
	{
		if (!discard()) return; String capturedScope = scope;
		host.capture(p -> { if (!disposed && Objects.equals(scope, capturedScope)) { dirty = false; newDraft(p); } }, this::message);
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
		if (rendering || draft == null) return; dirty = true; draftLabel.setText("Unsaved changes"); setupName.setToolTipText(setupName.getText() + " - Unsaved changes (Ctrl+S to save)"); code.setText(""); fieldsToDraft();persistRecovery();
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
			builder.removeAll(); builderTools.removeAll(); exportCopy.setEnabled(draft != null); exportPreview.setEnabled(draft != null); if (draft == null) { builder.add(card("Build a loadout", info("Capture your gear, create a blank setup or choose a Wiki preset.", 3), button("Capture gear", this::capture), button("New setup", () -> newDraft(SetupCodec.blank("New loadout"))), button("Find Wiki content", () -> tabs.setSelectedIndex(2)))); builder.revalidate(); builder.repaint(); return; }
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

            JPanel inventory = itemGrid(7, 4); inventory.setName("inventoryGrid");
            for (int i = 0; i < 28; i++) inventory.add(slot("inv", i)); builder.add(loadoutSection("Inventory", inventory));
            JPanel runes = itemGrid(1, 4); for (int i = 0; i < 4; i++) runes.add(slot("rp", i)); builder.add(loadoutSection("Rune Pouch", runes));

            JPanel equipment = new JPanel(new GridBagLayout()); equipment.setName("equipmentGrid");
            int[][] positions = {{0,1,0},{1,0,1},{2,1,1},{13,2,1},{3,0,2},{4,1,2},{5,2,2},{7,1,3},{9,0,4},{10,1,4},{12,2,4}};
            for (int[] position : positions) { GridBagConstraints c = new GridBagConstraints(); c.gridx = position[1]; c.gridy = position[2]; c.insets = new Insets(0, 0, 0, 0); equipment.add(slot("eq", position[0]), c); }
            equipment.setPreferredSize(new Dimension(132, 220)); builder.add(loadoutSection("Equipment", equipment));
            spellbook.setBorder(differences.contains("spellbook")?BorderFactory.createLineBorder(RED,2):javax.swing.UIManager.getBorder("ComboBox.border"));spellbook.setPreferredSize(new Dimension(132, 28)); builder.add(loadoutSection("Spellbook", spellbook));
            JPanel quiver = itemGrid(1, 1); quiver.add(slot("qv", 0)); builder.add(loadoutSection("Quiver", quiver));

            int extraCount = Math.max(4, ((s.getAsJsonObject("afi").size() + 3) / 4) * 4);
            JPanel extras = itemGrid(extraCount / 4, 4); for (int i = 0; i < extraCount; i++) extras.add(slot("afi", i)); builder.add(loadoutSection("Additional filtered items", extras));
            notes.setName("loadoutNotes"); notes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); builder.add(loadoutSection("Notes", notes));

            JPanel loadoutOptions = vertical(); loadoutOptions.add(button("Max pouch runes (16k)", () -> { for (JsonElement e : s.getAsJsonArray("rp")) if (!e.isJsonNull()) e.getAsJsonObject().addProperty("q", 16000); markDirty(); renderBuilder(); })); loadoutOptions.add(ignoreAmmo); loadoutOptions.add(button("Add filtered item", () -> editSlot("afi", SetupCodec.setup(draft).getAsJsonObject("afi").size()))); builder.add(disclosure("Loadout options", loadoutOptions));
            JPanel bankOptions = vertical(); bankOptions.add(filterBank); bankOptions.add(bankLayout); bankOptions.add(bottomToTop); bankOptions.add(row(button("Apply filter", () -> { filterBank.setSelected(true); fieldsToDraft(); markDirty(); syncBankFilter(); }), button("Clear filter", () -> { filterBank.setSelected(false); fieldsToDraft(); markDirty(); syncBankFilter(); }))); bankStatus.setRows(2); bankStatus.setMaximumSize(new Dimension(500, 54)); bankOptions.add(bankStatus); builder.add(disclosure("Bank filtering and layout", bankOptions));
            JPanel details = vertical(); details.add(text("Section")); details.add(section); details.add(favorite); builder.add(disclosure("Setup details", details));
            JPanel actions = vertical(); actions.add(row(button("Save copy", () -> saveDraft(true)), button("Compare gear", this::compareGear))); actions.add(button("Export setup", () -> { fieldsToDraft(); tabs.setSelectedIndex(3); refreshCode(); })); builder.add(disclosure("Setup actions", actions));
		}
		finally { rendering = false; themeForm(builderTools); themeForm(builder); builderTools.revalidate(); builderTools.repaint(); builder.revalidate(); builder.repaint(); if (viewport != null) SwingUtilities.invokeLater(() -> { if (!disposed && renderToken.equals(session)) viewport.setViewPosition(new java.awt.Point(0, Math.min(viewportPosition.y, Math.max(0, builder.getPreferredSize().height - viewport.getExtentSize().height)))); }); }
	}
    private void addResolutionGuide()
    {
        int remaining=0;String targetKey=null;int targetIndex=0;JsonObject target=null;
        for(String key:new String[]{"eq","inv","rp","qv"})
        {
            JsonArray items=SetupCodec.setup(draft).getAsJsonArray(key);
            for(int i=0;i<items.size();i++)if(!items.get(i).isJsonNull())
            {
                JsonObject item=items.get(i).getAsJsonObject();if(SetupCodec.id(item)==0 || item.has("q")&&item.get("q").isJsonNull())
                {remaining++;if(target==null){target=item;targetKey=key;targetIndex=i;}}
            }
        }
        if(target==null)return;
        JPanel guide=vertical();guide.setName("resolutionGuide");String key=targetKey;int index=targetIndex;JsonObject item=target;
        String label=item.has("label")?item.get("label").getAsString():key+" slot "+(index+1);
        guide.add(info(remaining+" choice(s) needed · "+label,2));
        if(item.has("choices") && item.get("choices").isJsonArray())
        {
            JComboBox<String> alternatives=new JComboBox<>();alternatives.addItem("Choose an alternative…");for(JsonElement option:item.getAsJsonArray("choices"))alternatives.addItem(option.getAsString());alternatives.setName("wikiAlternatives");guide.add(alternatives);
            alternatives.addActionListener(e->{if(alternatives.getSelectedIndex()<1)return;String chosen=(String)alternatives.getSelectedItem();ItemInfo exact=host.defaultItem(chosen);if(exact==null){message("Choose the exact item/variant using Item Search. Source conditions remain in Notes.");editSlot(key,index);return;}itemCache.put(exact.id,exact);attempt(()->{JsonObject replacement=item.deepCopy();replacement.addProperty("id",exact.id);replacement.addProperty("label",exact.name);validateSlot(key,index,replacement);put(key,index,replacement);changedSlot();});});
        }
        guide.add(button(SetupCodec.id(item)==0?"Choose item in chat":"Enter missing quantity",()->{if(SetupCodec.id(item)==0)editSlot(key,index);else editQuantity(key,index);}));builderTools.add(guide);
    }
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
		JsonArray a = s.getAsJsonArray(key); return index < a.size() ? a.get(index) : JsonNull.INSTANCE;
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
		JsonElement entry = get(key, index); int id = SetupCodec.id(entry); String empty = ""; String container = key.equals("eq") ? EQUIPMENT[index] : key.equals("inv") ? "Inventory" : key.equals("rp") ? "Rune Pouch" : key.equals("qv") ? "Quiver" : "Additional filtered items";
		LoadoutSlotButton button = new LoadoutSlotButton(id > 0 ? String.valueOf(id) : id == 0 ? "?" : empty); button.addActionListener(e -> editSlot(key, index)); button.setName("itemSlot"); button.putClientProperty("slotContainer", key); button.putClientProperty("slotIndex", index); button.setPreferredSize(new Dimension(44, 44)); button.setMinimumSize(new Dimension(44, 44)); button.setBackground(SLOT); button.setFont(FontManager.getRunescapeSmallFont()); button.setMargin(new Insets(1, 1, 1, 1)); button.setForeground(GOLD);
		button.setBorder(BorderFactory.createLineBorder(differences.contains(key + ":" + index) ? RED : BG, differences.contains(key + ":" + index) ? 2 : 1));
        button.setRolloverEnabled(true); button.setContentAreaFilled(true);
        button.getModel().addChangeListener(e -> button.setBorder(BorderFactory.createLineBorder(differences.contains(key + ":" + index) ? RED : button.getModel().isRollover() ? OUTLINE : BG)));
        button.getAccessibleContext().setAccessibleName((container + " slot " + (index + 1)) + (id < 0 ? " - empty" : " - edit item"));
        if (id < 0) button.setToolTipText("Empty " + container + " slot - click to add an item");
		if (id == 0) button.setToolTipText((entry.getAsJsonObject().has("label")?entry.getAsJsonObject().get("label").getAsString():"Unresolved item") + " · choose an exact Item ID");
		if (id > 0) host.describe(id, info -> { if (disposed) return; itemCache.put(id, info); button.itemIcon(info.icon); button.setText(info.icon == null ? "!" : ""); button.amount(SetupCodec.unlimited(entry) ? "∞" : key.equals("rp") && SetupCodec.quantity(entry)==0 ? "?" : SetupCodec.quantity(entry) > 1 || key.equals("rp") ? compact(SetupCodec.quantity(entry)) : ""); button.setToolTipText(info.name + " · " + id + " · quantity " + (SetupCodec.unlimited(entry) ? "unlimited (quantity ignored)" : SetupCodec.quantity(entry) == 0 ? "unknown" : SetupCodec.quantity(entry))); });
        button.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(javax.swing.JComponent c) { return MOVE; }
            @Override protected Transferable createTransferable(javax.swing.JComponent c) { String value = dragPayload(key, index); return value == null ? null : new StringSelection(value); }
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
        button.addMouseListener(drag); button.addMouseMotionListener(drag); button.setComponentPopupMenu(slotMenu(key,index)); return button;
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
        int length = key.equals("eq") ? 14 : key.equals("inv") ? 28 : key.equals("rp") ? 4 : key.equals("qv") ? 1 : 0;
        return index < length && !(key.equals("eq") && (index == 6 || index == 8 || index == 11));
    }
    String catalogueDragPayload() { ItemInfo item = catalogueList.getSelectedValue(); return item == null ? null : "loadout-item:" + session + ":" + item.id; }
    String dragPayload(String key, int index) { return validSlot(key, index) && !get(key, index).isJsonNull() ? "loadout-slot:" + session + ":" + key + ":" + index : null; }
    private boolean canDrop(String value, String key, int index)
    {
        if (disposed || !validSlot(key, index) || value == null) return false;
        String[] parts = value.split(":"); if (parts.length < 3 || !parts[1].equals(session)) return false;
        if (parts[0].equals("loadout-item") && parts.length == 3) { int id = Integer.parseInt(parts[2]); for (int i = 0; i < catalogueModel.size(); i++) if (catalogueModel.get(i).id == id) return true; return false; }
        if (!parts[0].equals("loadout-slot") || parts.length != 4 || !validSlot(parts[2], Integer.parseInt(parts[3]))) return false;
        JsonElement item = get(parts[2], Integer.parseInt(parts[3])); if (item.isJsonNull()) return false;
        validateSlot(key, index, item); validateSlot(parts[2], Integer.parseInt(parts[3]), get(key, index)); return true;
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
            JsonElement a = get(from, old).deepCopy(), b = get(key, index).deepCopy();
            if (from.equals("afi") && key.equals("afi"))
            {
                List<JsonElement> items = new ArrayList<>(); for (Map.Entry<String, JsonElement> entry : SetupCodec.setup(draft).getAsJsonObject("afi").entrySet()) items.add(entry.getValue()); while (items.size() <= Math.max(old, index)) items.add(JsonNull.INSTANCE); items.set(old, b); items.set(index, a);
                JsonObject extras = new JsonObject(); for (JsonElement item : items) if (!item.isJsonNull()) extras.add(String.valueOf(SetupCodec.id(item)), item); SetupCodec.setup(draft).add("afi", extras);
            }
            else { checkExtraDuplicate(key, index, a); checkExtraDuplicate(from, old, b); fieldsToDraft(); put(from, old, b); put(key, index, a); }
            rebuildDraftLayout(); markDirty(); renderBuilder(); message("Moved item. Choose Save to keep your changes."); return true;
        }
        catch (RuntimeException ex) { message(failureMessage(ex)); return false; }
    }

	private static String compact(int q) { return q >= 1000 && q % 1000 == 0 ? q / 1000 + "k" : String.valueOf(q); }
	private void validateSlot(String key, int index, JsonElement e)
	{
		if (e == null || e.isJsonNull()) return; ItemInfo info = itemCache.get(SetupCodec.id(e));
        if (!key.equals("inv") && info == null) throw new IllegalArgumentException("Item details are still loading. Choose an exact Item ID or try again.");
		if (key.equals("rp")) { if (info != null && !info.name.toLowerCase(java.util.Locale.ROOT).endsWith(" rune")) throw new IllegalArgumentException("Rune pouch accepts runes only."); if (SetupCodec.quantity(e) > 16000) throw new IllegalArgumentException("Rune pouch maximum is 16k."); }
		if (key.equals("qv") && info != null && !info.name.toLowerCase(java.util.Locale.ROOT).matches(".*(arrow|bolt)s?(?: \\(.*\\))?$")) throw new IllegalArgumentException("Quiver accepts arrows or bolts.");
		if (key.equals("eq") && info != null && info.slot != index) throw new IllegalArgumentException("This item belongs in a different Equipment slot.");
	}
    private javax.swing.JPopupMenu slotMenu(String key,int index)
    {
        javax.swing.JPopupMenu menu=new javax.swing.JPopupMenu(); JsonElement current=get(key,index);
        javax.swing.JMenuItem search=new javax.swing.JMenuItem("Update Slot from Search"); search.addActionListener(e->editSlot(key,index)); menu.add(search);
        if(!key.equals("afi")) { javax.swing.JMenuItem capture=new javax.swing.JMenuItem("Update Slot from " + (key.equals("eq") ? "Equipment" : key.equals("inv") ? "Inventory" : key.equals("rp") ? "Rune Pouch" : "Quiver")); capture.addActionListener(e->updateSlotFromContainer(key,index)); menu.add(capture); }
        javax.swing.JMenuItem quantity=new javax.swing.JMenuItem("Set quantity"); quantity.setEnabled(!current.isJsonNull() && SetupCodec.id(current)>0); quantity.addActionListener(e->editQuantity(key,index)); menu.add(quantity);
        javax.swing.JMenuItem fuzzy=new javax.swing.JMenuItem("Toggle Fuzzy"); fuzzy.setEnabled(!current.isJsonNull()); fuzzy.addActionListener(e->attempt(()->{ JsonObject item=get(key,index).getAsJsonObject(); item.addProperty("f",!item.has("f") || !item.get("f").getAsBoolean()); changedSlot(); })); menu.add(fuzzy);
        javax.swing.JMenu stack=new javax.swing.JMenu("Stack comparison"); stack.setEnabled(!current.isJsonNull() && !SetupCodec.unlimited(current));
        for(String value:new String[]{"None","Standard","Less_Than","Greater_Than"}) { javax.swing.JMenuItem option=new javax.swing.JMenuItem(value.replace('_',' ')); option.addActionListener(e->attempt(()->{ get(key,index).getAsJsonObject().addProperty("sc",value); changedSlot(); })); stack.add(option); } menu.add(stack);
        javax.swing.JMenuItem remove=new javax.swing.JMenuItem("Remove Item from Slot"); remove.setEnabled(!current.isJsonNull()); remove.addActionListener(e->{ put(key,index,JsonNull.INSTANCE); changedSlot(); }); menu.add(remove); return menu;
    }
    private void changedSlot() { fieldsToDraft(); rebuildDraftLayout(); markDirty(); renderBuilder(); }
    private void editSlot(String key,int index)
    {
        if(draft==null) return; fieldsToDraft(); String token=session; JsonElement before=get(key,index).deepCopy(); message("Choose an item in the RuneLite chatbox.");
        host.chooseItem(id->{ if(disposed || !token.equals(session) || id<1 || !before.equals(get(key,index))) return;
            host.describe(id,info->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; itemCache.put(id,info);
                if(info.stackable || key.equals("rp") || key.equals("qv")) host.enterQuantity(amountText(before),value->{ if(token.equals(session) && before.equals(get(key,index))) applySlotItem(key,index,id,value); },this::message);
                else applySlotItem(key,index,id,"1");
            });
        },this::message);
    }
    private static String amountText(JsonElement item)
    {
        return item.isJsonNull()?"1":item.getAsJsonObject().has("q")&&item.getAsJsonObject().get("q").isJsonNull()?"":String.valueOf(SetupCodec.quantity(item));
    }
    private void editQuantity(String key,int index)
    {
        JsonElement before=get(key,index).deepCopy(); String token=session; if(before.isJsonNull()) return;
        host.enterQuantity(amountText(before),value->{ if(!disposed && token.equals(session) && before.equals(get(key,index))) applySlotItem(key,index,SetupCodec.id(before),value); },this::message);
    }
    private void applySlotItem(String key,int index,int id,String amount)
    {
        attempt(()->{ int quantity=amount.trim().equals("0") ? 0 : SetupCodec.parseQuantity(amount);
            ItemInfo info=itemCache.get(id); if(info==null) throw new IllegalArgumentException("Item details are still loading."); if(!info.stackable && quantity>1) throw new IllegalArgumentException("Non-stackable items need separate slots.");
            JsonElement previous=get(key,index); JsonObject item=SetupCodec.item(id,quantity);
            if(!previous.isJsonNull()) for(String field:new String[]{"f","sc"}) if(previous.getAsJsonObject().has(field)) item.add(field,previous.getAsJsonObject().get(field).deepCopy());
            if(quantity==0) item.addProperty("sc","None"); validateSlot(key,index,item); checkExtraDuplicate(key,index,item); put(key,index,item); changedSlot(); message(quantity==0 ? "Unlimited: quantity matching is disabled for this slot." : "Slot updated.");
        });
    }
    private void updateSlotFromContainer(String key,int index)
    {
        String token=session; JsonElement before=get(key,index).deepCopy();
        host.capture(actual->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; JsonElement item=SetupCodec.setup(actual).getAsJsonArray(key).get(index).deepCopy();
            if(item.isJsonNull()) { put(key,index,JsonNull.INSTANCE); changedSlot(); return; }
            host.describe(SetupCodec.id(item),info->{ if(disposed || !token.equals(session) || !before.equals(get(key,index))) return; itemCache.put(info.id,info); attempt(()->{validateSlot(key,index,item);put(key,index,item);changedSlot();}); });
        },this::message);
    }

	private void saveDraft(boolean copy)
	{
		if (draft == null) return;
		attempt(() -> { fieldsToDraft(); SetupCodec.validate(draft, false); String chosenSection = sectionIds.get(Math.max(0, section.getSelectedIndex())); boolean chosenFavorite = favorite.isSelected(); String id = copy || editingId == null ? UUID.randomUUID().toString() : editingId;
			commit(l -> { SetupLibrary.Entry record = l.find(id); if (record == null) { record = new SetupLibrary.Entry(draft); record.id = id; l.setups.add(record); } record.portable = draft.deepCopy(); record.section = chosenSection; record.favorite = chosenFavorite; record.bankLayout = editorLayout; record.zigzagBottomToTop = editorBottom; }); editingId = id; dirty = false; clearRecovery(); draftLabel.setText("Saved setup"); setupName.setToolTipText(setupName.getText() + " - Saved setup"); message("Saved to this character/profile library."); });
	}
	private void compareGear()
	{
		if (draft == null) return; fieldsToDraft(); String token = session;
        host.compareSetup(draft.deepCopy(),ignoreAmmo.isSelected(),result->{if(disposed || !token.equals(session))return;differences.clear();differences.addAll(result);renderBuilder();message(result.size()+" difference(s). Fuzzy variants, Stack comparison, unordered Inventory and Ammo preferences are applied.");},this::message);
	}
	private JPanel exportTab()
	{
		JPanel p = new JPanel(new BorderLayout(0, 8)), controls = vertical(); controls.add(text("Export the open setup")); controls.add(format);conversionNotice.setName("conversionNotice");controls.add(conversionNotice); exportPreview = button("Show code", this::refreshCode); exportCopy = button("Copy code", () -> attempt(() -> { refreshCodeOrThrow(); getToolkit().getSystemClipboard().setContents(new StringSelection(code.getText()), null); message("Copied " + format.getSelectedItem() + "."); })); controls.add(row(exportPreview, exportCopy));
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
		JTextArea input = new JTextArea(12, 34); int action = JOptionPane.showConfirmDialog(this, new JScrollPane(input), "Paste Inventory Setup JSON, Bank Tag or library backup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE); if (action != JOptionPane.OK_OPTION) return;
		attempt(() ->
		{
			String text = input.getText().trim(); JsonElement parsed = text.startsWith("{") ? codec.parse(text) : null;
			if (parsed != null && parsed.isJsonObject() && parsed.getAsJsonObject().has("sections") && parsed.getAsJsonObject().has("setups"))
			{
				SetupLibrary restored = SetupLibrary.read(text, gson); if (JOptionPane.showConfirmDialog(this, "Replace this plugin's current library with " + restored.setups.size() + " saved setups? Export a backup first if needed.", "Restore library backup", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
				host.save(scope, restored.serialize(gson)); reloadScope(); message("Library backup restored."); return;
			}
			List<JsonObject> imported=codec.importText(text);boolean bankImport=!(text.startsWith("{")||text.startsWith("[")) || parsed!=null&&parsed.isJsonObject()&&parsed.getAsJsonObject().has("bankTag");
            JTextArea review=info("Add "+imported.size()+" setup(s)? Existing saved setups are retained.\n"+ConversionReview.imported(imported.get(0),bankImport?"Bank Tag":"Inventory Setup"),9);
            if(JOptionPane.showConfirmDialog(this,review,"Review import",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
			commit(l -> { for (JsonObject p : imported) l.setups.add(new SetupLibrary.Entry(p)); }); message("Imported " + imported.size() + " setup(s). Open them from Library."); renderLibrary();
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
		DefaultListModel<String> matches = new DefaultListModel<>(); JList<String> pages = new JList<>(matches); pages.setName("wikiSuggestions"); pages.putClientProperty("html.disable", true); pages.setVisibleRowCount(4); pages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); JCheckBox strategies = new JCheckBox("Use Strategies subpage", true);
		p.add(text("Find content")); p.add(query);
		JTextArea searchHint = info("Press Enter or Search Wiki to request suggestions. No background polling.", 2); p.add(searchHint);
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
				if (error != null) { searchHint.setText("Search failed. Try again."); message(error); }
				else { for (String name : titles) matches.addElement(name); searchHint.setText(titles.isEmpty() ? "No pages found. Try another name." : "Select a page to load its setups."); }
			});
		});
		wikiSuggestTimer = new Timer(250, e ->
        {
            String term=query.getText().trim(),owner=recoveryOwner;long revision=wikiSearchRevision;
            host.localWikiSuggestions(term,titles->{if(disposed || revision!=wikiSearchRevision || !Objects.equals(owner,recoveryOwner))return;matches.clear();for(String name:titles)matches.addElement(name);searchHint.setText(titles.isEmpty()?"Press Enter or Search Wiki to load suggestions.":"Cached suggestions · Search Wiki finds more.");});
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
		query.addActionListener(e -> find.doClick()); p.add(find); JScrollPane pageList = new JScrollPane(pages); pageList.setPreferredSize(new Dimension(210, 96)); pageList.setMaximumSize(new Dimension(500, 110)); p.add(pageList); p.add(strategies);
		wikiPageStatus.setRows(4); wikiPageStatus.setMaximumSize(new Dimension(500, 78)); wikiPageStatus.setName("wikiPageStatus"); wikiPageStatus.setEditable(false); wikiPageStatus.setLineWrap(true); wikiPageStatus.setWrapStyleWord(true);
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
				host.fetchWikiPage(requested, strategies.isSelected(), force, (loaded, error) ->
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
		pages.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectPage.run(); }); strategies.addActionListener(e -> selectPage.run());
		url.addActionListener(e -> fetchPage.run()); p.add(row(button("Use cache", fetchPage),button("Refresh Wiki",()->{if(JOptionPane.showConfirmDialog(this,"Fetch a fresh copy from OSRS Wiki? Cached pages already work without a new request.","Refresh Wiki source",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){refreshRequested[0]=true;fetchPage.run();}})));
		JPanel advanced = vertical(); advanced.setVisible(false);
		p.add(button("URL / pasted wikitext", () -> { advanced.setVisible(!advanced.isVisible()); p.revalidate(); })); p.add(advanced);
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
			choose.setName("wikiPreset:" + variant); choose.setMaximumSize(new Dimension(500, 44)); choose.setToolTipText("Open " + variant + " in Builder"); wikiPresetRows.add(choose);
		}
		wikiPageStatus.setText(presets.size() + " setup(s) found. Choose one below."); wikiPresetRows.invalidate(); if (wikiPresetRows.getParent() != null) wikiPresetRows.getParent().invalidate(); wikiPresetRows.revalidate(); wikiPresetRows.repaint();
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
