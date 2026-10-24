/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarLayoutTest
{
	private static Component find(Container root, String name)
	{
		for (Component child : root.getComponents())
		{
			if (name.equals(child.getName()) || child instanceof JButton && name.equals(((JButton) child).getText()) || child instanceof JCheckBox && name.equals(((JCheckBox) child).getText())) return child;
			if (child instanceof Container) { Component found = find((Container) child, name); if (found != null) return found; }
		}
		return null;
	}
	private static void layout(Container root) { root.doLayout(); for (Component c : root.getComponents()) if (c instanceof Container) layout((Container)c); }
	@Test public void navigationStaysFixedAndSaveRemainsOutsideTheScrollArea() throws Exception
	{
		System.setProperty("java.awt.headless", "true"); String fixture = WikiPageTest.vorkath();
		SwingUtilities.invokeAndWait(() ->
		{
			for (int width : new int[]{220, 260})
			{
				SetupLibrary library = new SetupLibrary(); SetupLibrary.Section section = new SetupLibrary.Section("PvM"); library.sections.add(section); SetupLibrary.Entry saved = new SetupLibrary.Entry(SetupCodec.blank("The Fractured Archive")); saved.section = section.id; saved.favorite = true; library.setups.add(saved); SetupLibrary.Entry longTitle = new SetupLibrary.Entry(SetupCodec.blank("The Fractured Archive - Ranged setup")); longTitle.section = section.id; library.setups.add(longTitle);
				int[] supportClicks = {0};
                PanelTest.FakeHost host = new PanelTest.FakeHost()
				{
					@Override public String read(String scope) { return library.serialize(RuneLiteAPI.GSON); }
                    @Override public void openSupportPage() { supportClicks[0]++; }
                    @Override public java.util.List<StudioPanel.ItemInfo> search(String query) { return java.util.Arrays.asList(new StudioPanel.ItemInfo(22978,"Dragon hunter lance",false,3,null),new StudioPanel.ItemInfo(21012,"Dragon hunter crossbow",false,3,null)); }
					@Override public void searchWiki(String q, BiConsumer<List<String>, String> cb) { cb.accept(Collections.singletonList("Vorkath/Strategies"), null); }
					@Override public void fetchWikiPage(String url, boolean prefer, BiConsumer<WikiClient.Page, String> cb) { cb.accept(new WikiClient.Page("Vorkath/Strategies", fixture), null); }
				};
				StudioPanel panel = new StudioPanel(host, RuneLiteAPI.GSON); panel.setSize(width, 900);
				assertEquals("Support link never opens automatically", 0, supportClicks[0]);
                JButton coffee = (JButton)find(panel, "supportCoffee"); assertEquals("Buy a coffee", coffee.getText()); assertNotNull(coffee.getIcon()); assertEquals("https://buymeacoffee.com/pascalvdbe9", coffee.getAccessibleContext().getAccessibleDescription()); coffee.doClick(); assertEquals(1, supportClicks[0]);
                assertFalse(((JButton)find(panel, "Copy code")).isEnabled()); panel.newDraft(SetupCodec.blank("Vorkath")); assertTrue(((JButton)find(panel, "Copy code")).isEnabled());
				layout(panel); layout(panel); String[] routes = {"Library", "Builder", "Wiki", "Export"}; Rectangle[] bounds = new Rectangle[4];
				for (int i = 0; i < 4; i++) { JButton route = (JButton)find(panel, "nav:" + routes[i]); bounds[i] = route.getBounds(); assertTrue(route.getWidth() >= 80); assertTrue(route.getHeight() >= 30); assertEquals(StudioTheme.ACTION.getFamily(), route.getFont().getFamily()); }
				for (int tab = 0; tab < 4; tab++)
				{
					((JButton)find(panel, "nav:" + routes[tab])).doClick();
					if (tab == 2) { ((JTextField)find(panel, "wikiSearch")).setText("Vorkath"); ((JButton)find(panel, "Search Wiki")).doClick(); ((JList<?>)find(panel, "wikiSuggestions")).setSelectedIndex(0); ((JTextField)find(panel, "wikiPageUrl")).postActionEvent(); }
					layout(panel); layout(panel);
                    if (tab == 0)
                    {
                        assertTrue(find(panel, "libraryToolbar").getHeight() <= 30); assertNotNull(find(panel, "libraryTool:Import setup"));
                        JButton coffeeControl = (JButton)find(panel, "supportCoffee"); assertTrue(coffeeControl.getWidth() >= 80); assertTrue(coffeeControl.getX() + coffeeControl.getWidth() <= coffeeControl.getParent().getWidth());
                        JTextField librarySearch = (JTextField)find(panel, "librarySearch"); librarySearch.setText("Archive"); ((JButton)find(panel, "libraryTool:Clear search")).doClick(); assertEquals("", librarySearch.getText()); layout(panel); layout(panel);
                        JButton title = (JButton)find(panel, "libraryOpen:" + saved.id), star = (JButton)find(panel, "libraryFavorite:" + saved.id), trash = (JButton)find(panel, "libraryArchive:" + saved.id);
                        assertEquals("The Fractured Archive", title.getToolTipText());
                        assertTrue("Long names fit the available width", title.getFontMetrics(title.getFont()).stringWidth(title.getText()) < title.getWidth());
                        if (title.getFontMetrics(title.getFont()).stringWidth(title.getToolTipText()) > title.getWidth() - title.getInsets().left - title.getInsets().right - 4) assertTrue("Narrow sidebar truncates long names: text=" + title.getText() + " bounds=" + title.getBounds() + " insets=" + title.getInsets(), title.getText().endsWith("..."));
                        Rectangle starBounds = SwingUtilities.convertRectangle(star.getParent(), star.getBounds(), title.getParent());
                        Rectangle trashBounds = SwingUtilities.convertRectangle(trash.getParent(), trash.getBounds(), title.getParent());
                        assertEquals(title.getY(), starBounds.y); assertEquals(title.getY(), trashBounds.y);
                        assertTrue(title.getX() + title.getWidth() <= starBounds.x); assertTrue(starBounds.x + starBounds.width <= trashBounds.x);
                        assertTrue(star.getWidth() >= 28); assertTrue(trash.getWidth() >= 28); assertNotNull(trash.getIcon());
                        JButton shortened = (JButton)find(panel, "libraryOpen:" + longTitle.id);
                        assertTrue("Long titles explicitly end with three dots", shortened.getText().endsWith("..."));
                        assertEquals("The Fractured Archive - Ranged setup", shortened.getToolTipText());
                    }
                    if (tab == 1)
                    {
                        String[] sections = {"Inventory", "Rune Pouch", "Equipment", "Spellbook", "Quiver", "Additional filtered items", "Notes"}; int previousBottom = -1;
                        for (String sectionTitle : sections) { Component block = find(panel, "loadoutSection:" + sectionTitle); assertNotNull(block); assertTrue(block.getY() >= previousBottom); previousBottom = block.getY() + block.getHeight(); }
                        assertEquals(176, find(panel, "inventoryGrid").getWidth()); assertEquals(308, find(panel, "inventoryGrid").getHeight());
                        assertEquals(132, find(panel, "equipmentGrid").getWidth()); assertEquals(220, find(panel, "equipmentGrid").getHeight());
                        assertNotNull(find(panel, "+ Loadout options")); assertNotNull(find(panel, "Max pouch runes (16k)"));
                        ((JButton)find(panel,"+ Find items")).doClick(); JTextField itemSearch=(JTextField)find(panel,"catalogueSearch"); itemSearch.setText("dragon hunter"); itemSearch.postActionEvent(); layout(panel); layout(panel);
                        assertTrue("Expanded item search occupies visible header space", find(panel,"- Find items").getParent().getHeight() > 180); assertTrue(itemSearch.getWidth() >= 120); assertTrue(find(panel,"catalogueItems").getHeight() <= 110);
                        try { File dir=new File("build/previews/optimized-studio"); dir.mkdirs(); BufferedImage preview=new BufferedImage(width,900,BufferedImage.TYPE_INT_RGB); Graphics2D graphics=preview.createGraphics(); panel.printAll(graphics); graphics.dispose(); ImageIO.write(preview,"png",new File(dir,width+"-catalogue.png")); } catch(java.io.IOException ex) { throw new AssertionError(ex); }
                        ((JButton)find(panel,"- Find items")).doClick(); layout(panel); layout(panel);
                    }
					if (tab == 2) { Component choice = find(panel, "wikiPreset:Ranged (crossbow)"); assertNotNull(choice); assertTrue("Wiki choices have visible width", choice.getWidth() > 80); assertTrue("Wiki choices have clickable height", choice.getHeight() >= 26); }
					for (int i = 0; i < 4; i++) assertEquals("Navigation must not reorder", bounds[i], find(panel, "nav:" + routes[i]).getBounds());
					assertNull(find(panel, "Enable OSRS Wiki requests"));
					try { File dir = new File("build/previews/sidebar-redesign"); dir.mkdirs(); BufferedImage image = new BufferedImage(width, 900, BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics(); panel.printAll(g); g.dispose(); ImageIO.write(image, "png", new File(dir, width + "-" + routes[tab].toLowerCase() + ".png")); } catch (java.io.IOException e) { throw new AssertionError(e); }
				}
				assertNull("Save stays visible while the item grid scrolls", SwingUtilities.getAncestorOfClass(JViewport.class, find(panel, "Save")));
				((JButton)find(panel, "nav:Builder")).doClick(); ((JButton)find(panel, "+ Bank filtering and layout")).doClick(); ((JButton)find(panel,"setupToggle:filter")).doClick(); assertNotNull("Expanded options survive rerendering", find(panel, "- Bank filtering and layout")); ((JButton)find(panel, "libraryFavorite:" + saved.id)).doClick(); assertFalse(SetupLibrary.read(host.saved, RuneLiteAPI.GSON).find(saved.id).favorite);
                ((JButton)find(panel, "libraryArchive:" + saved.id)).doClick(); assertTrue(SetupLibrary.read(host.saved, RuneLiteAPI.GSON).find(saved.id).archived);
                ((JCheckBox)find(panel, "Archived")).doClick(); ((JButton)find(panel, "libraryArchive:" + saved.id)).doClick(); assertFalse(SetupLibrary.read(host.saved, RuneLiteAPI.GSON).find(saved.id).archived);
                panel.dispose();
			}
		});
	}
}
