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
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiSearchPanelTest
{
	private static final class SearchHost extends PanelTest.FakeHost
	{
		final List<String> queries = new ArrayList<>();
		final List<BiConsumer<List<String>, String>> replies = new ArrayList<>();
		CompletableFuture<String> nextSearch = new CompletableFuture<>();
		@Override public void searchWiki(String query, BiConsumer<List<String>, String> callback)
		{
			queries.add(query); replies.add(callback); nextSearch.complete(query);
		}
	}
	private static Component find(Container root, String name)
	{
		for (Component child : root.getComponents())
		{
			if (name.equals(child.getName()) || child instanceof JButton && name.equals(((JButton) child).getText()) || child instanceof JCheckBox && name.equals(((JCheckBox) child).getText())) return child;
			if (child instanceof Container) { Component match = find((Container) child, name); if (match != null) return match; }
		}
		return null;
	}
	@Test public void searchesRequireExplicitActionIgnoreOldRepliesAndStopWhenDisposed() throws Exception
	{
		System.setProperty("java.awt.headless", "true");
		SearchHost host = new SearchHost(); StudioPanel[] panel = new StudioPanel[1]; JTextField[] query = new JTextField[1]; JList<?>[] suggestions = new JList<?>[1]; JButton[] search = new JButton[1];
		SwingUtilities.invokeAndWait(() ->
		{
			panel[0] = new StudioPanel(host, RuneLiteAPI.GSON);
			assertTrue("Opening the panel must not make network requests", host.queries.isEmpty());
			for (Component c : panel[0].getComponents()) if (c instanceof JTabbedPane) ((JTabbedPane) c).setSelectedIndex(2);
			query[0] = (JTextField) find(panel[0], "wikiSearch"); suggestions[0] = (JList<?>) find(panel[0], "wikiSuggestions"); search[0] = (JButton) find(panel[0], "Search Wiki");
			query[0].setText("Vork"); query[0].setText("Vorkath"); assertTrue(host.queries.isEmpty()); search[0].doClick();
		});
		assertEquals("Vorkath", host.nextSearch.get(3, TimeUnit.SECONDS));
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(1, host.queries.size());
			query[0].setText("Zulrah"); search[0].doClick();
			query[0].setText("Vorkath"); search[0].doClick();
			host.replies.get(0).accept(Collections.singletonList("Old Vorkath"), null);
			assertEquals(0, suggestions[0].getModel().getSize());
			host.replies.get(1).accept(Collections.singletonList("Zulrah"), null);
			assertEquals(0, suggestions[0].getModel().getSize());
			host.replies.get(2).accept(Collections.singletonList("Vorkath"), null);
			assertEquals("Vorkath", suggestions[0].getModel().getElementAt(0));
			panel[0].reloadScope(); host.replies.get(2).accept(Collections.singletonList("Late reply"), null);
			assertEquals("Vorkath", suggestions[0].getModel().getElementAt(0));
			query[0].setText("V"); search[0].doClick(); assertEquals(3, host.queries.size());
			assertNull("Wiki requests must not require a checkbox", find(panel[0], "Enable OSRS Wiki requests"));
			query[0].setText("Zulrah"); panel[0].dispose();
		});
		CompletableFuture<Void> elapsed = new CompletableFuture<>();
		SwingUtilities.invokeAndWait(() -> { Timer check = new Timer(500, e -> elapsed.complete(null)); check.setRepeats(false); check.start(); });
		elapsed.get(3, TimeUnit.SECONDS);
		SwingUtilities.invokeAndWait(() -> assertEquals("Disposal cancels pending suggestions", 3, host.queries.size()));
	}
}
