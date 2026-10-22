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
import java.awt.Dimension;
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
import javax.swing.JDialog;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public final class StudioPanel extends PluginPanel
{
	public interface Host
	{
		String scope();
		String read(String scope);
		void save(String scope, String text);
		void capture(Consumer<JsonObject> success, Consumer<String> error);
		void describe(int id, Consumer<ItemInfo> callback);
		List<ItemInfo> search(String query);
		String catalogueStatus();
		boolean wikiEnabled();
		void enableWiki(boolean enabled);
		void fetchWiki(String url, BiConsumer<String, String> callback);
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
	private static final Color GOLD = new Color(235, 190, 85), BG = new Color(37, 36, 32), SLOT = new Color(56, 52, 43), RED = new Color(195, 92, 72);
	private final Host host;
	private final Gson gson;
	private final SetupCodec codec;
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel libraryRows = vertical(), builder = vertical();
	private final JTextField search = new JTextField(), setupName = new JTextField();
	private final JCheckBox favorites = new JCheckBox("Favorites only"), archived = new JCheckBox("Show archived"), favorite = new JCheckBox("Favorite"), ignoreAmmo = new JCheckBox("Ignore Ammo quantities");
	private final JComboBox<String> spellbook = new JComboBox<>(new String[]{"Standard", "Ancient", "Lunar", "Arceuus", "None"});
	private final JCheckBox filterBank = new JCheckBox("Filter Bank"), bottomToTop = new JCheckBox("Zigzag: bottom row first");
	private final JComboBox<BankLayouts.Mode> bankLayout = new JComboBox<>(BankLayouts.Mode.values());
	private final JTextArea bankStatus = new JTextArea(3, 18);
	private BankLayouts.Mode editorLayout = BankLayouts.Mode.PRESET;
	private boolean editorBottom;
	private final JComboBox<String> section = new JComboBox<>(), format = new JComboBox<>(new String[]{"Inventory Setup JSON", "Bank Tag (RuneLite)", "Bank Tag Layouts"});
	private final JTextArea notes = new JTextArea(4, 18), code = new JTextArea(9, 18), status = new JTextArea(3, 18);
	private final JLabel scopeLabel = text(""), draftLabel = text("No setup open");
	private final Map<Integer, ItemInfo> itemCache = new HashMap<>();
	private final Set<String> collapsed = new HashSet<>(), differences = new HashSet<>();
	private final List<String> sectionIds = new ArrayList<>();
	private SetupLibrary library = new SetupLibrary();
	private JsonObject draft;
	private String editingId, scope, session = UUID.randomUUID().toString();
	private String editorSection;
	private boolean editorFavorite;
	private boolean dirty, rendering, disposed, storageError;

	public StudioPanel(Host host, Gson gson)
	{
		super(false); this.host = host; this.gson = gson; codec = new SetupCodec(gson);
		setLayout(new BorderLayout(0, 6)); setBackground(BG); setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6)); setPreferredSize(new Dimension(240, 900));
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabs.setFont(tabs.getFont().deriveFont(10f));
		JPanel heading = vertical(); JLabel title = text("LOADOUT STUDIO · TEST"); title.setForeground(GOLD); heading.add(title); heading.add(scopeLabel); add(heading, BorderLayout.NORTH);
		tabs.addTab("Library", libraryTab()); tabs.addTab("Builder", scroll(builder)); tabs.addTab("Wiki", scroll(wikiTab())); tabs.addTab("Export", exportTab()); add(tabs, BorderLayout.CENTER);
		status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true); status.setBackground(BG); status.setForeground(GOLD); status.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2)); add(status, BorderLayout.SOUTH);
		watch(setupName, () -> { if (!rendering) { fieldsToDraft(); markDirty(); } }); reloadScope();
		filterBank.addActionListener(e -> { if (!rendering) { fieldsToDraft(); markDirty(); syncBankFilter(); } });
		bankLayout.addActionListener(e -> { if (!rendering && draft != null) { editorLayout = (BankLayouts.Mode) bankLayout.getSelectedItem(); rebuildDraftLayout(); markDirty(); renderBuilder(); } });
		bottomToTop.addActionListener(e -> { if (!rendering && draft != null) { editorBottom = bottomToTop.isSelected(); if (editorLayout == BankLayouts.Mode.ZIGZAG) rebuildDraftLayout(); markDirty(); } });
		bankStatus.setEditable(false); bankStatus.setLineWrap(true); bankStatus.setWrapStyleWord(true);
	}
	public void dispose() { disposed = true; }
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
		Column() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(Component.LEFT_ALIGNMENT); }
		@Override protected void addImpl(Component child, Object constraints, int index)
		{
			if (child instanceof javax.swing.JComponent) ((javax.swing.JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);
			if (child instanceof JTextField || child instanceof JComboBox) child.setMaximumSize(new Dimension(500, 28));
			super.addImpl(child, constraints, index);
		}
		@Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(220, 600); }
		@Override public boolean getScrollableTracksViewportWidth() { return true; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
		@Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) { return 18; }
		@Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) { return Math.max(18, r.height - 18); }
	}
	private static JPanel vertical() { return new Column(); }
	private static JLabel text(String text) { JLabel label = new JLabel(text); label.putClientProperty("html.disable", true); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
	private static JButton button(String label, Runnable action) { JButton button = new JButton(label); button.putClientProperty("html.disable", true); button.setMargin(new Insets(5, 6, 5, 6)); button.addActionListener(e -> action.run()); return button; }
	private static JPanel row(Component... children) { JPanel p = new JPanel(new GridLayout(0, Math.max(1, Math.min(2, children.length)), 3, 3)); p.setAlignmentX(Component.LEFT_ALIGNMENT); for (Component c : children) p.add(c); p.setMaximumSize(new Dimension(500, p.getPreferredSize().height)); return p; }
	private static JScrollPane scroll(Component c) { JScrollPane pane = new JScrollPane(c); pane.setBorder(null); pane.getVerticalScrollBar().setUnitIncrement(18); pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return pane; }
	private static void watch(JTextField field, Runnable action) { field.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); } }); }
	private void message(String value) { if (!disposed) status.setText(value == null ? "Operation failed." : value); }
	private void attempt(Runnable action) { try { action.run(); } catch (RuntimeException e) { message(e.getMessage()); } }
	public void reloadScope()
	{
		host.clearBankFilter(); bankMessage("Bank filter off.");
		if (disposed) return; scope = host.scope(); draft = null; editingId = null; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); code.setText(""); storageError = false;
		try { library = SetupLibrary.read(host.read(scope), gson); } catch (RuntimeException e) { library = new SetupLibrary(); storageError = true; message("Library could not be read. Existing data was not overwritten. Restore a backup before saving."); }
		scopeLabel.setText(scope == null ? "Local guest library" : "Character-specific library"); renderLibrary(); renderBuilder();
		if (!storageError) message("Test build: saves use this plugin's own RuneLite profile data. No automatic equipping or withdrawals.");
	}
	private void commit(Consumer<SetupLibrary> change)
	{
		if (storageError) throw new IllegalStateException("Restore a valid library backup before saving.");
		SetupLibrary next = SetupLibrary.read(library.serialize(gson), gson); change.accept(next); String json = next.serialize(gson); host.save(scope, json); library = next; renderLibrary();
	}
	private JPanel libraryTab()
	{
		JPanel p = vertical(); p.add(row(button("New", () -> newDraft(SetupCodec.blank("New loadout"))), button("Capture gear", this::capture), button("Import", this::importDialog)));
		p.add(row(button("Sections", this::manageSections), button("Backup", this::backup)));
		search.setToolTipText("Search saved setup names"); p.add(search); p.add(row(favorites, archived)); watch(search, this::renderLibrary); favorites.addActionListener(e -> renderLibrary()); archived.addActionListener(e -> renderLibrary()); p.add(scroll(libraryRows)); return p;
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
			JButton heading = button((collapsed.contains(groupId) ? "▸ " : "▾ ") + group.name + " (" + records.size() + ")", () -> { if (!collapsed.add(groupId)) collapsed.remove(groupId); renderLibrary(); }); heading.setForeground(GOLD); heading.setAlignmentX(LEFT_ALIGNMENT); libraryRows.add(heading);
			if (collapsed.contains(groupId)) continue;
			for (SetupLibrary.Entry entry : records)
			{
				JPanel card = vertical(); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 4, 5, 0), BorderFactory.createLineBorder(new Color(77, 74, 63))));
				JButton open = button(SetupCodec.name(entry.portable), () -> open(entry)); open.setHorizontalAlignment(JButton.LEFT); card.add(open);
				card.add(row(button(entry.favorite ? "★" : "☆", () -> attempt(() -> commit(l -> l.find(entry.id).favorite = !entry.favorite))), button("Open", () -> open(entry)), button(entry.archived ? "Restore" : "Archive", () -> attempt(() -> commit(l -> l.find(entry.id).archived = !entry.archived))))); libraryRows.add(card);
			}
		}
		if (libraryRows.getComponentCount() == 0) libraryRows.add(text("No saved setups. Create or import one.")); libraryRows.revalidate(); libraryRows.repaint();
	}
	private boolean discard()
	{
		return !dirty || JOptionPane.showConfirmDialog(this, "Discard the unsaved changes in the Builder?", "Unsaved setup", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}
	void open(SetupLibrary.Entry entry)
	{
		if (!discard()) return; draft = entry.portable.deepCopy(); editorLayout = entry.bankLayout == null ? BankLayouts.infer(draft) : entry.bankLayout; editorBottom = entry.bankLayout == null ? BankLayouts.inferBottom(draft) : entry.zigzagBottomToTop; editingId = entry.id; editorSection = entry.section; editorFavorite = entry.favorite; dirty = false; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); message("Opened saved setup. Changes are saved only when you choose Save.");
	}
	void newDraft(JsonObject p)
	{
		if (!discard()) return; draft = p.deepCopy(); editorLayout = BankLayouts.infer(draft); editorBottom = BankLayouts.inferBottom(draft); editingId = null; editorSection = null; editorFavorite = false; dirty = true; differences.clear(); session = UUID.randomUUID().toString(); renderBuilder(); syncBankFilter(); tabs.setSelectedIndex(1); message("New draft. Review it, then Save to your library.");
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
		if (rendering || draft == null) return; dirty = true; draftLabel.setText("Unsaved changes"); code.setText("");
	}
	private void fieldsToDraft()
	{
		if (draft == null) return; JsonObject s = SetupCodec.setup(draft); s.addProperty("name", setupName.getText().trim()); s.addProperty("sb", spellbook.getSelectedIndex()); s.addProperty("notes", notes.getText()); s.addProperty("fb", filterBank.isSelected());
	}
	private void renderBuilder()
	{
		if (disposed) return; rendering = true;
		try
		{
			builder.removeAll(); if (draft == null) { builder.add(text("Create, capture or open a setup.")); builder.revalidate(); builder.repaint(); return; }
			JsonObject s = SetupCodec.setup(draft); 
			setupName.setText(SetupCodec.name(draft)); setupName.setMaximumSize(new Dimension(500, 28)); spellbook.setSelectedIndex(s.has("sb") ? s.get("sb").getAsInt() : 4); notes.setText(s.has("notes") && !s.get("notes").isJsonNull() ? s.get("notes").getAsString() : ""); notes.setLineWrap(true); notes.setWrapStyleWord(true);
			section.removeAllItems(); sectionIds.clear(); section.addItem("Unfiled"); sectionIds.add(null); int selectedSection = 0;
			for (SetupLibrary.Section group : library.sections) { section.addItem(group.name); sectionIds.add(group.id); if (Objects.equals(group.id, editorSection)) selectedSection = sectionIds.size() - 1; } section.setSelectedIndex(selectedSection); favorite.setSelected(editorFavorite);
			builder.add(text("Setup name")); builder.add(setupName); builder.add(row(button("Save", () -> saveDraft(false)), button("Save copy", () -> saveDraft(true)), button("Compare", this::compareGear))); draftLabel.setText(dirty ? "Unsaved changes" : "Saved setup"); builder.add(draftLabel);
			builder.add(text("Section")); builder.add(section); builder.add(favorite); builder.add(text("Spellbook")); builder.add(spellbook);
			filterBank.setSelected(s.has("fb") && s.get("fb").getAsBoolean()); bankLayout.setSelectedItem(editorLayout); bottomToTop.setSelected(editorBottom); bottomToTop.setEnabled(editorLayout == BankLayouts.Mode.ZIGZAG);
			builder.add(text("Bank filtering & layout")); builder.add(filterBank); builder.add(bankLayout); builder.add(bottomToTop);
			builder.add(row(button("Apply filter", () -> { filterBank.setSelected(true); fieldsToDraft(); markDirty(); syncBankFilter(); }), button("Clear filter", () -> { filterBank.setSelected(false); fieldsToDraft(); markDirty(); syncBankFilter(); })));
			builder.add(bankStatus);
			builder.add(text("Equipment")); JPanel equipment = new JPanel(new GridBagLayout());
			int[][] positions = {{0,1,0},{1,0,1},{2,1,1},{13,2,1},{3,0,2},{4,1,2},{5,2,2},{7,1,3},{9,0,4},{10,1,4},{12,2,4}};
			for (int[] position : positions) { GridBagConstraints c = new GridBagConstraints(); c.gridx = position[1]; c.gridy = position[2]; c.insets = new Insets(2, 2, 2, 2); equipment.add(slot("eq", position[0]), c); } builder.add(equipment);
			builder.add(text("Inventory · 28 slots")); JPanel inventory = new JPanel(new GridLayout(7, 4, 3, 3)); for (int i = 0; i < 28; i++) inventory.add(slot("inv", i)); builder.add(inventory);
			builder.add(text("Rune pouch · max 16k")); JPanel runes = new JPanel(new GridLayout(1, 4, 3, 3)); for (int i = 0; i < 4; i++) runes.add(slot("rp", i)); builder.add(runes); builder.add(button("Max runes (16k)", () -> { for (JsonElement e : s.getAsJsonArray("rp")) if (!e.isJsonNull()) e.getAsJsonObject().addProperty("q", 16000); markDirty(); renderBuilder(); }));
			builder.add(text("Quiver · extra Ammo")); builder.add(row(slot("qv", 0))); builder.add(ignoreAmmo); builder.add(text("Additional filtered items")); JPanel extras = new JPanel(new GridLayout(0, 4, 3, 3)); int index = 0; for (Map.Entry<String, JsonElement> entry : s.getAsJsonObject("afi").entrySet()) extras.add(slot("afi", index++)); builder.add(extras); builder.add(button("Add filtered item", () -> editSlot("afi", SetupCodec.setup(draft).getAsJsonObject("afi").size())));
			builder.add(text("Notes")); builder.add(new JScrollPane(notes)); builder.add(row(button("Search items", () -> editSlot("inv", firstEmpty())), button("Export", () -> { fieldsToDraft(); tabs.setSelectedIndex(3); refreshCode(); })));
		}
		finally { rendering = false; builder.revalidate(); builder.repaint(); }
	}
	private int firstEmpty() { JsonArray inv = SetupCodec.setup(draft).getAsJsonArray("inv"); for (int i = 0; i < 28; i++) if (inv.get(i).isJsonNull()) return i; message("Inventory is full. Choose a slot to replace."); return 0; }
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
		JsonElement entry = get(key, index); int id = SetupCodec.id(entry); String empty = key.equals("eq") ? EQUIPMENT[index] : "+";
		JButton button = button(id > 0 ? String.valueOf(id) : id == 0 ? "?" : empty, () -> editSlot(key, index)); button.setPreferredSize(new Dimension(47, 46)); button.setMinimumSize(new Dimension(42, 42)); button.setBackground(SLOT); button.setFont(button.getFont().deriveFont(10f)); button.setMargin(new Insets(1, 1, 1, 1)); button.setForeground(GOLD);
		button.setBorder(BorderFactory.createLineBorder(differences.contains(key + ":" + index) ? RED : new Color(109, 101, 77), differences.contains(key + ":" + index) ? 2 : 1));
		if (id == 0) button.setToolTipText(entry.getAsJsonObject().get("label").getAsString() + " · choose an exact Item ID");
		if (id > 0) host.describe(id, info -> { if (disposed) return; itemCache.put(id, info); button.setIcon(info.icon); button.setText(SetupCodec.quantity(entry) > 1 ? compact(SetupCodec.quantity(entry)) : ""); button.setHorizontalTextPosition(JButton.CENTER); button.setVerticalTextPosition(JButton.TOP); button.setToolTipText(info.name + " · " + id + " · quantity " + (SetupCodec.quantity(entry) == 0 ? "unknown" : SetupCodec.quantity(entry))); });
		button.setTransferHandler(new TransferHandler()
		{
			@Override public int getSourceActions(javax.swing.JComponent c) { return MOVE; }
			@Override protected Transferable createTransferable(javax.swing.JComponent c) { return get(key, index).isJsonNull() ? null : new StringSelection("loadout-slot:" + session + ":" + key + ":" + index); }
			@Override public boolean canImport(TransferSupport support) { return support.isDataFlavorSupported(DataFlavor.stringFlavor); }
			@Override public boolean importData(TransferSupport support)
			{
				try { String value = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor); String[] parts = value.split(":"); if (parts.length != 4 || !parts[0].equals("loadout-slot") || !parts[1].equals(session)) return false; String from = parts[2]; int old = Integer.parseInt(parts[3]); if (key.equals("afi") || !java.util.Arrays.asList("eq", "inv", "rp", "qv").contains(from) || old < 0 || old > 255) return false; fieldsToDraft(); JsonElement a = get(from, old).deepCopy(), b = get(key, index).deepCopy(); validateSlot(key, index, a); validateSlot(from, old, b); put(from, old, b); put(key, index, a); rebuildDraftLayout(); markDirty(); renderBuilder(); return true; } catch (Exception e) { message(e.getMessage()); return false; }
			}
		});
		button.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { @Override public void mouseDragged(MouseEvent e) { button.getTransferHandler().exportAsDrag(button, e, TransferHandler.MOVE); } }); return button;
	}
	private static String compact(int q) { return q >= 1000 && q % 1000 == 0 ? q / 1000 + "k" : String.valueOf(q); }
	private void validateSlot(String key, int index, JsonElement e)
	{
		if (e == null || e.isJsonNull()) return; ItemInfo info = itemCache.get(SetupCodec.id(e));
		if (key.equals("rp")) { if (info != null && !info.name.toLowerCase(java.util.Locale.ROOT).endsWith(" rune")) throw new IllegalArgumentException("Rune pouch accepts runes only."); if (SetupCodec.quantity(e) > 16000) throw new IllegalArgumentException("Rune pouch maximum is 16k."); }
		if (key.equals("qv") && info != null && !info.name.toLowerCase(java.util.Locale.ROOT).matches(".*(arrow|bolt)s?(?: \\(.*\\))?$")) throw new IllegalArgumentException("Quiver accepts arrows or bolts.");
		if (key.equals("eq") && info != null && info.slot >= 0 && info.slot != index) throw new IllegalArgumentException("This item belongs in a different Equipment slot.");
	}
	private void editSlot(String key, int index)
	{
		if (draft == null) return; fieldsToDraft(); JsonElement previous = get(key, index); String capturedSession = session;
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Choose item · " + key + " " + (index + 1), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		JPanel p = vertical(); p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); JTextField query = new JTextField(), idField = new JTextField(SetupCodec.id(previous) > 0 ? String.valueOf(SetupCodec.id(previous)) : ""), amount = new JTextField(previous.isJsonNull() ? "1" : SetupCodec.quantity(previous) == 0 ? "" : String.valueOf(SetupCodec.quantity(previous))); JCheckBox fuzzy = new JCheckBox("Fuzzy matching", !previous.isJsonNull() && previous.getAsJsonObject().has("f") && previous.getAsJsonObject().get("f").getAsBoolean()); JComboBox<String> compare = new JComboBox<>(new String[]{"None", "Standard", "Less_Than", "Greater_Than"}); if (!previous.isJsonNull() && previous.getAsJsonObject().has("sc")) compare.setSelectedItem(previous.getAsJsonObject().get("sc").getAsString());
		DefaultListModel<ItemInfo> model = new DefaultListModel<>(); JList<ItemInfo> results = new JList<>(model); results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); results.setVisibleRowCount(9); JLabel count = text(host.catalogueStatus()); JTextArea error = new JTextArea(2, 32); error.setEditable(false); error.setLineWrap(true); error.setForeground(RED);
		Runnable find = () -> { model.clear(); for (ItemInfo info : host.search(query.getText())) model.addElement(info); count.setText(host.catalogueStatus() + " · " + model.size() + " shown"); }; watch(query, find);
		results.addListSelectionListener(e -> { if (results.getSelectedValue() != null) idField.setText(String.valueOf(results.getSelectedValue().id)); });
		p.add(text("Search English item names or Item ID")); p.add(query); p.add(count); p.add(new JScrollPane(results)); p.add(text("Exact Item ID · charged/ornamented variants stay distinct")); p.add(idField); p.add(text("Quantity · 16k = 16000")); p.add(amount); p.add(fuzzy); p.add(text("Stack comparison")); p.add(compare); p.add(error);
		JButton apply = button("Use item", () -> attempt(() -> { }));
		for (java.awt.event.ActionListener listener : apply.getActionListeners()) apply.removeActionListener(listener);
		apply.addActionListener(event ->
		{
			try
			{
				int id = Integer.parseInt(idField.getText().trim()), quantity = SetupCodec.parseQuantity(amount.getText()); if (id <= 0) throw new IllegalArgumentException("Choose a positive Item ID."); apply.setEnabled(false);
				host.describe(id, info ->
				{
					apply.setEnabled(true); if (!capturedSession.equals(session) || disposed) { dialog.dispose(); return; }
					try { itemCache.put(id, info); if (!info.stackable && quantity > 1) throw new IllegalArgumentException("Non-stackable items need separate Inventory slots."); JsonObject item = SetupCodec.item(id, quantity); if (fuzzy.isSelected()) item.addProperty("f", true); if (!compare.getSelectedItem().equals("None")) item.addProperty("sc", (String) compare.getSelectedItem()); validateSlot(key, index, item); put(key, index, item); rebuildDraftLayout(); markDirty(); renderBuilder(); dialog.dispose(); } catch (RuntimeException e) { error.setText(e.getMessage()); }
				});
			}
			catch (RuntimeException e) { error.setText(e.getMessage()); }
		});
		p.add(row(apply, button("Clear slot", () -> { if (capturedSession.equals(session)) { put(key, index, JsonNull.INSTANCE); rebuildDraftLayout(); markDirty(); renderBuilder(); } dialog.dispose(); }), button("Cancel", dialog::dispose)));
		dialog.setContentPane(p); dialog.pack(); dialog.setMinimumSize(new Dimension(380, 480)); dialog.setLocationRelativeTo(this); find.run(); dialog.setVisible(true);
	}
	private void saveDraft(boolean copy)
	{
		if (draft == null) return;
		attempt(() -> { fieldsToDraft(); SetupCodec.validate(draft, false); String chosenSection = sectionIds.get(Math.max(0, section.getSelectedIndex())); boolean chosenFavorite = favorite.isSelected(); String id = copy || editingId == null ? UUID.randomUUID().toString() : editingId;
			commit(l -> { SetupLibrary.Entry record = l.find(id); if (record == null) { record = new SetupLibrary.Entry(draft); record.id = id; l.setups.add(record); } record.portable = draft.deepCopy(); record.section = chosenSection; record.favorite = chosenFavorite; record.bankLayout = editorLayout; record.zigzagBottomToTop = editorBottom; }); editingId = id; dirty = false; draftLabel.setText("Saved setup"); message("Saved to this character/profile library."); });
	}
	private void compareGear()
	{
		if (draft == null) return; fieldsToDraft(); String token = session;
		host.capture(actual -> { if (!token.equals(session)) return; differences.clear(); JsonObject current = SetupCodec.setup(actual); for (String key : new String[]{"eq", "inv", "rp", "qv"}) { int length = key.equals("eq") ? 14 : key.equals("inv") ? 28 : key.equals("rp") ? 4 : 1; for (int i = 0; i < length; i++) { JsonElement expected = get(key, i), present = current.getAsJsonArray(key).size() > i ? current.getAsJsonArray(key).get(i) : JsonNull.INSTANCE; boolean quantity = !(ignoreAmmo.isSelected() && (key.equals("qv") || key.equals("eq") && i == 13)); if (SetupCodec.id(expected) != SetupCodec.id(present) || quantity && SetupCodec.quantity(expected) != SetupCodec.quantity(present)) differences.add(key + ":" + i); } } renderBuilder(); message(differences.size() + " slot differences. Red borders mark differences; fuzzy/Stack comparison rules are preserved in exports but this test comparison uses exact IDs and quantities."); }, this::message);
	}
	private JPanel exportTab()
	{
		JPanel p = vertical(); p.add(text("Export the open Builder setup")); p.add(format); p.add(row(button("Show code", this::refreshCode), button("Copy code", () -> attempt(() -> { refreshCodeOrThrow(); getToolkit().getSystemClipboard().setContents(new StringSelection(code.getText()), null); message("Copied " + format.getSelectedItem() + ". No setup data was sent to a server."); }))));
		code.setEditable(false); code.setLineWrap(false); p.add(new JScrollPane(code)); JTextArea help = new JTextArea("Inventory Setup JSON: green import icon → Import setup.. → Enter setup data.\n\nBank Tag: right-click the Bank + tab → Import tag tab.\n\nBank Tags omit Equipment slots, quantities, Spellbook and Notes. Conversion keeps the full original open in the Builder.\n\nUse Library → Backup for all saved sections and setups."); help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); p.add(help); format.addActionListener(e -> { code.setText(""); if (draft != null) refreshCode(); });
		spellbook.addActionListener(e -> { if (!rendering) { fieldsToDraft(); markDirty(); } }); ignoreAmmo.addActionListener(e -> markDirty()); favorite.addActionListener(e -> { if (!rendering) { editorFavorite = favorite.isSelected(); markDirty(); } }); section.addActionListener(e -> { if (!rendering && section.getSelectedIndex() >= 0 && section.getSelectedIndex() < sectionIds.size()) { editorSection = sectionIds.get(section.getSelectedIndex()); markDirty(); } }); notes.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { if (!rendering) { fieldsToDraft(); markDirty(); } } public void removeUpdate(DocumentEvent e) { if (!rendering) { fieldsToDraft(); markDirty(); } } public void changedUpdate(DocumentEvent e) { } }); return p;
	}
	private void refreshCode() { attempt(this::refreshCodeOrThrow); }
	private void refreshCodeOrThrow()
	{
		code.setText(""); if (draft == null) throw new IllegalStateException("Open a setup in the Builder first."); fieldsToDraft(); JsonObject p = draft.deepCopy();
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
			List<JsonObject> imported = codec.importText(text); if (JOptionPane.showConfirmDialog(this, "Add " + imported.size() + " setup(s)? First: " + SetupCodec.name(imported.get(0)) + "\nExisting saved setups are retained.", "Review import", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
			commit(l -> { for (JsonObject p : imported) l.setups.add(new SetupLibrary.Entry(p)); }); message("Imported " + imported.size() + " setup(s). Open them from Library."); renderLibrary();
		});
	}
	private void backup()
	{
		attempt(() -> { String text = library.serialize(gson); getToolkit().getSystemClipboard().setContents(new StringSelection(text), null); message("Complete library backup copied. Save this JSON to a file; restore with Import."); });
	}
	private JPanel wikiTab()
	{
		JPanel p = vertical(); JTextField url = new JTextField(), title = new JTextField(); url.setMaximumSize(new Dimension(500, 28)); title.setMaximumSize(new Dimension(500, 28)); JTextArea raw = new JTextArea(10, 18); raw.setLineWrap(true); JCheckBox enabled = new JCheckBox("Enable OSRS Wiki requests", host.wikiEnabled()); enabled.addActionListener(e -> host.enableWiki(enabled.isSelected())); p.add(enabled);
		JTextArea notice = new JTextArea("Optional: sends your IP address, search query and requested page title to oldschool.runescape.wiki. No gear or account details are uploaded. Pasted wikitext works offline."); notice.setRows(6); notice.setEditable(false); notice.setLineWrap(true); notice.setWrapStyleWord(true); p.add(notice);
		JTextField query = new JTextField(); DefaultListModel<String> matches = new DefaultListModel<>(); JList<String> pages = new JList<>(matches); pages.putClientProperty("html.disable", true); pages.setVisibleRowCount(5); pages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); JCheckBox strategies = new JCheckBox("Use Strategies subpage", true);
		p.add(text("Search Wiki / PvM content")); p.add(query);
		JButton find = button("Search Wiki", () -> attempt(() -> { String requested = query.getText().trim(), capturedScope = scope; message("Searching OSRS Wiki..."); host.searchWiki(requested, (titles, error) -> { if (!Objects.equals(scope, capturedScope) || !query.getText().trim().equals(requested)) return; matches.clear(); if (error != null) message(error); else { for (String name : titles) matches.addElement(name); message(titles.isEmpty() ? "No pages found. Try another name." : "Select a page, then fetch its setups."); } }); }));
		query.addActionListener(e -> find.doClick()); p.add(find); p.add(new JScrollPane(pages)); p.add(strategies);
		Runnable selectPage = () -> { String selected = pages.getSelectedValue(); if (selected != null) { title.setText(selected.split("(?i)/Strategies", 2)[0]); url.setText(WikiClient.articleUrl(strategies.isSelected() && !selected.toLowerCase(java.util.Locale.ROOT).endsWith("/strategies") ? selected + "/Strategies" : selected)); } };
		pages.addListSelectionListener(e -> selectPage.run()); strategies.addActionListener(e -> selectPage.run());
		p.add(text("Wiki article URL")); p.add(url); p.add(text("Page name (pasted text)")); p.add(title); p.add(text("Wikitext (optional)")); p.add(new JScrollPane(raw));
		Runnable generate = () -> attempt(() -> generateWiki(raw.getText(), title.getText().trim(), url.getText().trim().isEmpty() ? "Pasted wikitext" : url.getText().trim()));
		p.add(button("Generate from pasted text", generate)); p.add(button("Fetch Wiki page", () -> attempt(() -> { String requested = url.getText().trim(), page = WikiClient.title(requested), capturedScope = scope; message("Fetching public Wiki wikitext…"); host.fetchWiki(requested, (text, error) -> { if (!Objects.equals(scope, capturedScope)) return; if (error != null) message(error); else attempt(() -> generateWiki(text, page, requested)); }); })));
		JTextArea limits = new JTextArea("Test version: concrete Equipment, Inventory and Rune pouch templates. Ranked Recommended equipment tables need manual choices and are not expanded. Exact-name matches are resolved from the game item catalogue; ambiguous variants stay unresolved."); limits.setRows(9); limits.setEditable(false); limits.setLineWrap(true); limits.setWrapStyleWord(true); p.add(limits); return p;
	}
	private void generateWiki(String text, String title, String source)
	{
		List<JsonObject> presets = new WikiParser().parse(text, title, source);
		for (JsonObject p : presets)
		{
			JsonObject s = SetupCodec.setup(p);
			for (String key : new String[]{"eq", "inv", "rp", "qv"}) for (JsonElement e : s.getAsJsonArray(key)) if (!e.isJsonNull())
			{
				JsonObject item = e.getAsJsonObject(); String label = item.get("label").getAsString(); List<ItemInfo> exact = new ArrayList<>(); for (ItemInfo info : host.search(label)) if (info.name.equalsIgnoreCase(label)) exact.add(info);
				if (exact.size() == 1) { item.addProperty("id", exact.get(0).id); itemCache.put(exact.get(0).id, exact.get(0)); }
			}
			SetupCodec.rebuildLayout(p);
		}
		String[] labels = new String[presets.size()]; for (int i = 0; i < labels.length; i++) labels[i] = (i + 1) + ". " + SetupCodec.name(presets.get(i)) + " · " + SetupCodec.setup(presets.get(i)).get("notes").getAsString().split("\\n")[1];
		String selected = (String) JOptionPane.showInputDialog(this, "Choose a concrete preset", "Wiki setups", JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]); if (selected == null) return;
		newDraft(presets.get(java.util.Arrays.asList(labels).indexOf(selected))); message("Wiki draft loaded. '?' means unresolved Item ID. Review all quantities and Spellbook; Save can keep unresolved drafts, export requires resolved items.");
	}
}
