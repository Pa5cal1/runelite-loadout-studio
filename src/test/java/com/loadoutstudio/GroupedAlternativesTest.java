/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class GroupedAlternativesTest {
 private static JsonObject food(){JsonObject p=SetupCodec.blank("Food swaps");for(int i=0;i<20;i++){JsonObject item=SetupCodec.item(ItemID.MANTARAY,1);item.addProperty("label","Manta ray");item.addProperty("f",i%2==0);SetupCodec.setup(p).getAsJsonArray("inv").set(i,item);}SetupCodec.rebuildLayout(p);return p;}
 private static OwnedGear gear(int sharks){Map<Integer,Integer> counts=new LinkedHashMap<>();counts.put(ItemID.SHARK,sharks);Map<Integer,StudioPanel.ItemInfo> items=new LinkedHashMap<>();items.put(ItemID.MANTARAY,OwnedGearTest.item(ItemID.MANTARAY,"Manta ray",-1));items.put(ItemID.SHARK,OwnedGearTest.item(ItemID.SHARK,"Shark",-1));return new OwnedGear(counts,items,123,true);}
 private static void assertFood(StudioPanel panel,OwnedGearPanelTest.Host host,int id){OwnedGearPanelTest.click(panel,"Save");JsonObject saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable;for(int i=0;i<20;i++){JsonObject item=SetupCodec.setup(saved).getAsJsonArray("inv").get(i).getAsJsonObject();assertEquals(id,SetupCodec.id(item));assertEquals(1,SetupCodec.quantity(item));assertEquals(i%2==0,item.get("f").getAsBoolean());}}
 @Test public void twentyFoodSlotsShareOneChoiceAndApplySaveAndUndoTogether()throws Exception{SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();host.owned=gear(656);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(food());panel.refreshAlternatives();JComboBox<?> choice=(JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:inv:0");assertNotNull(choice);assertNull(OwnedGearPanelTest.find(panel,"ownedChoice:inv:1"));assertNotNull(OwnedGearPanelTest.find(panel,"Inventory ×20: Manta ray"));
  OwnedGearPanelTest.click(panel,"Use selected alternatives");assertFood(panel,host,ItemID.MANTARAY);panel.refreshAlternatives();((JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:inv:0")).setSelectedIndex(1);OwnedGearPanelTest.click(panel,"Use selected alternatives");assertFood(panel,host,ItemID.SHARK);OwnedGearPanelTest.click(panel,"Undo alternatives");assertFood(panel,host,ItemID.MANTARAY);panel.dispose();
 });}
 @Test public void groupedChoiceReservesStockForEverySlotWithoutPartialReplacement()throws Exception{SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();host.owned=gear(19);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(food());panel.refreshAlternatives();((JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:inv:0")).setSelectedIndex(1);OwnedGearPanelTest.click(panel,"Use selected alternatives");assertFood(panel,host,ItemID.MANTARAY);panel.dispose();
 });}
 @Test public void differentSourceRecommendationsRemainSeparate(){
  StudioPanel.ItemInfo shark=OwnedGearTest.item(ItemID.SHARK,"Shark",-1),monkfish=OwnedGearTest.item(ItemID.MONKFISH,"Monkfish",-1);JsonObject before=SetupCodec.item(ItemID.MANTARAY,1);
  OwnedGear.Proposal a=new OwnedGear.Proposal("inv",0,before,Arrays.asList(shark,monkfish),"Wiki"),b=new OwnedGear.Proposal("inv",1,before,Arrays.asList(monkfish,shark),"Wiki"),c=new OwnedGear.Proposal("inv",2,before,Collections.singletonList(shark),"Wiki");assertEquals(3,OwnedGear.grouped(Arrays.asList(a,b,c)).size());
 }
}
