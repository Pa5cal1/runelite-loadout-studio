package com.loadoutstudio;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;
public class BrandIconTest
{
    @Test public void nativeSidebarIconUsesTheFullRuneLiteTabSize(){
        BufferedImage icon=StudioBrandIcon.navigation();assertEquals(16,icon.getWidth());assertEquals(16,icon.getHeight());
        int minX=16,minY=16,maxX=-1,maxY=-1;for(int y=0;y<16;y++)for(int x=0;x<16;x++)if((icon.getRGB(x,y)>>>24)>0){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        assertEquals(0,minX);assertEquals(0,minY);assertEquals(15,maxX);assertEquals(15,maxY);
    }
    @Test public void rendersDistinctiveIconAtSidebarSizes() throws Exception
    {
        BufferedImage icon=StudioBrandIcon.create(); assertEquals(32,icon.getWidth()); assertEquals(32,icon.getHeight());
        BufferedImage preview=new BufferedImage(180,70,BufferedImage.TYPE_INT_RGB); Graphics2D g=preview.createGraphics(); g.setColor(new Color(37,37,37)); g.fillRect(0,0,180,70); int x=12;
        for(int size:new int[]{16,24,32}) { g.drawImage(icon,x,8,size,size,null); g.setColor(Color.WHITE); g.drawString(size+" px",x,60); x+=55; } g.dispose();
        File dir=new File("build/previews/optimized-studio"); dir.mkdirs(); ImageIO.write(preview,"png",new File(dir,"sidebar-icon.png"));
    }
}
