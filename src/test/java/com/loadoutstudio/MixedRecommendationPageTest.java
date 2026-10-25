/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class MixedRecommendationPageTest {
 @Test public void inventoryWithAdjacentRecommendationsGetsItsOwnEquipmentAndPouch(){
  String source="<tabber>\nMagic=\n{{Inventory|1=Shark}}{{Rune pouch|1=Fire rune}}{{Recommended equipment|style=Mage|head1={{plink|Ancestral hat}}}}\n|-|\nRanged=\n{{Inventory|1=Manta ray}}{{Recommended equipment|style=Range|head1={{plink|Masori mask}}}}\n</tabber>";
  List<JsonObject> presets=new WikiParser().parse(source,"Maggot King/Strategies","Example source");assertEquals(2,presets.size());
  assertEquals("Ancestral hat",SetupCodec.setup(presets.get(0)).getAsJsonArray("eq").get(0).getAsJsonObject().get("label").getAsString());assertEquals("Masori mask",SetupCodec.setup(presets.get(1)).getAsJsonArray("eq").get(0).getAsJsonObject().get("label").getAsString());assertFalse(SetupCodec.setup(presets.get(0)).getAsJsonArray("rp").get(0).isJsonNull());assertEquals("Shark",SetupCodec.setup(presets.get(0)).getAsJsonArray("inv").get(0).getAsJsonObject().get("label").getAsString());
 }
 @Test public void recommendationOnlyStylesAreNotDroppedWhenAnotherStyleHasInventory(){
  String source="<tabber>\nMagic=\n{{Inventory|1=Shark}}{{Recommended equipment|style=Mage|head1={{plink|Ancestral hat}}}}\n|-|\nMelee=\n{{Recommended equipment|style=Melee|weapon1={{plink|Elder maul}}}}\n</tabber>";List<JsonObject> presets=new WikiParser().parse(source,"Maggot King/Strategies","Example source");assertEquals(2,presets.size());assertTrue(SetupCodec.name(presets.get(1)).contains("Melee"));assertEquals("Elder maul",SetupCodec.setup(presets.get(1)).getAsJsonArray("eq").get(3).getAsJsonObject().get("label").getAsString());assertTrue(SetupCodec.setup(presets.get(1)).getAsJsonArray("inv").get(0).isJsonNull());
 }
 @Test public void explicitEquipmentKeepsItsDeliberatelyEmptySlots(){
  List<JsonObject> presets=new WikiParser().parse("{{Equipment|weapon=Rune scimitar}}{{Inventory|1=Shark}}{{Recommended equipment|style=Melee|head1={{plink|Torva full helm}}}}","Example/Strategies","Example source");assertTrue(SetupCodec.setup(presets.get(0)).getAsJsonArray("eq").get(0).isJsonNull());
 }
}
