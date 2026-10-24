/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemIdCompatibilityTest {
    @Test public void unavailableRealCounterpartLeavesOldTemporaryWikiItemUnresolved()throws Exception{SwingUtilities.invokeAndWait(()->{
        class Host extends PanelTest.FakeHost {String copied;@Override public void copyText(String text){copied=text;}}
        Host host=new Host();JsonObject source=SetupCodec.blank("Unresolved old source");JsonObject item=SetupCodec.item(ItemID.BR_DRAGON_CLAWS,1);item.addProperty("label","Unknown counterpart");JsonArray choices=new JsonArray();choices.add("Unknown counterpart");item.add("choices",choices);SetupCodec.setup(source).getAsJsonArray("inv").set(0,item);SetupCodec.rebuildLayout(source);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(source);assertEquals(0,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));assertFalse(host.selectedBankSetup.getAsJsonArray("layout").contains(new JsonPrimitive(ItemID.BR_DRAGON_CLAWS)));OwnedGearPanelTest.click(panel,"nav:Export");OwnedGearPanelTest.click(panel,"Copy code");assertNull(host.copied);assertEquals(ItemID.BR_DRAGON_CLAWS,SetupCodec.id(SetupCodec.setup(source).getAsJsonArray("inv").get(0)));panel.dispose();
    });}
    @Test public void chargedExactIdsStayExactWhileBankFuzzyUsesTheNativeBase(){
        SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);JsonObject p=SetupCodec.blank("Charges");int charged=ItemID.RING_OF_DUELING_1;SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(charged,1));SetupCodec.rebuildLayout(p);
        assertTrue(codec.bank(p,false).contains(","+charged+",layout"));JsonObject saved=codec.importText(codec.inventory(p)).get(0);assertEquals(charged,SetupCodec.id(SetupCodec.setup(saved).getAsJsonArray("inv").get(0)));
        SetupCodec.setup(p).getAsJsonArray("inv").get(0).getAsJsonObject().addProperty("f",true);assertTrue(codec.bank(p,false).contains(","+(-ItemVariationMapping.map(charged))));
    }
    @Test public void olderWikiTemporaryChoiceIsRepairedAsADraftButExplicitIdsAreRetained()throws Exception{SwingUtilities.invokeAndWait(()->{
        ItemCatalogue catalogue=new ItemCatalogue();catalogue.add(ItemID.BR_DRAGON_CLAWS,"Dragon claws",false,-1,-1,3,false,true);catalogue.add(ItemID.DRAGON_CLAWS,"Dragon claws",false,-1,-1,3,true,true);
        PanelTest.FakeHost host=new PanelTest.FakeHost(){@Override public StudioPanel.ItemInfo defaultItem(String name){return catalogue.firstExact(name);}};
        JsonObject p=SetupCodec.blank("Old Wiki");JsonObject item=SetupCodec.item(ItemID.BR_DRAGON_CLAWS,1);item.addProperty("label","Dragon claws");JsonArray choices=new JsonArray();choices.add("Dragon claws");item.add("choices",choices);SetupCodec.setup(p).getAsJsonArray("inv").set(0,item);JsonArray layout=new JsonArray();layout.add(-1);layout.add(ItemID.BR_DRAGON_CLAWS);p.add("layout",layout);SetupLibrary.Entry entry=new SetupLibrary.Entry(p);entry.bankLayout=BankLayouts.Mode.CUSTOM;StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry);assertEquals(ItemID.DRAGON_CLAWS,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));assertEquals(ItemID.DRAGON_CLAWS,host.selectedBankSetup.getAsJsonArray("layout").get(1).getAsInt());assertNull(host.saved);assertEquals(ItemID.BR_DRAGON_CLAWS,SetupCodec.id(SetupCodec.setup(p).getAsJsonArray("inv").get(0)));panel.dispose();
        JsonObject explicit=SetupCodec.blank("Explicit import");SetupCodec.setup(explicit).getAsJsonArray("inv").set(0,SetupCodec.item(ItemID.BR_DRAGON_CLAWS,1));SetupCodec.rebuildLayout(explicit);StudioPanel retained=new StudioPanel(host,RuneLiteAPI.GSON);retained.newDraft(explicit);assertEquals(ItemID.BR_DRAGON_CLAWS,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));retained.dispose();
    });}
    private static JsonObject fixture(){try(InputStreamReader in=new InputStreamReader(ItemIdCompatibilityTest.class.getResourceAsStream("/items/id-compatibility.json"),StandardCharsets.UTF_8)){return RuneLiteAPI.GSON.fromJson(in,JsonObject.class);}catch(java.io.IOException ex){throw new AssertionError(ex);}}
    @Test public void allIdsThroughTheAuditedCacheRangeMatchUpstreamFuzzyRules(){
        JsonObject data=fixture();Map<Integer,Integer> extra=new HashMap<>();for(JsonElement row:data.getAsJsonArray("customMappings"))extra.put(row.getAsJsonArray().get(0).getAsInt(),row.getAsJsonArray().get(1).getAsInt());assertEquals(83,extra.size());
        for(int id=0;id<=data.get("highestId").getAsInt();id++){int base=ItemVariationMapping.map(id);assertEquals("Fuzzy ID "+id,base==id?extra.getOrDefault(id,id):base,SetupVariationMapping.map(id));}
    }
    @Test public void everyDeclaredPositiveIdSurvivesInventorySerializationUnchanged(){
        JsonArray ids=fixture().getAsJsonArray("ids");SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);assertEquals(19996,ids.size());
        for(int start=0;start<ids.size();start+=28){JsonObject draft=SetupCodec.blank("ID audit");int end=Math.min(start+28,ids.size());for(int i=start;i<end;i++)SetupCodec.setup(draft).getAsJsonArray("inv").set(i-start,SetupCodec.item(ids.get(i).getAsInt(),1));SetupCodec.rebuildLayout(draft);JsonObject restored=codec.importText(codec.inventory(draft)).get(0);for(int i=start;i<end;i++)assertEquals(ids.get(i).getAsInt(),SetupCodec.id(SetupCodec.setup(restored).getAsJsonArray("inv").get(i-start)));}
    }
    @Test public void allUpstreamSearchExclusionsAreAvoidedByAutomaticWikiChoice(){
        JsonArray ids=fixture().getAsJsonArray("automaticExclusions");assertEquals(1572,ids.size());ItemCatalogue catalogue=new ItemCatalogue();
        for(JsonElement id:ids){assertTrue(AutomaticItemExclusions.contains(id.getAsInt()));catalogue.add(id.getAsInt(),"Audit duplicate",false,-1,-1,3,true,true);}
        assertNull(catalogue.firstExact("Audit duplicate"));catalogue.add(ItemID.ULTOR_RING,"Audit duplicate",false,-1,-1,12,true,true);assertEquals(ItemID.ULTOR_RING,catalogue.firstExact("Audit duplicate").id);
    }
    @Test public void bankExportCanonicalizesEveryContainerAndLayoutWithoutRewritingInventory(){
        JsonObject p=SetupCodec.blank("Worn and noted");JsonObject setup=SetupCodec.setup(p);setup.getAsJsonArray("eq").set(0,SetupCodec.item(200,1));setup.getAsJsonArray("inv").set(0,SetupCodec.item(300,500));setup.getAsJsonArray("rp").set(0,SetupCodec.item(400,16000));setup.getAsJsonArray("qv").set(0,SetupCodec.item(500,200));setup.getAsJsonObject("afi").add("600",SetupCodec.item(600,1));SetupCodec.rebuildLayout(p);String before=p.toString();SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);
        for(boolean legacy:new boolean[]{false,true}){JsonObject copy=codec.importText(codec.bank(p,legacy,id->id+1000)).get(0);Set<Integer> ids=new HashSet<>();for(JsonElement item:BankLayouts.items(copy))ids.add(SetupCodec.id(item));assertTrue(ids.containsAll(Arrays.asList(1200,1300,1400,1500,1600)));for(JsonElement cell:copy.getAsJsonArray("layout"))assertTrue(cell.getAsInt()==-1||cell.getAsInt()>=1200);}
        assertEquals(before,p.toString());JsonObject preserved=codec.importText(codec.inventory(p)).get(0);assertEquals(200,SetupCodec.id(SetupCodec.setup(preserved).getAsJsonArray("eq").get(0)));assertEquals(300,SetupCodec.id(SetupCodec.setup(preserved).getAsJsonArray("inv").get(0)));
    }
    @Test public void unknownIdsBlockExternalCopyButNativeDraftBackupStillWorks()throws Exception{SwingUtilities.invokeAndWait(()->{
        ItemCatalogue definitions=new ItemCatalogue();definitions.add(ItemID.ULTOR_RING,"Ultor ring",false,-1,-1);definitions.indexed();
        class Host extends PanelTest.FakeHost {String copied;@Override public String itemExportError(JsonObject p){return definitions.exportError(p);}@Override public void copyText(String text){copied=text;}}
        Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject source=SetupCodec.blank("Future import");SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(1000000001,1));SetupCodec.rebuildLayout(source);panel.newDraft(source);OwnedGearPanelTest.click(panel,"nav:Export");OwnedGearPanelTest.click(panel,"Copy code");assertNull(host.copied);assertEquals("",((JTextArea)OwnedGearPanelTest.find(panel,"exportCode")).getText());assertTrue(((JTextArea)OwnedGearPanelTest.find(panel,"exportFeedback")).getText().contains("not a named item"));((JComboBox<?>)OwnedGearPanelTest.find(panel,"exportFormat")).setSelectedIndex(3);OwnedGearPanelTest.click(panel,"Copy code");assertNotNull(host.copied);assertTrue(host.copied.contains("1000000001"));panel.dispose();
    });}
    @Test public void definitionValidationWaitsForIndexAndIncludesLayoutOnlyIds(){
        ItemCatalogue catalogue=new ItemCatalogue();JsonObject p=SetupCodec.blank("Validation");catalogue.add(ItemID.ULTOR_RING,"Ultor ring",false,-1,-1);SetupCodec.setup(p).getAsJsonArray("inv").set(0,SetupCodec.item(ItemID.ULTOR_RING,1));SetupCodec.rebuildLayout(p);assertTrue(catalogue.exportError(p).contains("loading"));catalogue.indexed();assertNull(catalogue.exportError(p));p.getAsJsonArray("layout").add(999999);assertTrue(catalogue.exportError(p).contains("999999"));catalogue.clear();assertTrue(catalogue.exportError(p).contains("loading"));
    }
}
