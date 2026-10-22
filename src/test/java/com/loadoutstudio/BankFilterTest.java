package com.loadoutstudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.function.IntPredicate;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankFilterTest
{
	private JsonObject setup()
	{
		JsonObject p = SetupCodec.blank("Bank fixture"), s = SetupCodec.setup(p);
		s.getAsJsonArray("eq").set(0, SetupCodec.item(1, 1)); s.getAsJsonArray("eq").set(1, SetupCodec.item(2, 1)); s.getAsJsonArray("eq").set(3, SetupCodec.item(3, 1));
		for (int i = 0; i < 28; i++) s.getAsJsonArray("inv").set(i, SetupCodec.item(100 + i, 1));
		s.getAsJsonArray("rp").set(0, SetupCodec.item(201, 16000)); s.getAsJsonArray("qv").set(0, SetupCodec.item(501, 300));
		JsonArray bolts = new JsonArray(); bolts.add(SetupCodec.item(301, 10)); s.add("bp", bolts); s.getAsJsonObject("afi").add("401", SetupCodec.item(401, 1));
		p.add("layout", BankLayouts.generate(p, BankLayouts.Mode.PRESET, false)); return p;
	}
	@Test public void presetPositionsIncludeInventoryHolesAndExtraContainers()
	{
		JsonObject p = setup(); SetupCodec.setup(p).getAsJsonArray("inv").set(1, com.google.gson.JsonNull.INSTANCE);
		JsonArray l = BankLayouts.generate(p, BankLayouts.Mode.PRESET, false);
		assertEquals(1, l.get(1).getAsInt()); assertEquals(501, l.get(2).getAsInt()); assertEquals(100, l.get(4).getAsInt()); assertEquals(-1, l.get(5).getAsInt()); assertEquals(104, l.get(12).getAsInt()); assertEquals(201, l.get(40).getAsInt()); assertEquals(301, l.get(48).getAsInt()); assertEquals(401, l.get(56).getAsInt());
	}
	@Test public void emptySetupDefaultsToPresetAndZigzagStartsOnTop()
	{
		JsonObject p = SetupCodec.blank("Empty"); assertEquals(BankLayouts.Mode.PRESET, BankLayouts.infer(p)); assertFalse(BankLayouts.inferBottom(p));
	}
	@Test public void zigzagTopToBottomMatchesInventorySetupsSequence()
	{
		JsonArray l = BankLayouts.generate(setup(), BankLayouts.Mode.ZIGZAG, false);
		int[] positions = {16,24,17,25,18,26,19,27,20,28,21,29,22,30,23,31,32,40};
		for (int i = 0; i < positions.length; i++) assertEquals(100 + i, l.get(positions[i]).getAsInt());
		assertEquals(1, l.get(0).getAsInt()); assertEquals(2, l.get(8).getAsInt()); assertEquals(3, l.get(1).getAsInt()); assertEquals(501, l.get(9).getAsInt()); assertEquals(201, l.get(48).getAsInt()); assertEquals(301, l.get(52).getAsInt());
	}
	@Test public void zigzagBottomToTopAndDuplicatesArePreserved()
	{
		JsonObject p = setup(); SetupCodec.setup(p).getAsJsonArray("inv").set(1, SetupCodec.item(100, 1));
		JsonArray l = BankLayouts.generate(p, BankLayouts.Mode.ZIGZAG, true);
		assertEquals(100, l.get(24).getAsInt()); assertEquals(100, l.get(16).getAsInt()); assertEquals(102, l.get(25).getAsInt()); assertEquals(103, l.get(17).getAsInt());
	}
	@Test public void importedLayoutRetainsHolesAndAddsNewItems()
	{
		JsonObject p = setup(); JsonArray old = new JsonArray(); old.add(-1); old.add(100); old.add(-1); old.add(100); p.add("layout", old);
		JsonArray l = BankLayouts.generate(p, BankLayouts.Mode.CUSTOM, false); assertEquals(old, new JsonArraySlice(l, 4).value); assertTrue(l.contains(new com.google.gson.JsonPrimitive(201)));
	}
	private static final class JsonArraySlice { final JsonArray value = new JsonArray(); JsonArraySlice(JsonArray source, int size) { for (int i = 0; i < size; i++) value.add(source.get(i)); } }
	static final class FakeBank implements StudioBankFilter.Gateway
	{
		boolean open; String active; IntPredicate matches; int opened, closed, capacity = 1000; int[] layout;
		public boolean bankOpen() { return open; }
		public int capacity() { return capacity; }
		public String activeTag() { return active; }
		public int canonicalize(int id) { return id == 1000 ? 100 : id; }
		public int variation(int id) { return id == 150 ? 100 : id; }
		public void register(String tag, IntPredicate matches) { this.matches = matches; }
		public void open(String tag, int[] layout) { active = tag; opened++; this.layout = layout; }
		public void close() { active = null; closed++; }
		public void unregister(String tag) { matches = null; }
		public void discardLayout(String tag) { }
	}
	@Test public void filterWaitsForBankAndUsesImmutableSelection()
	{
		FakeBank bank = new FakeBank(); StudioBankFilter filter = new StudioBankFilter(bank, "studio"); JsonObject p = setup(); assertTrue(filter.select(p).contains("Open your Bank"));
		SetupCodec.setup(p).getAsJsonArray("inv").set(0, SetupCodec.item(999, 1)); bank.open = true; filter.apply(); assertTrue(bank.matches.test(100)); assertFalse(bank.matches.test(999)); assertEquals(1, bank.opened);
	}
	@Test public void exactAndFuzzyMembershipIncludeAllSetupContainers()
	{
		FakeBank bank = new FakeBank(); bank.open = true; StudioBankFilter filter = new StudioBankFilter(bank, "studio"); JsonObject p = setup(); filter.select(p);
		for (int id : new int[]{1, 100, 1000, 201, 301, 401, 501}) assertTrue(bank.matches.test(id)); assertFalse(bank.matches.test(150)); assertFalse(bank.matches.test(-1));
		SetupCodec.setup(p).getAsJsonArray("inv").get(0).getAsJsonObject().addProperty("f", true); filter.select(p); assertTrue(bank.matches.test(150));
	}
	@Test public void clearClosesOnlyTheOwnedTagAndStopsAutoReopening()
	{
		FakeBank bank = new FakeBank(); bank.open = true; StudioBankFilter filter = new StudioBankFilter(bank, "studio"); filter.select(setup()); bank.active = "user-raid-tag"; filter.clear(); assertEquals(0, bank.closed); assertEquals("user-raid-tag", bank.active); assertNull(bank.matches); int count = bank.opened; filter.apply(); assertEquals(count, bank.opened);
		filter.select(setup()); filter.clear(); assertEquals(1, bank.closed); assertNull(bank.active);
	}
	@Test public void filterOffDoesNotOpenBankTag()
	{
		FakeBank bank = new FakeBank(); bank.open = true; StudioBankFilter filter = new StudioBankFilter(bank, "studio"); JsonObject p = setup(); SetupCodec.setup(p).addProperty("fb", false); filter.select(p); assertEquals(0, bank.opened);
	}
	@Test(expected = IllegalArgumentException.class) public void unresolvedItemBlocksFilter()
	{
		JsonObject p = setup(); SetupCodec.setup(p).getAsJsonArray("inv").set(0, SetupCodec.item(0, 1)); new StudioBankFilter(new FakeBank(), "studio").select(p);
	}
	@Test(expected = IllegalArgumentException.class) public void excessiveRuntimeLayoutIsRejected()
	{
		FakeBank bank = new FakeBank(); bank.open = true; bank.capacity = 10; new StudioBankFilter(bank, "studio").select(setup());
	}
	@Test public void libraryPersistsPerSetupOptionsAndExportsRetainGeneratedPositions()
	{
		JsonObject p = setup(); p.add("layout", BankLayouts.generate(p, BankLayouts.Mode.ZIGZAG, true)); SetupLibrary library = new SetupLibrary(); SetupLibrary.Entry e = new SetupLibrary.Entry(p); e.bankLayout = BankLayouts.Mode.ZIGZAG; e.zigzagBottomToTop = true; library.setups.add(e);
		SetupLibrary restored = SetupLibrary.read(library.serialize(RuneLiteAPI.GSON), RuneLiteAPI.GSON); assertEquals(BankLayouts.Mode.ZIGZAG, restored.setups.get(0).bankLayout); assertTrue(restored.setups.get(0).zigzagBottomToTop);
		SetupCodec codec = new SetupCodec(RuneLiteAPI.GSON); assertEquals(p.get("layout"), codec.importText(codec.inventory(p)).get(0).get("layout")); assertTrue(codec.bank(p, false).contains(",24,100,")); assertTrue(codec.bank(p, true).contains(",100:24,"));
	}
}
