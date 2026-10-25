/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegitimateVariantSelectionTest {
    private static final int[] SALVES={ItemID.NZONE_SALVE_AMULET_E,ItemID.SW_SALVE_AMULET_E,ItemID.PVPA_SALVE_AMULET_E};
    @Test public void allDeclaredPositiveIdsRemainAvailableForExplicitSelection()throws Exception{
        try(InputStreamReader reader=new InputStreamReader(LegitimateVariantSelectionTest.class.getResourceAsStream("/items/id-compatibility.json"),StandardCharsets.UTF_8)){
            JsonObject data=RuneLiteAPI.GSON.fromJson(reader,JsonObject.class);for(JsonElement value:data.getAsJsonArray("ids")){int id=value.getAsInt();assertEquals("Manual choice "+id,id,ItemCatalogue.selectionId(id));}
        }
    }
    @Test public void salveVariantsAreRealDefaultsRatherThanUnresolvedItems(){
        ItemCatalogue catalogue=new ItemCatalogue();for(int id:SALVES){assertTrue(AutomaticItemExclusions.contains(id));assertFalse(AutomaticItemExclusions.avoidDefault(id));catalogue.add(id,"Salve amulet(ei)",false,-1,-1,2,false,true);}
        assertEquals(ItemID.NZONE_SALVE_AMULET_E,catalogue.firstExact("Salve amulet(ei)").id);
    }
    @Test public void imbuedMasksHelmsAndRingsAreNotRejectedByThePreferenceList(){
        int[][] families={{ItemID.NZONE_BLACK_MASK,ItemID.SW_BLACK_MASK,ItemID.PVPA_BLACK_MASK},{ItemID.SW_SLAYER_HELM_I,ItemID.PVPA_SLAYER_HELM_I},{ItemID.NZONE_SEER_RING,ItemID.SW_SEER_RING,ItemID.PVPA_SEER_RING},{ItemID.NZONE_BERZERKER_RING,ItemID.SW_BERZERKER_RING,ItemID.PVPA_BERZERKER_RING}};
        for(int[] family:families){ItemCatalogue catalogue=new ItemCatalogue();for(int id:family){assertEquals(id,ItemCatalogue.selectionId(id));assertFalse(AutomaticItemExclusions.avoidDefault(id));catalogue.add(id,"Imbued variant",false,-1,-1,0,false,true);}assertNotNull(catalogue.firstExact("Imbued variant"));}
    }
    @Test public void savedSourceChoiceKeepsTheUsersLegitimateSalveVariant()throws Exception{SwingUtilities.invokeAndWait(()->{
        PanelTest.FakeHost host=new PanelTest.FakeHost(){@Override public StudioPanel.ItemInfo defaultItem(String name){return new StudioPanel.ItemInfo(ItemID.NZONE_SALVE_AMULET_E,name,false,2,null);}};
        JsonObject source=SetupCodec.blank("Keep my imbue");JsonObject item=SetupCodec.item(ItemID.SW_SALVE_AMULET_E,1);item.addProperty("label","Salve amulet(ei)");JsonArray choices=new JsonArray();choices.add("Salve amulet(ei)");item.add("choices",choices);SetupCodec.setup(source).getAsJsonArray("eq").set(2,item);SetupCodec.rebuildLayout(source);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(source));assertEquals(ItemID.SW_SALVE_AMULET_E,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(2)));assertNull(host.saved);panel.dispose();
    });}
    private static JButton neck(Container root){for(Component child:root.getComponents()){if(child instanceof JButton&&"eq".equals(((JButton)child).getClientProperty("slotContainer"))&&Integer.valueOf(2).equals(((JButton)child).getClientProperty("slotIndex")))return (JButton)child;if(child instanceof Container){JButton found=neck((Container)child);if(found!=null)return found;}}return null;}
    @Test public void allThreeSalveChoicesCanBeInsertedSavedAndExportedFromTheSidebar()throws Exception{SwingUtilities.invokeAndWait(()->{
        class Host extends PanelTest.FakeHost {Consumer<Integer> selected;@Override public void chooseItem(Consumer<Integer> chosen,Consumer<String> error){selected=chosen;}@Override public void describe(int id,Consumer<StudioPanel.ItemInfo> done){done.accept(new StudioPanel.ItemInfo(id,"Salve amulet(ei)",false,2,null));}}
        for(int id:SALVES){Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(SetupCodec.blank("Salve variant"));neck(panel).doClick();assertNotNull(host.selected);host.selected.accept(ItemCatalogue.selectionId(id));assertEquals(id,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(2)));OwnedGearPanelTest.click(panel,"Save");SetupLibrary saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);JsonObject copy=codec.importText(codec.inventory(saved.setups.get(0).portable)).get(0);assertEquals(id,SetupCodec.id(SetupCodec.setup(copy).getAsJsonArray("eq").get(2)));panel.dispose();}
    });}
}
