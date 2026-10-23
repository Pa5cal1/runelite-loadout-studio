/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReplacementStockTest {
 private final StudioPanel.ItemInfo replacement=new StudioPanel.ItemInfo(2,"Replacement",true,-1,null);
 private JsonObject draft(int amount){JsonObject p=SetupCodec.blank("Stock");SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(1,amount));SetupCodec.setup(p).getAsJsonArray("inv").set(1,SetupCodec.item(1,amount));SetupCodec.rebuildLayout(p);return p;}
 private Map<OwnedGear.Proposal,StudioPanel.ItemInfo> choices(JsonObject p){Map<OwnedGear.Proposal,StudioPanel.ItemInfo> result=new LinkedHashMap<>();for(int i=0;i<2;i++)result.put(new OwnedGear.Proposal("inv",i,SetupCodec.setup(p).getAsJsonArray("inv").get(i),Collections.singletonList(replacement),"Fixture"),replacement);return result;}
 private OwnedGear gear(int count){return new OwnedGear(Collections.singletonMap(2,count),Collections.singletonMap(2,replacement),1,true);}
 @Test public void oneObservedItemCannotFillTwoReplacementSlots(){JsonObject p=draft(1),before=p.deepCopy();try{gear(1).requireReplacementStock(p,choices(p));fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("need 2"));}assertEquals(before,p);}
 @Test public void enoughCombinedStockIsAccepted(){JsonObject p=draft(25);gear(50).requireReplacementStock(p,choices(p));}
 @Test public void existingTargetSlotsAreReservedToo(){JsonObject p=draft(1);SetupCodec.setup(p).getAsJsonArray("eq").set(3,SetupCodec.item(2,1));try{gear(2).requireReplacementStock(p,choices(p));fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("need 3"));}}
 @Test public void unlimitedStillNeedsAnItemForEachSlotAndTotalsDoNotOverflow(){JsonObject p=draft(0);try{gear(1).requireReplacementStock(p,choices(p));fail();}catch(IllegalStateException expected){}JsonObject large=draft(Integer.MAX_VALUE);try{gear(Integer.MAX_VALUE).requireReplacementStock(large,choices(large));fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("4294967294"));}}
 @Test public void stockCheckIsConfigurableAndOnlySelectedTargetsAreChecked(){assertTrue(new LoadoutStudioConfig(){}.checkAlternativeStock());JsonObject p=draft(1);SetupCodec.setup(p).getAsJsonArray("inv").set(2,SetupCodec.item(99,200));gear(2).requireReplacementStock(p,choices(p));}
}
