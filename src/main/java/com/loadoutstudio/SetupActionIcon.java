/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.Icon;
final class SetupActionIcon implements Icon {
    private final int kind;private final Color color;
    SetupActionIcon(int kind,Color color){this.kind=kind;this.color=color;}
    public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
    public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(c.getForeground());
        if(kind==0){g.drawPolygon(new int[]{1,15,9,9,6,6},new int[]{2,2,8,14,12,8},6);}
        else if(kind==1){for(int i=0;i<3;i++){g.drawLine(6,3+i*5,14-i*2,3+i*5);g.drawLine(1,2+i*5,3,4+i*5);g.drawLine(3,4+i*5,5,1+i*5);}}
        else if(kind==2){g.fillPolygon(new int[]{3,6,14,11},new int[]{12,15,5,2},4);g.drawLine(1,15,6,15);}
        else if(kind==3){g.setColor(color);g.fillRect(1,12,14,3);g.setColor(c.getForeground());g.drawPolygon(new int[]{3,9,13,7},new int[]{5,1,7,11},4);g.drawLine(9,1,5,0);}
        else{Path2D star=new Path2D.Double();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5,d=i%2==0?7:3;double px=8+Math.cos(a)*d,py=8+Math.sin(a)*d;if(i==0)star.moveTo(px,py);else star.lineTo(px,py);}star.closePath();g.draw(star);}g.dispose();}
}
