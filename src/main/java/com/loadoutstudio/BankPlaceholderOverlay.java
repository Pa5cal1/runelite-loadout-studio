/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.IntFunction;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Missing layout items are drawn only; no synthetic Bank widgets or withdraw actions. */
final class BankPlaceholderOverlay extends Overlay {
    private final Client client;private final IndependentBankView view;private final ItemManager items;
    BankPlaceholderOverlay(Client client,IndependentBankView view,ItemManager items){
        this.client=client;this.view=view;this.items=items;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);drawAfterLayer(InterfaceID.Bankmain.ITEMS);
    }
    @Override public Dimension render(Graphics2D graphics){
        if(!view.active()||client.getGameState()!=GameState.LOGGED_IN)return null;
        Widget container=client.getWidget(InterfaceID.Bankmain.ITEMS);if(container==null||container.isHidden())return null;
        paint(graphics,container.getBounds(),container.getScrollY(),view.placeholders(),id->items.getImage(id,1,false));return null;
    }
    static void paint(Graphics2D graphics,Rectangle bounds,int scrollY,int[] cells,IntFunction<BufferedImage> image){
        Graphics2D layer=(Graphics2D)graphics.create();try{
            layer.clip(bounds);layer.setComposite(AlphaComposite.SrcOver.derive(0.53f));
            int first=Math.min(cells.length,Math.max(0,scrollY/36)*8);
            int end=Math.min(cells.length,Math.max(0,(scrollY+bounds.height+35)/36)*8);
            for(int pos=first;pos<end;pos++){
                if(cells[pos]<1)continue;int x=bounds.x+51+(pos%8)*48,y=bounds.y+(pos/8)*36-scrollY;
                if(!bounds.intersects(x,y,36,32))continue;
                BufferedImage sprite=image.apply(cells[pos]);if(sprite!=null)layer.drawImage(sprite,x,y,null);
            }
        }finally{layer.dispose();}
    }
}
