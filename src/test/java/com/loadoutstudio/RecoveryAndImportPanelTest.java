/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecoveryAndImportPanelTest
{
    static class Host extends PanelTest.FakeHost
    {
        String profile="one";Map<String,String> recovery=new HashMap<>();int imports;
        public String recoveryOwner(){return profile+":"+scope;}
        public String readRecovery(String scope,String owner){return recovery.get(owner);}
        public void saveRecovery(String scope,String owner,String text){assertEquals(recoveryOwner(),owner);if(text==null)recovery.remove(owner);else recovery.put(owner,text);}
        public void existingSetups(java.util.function.Consumer<ExistingSetupReader.Result> success,java.util.function.Consumer<String> error){imports++;success.accept(new ExistingSetupReader.Result());}
    }
    static Component find(Container c,String name){for(Component child:c.getComponents()){if(name.equals(child.getName()) || child instanceof JButton&&name.equals(((JButton)child).getText()))return child;if(child instanceof Container){Component result=find((Container)child,name);if(result!=null)return result;}}return null;}
    @Test public void dirtyDraftSurvivesPanelRestartAndStaysInItsOwnProfile() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel first=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject draft=SetupCodec.blank("Recover me");SetupCodec.setup(draft).getAsJsonArray("inv").set(0,SetupCodec.item(560,200000));first.newDraft(draft);((JTextArea)find(first,"loadoutNotes")).setText("Important unsaved note");assertNotNull(host.recovery.get(host.recoveryOwner()));first.dispose();
            host.profile="two";StudioPanel other=new StudioPanel(host,RuneLiteAPI.GSON);assertNull(find(other,"restoreDraft"));other.dispose();host.profile="one";StudioPanel restored=new StudioPanel(host,RuneLiteAPI.GSON);assertNotNull(find(restored,"restoreDraft"));((JButton)find(restored,"restoreDraft")).doClick();assertEquals("Recover me",SetupCodec.name(host.selectedBankSetup));assertEquals("Important unsaved note",((JTextArea)find(restored,"loadoutNotes")).getText());assertEquals(200000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));((JButton)find(restored,"Save")).doClick();assertFalse(host.recovery.containsKey(host.recoveryOwner()));restored.dispose();});
    }
    @Test public void importsAreExplicitAndCopiesKeepSectionsFavoritesAndOriginals() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);assertEquals(0,host.imports);((JButton)find(panel,"From installed plugins")).doClick();assertEquals(1,host.imports);JsonObject source=SetupCodec.blank("Source");ExistingSetupReader.Entry candidate=new ExistingSetupReader.Entry("Inventory Setups","PvM",source,true);assertEquals(1,panel.importCopies(Collections.singletonList(candidate)));assertEquals(1,panel.importCopies(Collections.singletonList(candidate)));SetupLibrary library=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);assertEquals(2,library.setups.size());assertEquals("Source (1)",SetupCodec.name(library.setups.get(1).portable));assertTrue(library.setups.get(0).favorite);assertEquals("PvM",library.sections.get(0).name);assertEquals(library.sections.get(0).id,library.setups.get(0).section);SetupCodec.setup(library.setups.get(0).portable).addProperty("name","Edited");assertEquals("Source",SetupCodec.name(source));panel.dispose();});
    }
    @Test public void recoveryAcceptsTransientEmptyNameButRejectsBrokenStructure()
    {
        JsonObject draft=SetupCodec.blank("");String data=DraftRecovery.encode(draft,null,null,false,BankLayouts.Mode.CUSTOM,true,true,RuneLiteAPI.GSON);assertEquals("",SetupCodec.name(DraftRecovery.decode(data,RuneLiteAPI.GSON).getAsJsonObject("portable")));SetupCodec.setup(draft).add("inv",new JsonArray());try{DraftRecovery.encode(draft,null,null,false,BankLayouts.Mode.PRESET,false,false,RuneLiteAPI.GSON);fail();}catch(IllegalArgumentException expected){}try{DraftRecovery.decode("{\"version\":99}",RuneLiteAPI.GSON);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void rankedGearDefaultsToFirstSourceChoiceAndRetainsConditions()
    {
        String wiki="{{Recommended equipment|style=Melee|head1={{plink|Torva full helm}} / {{plink|Serpentine helm}}{{efn|Needs anti-venom}}|body1={{plink|Void top}} + {{plink|Void robe}}}}";JsonObject draft=new WikiParser().parse(wiki,"Vorkath","fixture").get(0);JsonObject setup=SetupCodec.setup(draft);assertEquals(0,SetupCodec.id(setup.getAsJsonArray("eq").get(0)));assertEquals(2,setup.getAsJsonArray("eq").get(0).getAsJsonObject().getAsJsonArray("choices").size());assertEquals(1,setup.getAsJsonArray("eq").get(4).getAsJsonObject().getAsJsonArray("choices").size());assertTrue(setup.get("notes").getAsString().contains("Needs anti-venom"));assertTrue(setup.getAsJsonArray("inv").get(0).isJsonNull());assertTrue(WikiParser.variant(draft).contains("first choices"));
    }
    @Test public void conversionReviewDescribesActualLoss()
    {
        JsonObject p=SetupCodec.blank("Review");SetupCodec.setup(p).getAsJsonArray("eq").set(0,SetupCodec.item(100,1));SetupCodec.setup(p).getAsJsonArray("rp").set(0,SetupCodec.item(560,16000));String warning=ConversionReview.bank(p);assertTrue(warning.contains("1 worn slots"));assertTrue(warning.contains("1 Rune Pouch"));assertTrue(warning.contains("quantities"));assertTrue(warning.contains("Spellbook"));assertTrue(warning.contains("Converting back"));
    }
}
