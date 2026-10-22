/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiPagePanelTest
{
	private static final class PageHost extends PanelTest.FakeHost
	{
		final List<String> urls = new ArrayList<>();
		final List<BiConsumer<String, String>> replies = new ArrayList<>();
		final List<Integer> described = new ArrayList<>();
        @Override public void describe(int id, java.util.function.Consumer<StudioPanel.ItemInfo> callback) { described.add(id); super.describe(id, callback); }
        final ItemCatalogue catalogue = new ItemCatalogue();
        @Override public StudioPanel.ItemInfo exactItem(String name) { return catalogue.exact(name); }
        final CompletableFuture<String> firstPage = new CompletableFuture<>();
		@Override public void searchWiki(String query, BiConsumer<List<String>, String> callback) { callback.accept(Collections.singletonList("Vorkath/Strategies"), null); }
		@Override public void fetchWiki(String url, BiConsumer<String, String> callback) { urls.add(url); replies.add(callback); firstPage.complete(url); }
	}
	private static Component find(Container root, String name)
	{
		for (Component child : root.getComponents())
		{
			if (name.equals(child.getName()) || child instanceof JButton && name.equals(((JButton) child).getText())) return child;
			if (child instanceof Container) { Component found = find((Container) child, name); if (found != null) return found; }
		}
		return null;
	}
	@Test public void selectingPageLoadsChoicesWithoutFetchAndOpensChosenGear() throws Exception
	{
		System.setProperty("java.awt.headless", "true"); String fixture = WikiPageTest.vorkath();
		PageHost host = new PageHost();
        host.catalogue.add(22978, "Dragon hunter lance", false, -1, -1);
        host.catalogue.add(22979, "Dragon hunter lance", true, 799, -1);
        host.catalogue.add(26384, "Torva platebody", false, -1, -1);
        host.catalogue.add(26385, "Torva platebody", true, 799, -1);
        host.catalogue.add(13441, "Anglerfish", false, -1, -1);
        host.catalogue.add(13442, "Anglerfish", true, 799, -1);
        host.catalogue.add(560, "Death rune", true, -1, -1);
        StudioPanel[] panel = new StudioPanel[1]; JTabbedPane[] tabs = new JTabbedPane[1];
		SwingUtilities.invokeAndWait(() ->
		{
			panel[0] = new StudioPanel(host, RuneLiteAPI.GSON);
			for (Component c : panel[0].getComponents()) if (c instanceof JTabbedPane) tabs[0] = (JTabbedPane) c;
			tabs[0].setSelectedIndex(2); ((JTextField) find(panel[0], "wikiSearch")).setText("vorkath"); ((JButton) find(panel[0], "Search Wiki")).doClick();
			((JList<?>) find(panel[0], "wikiSuggestions")).setSelectedIndex(0);
		});
		assertEquals(WikiClient.articleUrl("Vorkath/Strategies"), host.firstPage.get(3, TimeUnit.SECONDS));
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals("Selection automatically fetches exactly once", 1, host.urls.size()); host.replies.get(0).accept(fixture, null);
			assertNotNull(find(panel[0], "wikiPreset:Ranged (crossbow)")); assertNotNull(find(panel[0], "wikiPreset:Ranged (blowpipe)")); assertNotNull(find(panel[0], "wikiPreset:Melee"));
			JTextField url = (JTextField) find(panel[0], "wikiPageUrl");
			url.setText(WikiClient.articleUrl("Zulrah/Strategies")); url.postActionEvent();
			url.setText(WikiClient.articleUrl("Vorkath/Strategies")); url.postActionEvent();
			host.replies.get(1).accept(null, "Old page failure"); assertFalse(((JTextArea) find(panel[0], "wikiPageStatus")).getText().contains("Old page failure"));
			host.replies.get(2).accept(fixture, null);
			((JButton) find(panel[0], "wikiPreset:Melee")).doClick(); assertEquals(1, tabs[0].getSelectedIndex());
			assertEquals("Vorkath", SetupCodec.name(host.selectedBankSetup));
			assertEquals("Torva platebody", SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4).getAsJsonObject().get("label").getAsString());
			assertEquals(22978, SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(3)));
            assertEquals(26384, SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4)));
            assertTrue("Concrete gear should request sprites", host.described.contains(22978));
            assertTrue(host.described.contains(26384));
            assertEquals(13441, SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(8)));
            assertTrue("Inventory sprites must be requested too", host.described.contains(13441));
            assertEquals("Do not guess Cheap prayer templates", 0, SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(5)));
            panel[0].dispose(); host.replies.get(2).accept(null, "Late page failure"); assertFalse(((JTextArea) find(panel[0], "wikiPageStatus")).getText().contains("Late page failure"));
		});
	}
	@Test public void selectingRedirectAliasKeepsUrlAndUsesCanonicalSetupName() throws Exception
	{
		System.setProperty("java.awt.headless", "true"); CompletableFuture<String> fetched = new CompletableFuture<>(); StudioPanel[] panel = new StudioPanel[1];
		PanelTest.FakeHost host = new PanelTest.FakeHost()
		{
			@Override public void searchWiki(String query, BiConsumer<List<String>, String> callback) { callback.accept(Collections.singletonList("Fractured archive strategies"), null); }
			@Override public void fetchWikiPage(String url, boolean preferStrategies, BiConsumer<WikiClient.Page, String> callback)
			{
				assertTrue(preferStrategies); callback.accept(new WikiClient.Page("The Fractured Archive/Strategies", "{{Equipment|head=Torva full helm}}{{Inventory|1=Shark}}"), null); fetched.complete(url);
			}
		};
		SwingUtilities.invokeAndWait(() ->
		{
			panel[0] = new StudioPanel(host, RuneLiteAPI.GSON);
			for (Component c : panel[0].getComponents()) if (c instanceof JTabbedPane) ((JTabbedPane) c).setSelectedIndex(2);
			((JTextField) find(panel[0], "wikiSearch")).setText("fractured archive"); ((JButton) find(panel[0], "Search Wiki")).doClick(); ((JList<?>) find(panel[0], "wikiSuggestions")).setSelectedIndex(0);
		});
		assertEquals(WikiClient.articleUrl("Fractured archive strategies"), fetched.get(3, TimeUnit.SECONDS));
		SwingUtilities.invokeAndWait(() -> { ((JButton) find(panel[0], "wikiPreset:Wiki preset 1")).doClick(); assertEquals("The Fractured Archive", SetupCodec.name(host.selectedBankSetup)); panel[0].dispose(); });
	}

    @Test public void wikiChoiceWaitsForCatalogueAndIgnoresLateResultAfterAnotherDraft() throws Exception
    {
        System.setProperty("java.awt.headless", "true");
        java.util.List<java.util.function.Consumer<String>> ready = new ArrayList<>();
        PanelTest.FakeHost host = new PanelTest.FakeHost()
        {
            @Override public void whenItemsReady(java.util.function.Consumer<String> callback) { ready.add(callback); }
            @Override public void fetchWikiPage(String url, boolean strategies, BiConsumer<WikiClient.Page, String> callback)
            { callback.accept(new WikiClient.Page("Vorkath/Strategies", "{{Equipment|head=Torva full helm}}{{Inventory|1=Anglerfish}}"), null); }
        };
        SwingUtilities.invokeAndWait(() ->
        {
            StudioPanel panel = new StudioPanel(host, RuneLiteAPI.GSON);
            for (Component c : panel.getComponents()) if (c instanceof JTabbedPane) ((JTabbedPane)c).setSelectedIndex(2);
            JTextField url = (JTextField)find(panel, "wikiPageUrl"); url.setText(WikiClient.articleUrl("Vorkath/Strategies")); url.postActionEvent();
            ((JButton)find(panel, "wikiPreset:Wiki preset 1")).doClick();
            assertEquals(1, ready.size()); assertNull(host.selectedBankSetup);
            ready.get(0).accept(null); assertEquals("Vorkath", SetupCodec.name(host.selectedBankSetup));
            ((JButton)find(panel, "Save")).doClick();
            ((JButton)find(panel, "wikiPreset:Wiki preset 1")).doClick();
            panel.newDraft(SetupCodec.blank("Another draft"));
            ready.get(1).accept(null);
            assertEquals("Another draft", SetupCodec.name(host.selectedBankSetup)); panel.dispose();
        });
    }

}
