/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ComparisonTest
{
    @Test public void matchingHonorsEveryStackRuleAndUnlimitedStillRequiresTheItem()
    {
        JsonObject expected=SetupCodec.item(100,100),actual=SetupCodec.item(100,50);
        assertTrue(SetupComparison.matches(expected,actual,false,id->id));expected.addProperty("sc","Standard");assertFalse(SetupComparison.matches(expected,actual,false,id->id));actual.addProperty("q",100);assertTrue(SetupComparison.matches(expected,actual,false,id->id));
        expected.addProperty("sc","Less_Than");actual.addProperty("q",150);assertTrue(SetupComparison.matches(expected,actual,false,id->id));actual.addProperty("q",50);assertFalse(SetupComparison.matches(expected,actual,false,id->id));
        expected.addProperty("sc","Greater_Than");assertTrue(SetupComparison.matches(expected,actual,false,id->id));actual.addProperty("q",150);assertFalse(SetupComparison.matches(expected,actual,false,id->id));assertTrue(SetupComparison.matches(expected,actual,true,id->id));
        expected.addProperty("q",0);assertTrue(SetupComparison.matches(expected,actual,false,id->id));actual.addProperty("id",101);assertFalse(SetupComparison.matches(expected,actual,false,id->id));expected.addProperty("f",true);assertTrue(SetupComparison.matches(expected,actual,false,id->id==101?100:id));assertFalse(SetupComparison.matches(expected,JsonNull.INSTANCE,false,id->id));
    }
    @Test public void unorderedInventoryUsesDistinctMatchesAndRetainsExactVariants()
    {
        JsonObject saved=SetupCodec.blank("Test"),actual=SetupCodec.blank("Actual");JsonObject fuzzy=SetupCodec.item(100,1);fuzzy.addProperty("f",true);SetupCodec.setup(saved).addProperty("uh",true);SetupCodec.setup(saved).getAsJsonArray("inv").set(0,fuzzy);SetupCodec.setup(saved).getAsJsonArray("inv").set(1,SetupCodec.item(100,1));SetupCodec.setup(actual).getAsJsonArray("inv").set(7,SetupCodec.item(100,1));SetupCodec.setup(actual).getAsJsonArray("inv").set(8,SetupCodec.item(101,1));
        assertTrue(SetupComparison.differences(saved,actual,false,id->id==101?100:id).isEmpty());SetupCodec.setup(actual).getAsJsonArray("inv").set(8,JsonNull.INSTANCE);assertFalse(SetupComparison.differences(saved,actual,false,id->id==101?100:id).isEmpty());SetupCodec.setup(saved).addProperty("uh",false);assertFalse(SetupComparison.differences(saved,actual,false,id->id).isEmpty());
    }
    @Test public void missingRuneAmountsRemainUnresolvedAndSpellbookDifferencesAreReported()
    {
        JsonObject item=SetupCodec.item(560,1);item.add("q",JsonNull.INSTANCE);assertFalse(SetupComparison.matches(item,SetupCodec.item(560,500),false,id->id));JsonObject a=SetupCodec.blank("A"),b=SetupCodec.blank("B");SetupCodec.setup(a).addProperty("sb",1);assertTrue(SetupComparison.differences(a,b,false,id->id).contains("spellbook"));
    }
    @Test public void nativeFuzzyGroupsIncludeInventorySetupsSpecificAlternatives()
    {
        assertEquals(SetupVariationMapping.map(net.runelite.api.gameval.ItemID.SARADOMIN_CAPE),SetupVariationMapping.map(net.runelite.api.gameval.ItemID.GUTHIX_CAPE));
        assertEquals(SetupVariationMapping.map(net.runelite.api.gameval.ItemID.DIVINE_RUNE_POUCH),SetupVariationMapping.map(net.runelite.api.gameval.ItemID.BH_RUNE_POUCH));
        JsonObject saved=SetupCodec.item(net.runelite.api.gameval.ItemID.SARADOMIN_CAPE,1),actual=SetupCodec.item(net.runelite.api.gameval.ItemID.GUTHIX_CAPE,1);assertFalse(SetupComparison.matches(saved,actual,false,SetupVariationMapping::map));saved.addProperty("f",true);assertTrue(SetupComparison.matches(saved,actual,false,SetupVariationMapping::map));
    }

}
