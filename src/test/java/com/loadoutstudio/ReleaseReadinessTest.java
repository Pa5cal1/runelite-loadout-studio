/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseReadinessTest {
 static class Host extends PanelTest.FakeHost {
  String recovery;boolean failSave;
  @Override public String read(String scope){return saved;}
  @Override public void save(String scope,String value){if(failSave)throw new IllegalStateException("Fixture save failed");super.save(scope,value);}
  @Override public String recoveryOwner(){return scope;}
  @Override public String readRecovery(String scope,String owner){return recovery;}
  @Override public void saveRecovery(String scope,String owner,String text){recovery=text;}
 }
 private static SetupLibrary.Entry seed(Host host){SetupLibrary l=new SetupLibrary();SetupLibrary.Entry entry=new SetupLibrary.Entry(SetupCodec.blank("Favorite test"));l.setups.add(entry);host.saved=l.serialize(RuneLiteAPI.GSON);return entry;}
 private static void click(StudioPanel p,String name){OwnedGearPanelTest.click(p,name);}
 @Test public void libraryFavoriteSurvivesBuilderSaveNavigationAndSaveCopy() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry e=seed(host);StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.openSaved(e.id);
  click(p,"nav:Library");click(p,"libraryFavorite:"+e.id);click(p,"nav:Builder");click(p,"Save");assertTrue(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);
  click(p,"+ Setup details");click(p,"Save copy");assertTrue(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.stream().allMatch(v->v.favorite));
  p.openSaved(e.id);click(p,"libraryFavorite:"+e.id);click(p,"Save");assertFalse(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);p.dispose();
 });}
 @Test public void favoriteSyncPreservesDirtyNotesAndRecovery() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry e=seed(host);StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.openSaved(e.id);
  JTextArea notes=(JTextArea)OwnedGearPanelTest.find(p,"loadoutNotes");notes.setText("Keep my unsaved instructions");click(p,"libraryFavorite:"+e.id);assertEquals("Keep my unsaved instructions",notes.getText());assertTrue(DraftRecovery.decode(host.recovery,RuneLiteAPI.GSON).get("favorite").getAsBoolean());p.dispose();
  StudioPanel restored=new StudioPanel(host,RuneLiteAPI.GSON);restored.openSaved(e.id);assertEquals("Keep my unsaved instructions",((JTextArea)OwnedGearPanelTest.find(restored,"loadoutNotes")).getText());click(restored,"Save");assertTrue(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);restored.dispose();
 });}
 @Test public void changingFavoriteBeforeRestoringRecoveryKeepsLatestValue() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry e=seed(host);host.recovery=DraftRecovery.encode(e.portable,e.id,null,false,BankLayouts.Mode.PRESET,false,false,RuneLiteAPI.GSON);StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);
  click(p,"libraryFavorite:"+e.id);assertTrue(DraftRecovery.decode(host.recovery,RuneLiteAPI.GSON).get("favorite").getAsBoolean());click(p,"restoreDraft");click(p,"Save");assertTrue(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);p.dispose();
 });}
 @Test public void failedFavoriteSaveLeavesDraftAndStoredValueUnchanged() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary.Entry e=seed(host);StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.openSaved(e.id);host.failSave=true;click(p,"nav:Library");click(p,"libraryFavorite:"+e.id);assertFalse(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);assertTrue(OwnedGearPanelTest.find(p,"libraryFeedback").isVisible());host.failSave=false;click(p,"Save");assertFalse(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(e.id).favorite);p.dispose();
 });}
 @Test public void exportErrorsAppearBesideExportAction() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject draft=SetupCodec.blank("Incomplete");JsonObject unresolved=SetupCodec.item(0,1);unresolved.addProperty("label","Missing fixture");SetupCodec.setup(draft).getAsJsonArray("inv").set(0,unresolved);p.newDraft(draft);click(p,"nav:Export");click(p,"Show code");JTextArea error=(JTextArea)OwnedGearPanelTest.find(p,"exportFeedback");assertTrue(error.isVisible());assertTrue(error.getText().contains("Resolve"));preview(p,"export-error");p.dispose();
 });}
 @Test public void amountMenuUsesPlainLabelsAndKeepsPortableCodes() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject draft=SetupCodec.blank("Amounts");SetupCodec.setup(draft).getAsJsonArray("inv").set(0,SetupCodec.item(560,100));SetupCodec.rebuildLayout(draft);p.newDraft(draft);
  JButton slot=findSlot(p);JMenu stack=null;for(java.awt.Component c:slot.getComponentPopupMenu().getComponents())if(c instanceof JMenu)stack=(JMenu)c;assertNotNull(stack);assertEquals("Ignore amount",stack.getItem(0).getText());assertEquals("Exactly",stack.getItem(1).getText());assertEquals("At least",stack.getItem(2).getText());assertEquals("At most",stack.getItem(3).getText());stack.getItem(2).doClick();click(p,"Save");assertEquals("Less_Than",SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("inv").get(0).getAsJsonObject().get("sc").getAsString());p.dispose();
 });}
 @Test public void combinedStockFailureKeepsDraftAndCanBeExplicitlyDisabled() throws Exception {SwingUtilities.invokeAndWait(()->{
  boolean[] verify={true};OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host(){@Override public LoadoutStudioConfig settings(){return new LoadoutStudioConfig(){@Override public boolean checkAlternativeStock(){return verify[0];}};}};
  host.owned=OwnedGearTest.gear(1,4);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject source=OwnedGearTest.setup();SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(1,1));SetupCodec.rebuildLayout(source);panel.open(new SetupLibrary.Entry(source));click(panel,"+ My gear alternatives");click(panel,"Check my Bank");
  ((JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:eq:4")).setSelectedIndex(1);((JComboBox<?>)OwnedGearPanelTest.find(panel,"ownedChoice:inv:0")).setSelectedIndex(1);click(panel,"Use selected alternatives");assertEquals(1,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4)));JTextArea error=(JTextArea)OwnedGearPanelTest.find(panel,"builderFeedback");assertTrue(error.isVisible());assertTrue(error.getText().contains("need 2"));preview(panel,"replacement-stock-error");preview(panel,"replacement-stock-error");preview(panel,"replacement-stock-error");
  verify[0]=false;click(panel,"Use selected alternatives");assertEquals(2,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4)));assertEquals(2,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));panel.dispose();
 });}
 private static void preview(StudioPanel panel,String name){panel.setSize(220,900);OwnedGearPanelTest.layout(panel);OwnedGearPanelTest.layout(panel);try{java.io.File dir=new java.io.File("build/previews/readiness");dir.mkdirs();java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(220,900,java.awt.image.BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,name+".png"));}catch(java.io.IOException ex){throw new AssertionError(ex);}}
 private static JButton findSlot(java.awt.Container root){for(java.awt.Component c:root.getComponents()){if(c instanceof JButton && "inv".equals(((JButton)c).getClientProperty("slotContainer")) && Integer.valueOf(0).equals(((JButton)c).getClientProperty("slotIndex")))return (JButton)c;if(c instanceof java.awt.Container){JButton found=findSlot((java.awt.Container)c);if(found!=null)return found;}}return null;}
}
