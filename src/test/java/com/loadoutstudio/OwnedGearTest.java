/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class OwnedGearTest {
 static StudioPanel.ItemInfo item(int id,String name,int slot){return new StudioPanel.ItemInfo(id,name,false,slot,null);}
 static OwnedGear gear(int amount,int slot){Map<Integer,Integer> amounts=new LinkedHashMap<>();amounts.put(2,amount);Map<Integer,StudioPanel.ItemInfo> items=new LinkedHashMap<>();items.put(1,item(1,"Bandos chestplate",4));items.put(2,item(2,"Fighter torso",slot));return new OwnedGear(amounts,items,123,true);}
 static JsonObject setup(){JsonObject p=SetupCodec.blank("Ironman");SetupCodec.setup(p).getAsJsonArray("eq").set(4,SetupCodec.item(1,1));SetupCodec.rebuildLayout(p);return p;}
 @Test public void missingBandosOffersObservedTorsoWithoutMutatingSetup(){JsonObject p=setup(),before=p.deepCopy();List<OwnedGear.Proposal> result=gear(1,4).alternatives(p,name->null);assertEquals(1,result.size());assertEquals(2,result.get(0).choices.get(0).id);assertEquals(p,before);}
 @Test public void placeholdersAndWrongSlotsAreNeverAlternatives(){assertTrue(gear(0,4).alternatives(setup(),name->null).isEmpty());assertTrue(gear(1,3).alternatives(setup(),name->null).isEmpty());}
 @Test public void existingWantedItemDoesNotTriggerReplacement(){OwnedGear base=gear(1,4);Map<Integer,Integer> amounts=new LinkedHashMap<>(base.amounts);amounts.put(1,1);assertTrue(new OwnedGear(amounts,base.items,123,true).alternatives(setup(),name->null).isEmpty());}
 @Test public void sourceChoicesUseTheActuallyOwnedSameNameVariant(){JsonObject p=setup();JsonArray options=new JsonArray();options.add("Alternative body");SetupCodec.setup(p).getAsJsonArray("eq").get(4).getAsJsonObject().add("choices",options);Map<Integer,StudioPanel.ItemInfo> items=new LinkedHashMap<>();items.put(3,item(3,"Alternative body",4));OwnedGear gear=new OwnedGear(Collections.singletonMap(3,1),items,123,true);List<OwnedGear.Proposal> result=gear.alternatives(p,name->item(2,name,4));assertEquals(3,result.get(0).choices.get(0).id);assertTrue(result.get(0).reason.startsWith("Wiki"));}
 @Test public void runeTypesAndUnknownFamiliesAreNotGuessed(){JsonObject p=SetupCodec.blank("Rune test");SetupCodec.setup(p).getAsJsonArray("rp").set(0,SetupCodec.item(1,1000));SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(999,1));assertTrue(gear(1,4).alternatives(p,name->null).isEmpty());}
}
