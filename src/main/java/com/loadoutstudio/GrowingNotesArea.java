/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.Dimension;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
final class GrowingNotesArea extends JTextArea
{
    GrowingNotesArea() { super(10,0); setLineWrap(true); setWrapStyleWord(true); setSize(176,200); ((DefaultCaret)getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE); }
    @Override public Dimension getPreferredSize() { Dimension size=super.getPreferredSize(); return new Dimension(176,Math.max(200,size.height)); }
}
