/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.*;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
/** Native RuneLite neutrals with restrained gold accents; applied to this sidebar only. */
final class StudioTheme {
    static final Color BACKGROUND=ColorScheme.DARK_GRAY_COLOR,SURFACE=ColorScheme.DARK_GRAY_HOVER_COLOR,CONTROL=ColorScheme.CONTROL_COLOR,BORDER=ColorScheme.MEDIUM_GRAY_COLOR,TEXT=ColorScheme.TEXT_COLOR,MUTED=ColorScheme.LIGHT_GRAY_COLOR,ACCENT=new Color(232,188,99),SELECTED=ColorScheme.DARKER_GRAY_HOVER_COLOR,FAVORITE=new Color(247,207,117),ERROR=new Color(230,139,134);
    static final Font BODY=new Font(Font.SANS_SERIF,Font.PLAIN,12),ACTION=new Font(Font.SANS_SERIF,Font.BOLD,12),SMALL=new Font(Font.SANS_SERIF,Font.PLAIN,11),SECTION=new Font(Font.SANS_SERIF,Font.BOLD,11),CODE=new Font(Font.MONOSPACED,Font.PLAIN,11);
    private StudioTheme(){}
    static JToolTip tooltip(JToolTip tip){tip.setFont(SMALL);tip.setBackground(SURFACE);tip.setForeground(TEXT);tip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),BorderFactory.createEmptyBorder(5,7,5,7)));return tip;}
    static Icon check(boolean selected){return new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(selected?SELECTED:CONTROL);g.fillRoundRect(1,1,13,13,3,3);g.setColor(!c.isEnabled()?MUTED:selected?ACCENT:BORDER);g.drawRoundRect(1,1,13,13,3,3);if(selected){g.setStroke(new BasicStroke(1.6f));g.drawLine(4,7,7,10);g.drawLine(7,10,12,4);}g.dispose();}};}
    static Icon route(int kind){return new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(c.getForeground());g.setStroke(new BasicStroke(1.3f));
        if(kind==0){for(int i=0;i<3;i++){g.drawRoundRect(2,2+i*5,12,3,1,1);}}
        else if(kind==1){for(int row=0;row<2;row++)for(int col=0;col<2;col++)g.drawRect(2+col*7,2+row*7,5,5);}
        else if(kind==2){g.drawOval(1,1,14,14);g.drawPolygon(new int[]{5,11,9},new int[]{11,5,10},3);}
        else{g.drawLine(2,9,2,14);g.drawLine(2,14,13,14);g.drawLine(13,14,13,9);g.drawLine(8,11,8,2);g.drawLine(4,6,8,2);g.drawLine(8,2,12,6);}g.dispose();}};}
}
