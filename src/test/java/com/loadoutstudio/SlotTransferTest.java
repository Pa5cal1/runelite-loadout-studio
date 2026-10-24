/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlotTransferTest
{
    private static final class Host extends PanelTest.FakeHost
    {
        final Map<Integer, StudioPanel.ItemInfo> items = new HashMap<>();
        Host()
        {
            item(13441,"Anglerfish",false,-1); item(22978,"Dragon hunter lance",false,3);
            item(560,"Death rune",true,-1); item(561,"Nature rune",true,-1);
            item(892,"Rune arrow",true,13); item(385,"Shark",false,-1);
        }
        void item(int id,String name,boolean stackable,int slot) { items.put(id,new StudioPanel.ItemInfo(id,name,stackable,slot,null)); }
        @Override public void describe(int id, java.util.function.Consumer<StudioPanel.ItemInfo> callback) { callback.accept(items.get(id)); }
        @Override public java.util.List<StudioPanel.ItemInfo> search(String query) { java.util.List<StudioPanel.ItemInfo> result=new ArrayList<>(); for(StudioPanel.ItemInfo i:items.values()) if(i.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) result.add(i); return result; }
    }
    private static Component find(Container root,String name)
    {
        for(Component c:root.getComponents()) { if(name.equals(c.getName()) || c instanceof JButton && name.equals(((JButton)c).getText())) return c; if(c instanceof Container) { Component found=find((Container)c,name); if(found!=null)return found; } } return null;
    }
    private static JButton slot(Container root,String key,int index)
    {
        for(Component c:root.getComponents()) { if(c instanceof JButton && key.equals(((JButton)c).getClientProperty("slotContainer")) && Integer.valueOf(index).equals(((JButton)c).getClientProperty("slotIndex"))) return (JButton)c; if(c instanceof Container) { JButton found=slot((Container)c,key,index); if(found!=null)return found; } } return null;
    }
    private static boolean drop(StudioPanel panel,String payload,String key,int index)
    {
        JButton target=slot(panel,key,index); return target.getTransferHandler().importData(new TransferHandler.TransferSupport(target,new StringSelection(payload)));
    }
    @Test public void inventorySwapPreservesQuantitiesAndInvalidEquipmentDropIsAtomic() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host(); StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON); JsonObject p=SetupCodec.blank("Transfer test"); JsonObject s=SetupCodec.setup(p);
            s.getAsJsonArray("inv").set(0,SetupCodec.item(13441,1)); s.getAsJsonArray("inv").set(1,SetupCodec.item(892,1200)); s.getAsJsonArray("inv").set(2,SetupCodec.item(22978,1)); panel.newDraft(p);
            assertTrue(drop(panel,panel.dragPayload("inv",0),"inv",1));
            assertEquals(892,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0))); assertEquals(1200,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            JsonObject before=host.selectedBankSetup.deepCopy(); assertFalse(drop(panel,panel.dragPayload("inv",1),"eq",3)); assertEquals(before,host.selectedBankSetup);
            assertTrue(drop(panel,panel.dragPayload("inv",2),"eq",3)); assertEquals(22978,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(3))); panel.dispose();
        });
    }
    @Test public void quiverAcceptsArrowsAndPouchClampsLargeStacksWithoutLosingTheRemainder() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host(); StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON); JsonObject p=SetupCodec.blank("Ammo test"),s=SetupCodec.setup(p);
            s.getAsJsonArray("inv").set(0,SetupCodec.item(892,1400)); s.getAsJsonArray("inv").set(1,SetupCodec.item(13441,1)); s.getAsJsonArray("inv").set(2,SetupCodec.item(560,16001)); s.getAsJsonArray("inv").set(3,SetupCodec.item(561,16000)); panel.newDraft(p);
            assertTrue(drop(panel,panel.dragPayload("inv",0),"qv",0)); assertEquals(1400,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("qv").get(0)));
            JsonObject before=host.selectedBankSetup.deepCopy(); assertFalse(drop(panel,panel.dragPayload("inv",1),"rp",0)); assertEquals(before,host.selectedBankSetup);assertTrue(drop(panel,panel.dragPayload("inv",2),"rp",0));assertEquals(16000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));assertEquals(1,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(2)));
            assertTrue(drop(panel,panel.dragPayload("inv",3),"rp",0)); assertEquals(561,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0))); panel.dispose();
        });
    }
    @Test public void additionalFilteredItemsCanMoveBothWaysWithoutCollapsingOtherItems() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host(); StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON); JsonObject p=SetupCodec.blank("Extra test"),s=SetupCodec.setup(p);
            s.getAsJsonArray("inv").set(0,SetupCodec.item(13441,1)); s.getAsJsonObject("afi").add("385",SetupCodec.item(385,1)); panel.newDraft(p);
            assertTrue(drop(panel,panel.dragPayload("inv",0),"afi",0)); assertEquals(385,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            assertTrue(drop(panel,panel.dragPayload("afi",0),"inv",1)); assertTrue(SetupCodec.setup(host.selectedBankSetup).getAsJsonObject("afi").entrySet().isEmpty()); assertEquals(13441,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(1))); panel.dispose();
        });
    }
    @Test public void staleMalformedAndEmptyDragsCannotModifyTheDraft() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host(); StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON); JsonObject p=SetupCodec.blank("Stale test"); SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(13441,1)); panel.newDraft(p);
            String payload=panel.dragPayload("inv",0); panel.reloadScope(); panel.newDraft(SetupCodec.blank("New profile draft")); JsonObject before=host.selectedBankSetup.deepCopy();
            assertFalse(drop(panel,payload,"inv",1)); assertFalse(panel.importItemDrop("loadout-slot:bad:inv:999","inv",1)); assertFalse(panel.importItemDrop("anything","inv",1)); assertNull(panel.dragPayload("inv",0)); assertEquals(before,host.selectedBankSetup); panel.dispose();
        });
    }
    @Test public void catalogueAddsToEmptyInventoryAndFullInventoryIsNotOverwritten() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host(); StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON); panel.newDraft(SetupCodec.blank("Catalogue test"));
            JTextField search=(JTextField)find(panel,"catalogueSearch"); search.setText("Rune arrow"); search.postActionEvent(); ((JButton)find(panel,"Add to Inventory")).doClick(); assertEquals(892,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            assertTrue(drop(panel,panel.catalogueDragPayload(),"qv",0)); assertEquals(892,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("qv").get(0))); assertEquals("Catalogue uses COPY, leaving Inventory intact",892,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            panel.reloadScope(); JsonObject full=SetupCodec.blank("Full"); for(int i=0;i<28;i++) SetupCodec.setup(full).getAsJsonArray("inv").set(i,SetupCodec.item(13441,1)); panel.newDraft(full); JsonObject before=host.selectedBankSetup.deepCopy(); ((JButton)find(panel,"Add to Inventory")).doClick(); assertEquals(before,host.selectedBankSetup);
            panel.getActionMap().get("saveSetup").actionPerformed(null); assertNotNull(host.saved);
            ((JButton)find(panel,"nav:Export")).doClick(); panel.getActionMap().get("findItems").actionPerformed(null); assertNotNull(find(panel,"- Find items"));
            for(Component component:panel.getComponents()) if(component instanceof JTabbedPane) assertEquals(1,((JTabbedPane)component).getSelectedIndex()); panel.dispose();
        });
    }
    @Test public void oversizedTransferKeepsOldPouchRunesAndRejectsWhenInventoryIsFull() throws Exception {SwingUtilities.invokeAndWait(()->{
        Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject p=SetupCodec.blank("Occupied pouch");SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(561,16001));SetupCodec.setup(p).getAsJsonArray("rp").set(0,SetupCodec.item(560,700));panel.newDraft(p);assertTrue(drop(panel,panel.dragPayload("inv",0),"rp",0));assertEquals(16000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));assertEquals(1,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));assertEquals(560,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(1)));assertEquals(700,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(1)));panel.dispose();
        Host fullHost=new Host();StudioPanel full=new StudioPanel(fullHost,RuneLiteAPI.GSON);JsonObject packed=SetupCodec.blank("Packed");for(int i=0;i<28;i++)SetupCodec.setup(packed).getAsJsonArray("inv").set(i,SetupCodec.item(385,1));SetupCodec.setup(packed).getAsJsonArray("inv").set(0,SetupCodec.item(561,16001));SetupCodec.setup(packed).getAsJsonArray("rp").set(0,SetupCodec.item(560,700));full.newDraft(packed);JsonObject before=fullHost.selectedBankSetup.deepCopy();assertFalse(drop(full,full.dragPayload("inv",0),"rp",0));assertEquals(before,fullHost.selectedBankSetup);full.dispose();
    });}
}
