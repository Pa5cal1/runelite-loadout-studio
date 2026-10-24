/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
/** A native drag image with a cursor fallback; no windows or game input are created. */
final class ItemDragImage {
    private static final Map<TransferHandler,DragSourceListener> listeners=new IdentityHashMap<>();
    static void prepare(TransferHandler handler,JComponent source,Icon icon) {
        finish(handler);if(icon==null)return;
        BufferedImage image=new BufferedImage(36,36,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();icon.paintIcon(source,g,2,2);g.dispose();handler.setDragImage(image);handler.setDragImageOffset(new Point(16,16));
        if(GraphicsEnvironment.isHeadless()||DragSource.isDragImageSupported())return;
        Dimension best=Toolkit.getDefaultToolkit().getBestCursorSize(36,36);if(best.width==0||best.height==0)return;
        Cursor cursor=Toolkit.getDefaultToolkit().createCustomCursor(image,new Point(0,0),"Loadout item");
        DragSourceListener listener=new DragSourceAdapter(){private void show(DragSourceEvent e){if(e.getDragSourceContext().getComponent()==source)e.getDragSourceContext().setCursor(cursor);}
            public void dragEnter(DragSourceDragEvent e){show(e);}public void dragOver(DragSourceDragEvent e){show(e);}public void dropActionChanged(DragSourceDragEvent e){show(e);}public void dragDropEnd(DragSourceDropEvent e){finish(handler);}};
        listeners.put(handler,listener);DragSource.getDefaultDragSource().addDragSourceListener(listener);
    }
    static void finish(TransferHandler handler){DragSourceListener listener=listeners.remove(handler);if(listener!=null)DragSource.getDefaultDragSource().removeDragSourceListener(listener);}
}
