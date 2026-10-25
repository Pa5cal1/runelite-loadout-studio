/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class StoragePouchAndNotesTest {
 @Test public void potionStorageAliasesUseTheSelectedDoseWithoutMultiplyingStock(){
  StudioPotionStorage cache=new StudioPotionStorage(null,null);StudioPotionStorage.Potion potion=new StudioPotionStorage.Potion(7,new int[]{101,102,103,104},23,4);cache.install(Collections.singletonList(potion),20);
  for(int id:new int[]{101,102,103,104})assertSame(potion,cache.forItem(id));assertEquals(104,potion.itemId());assertEquals(5,potion.count());assertEquals(35,potion.withdrawChild());assertEquals(36,potion.dosesChild());assertEquals(Collections.singletonMap(104,5),cache.counts());assertTrue(cache.current(potion));cache.invalidate();assertFalse(cache.current(potion));assertTrue(cache.counts().isEmpty());assertNull(cache.forItem(104));
 }
 @Test public void partialAndEmptyPotionStocksDoNotInventFullBottlesOrWithdrawActions(){
  StudioPotionStorage cache=new StudioPotionStorage(null,null);StudioPotionStorage.Potion partial=new StudioPotionStorage.Potion(0,new int[]{101,102,103,104},3,4),empty=new StudioPotionStorage.Potion(1,new int[]{201,202,203,204},0,4);cache.install(Arrays.asList(partial,empty),2);assertEquals(0,partial.count());assertTrue(cache.counts().isEmpty());assertNotNull(cache.forItem(101));assertNull(cache.forItem(201));String[] actions=StudioPotionStorage.actions(4,50,false);for(int i=0;i<10;i++)assertNull(actions[i]);assertEquals("Doses",actions[10]);
 }
 @Test public void storedPotionNativeOperationIdsFollowTheBankQuantitySetting(){
  String[] all=StudioPotionStorage.actions(4,50);assertEquals("Withdraw-All",all[0]);assertEquals("Withdraw-1",all[1]);assertEquals("Withdraw-5",all[2]);assertEquals("Withdraw-10",all[3]);assertEquals("Withdraw-50",all[4]);assertEquals("Withdraw-X",all[5]);assertEquals("Withdraw-All",all[6]);assertEquals("Withdraw-All-but-1",all[7]);assertEquals("Doses",all[10]);assertEquals("Withdraw-5",StudioPotionStorage.actions(1,0)[0]);assertNull(StudioPotionStorage.actions(0,0)[1]);assertNull(StudioPotionStorage.actions(0,0)[4]);
 }
 @Test public void changingStorageDosesInvalidatesOldTargetsAndClearDropsTheObservation(){
  StudioPotionStorage cache=new StudioPotionStorage(null,null);StudioPotionStorage.Potion four=new StudioPotionStorage.Potion(0,new int[]{101,102,103,104},20,4),two=new StudioPotionStorage.Potion(0,new int[]{101,102,103,104},20,2);cache.install(Collections.singletonList(four),1);cache.install(Collections.singletonList(two),1);assertFalse(cache.current(four));assertTrue(cache.current(two));assertEquals(Collections.singletonMap(102,10),cache.counts());cache.clear();assertTrue(cache.counts().isEmpty());assertNull(cache.forItem(102));
 }
 @Test public void divinePouchIsAnUpgradeButNotTheReverse(){assertTrue(EquipmentVariants.alternative(ItemID.DIVINE_RUNE_POUCH,ItemID.BH_RUNE_POUCH));assertFalse(EquipmentVariants.alternative(ItemID.BH_RUNE_POUCH,ItemID.DIVINE_RUNE_POUCH));}
 @Test public void builderUsesOwnedDivinePouchEvenWhenAnOrdinaryPouchIsAlsoOwned()throws Exception{SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();Map<Integer,Integer> counts=new LinkedHashMap<>();counts.put(ItemID.BH_RUNE_POUCH,1);counts.put(ItemID.DIVINE_RUNE_POUCH,1);Map<Integer,StudioPanel.ItemInfo> items=new HashMap<>();items.put(ItemID.BH_RUNE_POUCH,OwnedGearTest.item(ItemID.BH_RUNE_POUCH,"Rune pouch",-1));items.put(ItemID.DIVINE_RUNE_POUCH,OwnedGearTest.item(ItemID.DIVINE_RUNE_POUCH,"Divine rune pouch",-1));host.owned=new OwnedGear(counts,items,1,true);JsonObject source=SetupCodec.blank("Pouch upgrade");SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(ItemID.BH_RUNE_POUCH,1));SetupCodec.setup(source).getAsJsonArray("rp").set(0,SetupCodec.item(560,16001));SetupCodec.rebuildLayout(source);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(source);panel.refreshAlternatives();assertEquals(ItemID.DIVINE_RUNE_POUCH,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));assertEquals(16001,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));assertTrue(panel.autoSaveNow());assertEquals(ItemID.DIVINE_RUNE_POUCH,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("inv").get(0)));assertEquals(ItemID.BH_RUNE_POUCH,SetupCodec.id(SetupCodec.setup(source).getAsJsonArray("inv").get(0)));panel.dispose();
 });}
 @Test public void generatedNotesStayCompactButRankedAlternativesRemainAvailable(){
  String wiki="{{Recommended equipment|style=Magic|head1={{plink|Slayer helmet (i)}}{{efn|Only on task}} / {{plink|Ancestral hat}}|head2={{plink|Virtus mask}}}}";JsonObject p=new WikiParser().parse(wiki,"Maggot King","https://oldschool.runescape.wiki/w/Maggot_King/Strategies").get(0);String notes=SetupCodec.setup(p).get("notes").getAsString();assertEquals(2,notes.split("\\R").length);assertFalse(notes.contains("{{"));assertTrue(notes.length()<200);assertEquals(3,SetupCodec.setup(p).getAsJsonArray("eq").get(0).getAsJsonObject().getAsJsonArray("choices").size());
 }
 @Test public void legacyGeneratedNotesAreShortenedWithoutRemovingPersonalAdditions(){
  String source="Source: https://oldschool.runescape.wiki/w/Maggot_King/Strategies\nPreset: Magic\nReview Item IDs, quantities and Spellbook before use.\nWiki recommendation order (most to least effective); conditions:\nhead rank 1: {{plink|Slayer helmet (i)}}{{efn|Only on task}}\nbody rank 2: {{plink|Virtus robe top}}\nBring my own teleport";String compact=WikiParser.compactGeneratedNotes(source);assertFalse(compact.contains("{{"));assertTrue(compact.endsWith("Bring my own teleport"));assertTrue(compact.startsWith("Source:"));assertEquals("Personal notes\nhead rank 1: keep this",WikiParser.compactGeneratedNotes("Personal notes\nhead rank 1: keep this"));
 }
}
