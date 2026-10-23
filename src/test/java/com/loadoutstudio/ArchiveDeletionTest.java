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

public class ArchiveDeletionTest
{
    private static class Host extends PanelTest.FakeHost
    {
        boolean fail;
        int writes;
        @Override public String read(String scope) { return saved; }
        @Override public void save(String scope,String text) { if(fail)throw new IllegalStateException("Storage unavailable");writes++;super.save(scope,text); }
    }
    private static Component find(Container c,String name) { for(Component child:c.getComponents()){if(name.equals(child.getName())||child instanceof AbstractButton&&name.equals(((AbstractButton)child).getText()))return child;if(child instanceof Container){Component found=find((Container)child,name);if(found!=null)return found;}}return null; }
    private static void layout(Container c) { c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child); }
    private static SetupLibrary library()
    {
        SetupLibrary l=new SetupLibrary();SetupLibrary.Section section=new SetupLibrary.Section("PvM");l.sections.add(section);
        SetupLibrary.Entry entry=new SetupLibrary.Entry(SetupCodec.blank("The Fractured Archive - Ranged setup"));entry.id="archived";entry.archived=true;entry.favorite=true;entry.section=section.id;l.setups.add(entry);
        SetupLibrary.Entry active=new SetupLibrary.Entry(SetupCodec.blank("Vorkath"));active.id="active";l.setups.add(active);return l;
    }
    @Test public void deletionRequiresArchivedEntryAndConfirmationAndKeepsOtherData() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();host.saved=library().serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);
            assertFalse(panel.deleteArchived("active",true));assertFalse(panel.deleteArchived("archived",false));assertFalse(panel.deleteArchived("missing",true));assertEquals(0,host.writes);
            assertTrue(panel.deleteArchived("archived",true));SetupLibrary saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON);assertNull(saved.find("archived"));assertNotNull(saved.find("active"));assertEquals(1,saved.sections.size());assertFalse(panel.deleteArchived("archived",true));panel.dispose();
        });
    }
    @Test public void failedSaveDoesNotRemoveTheEntry() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();host.saved=library().serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);host.fail=true;
            try { panel.deleteArchived("archived",true);fail(); } catch(IllegalStateException expected) { }
            assertNotNull(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).find("archived"));host.fail=false;assertTrue(panel.deleteArchived("archived",true));panel.dispose();
        });
    }
    @Test public void deletingOpenArchivedEntryClosesOnlyItsDraftAndBankFilter() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();SetupLibrary l=library();host.saved=l.serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(l.find("archived"));int clearedBefore=host.clears;
            ((JTextArea)find(panel,"loadoutNotes")).setText("Unsaved change");assertNotNull(host.selectedBankSetup);assertTrue(panel.deleteArchived("archived",true));assertNull(host.selectedBankSetup);assertEquals(clearedBefore+1,host.clears);assertNotNull(find(panel,"New setup"));assertNull(find(panel,"libraryOpen:archived"));panel.dispose();
        });
    }
    @Test public void archivedRowsHaveSeparateRestoreAndTrashActionsAtNarrowWidths() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{try{
            for(int width:new int[]{220,260}) {
                Host host=new Host();host.saved=library().serialize(RuneLiteAPI.GSON);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.setSize(width,900);
                assertNotNull(find(panel,"libraryArchive:active"));assertNull(find(panel,"libraryDelete:active"));((JCheckBox)find(panel,"Archived")).doClick();layout(panel);layout(panel);
                JButton restore=(JButton)find(panel,"libraryArchive:archived"),trash=(JButton)find(panel,"libraryDelete:archived"),title=(JButton)find(panel,"libraryOpen:archived");
                assertTrue(restore.getToolTipText().startsWith("Restore setup"));assertTrue(trash.getToolTipText().startsWith("Delete permanently"));assertTrue(title.getText().endsWith("..."));
                assertEquals(restore.getY(),trash.getY());assertTrue(trash.getX()>=restore.getX()+restore.getWidth());assertTrue(trash.getWidth()>=26);assertTrue(trash.getParent().getWidth()>=trash.getX()+trash.getWidth());
                BufferedImage image=new BufferedImage(width,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();File dir=new File("build/previews/archive-actions");dir.mkdirs();ImageIO.write(image,"png",new File(dir,width+"-archived.png"));panel.dispose();
            }
        }catch(Exception e){throw new AssertionError(e);}});
    }
}
