/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import javax.swing.*;
import org.junit.Test;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import static org.junit.Assert.*;
public class IdentityMigrationTest {
 @Test public void knownBetaRingsAreRepairedButExplicitOtherIdsRemain(){assertEquals(ItemID.ULTOR_RING,ItemCatalogue.legacyRingId(ItemID.BETA_ITEM_2,"Ultor ring"));assertEquals(ItemID.MAGUS_RING,ItemCatalogue.legacyRingId(ItemID.BETA_ITEM_3,"Magus ring"));assertEquals(28308,ItemCatalogue.legacyRingId(28308,"Ultor ring"));assertEquals(100,ItemCatalogue.legacyRingId(100,"Fixture ring"));}
 @Test public void oldWikiRingRepairKeepsCustomPositionsAndNeedsExplicitSave() throws Exception {SwingUtilities.invokeAndWait(()->{
  PanelTest.FakeHost host=new PanelTest.FakeHost(){@Override public StudioPanel.ItemInfo defaultItem(String name){return name.equals("Ultor ring")?new StudioPanel.ItemInfo(ItemID.ULTOR_RING,"Ultor ring",false,12,null):null;}};
  JsonObject source=SetupCodec.blank("Old wiki");JsonObject ring=SetupCodec.item(ItemID.BETA_ITEM_2,1);ring.addProperty("label","Ultor ring");ring.addProperty("f",true);SetupCodec.setup(source).getAsJsonArray("eq").set(12,ring);JsonArray custom=new JsonArray();custom.add(-1);custom.add(-1);custom.add(ItemID.BETA_ITEM_2);source.add("layout",custom);SetupLibrary.Entry entry=new SetupLibrary.Entry(source);entry.bankLayout=BankLayouts.Mode.CUSTOM;StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry);
  assertEquals(ItemID.ULTOR_RING,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(12)));assertEquals(ItemID.ULTOR_RING,host.selectedBankSetup.getAsJsonArray("layout").get(2).getAsInt());assertNull(host.saved);assertEquals(ItemID.BETA_ITEM_2,SetupCodec.id(SetupCodec.setup(source).getAsJsonArray("eq").get(12)));OwnedGearPanelTest.click(panel,"Save");assertNotNull(host.saved);panel.dispose();
 });}
}
