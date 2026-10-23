/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.Dimension;
import javax.swing.JTextArea;

/** Read-only inline feedback follows its wrapped content instead of reserving empty rows. */
final class CompactStatusArea extends JTextArea
{
    CompactStatusArea(String text)
    {
        super(text); setEditable(false); setLineWrap(true); setWrapStyleWord(true); setSize(176, 40);
    }
    @Override public Dimension getPreferredSize()
    {
        Dimension size=super.getPreferredSize();return new Dimension(176, Math.max(18, size.height));
    }
    @Override public Dimension getMaximumSize() { return new Dimension(500,getPreferredSize().height); }
}
