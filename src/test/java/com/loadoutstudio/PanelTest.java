package com.loadoutstudio;

import com.google.gson.JsonObject;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class PanelTest
{
	static final class FakeHost implements StudioPanel.Host
	{
		String scope = "character-one";
		String saved;
		JsonObject selectedBankSetup;
		int clears;
		public String scope() { return scope; }
		public String read(String scope) { return null; }
		public void save(String scope, String text) { assertEquals(this.scope, scope); saved = text; }
		public void capture(Consumer<JsonObject> success, Consumer<String> error) { success.accept(SetupCodec.blank("Current gear")); }
		public void describe(int id, Consumer<StudioPanel.ItemInfo> callback) { callback.accept(new StudioPanel.ItemInfo(id, "Fixture item", true, -1, null)); }
		public List<StudioPanel.ItemInfo> search(String query) { return Collections.emptyList(); }
		public String catalogueStatus() { return "Fixture catalogue"; }
		public boolean wikiEnabled() { return false; }
		public void enableWiki(boolean enabled) { }
		public void fetchWiki(String url, BiConsumer<String, String> callback) { callback.accept(null, "Offline fixture"); }
		public void searchWiki(String query, BiConsumer<List<String>, String> callback) { callback.accept(Collections.singletonList("Vorkath"), null); }
		public void updateBankFilter(JsonObject portable, String scope, Consumer<String> callback) { selectedBankSetup = portable.deepCopy(); callback.accept("Bank filter ready. Open your Bank to apply it."); }
		public void clearBankFilter() { clears++; selectedBankSetup = null; }
	}
	private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container) layout((Container) child); }
	private static javax.swing.JButton findButton(Container c, String name)
	{
		for (Component child : c.getComponents())
		{
			if (child instanceof javax.swing.JButton && name.equals(((javax.swing.JButton) child).getText())) return (javax.swing.JButton) child;
			if (child instanceof Container) { javax.swing.JButton found = findButton((Container) child, name); if (found != null) return found; }
		}
		return null;
	}
	@Test public void saveAndClearUseVisibleBankControlsAndKeepLayoutSettings() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FakeHost host = new FakeHost(); StudioPanel panel = new StudioPanel(host, RuneLiteAPI.GSON); JsonObject p = SetupCodec.blank("Vorkath"); SetupCodec.setup(p).getAsJsonArray("inv").set(0, SetupCodec.item(100, 1)); p.add("layout", BankLayouts.generate(p, BankLayouts.Mode.ZIGZAG, true)); SetupLibrary.Entry entry = new SetupLibrary.Entry(p); entry.bankLayout = BankLayouts.Mode.ZIGZAG; entry.zigzagBottomToTop = true;
			panel.open(entry); assertEquals(p.get("layout"), host.selectedBankSetup.get("layout"));
			findButton(panel, "Clear filter").doClick(); assertFalse(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());
			findButton(panel, "Apply filter").doClick(); assertTrue(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());
			findButton(panel, "Save").doClick(); SetupLibrary saved = SetupLibrary.read(host.saved, RuneLiteAPI.GSON); assertEquals(BankLayouts.Mode.ZIGZAG, saved.setups.get(0).bankLayout); assertTrue(saved.setups.get(0).zigzagBottomToTop); assertEquals(p.get("layout"), saved.setups.get(0).portable.get("layout"));
			panel.reloadScope(); assertNull(host.selectedBankSetup); assertTrue(host.clears >= 2); panel.dispose();
		});
	}
	@Test public void sidebarRendersWithoutGameAndResetsOnProfileChange() throws Exception
	{
		System.setProperty("java.awt.headless", "true");
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				FakeHost host = new FakeHost(); StudioPanel panel = new StudioPanel(host, RuneLiteAPI.GSON); panel.setSize(260, 950);
				JTabbedPane tabs = null; for (Component c : panel.getComponents()) if (c instanceof JTabbedPane) tabs = (JTabbedPane) c; assertNotNull(tabs);
				panel.newDraft(SetupCodec.blank("Vorkath"));
				for (int tab : new int[]{1, 2})
				{
					tabs.setSelectedIndex(tab); layout(panel); layout(panel); BufferedImage image = new BufferedImage(260, 950, BufferedImage.TYPE_INT_RGB); java.awt.Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose(); File dir = new File("build/previews"); dir.mkdirs(); ImageIO.write(image, "png", new File(dir, tab == 1 ? "builder.png" : "wiki.png"));
				}
				host.scope = "character-two"; panel.reloadScope(); panel.dispose();
			}
			catch (Exception e) { throw new AssertionError(e); }
		});
	}
}
