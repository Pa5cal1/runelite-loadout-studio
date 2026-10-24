/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class StylingConsistencyTest
{
    private static JButton slot(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JButton && "itemSlot".equals(child.getName())) return (JButton) child;
            if (child instanceof Container) { JButton found = slot((Container) child); if (found != null) return found; }
        }
        return null;
    }
    private static void checkMenu(JPopupMenu menu)
    {
        for (Component child : menu.getComponents())
        {
            assertEquals(StudioTheme.BODY, child.getFont());
            assertEquals(StudioTheme.CONTROL, child.getBackground());
            if (child instanceof JMenu) checkMenu(((JMenu) child).getPopupMenu());
        }
    }
    @Test public void slotSubmenusAndLibraryMenusShareTheSidebarTheme() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SetupLibrary library = new SetupLibrary();
            SetupLibrary.Entry saved = new SetupLibrary.Entry(SetupCodec.blank("Saved setup")); library.setups.add(saved);
            PanelTest.FakeHost host = new PanelTest.FakeHost() {
                @Override public String read(String scope) { return library.serialize(RuneLiteAPI.GSON); }
            };
            StudioPanel panel = new StudioPanel(host, RuneLiteAPI.GSON);
            panel.newDraft(SetupCodec.blank("Style check"));
            JPopupMenu menu = slot(panel).getComponentPopupMenu(); assertNotNull(menu); checkMenu(menu);
            JButton title = (JButton) OwnedGearPanelTest.find(panel,"libraryOpen:" + saved.id);
            checkMenu(title.getComponentPopupMenu());
            assertEquals(2,title.getComponentPopupMenu().getComponentCount());
            menu.setSize(menu.getPreferredSize()); menu.doLayout();
            try {
                File dir = new File("build/previews/styling"); dir.mkdirs();
                BufferedImage image = new BufferedImage(menu.getWidth(),menu.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();menu.printAll(graphics);graphics.dispose();ImageIO.write(image,"png",new File(dir,"slot-menu.png"));
            } catch (java.io.IOException ex) { throw new AssertionError(ex); }
            panel.dispose();
        });
    }
    @Test public void nativeRuneLiteThemeKeepsNarrowLayoutsUsable() throws Exception
    {
        LookAndFeel previous=UIManager.getLookAndFeel();
        try {
            SwingUtilities.invokeAndWait(()->assertTrue(net.runelite.client.ui.laf.RuneLiteLAF.setup()));
            new SidebarLayoutTest().navigationStaysFixedAndSaveRemainsOutsideTheScrollArea();
            slotSubmenusAndLibraryMenusShareTheSidebarTheme();
        } finally {SwingUtilities.invokeAndWait(()->{try{UIManager.setLookAndFeel(previous);}catch(UnsupportedLookAndFeelException ex){throw new AssertionError(ex);}});}
    }
}
