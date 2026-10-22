/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class NativeEditorTest
{
    private static class Host extends PanelTest.FakeHost
    {
        Consumer<Integer> choose; Consumer<String> amount; int searches,prompts; String initial;
        @Override public void chooseItem(Consumer<Integer> callback,Consumer<String> error){choose=callback;searches++;}
        @Override public void enterQuantity(String initial,Consumer<String> callback,Consumer<String> error){amount=callback;this.initial=initial;prompts++;}
        @Override public void describe(int id,Consumer<StudioPanel.ItemInfo> callback){BufferedImage image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setColor(Color.GRAY);g.fillOval(3,3,26,26);g.dispose();callback.accept(new StudioPanel.ItemInfo(id,"Death rune",true,-1,new ImageIcon(image)));}
    }
    private static JButton slot(Container c,String key,int index){for(Component child:c.getComponents()){if(child instanceof JButton && key.equals(((JButton)child).getClientProperty("slotContainer")) && Integer.valueOf(index).equals(((JButton)child).getClientProperty("slotIndex")))return (JButton)child;if(child instanceof Container){JButton result=slot((Container)child,key,index);if(result!=null)return result;}}return null;}
    private static Component find(Container c,String name){for(Component child:c.getComponents()){if(name.equals(child.getName()))return child;if(child instanceof Container){Component result=find((Container)child,name);if(result!=null)return result;}}return null;}
    private static JMenuItem menu(JButton slot,String text){for(Component c:slot.getComponentPopupMenu().getComponents())if(c instanceof JMenuItem && text.equals(((JMenuItem)c).getText()))return (JMenuItem)c;throw new AssertionError(text);}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    @Test public void contextMenuUsesChatboxSearchAndQuantityThenExportsUnlimited() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.newDraft(SetupCodec.blank("Native editor"));
            menu(slot(p,"rp",0),"Update Slot from Search").doClick();assertEquals(1,host.searches);host.choose.accept(560);assertEquals(1,host.prompts);host.amount.accept("0");
            JsonElement rune=SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0);assertTrue(SetupCodec.unlimited(rune));assertEquals("None",rune.getAsJsonObject().get("sc").getAsString());
            JsonObject exported=RuneLiteAPI.GSON.fromJson(new SetupCodec(RuneLiteAPI.GSON).inventory(host.selectedBankSetup),JsonObject.class);assertEquals(0,SetupCodec.quantity(SetupCodec.setup(exported).getAsJsonArray("rp").get(0)));
            assertFalse(menu(slot(p,"rp",0),"Stack comparison").isEnabled());menu(slot(p,"rp",0),"Toggle Fuzzy").doClick();assertTrue(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0).getAsJsonObject().get("f").getAsBoolean());
            menu(slot(p,"rp",0),"Set quantity").doClick();host.amount.accept("16001");assertTrue(SetupCodec.unlimited(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));
            menu(slot(p,"rp",0),"Set quantity").doClick();host.amount.accept("16k");assertEquals(16000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));
            menu(slot(p,"rp",0),"Remove Item from Slot").doClick();assertTrue(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0).isJsonNull());p.dispose();
        });
    }
    @Test public void chatboxReplyCannotOverwriteAChangedDraft() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.newDraft(SetupCodec.blank("Old"));slot(p,"inv",0).doClick();Consumer<Integer> old=host.choose;p.reloadScope();p.newDraft(SetupCodec.blank("New"));old.accept(560);assertEquals(0,host.prompts);assertTrue(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0).isJsonNull());p.dispose();});
    }
    @Test public void notesHaveOnlyOuterScrollingAndGrowWithText() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.newDraft(SetupCodec.blank("Notes"));p.setSize(220,1200);layout(p);layout(p);JTextArea notes=(JTextArea)find(p,"loadoutNotes");int scrolls=0;for(Container c=notes.getParent();c!=null;c=c.getParent())if(c instanceof JScrollPane)scrolls++;assertEquals(1,scrolls);int height=notes.getPreferredSize().height;assertTrue(height>=200);notes.setText("A long setup note with instructions. ".repeat(80));layout(p);layout(p);assertTrue(notes.getPreferredSize().height>height);assertTrue(notes.getParent().getHeight()>=notes.getPreferredSize().height);p.dispose();});
    }
    @Test public void runeCountOverlayStaysInsideTheSlot() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{try{Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject data=SetupCodec.blank("Rune counts");for(int i=0;i<4;i++)SetupCodec.setup(data).getAsJsonArray("rp").set(i,SetupCodec.item(560,i==3?0:16000));p.newDraft(data);
            BufferedImage strip=new BufferedImage(176,44,BufferedImage.TYPE_INT_RGB);Graphics2D out=strip.createGraphics();for(int i=0;i<4;i++){JButton cell=slot(p,"rp",i);cell.setSize(44,44);BufferedImage image=new BufferedImage(44,44,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();cell.paint(g);g.dispose();int yellow=0;for(int y=0;y<16;y++)for(int x=0;x<44;x++){Color pixel=new Color(image.getRGB(x,y));if(pixel.getRed()>200&&pixel.getGreen()>180&&pixel.getBlue()<100)yellow++;}assertTrue("Count is visible inside the top of each cell",yellow>2);out.drawImage(image,i*44,0,null);}out.dispose();File dir=new File("build/previews/native-editor");dir.mkdirs();ImageIO.write(strip,"png",new File(dir,"rune-counts.png"));p.dispose();}catch(Exception ex){throw new AssertionError(ex);}});
    }
    @Test public void inventoryRunesHaveNoPouchCapAndCanIgnoreQuantity() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel p=new StudioPanel(host,RuneLiteAPI.GSON);p.newDraft(SetupCodec.blank("Inventory runes"));slot(p,"inv",0).doClick();host.choose.accept(560);host.amount.accept("200k");assertEquals(200000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            menu(slot(p,"inv",0),"Set quantity").doClick();host.amount.accept("0");assertTrue(SetupCodec.unlimited(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));assertNotNull(new SetupCodec(RuneLiteAPI.GSON).inventory(host.selectedBankSetup));p.dispose();});
    }

    @Test public void unknownQuantityDoesNotDefaultToUnlimited() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject draft=SetupCodec.blank("Unknown runes");JsonObject rune=SetupCodec.item(560,1);rune.add("q",JsonNull.INSTANCE);SetupCodec.setup(draft).getAsJsonArray("rp").set(0,rune);SetupCodec.rebuildLayout(draft);panel.newDraft(draft);menu(slot(panel,"rp",0),"Set quantity").doClick();assertEquals("",host.initial);host.amount.accept("15000");assertEquals(15000,SetupCodec.quantity(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("rp").get(0)));panel.dispose();});
    }

}
