/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class GearDifferenceTooltipTest {
    private static JsonObject setup(){JsonObject p=SetupCodec.blank("Tooltip test");JsonObject s=SetupCodec.setup(p);for(int i=0;i<28;i++)s.getAsJsonArray("inv").set(i,SetupCodec.item(560,1));for(int i:new int[]{0,1,2,3,4,5,7,9,10,12,13})s.getAsJsonArray("eq").set(i,SetupCodec.item(100,1));for(int i=0;i<3;i++)s.getAsJsonArray("rp").set(i,SetupCodec.item(560,16000));s.getAsJsonArray("inv").get(0).getAsJsonObject().addProperty("label","<html>"+"UntrustedLongName".repeat(20));SetupCodec.rebuildLayout(p);return p;}
    @Test public void fortyTwoDifferencesRenderAsABoundedGroupedTooltip()throws Exception{SwingUtilities.invokeAndWait(()->{try{
        StudioPanel p=new StudioPanel(new PanelTest.FakeHost(),RuneLiteAPI.GSON);p.newDraft(setup());p.acceptLiveGear(SetupCodec.blank("Live"));JLabel label=(JLabel)OwnedGearPanelTest.find(p,"gearReadiness");assertEquals("42 gear difference(s)",label.getText());String text=label.getToolTipText();assertTrue(text.contains("Equipment: 11"));assertTrue(text.contains("Inventory: 28"));assertTrue(text.contains("Rune Pouch: 3"));assertFalse(text.contains("UntrustedLongName"));assertFalse(text.contains("inv:"));
        JToolTip tip=label.createToolTip();tip.setTipText(text);Dimension size=tip.getPreferredSize();assertTrue("Tooltip width "+size.width,size.width<=280);assertTrue("Tooltip height "+size.height,size.height<=230);tip.setSize(size);BufferedImage image=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();tip.printAll(g);g.dispose();File dir=new File("build/previews/gear-tooltip");dir.mkdirs();ImageIO.write(image,"png",new File(dir,"summary.png"));p.dispose();
    }catch(Exception ex){throw new AssertionError(ex);}});}
    @Test public void explicitCompareRefreshesTheTooltipInsteadOfKeepingOldDifferences()throws Exception{SwingUtilities.invokeAndWait(()->{
        PanelTest.FakeHost host=new PanelTest.FakeHost(){@Override public void compareSetup(JsonObject expected,boolean ignore,java.util.function.Consumer<Set<String>> success,java.util.function.Consumer<String> error){success.accept(Collections.emptySet());}};StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.newDraft(setup());p.acceptLiveGear(SetupCodec.blank("Live"));OwnedGearPanelTest.click(p,"quickCompareGear");JLabel label=(JLabel)OwnedGearPanelTest.find(p,"gearReadiness");assertEquals("Gear matches setup",label.getText());assertFalse(label.getToolTipText().contains("Inventory: 28"));assertTrue(label.getToolTipText().contains("Compared"));p.dispose();
    });}
}
