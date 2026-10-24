/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/** A single compact row, with space reserved for all actions at every width. */
final class LibrarySetupRow extends JPanel
{
    LibrarySetupRow(String id, String title, boolean favorite, boolean archived, Runnable open, Runnable toggleFavorite, Runnable toggleArchive, Runnable delete)
    {
        super(new BorderLayout(2, 0));
        setName("libraryRow:" + id);
        setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 0));
        setPreferredSize(new Dimension(200, 32));
        setMinimumSize(new Dimension(0, 32));
        NameButton name = new NameButton(title);
        name.setName("libraryOpen:" + id);
        name.setToolTipText(title);
        name.getAccessibleContext().setAccessibleName("Open " + title);
        name.addActionListener(e -> open.run());
        add(name, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, archived ? 3 : 2, 2, 0));
        actions.setPreferredSize(new Dimension(archived ? 88 : 58, 30));
        actions.add(action("libraryFavorite:" + id, favorite ? "Remove favorite" : "Add favorite", title, icon(0, favorite), toggleFavorite));
        actions.add(action("libraryArchive:" + id, archived ? "Restore setup" : "Move setup to archive", title, icon(archived ? 2 : 1, false), toggleArchive));
        if (archived) actions.add(action("libraryDelete:" + id, "Delete permanently", title, icon(3, false), delete));
        add(actions, BorderLayout.EAST);
    }

    private static JButton action(String name, String label, String title, Icon icon, Runnable run)
    {
        JButton button = new JButton(icon);
        button.setName(name); button.setToolTipText(label + ": " + title);
        button.getAccessibleContext().setAccessibleName(label + ": " + title);
        button.setMargin(new Insets(0, 0, 0, 0)); button.setPreferredSize(new Dimension(28, 30));
        button.addActionListener(e -> run.run()); return button;
    }

    private static final class NameButton extends JButton
    {
        private final String title;
        NameButton(String title)
        {
            super(title); this.title = title;
            putClientProperty("html.disable", true);
            setHorizontalAlignment(LEFT); setMargin(new Insets(2, 5, 2, 3));
            setPreferredSize(new Dimension(120, 30)); setMinimumSize(new Dimension(0, 30));
        }
        @Override public JToolTip createToolTip(){JToolTip tip=super.createToolTip();tip.putClientProperty("html.disable",true);return StudioTheme.tooltip(tip);}
        @Override public void setBounds(int x, int y, int width, int height)
        {
            super.setBounds(x, y, width, height);
            if (title == null) return;
            FontMetrics metrics = getFontMetrics(getFont()); Insets insets = getInsets();
            int available = Math.max(0, width - insets.left - insets.right - 4);
            String display = title;
            if (metrics.stringWidth(display) > available)
            {
                int end = title.length();
                while (end > 0 && metrics.stringWidth(title.substring(0, end) + "...") > available) end--;
                display = metrics.stringWidth("...") <= available ? title.substring(0, end) + "..." : "";
            }
            if (!display.equals(getText())) setText(display);
        }
    }

    private static Icon icon(int kind, boolean selected)
    {
        return new Icon()
        {
            public int getIconWidth() { return 16; }
            public int getIconHeight() { return 16; }
            public void paintIcon(Component c, Graphics graphics, int x, int y)
            {
                Graphics2D g = (Graphics2D)graphics.create(); g.translate(x, y);
                g.setColor(selected ? StudioTheme.FAVORITE : c.getForeground());
                if (kind == 0)
                {
                    Path2D star = new Path2D.Double();
                    for (int i = 0; i < 10; i++) { double angle = -Math.PI / 2 + i * Math.PI / 5, radius = i % 2 == 0 ? 7 : 3; double px = 8 + Math.cos(angle) * radius, py = 8 + Math.sin(angle) * radius; if (i == 0) star.moveTo(px, py); else star.lineTo(px, py); }
                    star.closePath(); if (selected) g.fill(star); else g.draw(star);
                }
                else if (kind == 1)
                {
                    g.drawRect(2, 5, 12, 9); g.drawRect(1, 2, 14, 3); g.drawRect(6, 8, 4, 2);
                }
                else if (kind == 3)
                {
                    g.setColor(StudioTheme.ERROR); g.drawLine(2, 4, 13, 4); g.drawRect(6, 1, 4, 2); g.drawRect(4, 5, 8, 9); g.drawLine(7, 7, 7, 12); g.drawLine(9, 7, 9, 12);
                }
                else
                {
                    g.drawArc(3, 4, 10, 10, -80, 265); g.drawLine(3, 4, 3, 9); g.drawLine(3, 4, 8, 4);
                }
                g.dispose();
            }
        };
    }
}
