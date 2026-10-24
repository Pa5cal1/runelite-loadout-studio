/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class WikiPageTest
{
	static String vorkath() throws Exception
	{
		try (java.io.InputStream in = WikiPageTest.class.getResourceAsStream("/wiki/vorkath-strategies.wikitext")) { assertNotNull(in); return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
	}
	@Test public void vorkathTemplatesCanBeResolvedWithoutNullSlots() throws Exception
	{
		java.util.List<JsonObject> presets = new WikiParser().parse(vorkath(), "Vorkath/Strategies", "Wiki fixture");
		assertEquals(3, presets.size());
		assertEquals("Ranged (crossbow)", WikiParser.variant(presets.get(0)));
		assertEquals("Ranged (blowpipe)", WikiParser.variant(presets.get(1)));
		assertEquals("Melee", WikiParser.variant(presets.get(2)));
		JsonArray eq = SetupCodec.setup(presets.get(0)).getAsJsonArray("eq");
		assertEquals("Elite void top", eq.get(4).getAsJsonObject().get("label").getAsString());
		assertEquals("Void knight gloves", eq.get(9).getAsJsonObject().get("label").getAsString());
		assertEquals("Avernic treads (max)", eq.get(10).getAsJsonObject().get("label").getAsString());
		assertEquals("Vorkath (Ranged (crossbow))", SetupCodec.name(presets.get(0)));
		for (JsonObject p : presets) for (String key : new String[]{"eq", "inv", "rp", "qv"}) for (JsonElement e : SetupCodec.setup(p).getAsJsonArray(key))
		{
			assertNotNull("Empty Wiki slots must use JsonNull, never Java null: " + key, e);
			if (!e.isJsonNull()) assertNotNull(e.getAsJsonObject().get("label"));
		}
	}
	@Test public void missingPouchDoesNotBorrowOneFromNextTab()
	{
		String text = "<tabber>\nMelee=\n{{Equipment|torso=Torva platebody}}\n{{Inventory|1=Shark}}\n|-|\nRanged=\n{{Equipment|torso=Elite void top}}\n{{Inventory|1=Anglerfish}}\n{{Rune pouch|1=Law rune}}\n</tabber>";
		java.util.List<JsonObject> presets = new WikiParser().parse(text, "Example/Strategies", "Fixture");
		assertEquals(2, presets.size());
		assertTrue(SetupCodec.setup(presets.get(0)).getAsJsonArray("rp").get(0).isJsonNull());
		assertEquals("Law rune", SetupCodec.setup(presets.get(1)).getAsJsonArray("rp").get(0).getAsJsonObject().get("label").getAsString());
	}
}
