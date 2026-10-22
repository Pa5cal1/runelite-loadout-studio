/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsyncSlotImageTest
{
    @Test public void initiallyEmptyRuneLiteSpriteRepaintsWhenLoaded() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            LoadoutSlotButton cell = new LoadoutSlotButton("");
            int[] repaints = {0};
            RepaintManager previous = RepaintManager.currentManager(cell);
            RepaintManager.setCurrentManager(new RepaintManager() {
                @Override public void addDirtyRegion(JComponent component, int x, int y, int width, int height) {
                    if (component == cell) repaints[0]++;
                    super.addDirtyRegion(component, x, y, width, height);
                }
            });
            try {
            AsyncBufferedImage sprite = new AsyncBufferedImage(null, 32, 32, BufferedImage.TYPE_INT_ARGB);
            cell.itemIcon(new ImageIcon(sprite));
            assertEquals(0, sprite.getRGB(16, 16));
            int before = repaints[0];
            Graphics2D graphics = sprite.createGraphics();
            graphics.setColor(Color.MAGENTA); graphics.fillRect(0, 0, 32, 32); graphics.dispose();
            sprite.loaded();
            assertTrue("A delayed sprite must repaint its slot without hover or another UI event", repaints[0] > before);
            BufferedImage rendered = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D target = rendered.createGraphics(); cell.getIcon().paintIcon(cell, target, 0, 0); target.dispose();
            assertEquals(Color.MAGENTA.getRGB(), rendered.getRGB(16, 16));
            } finally { RepaintManager.setCurrentManager(previous); }
        });
    }
}
