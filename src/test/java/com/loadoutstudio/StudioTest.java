package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class StudioTest
{
	private final Gson gson = RuneLiteAPI.GSON;
	private final SetupCodec codec = new SetupCodec(gson);
	private JsonObject example()
	{
		JsonObject p = SetupCodec.blank("Nechryael"); JsonObject s = SetupCodec.setup(p);
		s.getAsJsonArray("inv").set(0, SetupCodec.item(6685, 1)); s.getAsJsonArray("inv").set(1, SetupCodec.item(6685, 1));
		s.getAsJsonArray("eq").set(3, SetupCodec.item(20997, 1)); s.getAsJsonArray("rp").set(0, SetupCodec.item(560, 16000));
		s.getAsJsonArray("qv").set(0, SetupCodec.item(33595, 1396)); s.addProperty("notes", "Review quantities\nSecond line");
		s.getAsJsonArray("inv").get(0).getAsJsonObject().addProperty("f", true); SetupCodec.rebuildLayout(p); return p;
	}
	@Test public void inventoryRoundTripPreservesSlotsNameFlagsAndLayout()
	{
		JsonObject p = example(); assertEquals(p, codec.importText(codec.inventory(p)).get(0));
		assertEquals(28, SetupCodec.setup(p).getAsJsonArray("inv").size());
	}
	@Test public void nativeBankRoundTripPreservesTagNameFuzzyAndPositions()
	{
		JsonObject p = example(); String bank = codec.bank(p, false); assertTrue(bank.startsWith("banktags,1,Nechryael,")); assertTrue(bank.contains(",-6685,"));
		JsonObject restored = codec.importText(bank).get(0); assertEquals("Nechryael", SetupCodec.name(restored));
		for (int i = 0; i < 56; i++) if (p.getAsJsonArray("layout").get(i).getAsInt() > 0) assertEquals(p.getAsJsonArray("layout").get(i), restored.getAsJsonArray("layout").get(i));
		assertTrue(codec.inventory(restored).contains("Imported from a Bank Tag"));
	}
	@Test public void legacyBankRoundTrip() { JsonObject p = example(); assertEquals("Nechryael", SetupCodec.name(codec.importText(codec.bank(p, true)).get(0))); }
	@Test public void quantitiesAcceptK() { assertEquals(16000, SetupCodec.parseQuantity("16k")); assertEquals(1500, SetupCodec.parseQuantity("1.5K")); }
	@Test(expected = IllegalArgumentException.class) public void fractionalRuneRejected() { SetupCodec.parseQuantity("0.0001k"); }
	@Test(expected = IllegalArgumentException.class) public void runeOverflowRejected() { JsonObject p = example(); SetupCodec.setup(p).getAsJsonArray("rp").set(0, SetupCodec.item(560, 16001)); codec.inventory(p); }
	@Test(expected = IllegalArgumentException.class) public void incompleteDraftCannotExport() { JsonObject p = example(); SetupCodec.setup(p).getAsJsonArray("inv").set(3, SetupCodec.item(0, 1)); codec.inventory(p); }
	@Test(expected = IllegalArgumentException.class) public void excessiveNestingRejected() { codec.parse("[".repeat(60) + "]".repeat(60)); }
	@Test(expected = IllegalArgumentException.class) public void externalUrlRejected() { WikiClient.title("https://example.com/w/Vorkath"); }
	@Test(expected = IllegalArgumentException.class) public void deceptiveUrlRejected() { WikiClient.title("https://oldschool.runescape.wiki@localhost/w/Vorkath"); }
	@Test public void wikiUnicodeUrlAndSearchResponse()
	{
		assertEquals("Chambers of Xeric/Strategies", WikiClient.title(WikiClient.articleUrl("Chambers of Xeric/Strategies")));
		assertEquals("Vorkath", WikiClient.searchTitles("[\"Vork\",[\"Vorkath\"],[\"\"],[\"https://ignored.invalid\"]]", gson).get(0));
	}
	@Test public void multilineWikiTemplatesStayInTheirColumn()
	{
		String text = "{| class=\"wikitable\"\n! Expert !! Budget\n|-\n| {{Equipment\n|head=Torva full helm\n|weapon=Twisted bow\n}}\n{{Inventory\n|1=Saradomin brew(4)\n|2=Shark\n}}\n| {{Equipment\n|head=Neitiznot faceguard\n}}\n{{Inventory\n|1=Shark\n}}\n|}";
		java.util.List<JsonObject> p = new WikiParser().parse(text, "Vorkath/Strategies", "Fixture"); assertEquals(2, p.size());
		assertEquals("Torva full helm", SetupCodec.setup(p.get(0)).getAsJsonArray("eq").get(0).getAsJsonObject().get("label").getAsString());
		assertEquals("Neitiznot faceguard", SetupCodec.setup(p.get(1)).getAsJsonArray("eq").get(0).getAsJsonObject().get("label").getAsString());
		assertEquals("Vorkath", SetupCodec.name(p.get(0)));
	}
	@Test public void sectionDeletionMovesSetupsToUnfiledAndBackupIsIndependent()
	{
		SetupLibrary l = new SetupLibrary(); SetupLibrary.Section section = new SetupLibrary.Section("PvM"); l.sections.add(section); SetupLibrary.Entry e = new SetupLibrary.Entry(example()); e.section = section.id; e.favorite = true; l.setups.add(e);
		SetupLibrary copy = SetupLibrary.read(l.serialize(gson), gson); l.removeSection(section.id); assertNull(l.setups.get(0).section); assertEquals(section.id, copy.setups.get(0).section); assertTrue(copy.setups.get(0).favorite);
	}
}
