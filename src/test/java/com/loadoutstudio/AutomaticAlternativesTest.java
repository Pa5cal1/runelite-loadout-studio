/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import net.runelite.http.api.RuneLiteAPI;
import static org.junit.Assert.*;
public class AutomaticAlternativesTest {
 @Test public void openingSetupFindsLocalOptionsWithoutAButtonAndPreservesNotes() throws Exception {
  CompletableFuture<Void> observed=new CompletableFuture<>();StudioPanel[] panel=new StudioPanel[1];JTextArea[] notes=new JTextArea[1];
  OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host(){@Override public void availableGear(JsonObject expected,java.util.function.Consumer<OwnedGear> success,java.util.function.Consumer<String> error){super.availableGear(expected,success,error);observed.complete(null);}};
  SwingUtilities.invokeAndWait(()->{panel[0]=new StudioPanel(host,RuneLiteAPI.GSON);panel[0].open(new SetupLibrary.Entry(OwnedGearTest.setup()));notes[0]=(JTextArea)OwnedGearPanelTest.find(panel[0],"loadoutNotes");notes[0].setText("Keep my notes");notes[0].setCaretPosition(4);});
  observed.get(3,TimeUnit.SECONDS);SwingUtilities.invokeAndWait(()->{assertNull(OwnedGearPanelTest.find(panel[0],"Check my Bank"));assertNotNull(OwnedGearPanelTest.find(panel[0],"ownedChoice:eq:4"));assertEquals("Keep my notes",notes[0].getText());assertEquals(4,notes[0].getCaretPosition());assertSame(notes[0],OwnedGearPanelTest.find(panel[0],"loadoutNotes"));panel[0].dispose();});
 }
 @Test public void explicitDoseIsPreservedAndMissingDoseUsesFullPotion(){ItemCatalogue c=new ItemCatalogue();c.add(1,"Guthix rest(1)",false,-1,-1);c.add(2,"Guthix rest(4)",false,-1,-1);assertEquals(2,c.firstExact("Guthix rest").id);assertEquals(1,c.firstExact("Guthix rest(1)").id);}
 @Test public void noArbitrarySameSlotAlternativesAreInvented(){JsonObject p=SetupCodec.blank("Void");SetupCodec.setup(p).getAsJsonArray("eq").set(0,SetupCodec.item(1,1));Map<Integer,StudioPanel.ItemInfo> info=new LinkedHashMap<>();info.put(1,new StudioPanel.ItemInfo(1,"Void ranger helm",false,0,null));info.put(2,new StudioPanel.ItemInfo(2,"Torva full helm",false,0,null));assertTrue(new OwnedGear(Collections.singletonMap(2,1),info,1,true).alternatives(p,n->null).isEmpty());}
 @Test public void fuzzyBankLayoutMapsActualVariantsBackToTheSavedItem(){BankFilterTest.FakeBank bank=new BankFilterTest.FakeBank();bank.open=true;JsonObject p=SetupCodec.blank("Fuzzy");JsonObject item=SetupCodec.item(100,1);item.addProperty("f",true);SetupCodec.setup(p).getAsJsonArray("inv").set(0,item);SetupCodec.rebuildLayout(p);StudioBankFilter filter=new StudioBankFilter(bank,"studio");filter.select(p);assertEquals(100,filter.layoutItemId(150));bank.layout=new int[]{-1,150};JsonArray moved=filter.readChangedLayout();assertNotNull(moved);assertEquals(100,moved.get(1).getAsInt());}
 @Test public void dragPreviewUsesTheItemImage(){javax.swing.TransferHandler handler=new javax.swing.TransferHandler("text");java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(3,3,0xFFFF0000);JButton source=new JButton();ItemDragImage.prepare(handler,source,new ImageIcon(image));assertNotNull(handler.getDragImage());java.awt.image.BufferedImage drag=(java.awt.image.BufferedImage)handler.getDragImage();assertEquals(0xFFFF0000,drag.getRGB(5,5));ItemDragImage.finish(handler);}
}
