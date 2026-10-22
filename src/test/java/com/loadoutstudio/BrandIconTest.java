package com.loadoutstudio;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;
public class BrandIconTest
{
    @Test public void rendersDistinctiveIconAtSidebarSizes() throws Exception
    {
        BufferedImage icon=StudioBrandIcon.create(); assertEquals(32,icon.getWidth()); assertEquals(32,icon.getHeight());
        BufferedImage preview=new BufferedImage(180,70,BufferedImage.TYPE_INT_RGB); Graphics2D g=preview.createGraphics(); g.setColor(new Color(37,37,37)); g.fillRect(0,0,180,70); int x=12;
        for(int size:new int[]{16,24,32}) { g.drawImage(icon,x,8,size,size,null); g.setColor(Color.WHITE); g.drawString(size+" px",x,60); x+=55; } g.dispose();
        File dir=new File("build/previews/optimized-studio"); dir.mkdirs(); ImageIO.write(preview,"png",new File(dir,"sidebar-icon.png"));
    }
}
