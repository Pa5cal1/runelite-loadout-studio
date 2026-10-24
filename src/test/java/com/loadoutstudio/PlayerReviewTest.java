/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import net.runelite.http.api.RuneLiteAPI;
import static org.junit.Assert.*;
public class PlayerReviewTest {
 @Test public void replacementKeepsCustomPositionsAndUndoRestoresThem() throws Exception {SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host h=new OwnedGearPanelTest.Host();JsonObject p=OwnedGearTest.setup();JsonArray cells=new JsonArray();for(int i=0;i<25;i++)cells.add(-1);cells.set(21,new JsonPrimitive(1));p.add("layout",cells);SetupLibrary.Entry e=new SetupLibrary.Entry(p);e.bankLayout=BankLayouts.Mode.CUSTOM;StudioPanel panel=new StudioPanel(h,RuneLiteAPI.GSON);panel.open(e);OwnedGear.Proposal proposal=h.owned.alternatives(p,n->null).get(0);panel.applyAlternatives(Collections.singletonMap(proposal,proposal.choices.get(0)));assertEquals(2,h.selectedBankSetup.getAsJsonArray("layout").get(21).getAsInt());assertEquals(25,h.selectedBankSetup.getAsJsonArray("layout").size());panel.undoAlternatives();assertEquals(cells,h.selectedBankSetup.get("layout"));panel.dispose();
 });}
 @Test public void anotherSlotUsingOldItemRetainsItsCustomPosition() throws Exception {SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host h=new OwnedGearPanelTest.Host();JsonObject p=OwnedGearTest.setup();SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(1,1));JsonArray cells=new JsonArray();for(int i=0;i<22;i++)cells.add(-1);cells.set(21,new JsonPrimitive(1));p.add("layout",cells);SetupLibrary.Entry e=new SetupLibrary.Entry(p);e.bankLayout=BankLayouts.Mode.CUSTOM;StudioPanel panel=new StudioPanel(h,RuneLiteAPI.GSON);panel.open(e);OwnedGear.Proposal proposal=h.owned.alternatives(p,n->null).stream().filter(a->a.container.equals("eq")).findFirst().get();panel.applyAlternatives(Collections.singletonMap(proposal,proposal.choices.get(0)));assertEquals(1,h.selectedBankSetup.getAsJsonArray("layout").get(21).getAsInt());assertEquals(2,h.selectedBankSetup.getAsJsonArray("layout").get(22).getAsInt());panel.dispose();
 });}
 @Test public void alternativesShowKnownNameWhenMissingItemIsNotInObservedBank() throws Exception {SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host h=new OwnedGearPanelTest.Host();JsonObject p=OwnedGearTest.setup();JsonObject item=SetupCodec.setup(p).getAsJsonArray("eq").get(4).getAsJsonObject();item.addProperty("label","Bandos chestplate");Map<Integer,StudioPanel.ItemInfo> onlyOwned=Collections.singletonMap(2,OwnedGearTest.item(2,"Fighter torso",4));h.owned=new OwnedGear(Collections.singletonMap(2,1),onlyOwned,123,true);StudioPanel panel=new StudioPanel(h,RuneLiteAPI.GSON);panel.newDraft(p);panel.refreshAlternatives();assertNotNull(OwnedGearPanelTest.find(panel,"Body: Bandos chestplate"));panel.dispose();
 });}
}
