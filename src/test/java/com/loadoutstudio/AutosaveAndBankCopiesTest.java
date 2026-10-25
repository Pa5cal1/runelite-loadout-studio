/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class AutosaveAndBankCopiesTest {
 static class Host extends ReleaseReadinessTest.Host {String owner="profile-one";@Override public String recoveryOwner(){return owner+":"+scope;}}
 private static SetupLibrary.Entry seed(Host host,String name){SetupLibrary library=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);SetupLibrary.Entry entry=new SetupLibrary.Entry(SetupCodec.blank(name));library.setups.add(entry);host.saved=library.serialize(RuneLiteAPI.GSON);return entry;}
 private static JTextArea notes(StudioPanel panel){return (JTextArea)OwnedGearPanelTest.find(panel,"loadoutNotes");}
 @Test public void switchingSetupsAutosavesPendingEditsWithoutAConfirmation()throws Exception{SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry one=seed(host,"First"),two=seed(host,"Second");StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.openSaved(one.id);notes(panel).setText("Keep this without clicking Save");panel.openSaved(two.id);assertEquals("Keep this without clicking Save",SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(one.id).portable).get("notes").getAsString());assertEquals("Second",SetupCodec.name(host.selectedBankSetup));panel.openSaved(one.id);assertEquals("Keep this without clicking Save",notes(panel).getText());notes(panel).setText("Reopen the current setup too");panel.openSaved(one.id);assertEquals("Reopen the current setup too",notes(panel).getText());panel.dispose();
 });}
 @Test public void autosaveFailureKeepsRecoveryAndCurrentSetupUntilRetry()throws Exception{SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry one=seed(host,"First"),two=seed(host,"Second");StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.openSaved(one.id);notes(panel).setText("Keep on failure");host.failSave=true;assertFalse(panel.autoSaveNow());assertNotNull(host.recovery);panel.openSaved(two.id);assertEquals("First",SetupCodec.name(host.selectedBankSetup));host.failSave=false;assertTrue(panel.autoSaveNow());panel.openSaved(two.id);assertEquals("Keep on failure",SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(one.id).portable).get("notes").getAsString());panel.dispose();
 });}
 @Test public void pendingAutosaveCannotWriteIntoAnotherRuneLiteProfile()throws Exception{SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry one=seed(host,"Scoped");StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.openSaved(one.id);notes(panel).setText("Original profile only");String before=host.saved;host.owner="profile-two";assertFalse(panel.autoSaveNow());assertEquals(before,host.saved);panel.dispose();
 });}
 @Test public void emptyAlternativeResultsCollapseAPreviouslyExpandedBlock()throws Exception{SwingUtilities.invokeAndWait(()->{
  OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(OwnedGearTest.setup());panel.refreshAlternatives();JButton heading=(JButton)OwnedGearPanelTest.find(panel,"disclosure:My gear alternatives");assertEquals("- My gear alternatives",heading.getText());host.owned=new OwnedGear(Collections.emptyMap(),Collections.emptyMap(),1,true);panel.refreshAlternatives();assertEquals("+ My gear alternatives",heading.getText());panel.dispose();
 });}
 @Test public void delayedStockCheckAcceptsUnrelatedEditsAndPreservesThem()throws Exception{SwingUtilities.invokeAndWait(()->{
  class DelayedHost extends OwnedGearPanelTest.Host {boolean defer;java.util.function.Consumer<OwnedGear> pending;@Override public void availableGear(JsonObject p,java.util.function.Consumer<OwnedGear> success,java.util.function.Consumer<String> error){if(defer)pending=success;else success.accept(owned);}}
  DelayedHost host=new DelayedHost();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(OwnedGearTest.setup());panel.refreshAlternatives();((JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:eq:4")).setSelectedIndex(1);host.defer=true;OwnedGearPanelTest.click(panel,"Use selected alternatives");notes(panel).setText("Changed while stock was checked");host.pending.accept(host.owned);assertEquals(2,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4)));assertEquals("Changed while stock was checked",notes(panel).getText());panel.dispose();
 });}
 @Test public void everyRepeatedOwnedFoodCellGetsAnInteractiveSourceButMissingFoodDoesNot(){
  int[] layout={391,391,391,-1,385,385},ids={391,385},amounts={1499,0};int[] positions=BankViewPlan.positions(ids,layout,id->id);int[] sources=BankViewPlan.duplicateSources(ids,amounts,layout,positions,id->id);assertArrayEquals(new int[]{-1,0,0,-1,-1,-1},sources);assertArrayEquals(new int[]{1499,0},amounts);
  assertArrayEquals(new int[]{-1,0},BankViewPlan.duplicateSources(new int[]{500},new int[]{12},new int[]{391,391},new int[]{0},id->id==500?391:id));
 }
 @Test public void fourPouchRunesAreCentredUnderEquipmentWithoutOverlappingInventoryOrBoltPouch(){
  JsonObject p=SetupCodec.blank("Four runes");JsonObject s=SetupCodec.setup(p);for(int i=0;i<4;i++)s.getAsJsonArray("rp").set(i,SetupCodec.item(560+i,16000));s.add("bp",SetupCodec.empty(4));s.getAsJsonArray("bp").set(0,SetupCodec.item(892,500));s.getAsJsonArray("inv").set(24,SetupCodec.item(385,1));JsonArray layout=BankLayouts.generate(p,BankLayouts.Mode.PRESET,false);int[] positions={40,42,48,50};for(int i=0;i<4;i++)assertEquals(560+i,layout.get(positions[i]).getAsInt());assertEquals(385,layout.get(52).getAsInt());assertEquals(892,layout.get(56).getAsInt());
 }
 @Test public void pouchPlanningPreservesAmountsWhileComparisonAndPluginExportRespectCapacity(){
  SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);JsonObject p=SetupCodec.blank("Planned runes"),s=SetupCodec.setup(p);JsonObject rune=SetupCodec.item(560,16001);rune.addProperty("sc","Standard");s.getAsJsonArray("rp").set(0,rune);s.getAsJsonArray("inv").set(0,SetupCodec.item(561,200000));SetupCodec.rebuildLayout(p);JsonObject actual=p.deepCopy();SetupCodec.setup(actual).getAsJsonArray("rp").get(0).getAsJsonObject().addProperty("q",16000);assertFalse(SetupComparison.differences(p,actual,false,id->id).contains("rp:0"));JsonObject exported=RuneLiteAPI.GSON.fromJson(codec.inventory(p),JsonObject.class);assertEquals(16000,SetupCodec.quantity(SetupCodec.setup(exported).getAsJsonArray("rp").get(0)));assertEquals(200000,SetupCodec.quantity(SetupCodec.setup(exported).getAsJsonArray("inv").get(0)));assertEquals(16001,SetupCodec.quantity(SetupCodec.setup(codec.importText(codec.studio(p,false,false)).get(0)).getAsJsonArray("rp").get(0)));
 }
}
