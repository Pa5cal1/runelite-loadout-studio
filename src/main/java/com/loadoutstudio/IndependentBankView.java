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
    private final Set<Widget> copies=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Integer,Integer> copyIds=new HashMap<>();
    void removeCopies(){
        Widget container=client.getWidget(InterfaceID.Bankmain.ITEMS);if(container!=null&&container.getChildren()!=null){Widget[] children=container.getChildren();for(Widget copy:copies){copy.setHidden(true);copy.clearActions();}int length=children.length;while(length>0&&copies.contains(children[length-1]))length--;if(length!=children.length)container.setChildren(Arrays.copyOf(children,length));}copies.clear();copyIds.clear();
    }
    void rewrite(net.runelite.api.events.MenuOptionClicked event){
        if(!active||event.getParam1()!=InterfaceID.Bankmain.ITEMS||!copyIds.containsKey(event.getParam0()))return;
        if(event.getMenuAction()!=net.runelite.api.MenuAction.CC_OP&&event.getMenuAction()!=net.runelite.api.MenuAction.CC_OP_LOW_PRIORITY)return;
        int id=copyIds.get(event.getParam0());net.runelite.api.ItemContainer bank=client.getItemContainer(net.runelite.api.gameval.InventoryID.BANK);int index=bank==null?-1:bank.find(id);
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
            Widget source=children[duplicateSources[pos]],copy=container.createChild(-1,source.getType());copies.add(copy);copyIds.put(copy.getIndex(),source.getItemId());
            copy.setItemId(source.getItemId());copy.setItemQuantity(source.getItemQuantity());copy.setItemQuantityMode(source.getItemQuantityMode());copy.setName(source.getName());copy.setClickMask(source.getClickMask());copy.setOnOpListener(source.getOnOpListener());copy.setHasListener(true);copy.setBorderType(source.getBorderType());
            copy.setOriginalWidth(source.getOriginalWidth());copy.setOriginalHeight(source.getOriginalHeight());copy.setOriginalX(51+(pos%8)*48);copy.setOriginalY((pos/8)*36);copy.setHidden(false);
            String[] actions=source.getActions();if(actions!=null)for(int i=0;i<actions.length;i++)if(actions[i]!=null)copy.setAction(i,actions[i]);
            final int position=pos;copy.setDragDeadTime(source.getDragDeadTime());copy.setDragDeadZone(source.getDragDeadZone());copy.setDragParent(source.getDragParent());copy.setOnDragCompleteListener((JavaScriptCallback)e->drop(container,position,copy.getItemId()));copy.revalidate();placeholders[pos]=-1;
        }
        int height=Math.max(container.getHeight(),((Math.max(last,cells.length-1)/8)+1)*36);container.setScrollHeight(height);container.setScrollY(Math.max(0,Math.min(container.getScrollY(),height-container.getHeight())));container.revalidate();
    }
    private void drop(Widget container,int from,int realId){
        if(!active)return;net.runelite.api.Point mouse=client.getMouseCanvasPosition();Rectangle bounds=container.getBounds();
        if(mouse==null||!bounds.contains(mouse.getX(),mouse.getY()))return;
        int relativeX=mouse.getX()-bounds.x-51+6;if(relativeX<0)return;int column=relativeX/48,row=(mouse.getY()-bounds.y+container.getScrollY())/36;
        if(column<0||column>7||row<0||row*8+column>8191)return;int to=row*8+column;if(from==to)return;
        int length=Math.max(cells.length,Math.max(from,to)+1);int[] updated=Arrays.copyOf(cells,length);Arrays.fill(updated,cells.length,length,-1);
        if(updated[from]<1)updated[from]=canonical.applyAsInt(realId);int swap=updated[to];updated[to]=updated[from];updated[from]=swap;cells=updated;
        // The gesture changes this plugin's layout, not the game's Bank order.
        client.setDraggedOnWidget(null);changed.run();thread.invokeLater(this::arrange);
    }

}
