/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class NativeBankMenuTest {
 /** Reproduce the client's parent==root assertion when another submenu opens. */
 private static class NativeMenu implements Menu {
  final int depth;final List<MenuEntry> entries=new ArrayList<>();NativeMenu(int depth){this.depth=depth;}
  public MenuEntry createMenuEntry(int index){Entry e=new Entry(this);entries.add(e);return e;}
  public MenuEntry[] getMenuEntries(){return entries.toArray(new MenuEntry[0]);}public void setMenuEntries(MenuEntry[] e){entries.clear();entries.addAll(Arrays.asList(e));}public void removeMenuEntry(MenuEntry e){entries.remove(e);}
  public int getMenuX(){return 0;}public int getMenuY(){return 0;}public int getMenuWidth(){return 0;}public int getMenuHeight(){return 0;}
  Entry find(String name){return (Entry)entries.stream().filter(e->e.getOption().equals(name)).findFirst().orElseThrow(()->new AssertionError(name));}
 }
 private static class Entry implements MenuEntry {
  final NativeMenu owner;String option="",target="";MenuAction type;int id,p0,p1,world,itemId;boolean force,deprioritized;Consumer<MenuEntry> click;NativeMenu sub;
  Entry(NativeMenu owner){this.owner=owner;}
  public String getOption(){return option;}public MenuEntry setOption(String v){option=v;return this;}public String getTarget(){return target;}public MenuEntry setTarget(String v){target=v;return this;}
  public int getIdentifier(){return id;}public MenuEntry setIdentifier(int v){id=v;return this;}public MenuAction getType(){return type;}public MenuEntry setType(MenuAction v){type=v;return this;}
  public int getParam0(){return p0;}public MenuEntry setParam0(int v){p0=v;return this;}public int getParam1(){return p1;}public MenuEntry setParam1(int v){p1=v;return this;}
  public boolean isForceLeftClick(){return force;}public MenuEntry setForceLeftClick(boolean v){force=v;return this;}public int getWorldViewId(){return world;}public MenuEntry setWorldViewId(int v){world=v;return this;}
  public boolean isDeprioritized(){return deprioritized;}public MenuEntry setDeprioritized(boolean v){deprioritized=v;return this;}public MenuEntry onClick(Consumer<MenuEntry> v){click=v;return this;}public Consumer<MenuEntry> onClick(){return click;}
  public boolean isItemOp(){return false;}public int getItemOp(){return 0;}public int getItemId(){return itemId;}public MenuEntry setItemId(int v){itemId=v;return this;}public Widget getWidget(){return null;}public NPC getNpc(){return null;}public Player getPlayer(){return null;}public Actor getActor(){return null;}
  public Menu getSubMenu(){return sub;}public Menu createSubMenu(){if(owner.depth!=0)throw new AssertionError("Native submenu parent must be root");sub=new NativeMenu(1);return sub;}public void deleteSubMenu(){sub=null;}
  void click(){assertNotNull(click);click.accept(this);}
 }
 @Test public void layoutAndSavedLinksNeverCreateSecondLevelMenusAndKeepNativeWornAction(){
  NativeMenu root=new NativeMenu(0);MenuEntry nativeEntry=root.createMenuEntry(-1).setOption("Show worn items").setType(MenuAction.CC_OP);List<String> opened=new ArrayList<>();
  StudioBankMenu.add(root,true,true,Arrays.asList(new StudioPanel.QuickLink("one","First"),new StudioPanel.QuickLink("two","Second")),action->{},opened::add);
  assertSame(nativeEntry,root.getMenuEntries()[0]);NativeMenu sub=root.find("Loadout Studio").sub;assertEquals(9,sub.entries.size());for(MenuEntry entry:sub.entries){assertNull(entry.getSubMenu());assertNotNull(entry.onClick());}
  sub.find("Layout: Preset");sub.find("Layout: Zigzag (bottom first)");sub.find("Open: Second").click();assertEquals(Collections.singletonList("two"),opened);
 }
 @Test public void flatPresetZigzagClearAndReturnStillDispatchToTheCurrentDraft()throws Exception{SwingUtilities.invokeAndWait(()->{
  BankMenuWorkflowTest.Host host=new BankMenuWorkflowTest.Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);com.google.gson.JsonObject p=SetupCodec.blank("Flat menu");SetupCodec.setup(p).getAsJsonArray("inv").set(2,SetupCodec.item(560,15000));SetupCodec.rebuildLayout(p);panel.newDraft(p);NativeMenu root=new NativeMenu(0);StudioBankMenu.add(root,true,true,Collections.emptyList(),a->a.accept(panel),id->fail());NativeMenu menu=root.find("Loadout Studio").sub;
  menu.find("Layout: Zigzag (bottom first)").click();assertTrue(host.explicit);assertEquals(BankLayouts.generate(p,BankLayouts.Mode.ZIGZAG,true),host.selectedBankSetup.get("layout"));menu.find("Clear filter").click();assertFalse(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());menu.find("Return to loadout").click();assertTrue(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());panel.dispose();
 });}
 @Test public void menuTogglesKeepOnlyAllowedActionsAndNoSelectionCanStillOpenASavedSetup(){
  NativeMenu root=new NativeMenu(0);StudioBankMenu.add(root,true,false,Collections.emptyList(),a->{},id->{});assertEquals(4,root.find("Loadout Studio").sub.entries.size());NativeMenu empty=new NativeMenu(0);StudioBankMenu.add(empty,false,true,Collections.emptyList(),a->fail(),id->fail());assertEquals(0,empty.entries.size());NativeMenu links=new NativeMenu(0);List<String> opened=new ArrayList<>();StudioBankMenu.add(links,false,false,Collections.singletonList(new StudioPanel.QuickLink("saved","Saved setup")),a->fail(),opened::add);links.find("Loadout Studio").sub.find("Open: Saved setup").click();assertEquals(Collections.singletonList("saved"),opened);
 }
}
