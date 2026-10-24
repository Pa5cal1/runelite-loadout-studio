/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class OptimizationLayoutTest
{
    static Component find(Container c,String key){return RecoveryAndImportPanelTest.find(c,key);}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    static void preview(StudioPanel panel,String name) throws Exception{layout(panel);layout(panel);BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();File folder=new File("build/previews/persistence-import");folder.mkdirs();ImageIO.write(image,"png",new File(folder,name+".png"));}
    @Test public void importReviewFitsNarrowSidebarAndRendersNamesAsPlainText() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{try{for(int width:new int[]{220,260})
        {
            PanelTest.FakeHost host=new PanelTest.FakeHost(){public void existingSetups(java.util.function.Consumer<ExistingSetupReader.Result> callback,java.util.function.Consumer<String> error){ExistingSetupReader.Result result=new ExistingSetupReader.Result();result.entries.add(new ExistingSetupReader.Entry("Inventory Setups","PvM",SetupCodec.blank("<html><b>Example</b>"),true));JsonObject bank=SetupCodec.blank("Vorkath supplies");SetupCodec.setup(bank).getAsJsonArray("inv").set(0,SetupCodec.item(100,1));result.entries.add(new ExistingSetupReader.Entry("Bank Tag",null,bank,false));callback.accept(result);}};
            StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.setSize(width,1000);((JButton)find(panel,"libraryTool:Import setup")).doClick();((JButton)find(panel,"From installed plugins")).doClick();JList<?> choices=(JList<?>)find(panel,"existingSetups");assertEquals(2,choices.getModel().getSize());choices.setSelectedIndex(1);assertTrue(((JTextArea)find(panel,"existingReview")).getText().contains("unknown"));
            @SuppressWarnings("unchecked") javax.swing.ListCellRenderer<Object> renderer=(javax.swing.ListCellRenderer<Object>)choices.getCellRenderer();JLabel label=(JLabel)renderer.getListCellRendererComponent((JList)choices,choices.getModel().getElementAt(0),0,false,false);assertEquals(Boolean.TRUE,label.getClientProperty("html.disable"));assertNull(label.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey));
            LibrarySetupRow row=new LibrarySetupRow("safety","<html><b>Example</b>",false,false,()->{},()->{},()->{},()->{});JButton name=(JButton)find(row,"libraryOpen:safety");JToolTip tip=name.createToolTip();tip.setTipText(name.getToolTipText());assertEquals(Boolean.TRUE,tip.getClientProperty("html.disable"));assertNull(tip.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey));
            preview(panel,width+"-import");assertTrue(find(panel,"Save copies").getWidth()>=70);assertTrue(find(panel,"Open copy").getWidth()>=70);for(String action:new String[]{"Save copies","Open copy"}){JButton control=(JButton)find(panel,action);assertTrue("Action label must fit: "+action,control.getFontMetrics(control.getFont()).stringWidth(action)<=control.getWidth()-control.getInsets().left-control.getInsets().right);}panel.dispose();
        }}catch(Exception e){throw new AssertionError(e);}});
    }
    @Test public void wikiShowsCachedDateWithoutChangingSourceName() throws Exception
    {
        CompletableFuture<Void> loaded=new CompletableFuture<>();StudioPanel[] panel={null};
        SwingUtilities.invokeAndWait(()->{PanelTest.FakeHost host=new PanelTest.FakeHost(){public void fetchWikiPage(String url,boolean strategies,boolean refresh,java.util.function.BiConsumer<WikiClient.Page,String> callback){callback.accept(new WikiClient.Page("Vorkath/Strategies","{{Inventory|1=Anglerfish}}",System.currentTimeMillis()-1000,true,false),null);loaded.complete(null);}};panel[0]=new StudioPanel(host,RuneLiteAPI.GSON);panel[0].setSize(220,1000);((JButton)find(panel[0],"Wiki")).doClick();JTextField query=(JTextField)find(panel[0],"wikiSearch");query.setText("Vorkath");query.postActionEvent();((JList<?>)find(panel[0],"wikiSuggestions")).setSelectedIndex(0);});
        loaded.get(3,TimeUnit.SECONDS);SwingUtilities.invokeAndWait(()->{try{assertTrue(((JTextArea)find(panel[0],"wikiPageStatus")).getText().contains("Cached source"));preview(panel[0],"220-wiki-cache");for(String action:new String[]{"Refresh Wiki"}){JButton control=(JButton)find(panel[0],action);assertTrue("Wiki action must fit: "+action,control.getFontMetrics(control.getFont()).stringWidth(action)<=control.getWidth()-control.getInsets().left-control.getInsets().right);}panel[0].dispose();}catch(Exception e){throw new AssertionError(e);}});
    }
}
