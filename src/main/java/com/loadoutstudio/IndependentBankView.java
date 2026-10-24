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

/** Independent Bank view. Real item IDs, quantities and child indices are never rewritten. */
final class IndependentBankView {
    private final Client client;private final ClientThread thread;private final BankSearch search;private final IntUnaryOperator canonical;
    
    private boolean active,rebuildQueued;private int[] cells=new int[0];private IntPredicate matches=id->false;private Runnable changed=()->{};
    IndependentBankView(Client client,ClientThread thread,BankSearch search,IntUnaryOperator canonical){this.client=client;this.thread=thread;this.search=search;this.canonical=canonical;}
    boolean active(){return active;}int[] layout(){return cells.clone();}void onChanged(Runnable changed){this.changed=changed;}
    void select(int[] layout,IntPredicate predicate){cells=layout.clone();matches=predicate;active=true;rebuild();}
    void clear(){boolean wasActive=active;active=false;if(wasActive)rebuild();}
    private void rebuild(){if(rebuildQueued)return;rebuildQueued=true;thread.invokeLater(()->{rebuildQueued=false;search.layoutBank();});}
    void callback(ScriptCallbackEvent event){Widget bank=client.getWidget(InterfaceID.Bankmain.ITEMS);if(!active||bank==null||bank.isHidden())return;BankViewPlan.callback(event.getEventName(),client.getIntStack(),client.getIntStackSize(),matches);}

    void arrange(){
        if(!active)return;Widget container=client.getWidget(InterfaceID.Bankmain.ITEMS);if(container==null||container.isHidden()||container.getChildren()==null)return;
        Widget[] children=container.getChildren();int[] ids=new int[children.length];for(int i=0;i<children.length;i++){Widget item=children[i];ids[i]=item!=null&&!item.isHidden()&&matches.test(item.getItemId())?item.getItemId():-1;}int[] positions=BankViewPlan.positions(ids,cells,canonical);int last=-1;
        for(int child=0;child<children.length;child++){Widget item=children[child];
            if(item==null||item.getItemId()<1||item.isHidden()||!matches.test(item.getItemId()))continue;
            int pos=positions[child];if(pos<0)continue;last=Math.max(last,pos);item.setOriginalX(51+(pos%8)*48);item.setOriginalY((pos/8)*36);item.revalidate();
            final int source=pos;
            item.setOnDragCompleteListener((JavaScriptCallback)e->drop(container,source,item.getItemId()));
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
