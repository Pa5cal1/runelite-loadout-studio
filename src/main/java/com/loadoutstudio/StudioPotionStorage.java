/* SPDX-License-Identifier: BSD-2-Clause
 * Native potion enumeration and widget routing follow RuneLite's PotionStorage
 * by Adam (2024). Redistribution notice: runelite-potion-storage-LICENSE.txt.
 */
package com.loadoutstudio;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
/** Local, on-demand potion-store observation; independent of Bank Tags enablement. */
final class StudioPotionStorage {
 static final int COMPONENTS_PER_POTION=5;
 static final class Potion {
  final int index;final int[] ids;final int doses,withdrawDose;
  Potion(int index,int[] ids,int doses,int withdrawDose){this.index=index;this.ids=ids.clone();this.doses=doses;this.withdrawDose=withdrawDose;}
  int itemId(){return withdrawDose>0&&withdrawDose<=ids.length?ids[withdrawDose-1]:-1;}
  int count(){return withdrawDose>0?doses/withdrawDose:0;}
  int withdrawChild(){return index*COMPONENTS_PER_POTION;}
  int dosesChild(){return withdrawChild()+1;}
 }
 private final Client client;private final ItemManager items;
 private final Map<Integer,Potion> byItem=new LinkedHashMap<>();private final Set<Integer> watched=new HashSet<>();private boolean dirty=true;private int potionCount;
 StudioPotionStorage(Client client,ItemManager items){this.client=client;this.items=items;}
 String name(int id){return "<col=ff9040>"+items.getItemComposition(id).getMembersName()+"</col> (Potion storage)";}
 void clear(){byItem.clear();watched.clear();potionCount=0;dirty=true;}
 void invalidate(){dirty=true;}
 boolean changed(int varp){
  boolean relevant=watched.contains(varp)||varp==VarPlayerID.POTIONSTORE_BASE_VAR_1||varp==VarPlayerID.POTIONSTORE_BASE_VAR_2||varp==VarPlayerID.POTIONSTORE_BASE_VAR_3||varp==VarPlayerID.POTIONSTORE_BASE_VAR_4||varp==VarPlayerID.POTIONSTORE_BASE_VAR_5||varp==VarPlayerID.POTIONSTORE_BASE_VAR_6;
  if(relevant)dirty=true;return relevant;
 }
 /** Call only at tick end, outside the client-script VM. */
 void refresh(){
  if(!dirty||client.getGameState()!=GameState.LOGGED_IN)return;
  Widget bank=client.getWidget(InterfaceID.Bankmain.ITEMS),storage=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);if(bank==null||bank.isHidden()||storage==null)return;
  List<Potion> next=new ArrayList<>();int index=0;
  for(int enumId:new int[]{EnumID.POTIONSTORE_POTIONS,EnumID.POTIONSTORE_UNFINISHED_POTIONS}){
   EnumComposition list=client.getEnum(enumId);if(list==null)continue;
   for(int potionEnumId:list.getIntVals()){
    EnumComposition potion=client.getEnum(potionEnumId);client.runScript(ScriptID.POTIONSTORE_DOSES,potionEnumId);int doses=client.getIntStack()[0];client.runScript(ScriptID.POTIONSTORE_WITHDRAW_DOSES,potionEnumId);int selected=client.getIntStack()[0];int[] ids=new int[4];for(int i=0;i<4;i++)ids[i]=potion.getIntValue(i+1);
    Potion entry=new Potion(index++,ids,doses,selected);next.add(entry);
   }
  }
  install(next,index);int[] triggers=storage.getVarTransmitTrigger();if(triggers!=null)for(int varp:triggers)watched.add(varp);
 }
 void install(List<Potion> potions,int total){byItem.clear();for(Potion potion:potions)if(potion.doses>0&&potion.withdrawDose>0&&potion.withdrawDose<=4&&potion.itemId()>0)for(int id:potion.ids)if(id>0)byItem.put(id,potion);potionCount=total;dirty=false;}
 void unavailable(){byItem.clear();dirty=false;}
 Potion forItem(int requested){return dirty?null:byItem.get(requested);}
 boolean current(Potion potion){Potion latest=byItem.get(potion.itemId());return !dirty&&latest!=null&&latest.index==potion.index&&latest.withdrawDose==potion.withdrawDose&&latest.doses>0;}
 Map<Integer,Integer> counts(){Map<Integer,Integer> result=new LinkedHashMap<>();if(!dirty)for(Potion p:byItem.values())if(p.count()>0)result.put(p.itemId(),p.count());return result;}
 boolean prepareWidgets(){
  Widget storage=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);if(storage==null||dirty)return false;
  if(storage.getChildren()==null){int index=0;for(int i=0;i<potionCount;i++)for(int child=0;child<COMPONENTS_PER_POTION;child++)storage.createChild(index++,WidgetType.GRAPHIC);storage.createChild(index++,WidgetType.GRAPHIC);storage.createChild(index++,WidgetType.RECTANGLE);storage.createChild(index++,WidgetType.TEXT);storage.createChild(index++,WidgetType.RECTANGLE);storage.createChild(index,WidgetType.TEXT);}return true;
 }
 static String[] actions(int quantityType,int requested,boolean canWithdraw){String[] actions=actions(quantityType,requested);if(!canWithdraw)Arrays.fill(actions,0,10,null);return actions;}
 /** The same existing native operation IDs as the Bank and potion-store layouts. */
 static String[] actions(int quantityType,int requested){String suffix=quantityType==1?"5":quantityType==2?"10":quantityType==3?String.valueOf(Math.max(1,requested)):quantityType==4?"All":"1";String[] actions=new String[11];actions[0]="Withdraw-"+suffix;if(quantityType!=0)actions[1]="Withdraw-1";actions[2]="Withdraw-5";actions[3]="Withdraw-10";if(requested>0)actions[4]="Withdraw-"+requested;actions[5]="Withdraw-X";actions[6]="Withdraw-All";actions[7]="Withdraw-All-but-1";actions[10]="Doses";return actions;}
}
