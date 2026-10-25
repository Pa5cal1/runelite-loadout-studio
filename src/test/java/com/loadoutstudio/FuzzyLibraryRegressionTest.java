/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class FuzzyLibraryRegressionTest {
    @Test public void wikiAnchoredCapeUsesTheConcretePictureItem(){
        String link="{{plink|God capes#Imbuing|txt=Imbued god cape|pic=Imbued Saradomin cape}}";
        assertEquals("Imbued Saradomin cape",WikiParser.itemName(link));
        JsonObject p=new WikiParser().parse("{{Recommended equipment|style=Magic|cape1="+link+"}}","Fixture","https://oldschool.runescape.wiki/w/Fixture").get(0);
        assertEquals("Imbued Saradomin cape",SetupCodec.setup(p).getAsJsonArray("eq").get(1).getAsJsonObject().get("label").getAsString());
        assertEquals("Imbued Guthix cape",ItemCatalogue.normalize("God capes#Imbuing"));
    }
    @Test public void fuzzyCombinedImbuedMaxCapesMatchButExactIdsRemainDistinct(){
        assertEquals(SetupVariationMapping.fuzzy(ItemID.MA2_GUTHIX_CAPE),SetupVariationMapping.fuzzy(ItemID.SKILLCAPE_MAX_SARADOMIN2_TROUVER));
        assertNotEquals(SetupVariationMapping.fuzzy(ItemID.SARADOMIN_CAPE),SetupVariationMapping.fuzzy(ItemID.SKILLCAPE_MAX_SARADOMIN2));
        JsonObject p=SetupCodec.blank("Cape filter");JsonObject cape=SetupCodec.item(ItemID.SKILLCAPE_MAX_SARADOMIN2_TROUVER,1);SetupCodec.setup(p).getAsJsonArray("eq").set(1,cape);SetupCodec.rebuildLayout(p);
        BankFilterTest.FakeBank bank=new BankFilterTest.FakeBank();bank.open=true;StudioBankFilter filter=new StudioBankFilter(bank,"fixture");filter.select(p);assertFalse(bank.matches.test(ItemID.MA2_GUTHIX_CAPE));
        cape.addProperty("f",true);filter.select(p);assertTrue(bank.matches.test(ItemID.MA2_GUTHIX_CAPE));assertEquals(ItemID.SKILLCAPE_MAX_SARADOMIN2_TROUVER,filter.layoutItemId(ItemID.MA2_GUTHIX_CAPE));
    }
    private static LoadoutSlotButton slot(Container root){for(Component c:root.getComponents()){if(c instanceof LoadoutSlotButton&&"inv".equals(((LoadoutSlotButton)c).getClientProperty("slotContainer"))&&Integer.valueOf(0).equals(((LoadoutSlotButton)c).getClientProperty("slotIndex")))return (LoadoutSlotButton)c;if(c instanceof Container){LoadoutSlotButton found=slot((Container)c);if(found!=null)return found;}}return null;}
    @Test public void togglingFuzzyImmediatelyUpdatesTheSlotMarkerAndSavedFlag()throws Exception{SwingUtilities.invokeAndWait(()->{
        OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();JsonObject p=SetupCodec.blank("Seed box");SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(13639,1));SetupCodec.rebuildLayout(p);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(p);
        LoadoutSlotButton initial=slot(panel);assertEquals(Boolean.FALSE,initial.getClientProperty("fuzzy"));for(Component c:initial.getComponentPopupMenu().getComponents())if(c instanceof JMenuItem&&"Toggle Fuzzy".equals(((JMenuItem)c).getText()))((JMenuItem)c).doClick();
        assertEquals(Boolean.TRUE,slot(panel).getClientProperty("fuzzy"));assertTrue(panel.autoSaveNow());assertTrue(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("inv").get(0).getAsJsonObject().get("f").getAsBoolean());panel.dispose();
    });}
    @Test public void setupNameAcceptsDropsIntoItsSectionAndStillProvidesADragSource()throws Exception{SwingUtilities.invokeAndWait(()->{
        OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();SetupLibrary library=new SetupLibrary();SetupLibrary.Section group=new SetupLibrary.Section("Bosses");library.sections.add(group);
        SetupLibrary.Entry source=new SetupLibrary.Entry(SetupCodec.blank("Source")),target=new SetupLibrary.Entry(SetupCodec.blank("Target"));target.section=group.id;target.favorite=true;library.setups.add(source);library.setups.add(target);host.saved=library.serialize(RuneLiteAPI.GSON);
        StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(source);String payload=panel.libraryDragPayload(source.id);
        JComponent name=(JComponent)OwnedGearPanelTest.find(panel,"libraryOpen:"+target.id);TransferHandler handler=name.getTransferHandler();assertEquals(TransferHandler.MOVE,handler.getSourceActions(name));TransferHandler.TransferSupport drop=new TransferHandler.TransferSupport(name,new StringSelection(payload));assertTrue(handler.canImport(drop));assertTrue(handler.importData(drop));
        SetupLibrary saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);assertEquals(2,saved.setups.size());assertEquals(group.id,saved.find(source.id).section);assertEquals(group.id,saved.find(target.id).section);assertTrue(saved.find(target.id).favorite);
        JComponent moved=(JComponent)OwnedGearPanelTest.find(panel,"libraryOpen:"+source.id);assertEquals(TransferHandler.MOVE,moved.getTransferHandler().getSourceActions(moved));assertFalse(moved.getTransferHandler().canImport(new TransferHandler.TransferSupport(moved,new StringSelection("not-a-setup"))));panel.dispose();
    });}
    @Test public void setupDropsIntoEmptySectionsAndUnfiledPersistWithoutDuplication()throws Exception{SwingUtilities.invokeAndWait(()->{
        OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();SetupLibrary library=new SetupLibrary();SetupLibrary.Section group=new SetupLibrary.Section("Raids");library.sections.add(group);SetupLibrary.Entry entry=new SetupLibrary.Entry(SetupCodec.blank("Move me"));library.setups.add(entry);host.saved=library.serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry);
        String payload=panel.libraryDragPayload(entry.id);JComponent header=(JComponent)OwnedGearPanelTest.find(panel,"libraryGroup:"+group.id);TransferHandler.TransferSupport drop=new TransferHandler.TransferSupport(header,new StringSelection(payload));assertTrue(header.getTransferHandler().canImport(drop));assertTrue(header.getTransferHandler().importData(drop));
        SetupLibrary result=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);assertEquals(1,result.setups.size());assertEquals(group.id,result.find(entry.id).section);
        assertTrue(panel.moveLibraryDrop(payload,null));assertNull(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(entry.id).section);assertFalse(panel.moveLibraryDrop(payload,"missing-section"));host.scope="other-character";assertFalse(panel.moveLibraryDrop(payload,group.id));panel.dispose();
    });}
}
