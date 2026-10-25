/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.Rectangle;
import java.util.*;
import java.util.function.*;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.bank.BankSearch;

/** Independent Bank view. Copies reuse native actions and route to the real Bank slot. */
final class IndependentBankView {
    private final Client client;private final ClientThread thread;private final BankSearch search;private final IntUnaryOperator canonical;
    
    private boolean active,rebuildQueued;private int[] cells=new int[0],placeholders=new int[0];private IntPredicate matches=id->false;private Runnable changed=()->{};
    private Consumer<Throwable> failed=error->{};
    private final Map<Integer,Integer> displayedStock=new HashMap<>();
    private final Map<Widget,CopyState> copies=new IdentityHashMap<>();
    private final Map<Integer,Integer> copyIds=new HashMap<>();
    private final Map<Integer,StudioPotionStorage.Potion> storageCopies=new HashMap<>();
    private StudioPotionStorage potions;
    void potions(StudioPotionStorage value){potions=value;}
    /** Borrow server-configured native slots rather than append unconfigured widgets. */
    private static final class CopyState {
        final int id,quantity,mode,x,y,opacity,border;final boolean hidden;final String name;final String[] actions;final Object[] op;
        CopyState(Widget w){id=w.getItemId();quantity=w.getItemQuantity();mode=w.getItemQuantityMode();x=w.getOriginalX();y=w.getOriginalY();opacity=w.getOpacity();border=w.getBorderType();hidden=w.isSelfHidden();name=w.getName();actions=w.getActions()==null?null:w.getActions().clone();op=w.getOnOpListener();}
        void restore(Widget w){w.setItemId(id);w.setItemQuantity(quantity);w.setItemQuantityMode(mode);w.setOriginalX(x);w.setOriginalY(y);w.setOpacity(opacity);w.setBorderType(border);w.setName(name);w.clearActions();if(actions!=null)for(int i=0;i<actions.length;i++)if(actions[i]!=null)w.setAction(i,actions[i]);w.setOnOpListener(op);w.setHidden(hidden);w.revalidate();}
    }
    void removeCopies(){for(Map.Entry<Widget,CopyState> copy:copies.entrySet())copy.getValue().restore(copy.getKey());copies.clear();copyIds.clear();storageCopies.clear();}
    Widget proxy(Widget[] children,int[] ids,Widget source,int position,int id,int quantity,String name,String[] actions){
        for(int index=0;index<children.length;index++){
            Widget w=children[index];if(w==null||ids[index]>0||copies.containsKey(w)||w.getType()!=net.runelite.api.widgets.WidgetType.GRAPHIC||w.getOriginalHeight()!=32)continue;
            copies.put(w,new CopyState(w));copyIds.put(w.getIndex(),id);w.setItemId(id);w.setItemQuantity(quantity);w.setItemQuantityMode(net.runelite.api.widgets.ItemQuantityMode.STACKABLE);w.setName(name);w.setOpacity(0);w.clearActions();if(actions!=null)for(int i=0;i<actions.length;i++)if(actions[i]!=null)w.setAction(i,actions[i]);
            // Retain this native slot's server-provided operation permissions.
            w.setOriginalX(51+(position%8)*48);w.setOriginalY((position/8)*36);w.setHidden(false);if(source!=null){w.setOnOpListener(source.getOnOpListener());w.setBorderType(source.getBorderType());}
            w.setOnDragCompleteListener((JavaScriptCallback)e->drop(client.getWidget(InterfaceID.Bankmain.ITEMS),position,id));w.revalidate();return w;
        }return null;
    }
    void rewrite(net.runelite.api.events.MenuOptionClicked event){
        if(!active||event.getParam1()!=InterfaceID.Bankmain.ITEMS||!copyIds.containsKey(event.getParam0()))return;
        if(event.getMenuAction()!=net.runelite.api.MenuAction.CC_OP&&event.getMenuAction()!=net.runelite.api.MenuAction.CC_OP_LOW_PRIORITY)return;
        int id=copyIds.get(event.getParam0());StudioPotionStorage.Potion potion=storageCopies.get(event.getParam0());
        if(potion!=null){
            if(potions==null||!potions.current(potion)||!potions.prepareWidgets()){event.consume();rebuild();return;}
            event.getMenuEntry().setParam1(InterfaceID.Bankmain.POTIONSTORE_ITEMS).setItemId(id);
            if(event.getId()>0xffff)event.getMenuEntry().setParam0(potion.dosesChild()).setIdentifier((event.getId()>>16)-1);else {if(potion.count()<1){event.consume();return;}event.getMenuEntry().setParam0(potion.withdrawChild());}
            return;
        }
        net.runelite.api.ItemContainer bank=client.getItemContainer(net.runelite.api.gameval.InventoryID.BANK);int index=bank==null||bank.count(id)<1?-1:bank.find(id);
        if(index<0){event.consume();rebuild();return;}event.getMenuEntry().setParam0(index).setItemId(id);
    }
    int stock(int id){return displayedStock.getOrDefault(canonical.applyAsInt(id),0);}
    void onFailure(Consumer<Throwable> failed){this.failed=failed;}
    IndependentBankView(Client client,ClientThread thread,BankSearch search,IntUnaryOperator canonical){this.client=client;this.thread=thread;this.search=search;this.canonical=canonical;}
    boolean active(){return active;}int[] layout(){return cells.clone();}int[] placeholders(){return active?placeholders.clone():new int[0];}void onChanged(Runnable changed){this.changed=changed;}
    void select(int[] layout,IntPredicate predicate){cells=layout.clone();placeholders=new int[0];displayedStock.clear();matches=predicate;active=true;rebuild();}
    void clear(){boolean wasActive=active;active=false;placeholders=new int[0];displayedStock.clear();removeCopies();if(wasActive)rebuild();}
    private void rebuild(){if(rebuildQueued)return;rebuildQueued=true;thread.invokeAtTickEnd(()->{rebuildQueued=false;try{search.layoutBank();}catch(RuntimeException|AssertionError error){active=false;placeholders=new int[0];failed.accept(error);}});}
    void callback(ScriptCallbackEvent event){Widget bank=client.getWidget(InterfaceID.Bankmain.ITEMS);if(!active||bank==null||bank.isHidden())return;BankViewPlan.callback(event.getEventName(),client.getIntStack(),client.getIntStackSize(),matches);}

    void arrange(){
        if(!active)return;removeCopies();Widget container=client.getWidget(InterfaceID.Bankmain.ITEMS);if(container==null||container.isHidden()||container.getChildren()==null)return;
        Widget[] children=container.getChildren();int[] ids=new int[children.length],amounts=new int[children.length];for(int i=0;i<children.length;i++){Widget item=children[i];ids[i]=item!=null&&!item.isHidden()&&matches.test(item.getItemId())?item.getItemId():-1;amounts[i]=item==null?0:item.getItemQuantity();}displayedStock.clear();displayedStock.putAll(BankViewPlan.stock(ids,amounts,canonical));int[] positions=BankViewPlan.positions(ids,cells,canonical);int last=-1;
        for(int child=0;child<children.length;child++){Widget item=children[child];
            if(item==null||item.getItemId()<1||item.isHidden()||!matches.test(item.getItemId()))continue;
            int pos=positions[child];if(pos<0)continue;last=Math.max(last,pos);item.setOriginalX(51+(pos%8)*48);item.setOriginalY((pos/8)*36);item.revalidate();
            final int source=pos;
            item.setOnDragCompleteListener((JavaScriptCallback)e->drop(container,source,item.getItemId()));
        }
        placeholders=BankViewPlan.placeholders(cells,positions);
        int[] duplicateSources=BankViewPlan.duplicateSources(ids,amounts,cells,positions,canonical);
        for(int pos=0;pos<duplicateSources.length;pos++)if(duplicateSources[pos]>=0){
            Widget source=children[duplicateSources[pos]];String[] actions=BankViewPlan.bankActions(client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_QUANTITY_TYPE),client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_REQUESTEDQUANTITY),source.getActions());if(proxy(children,ids,source,pos,source.getItemId(),source.getItemQuantity(),source.getName(),actions)!=null)placeholders[pos]=-1;
        }
        if(potions!=null)for(int pos=0;pos<cells.length;pos++)if(cells[pos]>0){
            boolean real=false;for(int source=0;source<positions.length;source++)if(positions[source]==pos&&amounts[source]>0){real=true;break;}if(real||duplicateSources[pos]>=0)continue;
            StudioPotionStorage.Potion potion=potions.forItem(cells[pos]);if(potion==null)continue;
            for(int source=0;source<positions.length;source++)if(positions[source]==pos&&amounts[source]==0){children[source].setHidden(true);ids[source]=-1;}
            int actual=potion.itemId(),quantity=potion.count();Widget copy=proxy(children,ids,null,pos,actual,quantity,potions.name(actual),StudioPotionStorage.actions(client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_QUANTITY_TYPE),client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_REQUESTEDQUANTITY),quantity>0));
            if(copy!=null){storageCopies.put(copy.getIndex(),potion);for(int dose=1;dose<=4;dose++)if(potion.ids[dose-1]>0)copy.setSubOp(10,5+dose,dose+" Dose"+(dose==1?"":"s"));placeholders[pos]=-1;displayedStock.put(canonical.applyAsInt(cells[pos]),quantity);}
        }
        int height=Math.max(container.getHeight(),((Math.max(last,cells.length-1)/8)+1)*36);container.setScrollHeight(height);container.setScrollY(Math.max(0,Math.min(container.getScrollY(),height-container.getHeight())));container.revalidate();
    }
    private void drop(Widget container,int from,int realId){
        if(!active||container==null)return;net.runelite.api.Point mouse=client.getMouseCanvasPosition();Rectangle bounds=container.getBounds();
        if(mouse==null||!bounds.contains(mouse.getX(),mouse.getY()))return;
        int relativeX=mouse.getX()-bounds.x-51+6;if(relativeX<0)return;int column=relativeX/48,row=(mouse.getY()-bounds.y+container.getScrollY())/36;
        if(column<0||column>7||row<0||row*8+column>8191)return;int to=row*8+column;if(from==to)return;
        int length=Math.max(cells.length,Math.max(from,to)+1);int[] updated=Arrays.copyOf(cells,length);Arrays.fill(updated,cells.length,length,-1);
        if(updated[from]<1)updated[from]=canonical.applyAsInt(realId);int swap=updated[to];updated[to]=updated[from];updated[from]=swap;cells=updated;
        // The gesture changes this plugin's layout, not the game's Bank order.
        client.setDraggedOnWidget(null);changed.run();thread.invokeLater(this::arrange);
    }

}
