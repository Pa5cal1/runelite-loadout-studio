/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
final class StudioBrandIcon
{
    static BufferedImage create()
    {
        BufferedImage source=ImageUtil.loadImageResource(StudioBrandIcon.class,"combat-tutor.png"), icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=icon.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);int minX=source.getWidth(),minY=source.getHeight(),maxX=0,maxY=0;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>0){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        graphics.drawImage(source,0,0,32,32,minX,minY,maxX+1,maxY+1,null);graphics.dispose();return icon;
    }
}
