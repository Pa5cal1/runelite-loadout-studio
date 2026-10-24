/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import javax.swing.Icon;

/** Small locally drawn controls; no icon font or remote asset requests. */
final class SidebarIcon implements Icon
{
    enum Kind { COFFEE, NEW, CAPTURE, IMPORT, SECTIONS, SEARCH, CLEAR }
    private final Kind kind;
    SidebarIcon(Kind kind) { this.kind = kind; }
    public int getIconWidth() { return 16; }
    public int getIconHeight() { return 16; }
    public void paintIcon(Component component, Graphics graphics, int x, int y)
    {
        Graphics2D g = (Graphics2D)graphics.create(); g.translate(x, y);
        g.setColor(kind == Kind.NEW ? StudioTheme.ACCENT : component.getForeground());
        switch (kind)
        {
            case COFFEE:
                g.drawRoundRect(3, 6, 9, 7, 2, 2); g.drawArc(11, 7, 4, 5, -90, 180); g.drawLine(2, 15, 13, 15);
                g.setColor(StudioTheme.FAVORITE); g.drawLine(5, 1, 5, 3); g.drawLine(9, 2, 9, 4); break;
            case NEW: g.fillRect(7, 2, 2, 12); g.fillRect(2, 7, 12, 2); break;
            case CAPTURE:
                g.drawRoundRect(1, 5, 13, 9, 2, 2); g.drawRect(4, 2, 6, 3); g.drawOval(5, 7, 5, 5); break;
            case IMPORT:
                g.drawLine(2, 10, 2, 14); g.drawLine(2, 14, 14, 14); g.drawLine(14, 14, 14, 10);
                g.drawLine(8, 1, 8, 10); g.drawLine(4, 6, 8, 10); g.drawLine(8, 10, 12, 6); break;
            case SECTIONS:
                g.drawRect(2, 2, 11, 4); g.drawRect(2, 9, 11, 4); break;
            case SEARCH:
                g.drawOval(2, 2, 8, 8); g.drawLine(10, 10, 14, 14); break;
            case CLEAR: g.drawLine(4, 4, 12, 12); g.drawLine(4, 12, 12, 4); break;
        }
        g.dispose();
    }
}
