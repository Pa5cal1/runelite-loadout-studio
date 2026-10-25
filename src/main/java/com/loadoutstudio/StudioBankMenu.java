/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
/** The native client permits submenus only directly below the root menu. */
final class StudioBankMenu {
 static void add(Menu root,boolean selected,boolean layouts,List<StudioPanel.QuickLink> links,Consumer<Consumer<StudioPanel>> dispatch,Consumer<String> open){
  if(!selected&&links.isEmpty())return;
  Menu menu=selected?root.createMenuEntry(-1).setOption("Loadout:").setType(MenuAction.RUNELITE).createSubMenu():null;
  if(selected){
   action(menu,"Show bank",()->dispatch.accept(StudioPanel::showBankFromMenu));
   action(menu,"Return to loadout",()->dispatch.accept(StudioPanel::activateFromBankMenu));
   action(menu,"Filter Bank",()->dispatch.accept(StudioPanel::activateFromBankMenu));
   action(menu,"Clear filter",()->dispatch.accept(StudioPanel::clearFromBankMenu));
   if(layouts){
    action(menu,"Layout: Preset",()->dispatch.accept(p->p.layoutFromBankMenu(BankLayouts.Mode.PRESET,false)));
    action(menu,"Layout: Zigzag (top first)",()->dispatch.accept(p->p.layoutFromBankMenu(BankLayouts.Mode.ZIGZAG,false)));
    action(menu,"Layout: Zigzag (bottom first)",()->dispatch.accept(p->p.layoutFromBankMenu(BankLayouts.Mode.ZIGZAG,true)));
   }
  }
  Menu saved=links.isEmpty()?null:root.createMenuEntry(-1).setOption("Loadout setups:").setType(MenuAction.RUNELITE).createSubMenu();
  for(StudioPanel.QuickLink link:links)action(saved,"Open: "+link.name.replace("<","").replace(">",""),()->open.accept(link.id));
 }
 private static void action(Menu menu,String label,Runnable callback){menu.createMenuEntry(-1).setOption(label).setType(MenuAction.RUNELITE).onClick(e->callback.run());}
}
