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
        Graphics2D graphics=icon.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);graphics.drawImage(source,2,2,28,28,null);graphics.dispose();return icon;
    }
}
