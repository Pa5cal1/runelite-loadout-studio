/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import javax.swing.JButton;
import net.runelite.client.ui.FontManager;

/** Counts are an overlay inside the cell, not a second layout row above its icon. */
final class LoadoutSlotButton extends JButton
{
    private String amount = "";
    LoadoutSlotButton(String text) { super(text); putClientProperty("html.disable", true); }
    void amount(String value) { amount = value; repaint(); }
    @Override protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        if (amount.isEmpty()) return;
        Graphics2D g=(Graphics2D)graphics.create(); g.setClip(0,0,getWidth(),getHeight());
        g.setFont(amount.equals("∞") ? new Font(Font.SANS_SERIF,Font.BOLD,12) : FontManager.getRunescapeSmallFont());
        int baseline=Math.min(getHeight()-2,g.getFontMetrics().getAscent()+1);
        g.setColor(Color.BLACK); g.drawString(amount,3,baseline+1); g.setColor(Color.YELLOW); g.drawString(amount,2,baseline); g.dispose();
    }
}
