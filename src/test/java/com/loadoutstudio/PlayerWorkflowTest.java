/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonObject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerWorkflowTest
{
    private static class Host extends PanelTest.FakeHost
    {
        Consumer<JsonObject> pendingCapture;
        int comparisons, requests;
        @Override public String read(String scope) { return saved; }
        @Override public void capture(Consumer<JsonObject> success, Consumer<String> error) { pendingCapture=success; }
        @Override public void compareSetup(JsonObject p,boolean ignore,Consumer<java.util.Set<String>> success,Consumer<String> error) { comparisons++;success.accept(java.util.Collections.emptySet()); }
        @Override public void searchWiki(String query,java.util.function.BiConsumer<java.util.List<String>,String> callback) { requests++; }
    }
    private static Component find(Container root,String name)
    {
        for(Component child:root.getComponents()) {
            if(name.equals(child.getName()) || child instanceof AbstractButton && name.equals(((AbstractButton)child).getText()) || child instanceof JLabel && name.equals(((JLabel)child).getText()))return child;
            if(child instanceof Container){Component found=find((Container)child,name);if(found!=null)return found;}
        }return null;
    }
    private static void click(Container root,String name) { AbstractButton b=(AbstractButton)find(root,name);assertNotNull(name,b);b.doClick(); }
    private static void layout(Container root) { root.doLayout();for(Component child:root.getComponents())if(child instanceof Container)layout((Container)child); }
    private static SetupLibrary.Entry entry(String name) { return new SetupLibrary.Entry(SetupCodec.blank(name)); }

    @Test public void searchingAndFavoritesRevealCollapsedSectionsAndPreserveTheirState() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();SetupLibrary library=new SetupLibrary();SetupLibrary.Section section=new SetupLibrary.Section("PvM");library.sections.add(section);SetupLibrary.Entry saved=entry("Vorkath");saved.section=section.id;saved.favorite=true;library.setups.add(saved);host.saved=library.serialize(RuneLiteAPI.GSON);
            StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);click(panel,"- PvM (1)");assertNull(find(panel,"libraryOpen:"+saved.id));
            JTextField search=(JTextField)find(panel,"librarySearch");search.setText("vork");assertNotNull(find(panel,"libraryOpen:"+saved.id));assertFalse(((JButton)find(panel,"- PvM (1)")).isEnabled());
            search.setText("");assertNull(find(panel,"libraryOpen:"+saved.id));click(panel,"Favorites");assertNotNull(find(panel,"libraryOpen:"+saved.id));click(panel,"Favorites");assertNull(find(panel,"libraryOpen:"+saved.id));assertNotNull(find(panel,"+ PvM (1)"));panel.dispose();
        });
    }
    @Test public void emptyLibraryOffersDirectWikiRouteWithoutMakingARequestAndFiltersCanBeReset() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);click(panel,"Find Wiki content");assertEquals(0,host.requests);click(panel,"nav:Library");
            click(panel,"Favorites");((JTextField)find(panel,"librarySearch")).setText("missing");click(panel,"Archived");click(panel,"Clear filters");
            assertFalse(((JCheckBox)find(panel,"Favorites")).isSelected());assertFalse(((JCheckBox)find(panel,"Archived")).isSelected());assertEquals("",((JTextField)find(panel,"librarySearch")).getText());assertNotNull(find(panel,"Start a loadout"));panel.dispose();
        });
    }
    @Test public void frequentActionsStayAboveTheLoadoutAndFitNarrowSidebars() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{try{
            for(int width:new int[]{220,260}) {
                Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.setSize(width,900);panel.open(entry("Vorkath"));layout(panel);layout(panel);
                JButton filter=(JButton)find(panel,"setupToggle:filter"),compare=(JButton)find(panel,"quickCompareGear");assertNull(SwingUtilities.getAncestorOfClass(JViewport.class,filter));assertNull(SwingUtilities.getAncestorOfClass(JViewport.class,compare));
                assertTrue(filter.getWidth()>=26);assertTrue(compare.getWidth()>=80);assertTrue(compare.getX()+compare.getWidth()<=compare.getParent().getWidth());
                assertTrue("Compare label fits without truncation",compare.getFontMetrics(compare.getFont()).stringWidth(compare.getText())<=compare.getWidth()-compare.getInsets().left-compare.getInsets().right);
                click(panel,"quickCompareGear");assertEquals(1,host.comparisons);click(panel,"setupToggle:filter");assertFalse(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());assertEquals(Boolean.FALSE,((JButton)find(panel,"setupToggle:filter")).getClientProperty("selected"));
                click(panel,"setupToggle:filter");assertTrue(SetupCodec.setup(host.selectedBankSetup).get("fb").getAsBoolean());assertEquals(Boolean.TRUE,((JButton)find(panel,"setupToggle:filter")).getClientProperty("selected"));
                layout(panel);layout(panel);BufferedImage image=new BufferedImage(width,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();File dir=new File("build/previews/player-workflows");dir.mkdirs();ImageIO.write(image,"png",new File(dir,width+"-builder.png"));panel.dispose();
            }
        }catch(Exception e){throw new AssertionError(e);}});
    }
    @Test public void delayedCaptureCannotOverwriteEditsMadeWhileWaiting() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry("Vorkath"));click(panel,"libraryTool:Capture gear");assertNotNull(host.pendingCapture);
            ((JTextArea)find(panel,"loadoutNotes")).setText("Keep my new notes");host.pendingCapture.accept(SetupCodec.blank("Captured gear"));assertEquals("Keep my new notes",((JTextArea)find(panel,"loadoutNotes")).getText());click(panel,"Save");assertEquals("Vorkath",SetupCodec.name(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable));panel.dispose();
        });
    }
    @Test public void delayedCaptureCannotReplaceAnotherSetupButCurrentCaptureStillOpens() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry("Vorkath"));click(panel,"libraryTool:Capture gear");Consumer<JsonObject> old=host.pendingCapture;
            panel.open(entry("Zulrah"));old.accept(SetupCodec.blank("Old capture"));click(panel,"Save");assertEquals("Zulrah",SetupCodec.name(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable));
            click(panel,"libraryTool:Capture gear");host.pendingCapture.accept(SetupCodec.blank("Current gear"));click(panel,"Save");assertEquals(2,SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.size());panel.dispose();
        });
    }
}
