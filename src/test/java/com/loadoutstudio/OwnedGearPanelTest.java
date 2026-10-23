/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class OwnedGearPanelTest {
 static class Host extends PanelTest.FakeHost {
  Consumer<JsonObject> live;Consumer<JsonArray> layout;OwnedGear owned=OwnedGearTest.gear(1,4);int requests;
  @Override public String read(String scope){return saved;}
  @Override public void observeGear(Consumer<JsonObject> observer){live=observer;}
  @Override public void availableGear(JsonObject p,Consumer<OwnedGear> success,Consumer<String> error){requests++;success.accept(owned);}
  @Override public void describe(int id,Consumer<StudioPanel.ItemInfo> callback){StudioPanel.ItemInfo info=owned.items.get(id);callback.accept(info==null?OwnedGearTest.item(id,"Fixture",-1):info);}
  @Override public void updateBankFilter(JsonObject p,String scope,Consumer<String> status,Consumer<JsonArray> changed){layout=changed;super.updateBankFilter(p,scope,status);}
 }
 static Component find(Container root,String name){for(Component child:root.getComponents()){if(name.equals(child.getName())||child instanceof AbstractButton&&name.equals(((AbstractButton)child).getText())||child instanceof JLabel&&name.equals(((JLabel)child).getText()))return child;if(child instanceof Container){Component result=find((Container)child,name);if(result!=null)return result;}}return null;}
 static void click(Container root,String name){AbstractButton b=(AbstractButton)find(root,name);assertNotNull(name,b);b.doClick();}
 static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container)layout((Container)c);}
 @Test public void ownedAlternativeRequiresSelectionKeepsOriginalUntilApplyAndSupportsUndo() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(OwnedGearTest.setup()));click(panel,"+ My gear alternatives");click(panel,"Check my Bank");JComboBox<?> choice=(JComboBox<?>)find(panel,"ownedChoice:eq:4");assertNotNull(choice);assertEquals(-1,((StudioPanel.ItemInfo)choice.getSelectedItem()).id);click(panel,"Use selected alternatives");click(panel,"Save");assertEquals(1,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("eq").get(4)));
  click(panel,"Check my Bank");((JComboBox<?>)find(panel,"ownedChoice:eq:4")).setSelectedIndex(1);click(panel,"Use selected alternatives");click(panel,"Save");assertEquals(2,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("eq").get(4)));click(panel,"Undo alternatives");click(panel,"Save");assertEquals(1,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("eq").get(4)));panel.dispose();
 });}
 @Test public void unknownBankDoesNotInventOwnership() throws Exception {SwingUtilities.invokeAndWait(()->{Host host=new Host();host.owned=new OwnedGear(Collections.emptyMap(),Collections.emptyMap(),0,false);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(OwnedGearTest.setup()));click(panel,"Check my Bank");assertNull(find(panel,"Use selected alternatives"));assertNotNull(find(panel,"Check my Bank"));panel.dispose();});}
 @Test public void liveComparisonUpdatesWithoutReplacingNotesEditorAndClearsAtLogout() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);SetupLibrary.Entry entry=new SetupLibrary.Entry(OwnedGearTest.setup());panel.open(entry);JTextArea notes=(JTextArea)find(panel,"loadoutNotes");host.live.accept(SetupCodec.blank("Actual"));assertEquals("1 gear difference(s)",((JLabel)find(panel,"gearReadiness")).getText());host.live.accept(entry.portable);assertEquals("Gear matches setup",((JLabel)find(panel,"gearReadiness")).getText());assertSame(notes,find(panel,"loadoutNotes"));host.live.accept(null);assertEquals("Live check: log in",((JLabel)find(panel,"gearReadiness")).getText());panel.dispose();assertNull(host.live);
 });}
 @Test public void changedNativeLayoutPersistsWithSaveAndLateLayoutCannotAffectAnotherDraft() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(OwnedGearTest.setup()));Consumer<JsonArray> old=host.layout;JsonArray cells=new JsonArray();cells.add(1);old.accept(cells);click(panel,"Save");SetupLibrary.Entry saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0);assertEquals(BankLayouts.Mode.CUSTOM,saved.bankLayout);assertEquals(cells,saved.portable.getAsJsonArray("layout"));panel.open(new SetupLibrary.Entry(SetupCodec.blank("Other")));old.accept(cells);click(panel,"Save");assertEquals(2,SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.size());assertEquals(0,SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("eq").get(4))+1);panel.dispose();
 });}
 @Test public void libraryUpgradeIsSelectiveAndUndoPreservesOtherSetups() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary library=new SetupLibrary();SetupLibrary.Entry one=new SetupLibrary.Entry(OwnedGearTest.setup()),two=new SetupLibrary.Entry(OwnedGearTest.setup());library.setups.add(one);library.setups.add(two);host.saved=library.serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.replaceLibraryItems(OwnedGearTest.item(1,"Bandos chestplate",4),OwnedGearTest.item(2,"Fighter torso",4),Collections.singleton(one.id));SetupLibrary changed=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);assertEquals(2,SetupCodec.id(SetupCodec.setup(changed.find(one.id).portable).getAsJsonArray("eq").get(4)));assertEquals(1,SetupCodec.id(SetupCodec.setup(changed.find(two.id).portable).getAsJsonArray("eq").get(4)));panel.undoUpgrade();assertEquals(1,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(one.id).portable).getAsJsonArray("eq").get(4)));panel.dispose();
 });}
 @Test public void alternativesStayInsideNarrowSidebar() throws Exception {SwingUtilities.invokeAndWait(()->{try{for(int width:new int[]{220,260}){Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.setSize(width,900);panel.open(new SetupLibrary.Entry(OwnedGearTest.setup()));click(panel,"+ My gear alternatives");click(panel,"Check my Bank");layout(panel);layout(panel);Component choice=find(panel,"ownedChoice:eq:4");assertNotNull(choice);assertTrue(choice.getWidth()>100);assertTrue(choice.getWidth()<=width);BufferedImage image=new BufferedImage(width,900,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();File dir=new File("build/previews/owned-alternatives");dir.mkdirs();ImageIO.write(image,"png",new File(dir,width+"-alternatives.png"));panel.dispose();}}catch(Exception e){throw new AssertionError(e);}});}
 @Test public void staleAlternativeCannotOverwriteNewEditsAndUndoCannotEraseLaterNotes() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(OwnedGearTest.setup()));click(panel,"Check my Bank");((JComboBox<?>)find(panel,"ownedChoice:eq:4")).setSelectedIndex(1);((JTextArea)find(panel,"loadoutNotes")).setText("New notes");click(panel,"Use selected alternatives");click(panel,"Save");assertEquals(1,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("eq").get(4)));
  click(panel,"Check my Bank");((JComboBox<?>)find(panel,"ownedChoice:eq:4")).setSelectedIndex(1);click(panel,"Use selected alternatives");((JTextArea)find(panel,"loadoutNotes")).setText("Later notes");try{panel.undoAlternatives();fail();}catch(IllegalStateException expected){}assertEquals("Later notes",((JTextArea)find(panel,"loadoutNotes")).getText());panel.dispose();
 });}
 @Test public void upgradeFailureLeavesSavedLibraryAndDraftIntact() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host(){@Override public void save(String scope,String text){throw new IllegalStateException("Storage unavailable");}};SetupLibrary library=new SetupLibrary();SetupLibrary.Entry entry=new SetupLibrary.Entry(OwnedGearTest.setup());library.setups.add(entry);host.saved=library.serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);try{panel.replaceLibraryItems(OwnedGearTest.item(1,"Bandos chestplate",4),OwnedGearTest.item(2,"Fighter torso",4),Collections.singleton(entry.id));fail();}catch(IllegalStateException expected){}assertEquals(1,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(entry.id).portable).getAsJsonArray("eq").get(4)));panel.dispose();
 });}
 @Test public void recentAndManualOrderingPersistInOwnLibrary() throws Exception {SwingUtilities.invokeAndWait(()->{
  Host host=new Host();SetupLibrary library=new SetupLibrary();SetupLibrary.Entry one=new SetupLibrary.Entry(SetupCodec.blank("Zulrah")),two=new SetupLibrary.Entry(SetupCodec.blank("Vorkath"));library.setups.add(one);library.setups.add(two);host.saved=library.serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(one);assertTrue(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find(one.id).lastOpened>0);panel.moveSetup(two.id,-1);assertEquals(two.id,SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).id);assertEquals("Manual",((JComboBox<?>)find(panel,"librarySort")).getSelectedItem());panel.dispose();
 });}
}
