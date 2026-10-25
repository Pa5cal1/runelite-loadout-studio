/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankPlaceholderTest {
 @Test public void repeatedFoodCellsKeepRealStockAndNeverMultiplyOrInventIt(){
  int[] layout={391,391,391,391};int[] ids={391,391,385,-1};int[] amounts={1499,1499,5,999};Map<Integer,Integer> stock=BankViewPlan.stock(ids,amounts,id->id);
  assertEquals(Integer.valueOf(1499),stock.get(391));assertFalse(stock.containsKey(-1));assertNull(stock.get(999));int[] occupied=BankViewPlan.positions(new int[]{391},layout,id->id);assertArrayEquals(new int[]{-1,391,391,391},BankViewPlan.placeholders(layout,occupied));
 }
 @Test public void repeatedFoodQuantitiesArePaintedInsideEveryVisibleCopy(){
  BufferedImage target=new BufferedImage(240,75,BufferedImage.TYPE_INT_ARGB);Graphics2D g=target.createGraphics();java.util.List<Integer> ids=new ArrayList<>();
  BankPlaceholderOverlay.paint(g,new Rectangle(0,0,240,72),0,new int[]{391,391,-1,391},id->{ids.add(id);return null;},id->1499);assertEquals(Arrays.asList(391,391,391),ids);
  for(int x:new int[]{51,99,195}){boolean visible=false;for(int px=x;px<x+32;px++)for(int y=0;y<12;y++)visible|=(target.getRGB(px,y)>>>24)>0;assertTrue("Stock label at "+x,visible);}g.dispose();
 }
 @Test public void missingItemsAndRepeatedFoodRemainVisibleWithoutFillingLayoutHoles(){
  int[] layout={10,-1,20,20,30},bank={20};int[] positions=BankViewPlan.positions(bank,layout,id->id);
  assertArrayEquals(new int[]{10,-1,-1,20,30},BankViewPlan.placeholders(layout,positions));
  assertArrayEquals(new int[]{10,-1,20,20,30},layout);assertArrayEquals(new int[]{20},bank);
 }
 @Test public void realBankPlaceholderAndFuzzyCounterpartOccupyTheirRequestedCell(){
  int[] layout={10,20,10};int[] positions=BankViewPlan.positions(new int[]{501,21,99},layout,id->id==501?10:id==21?20:id);
  assertArrayEquals(new int[]{-1,-1,10},BankViewPlan.placeholders(layout,positions));
  assertArrayEquals(new int[]{0,1,3},positions);
 }
 private static BufferedImage sprite(){BufferedImage b=new BufferedImage(36,32,BufferedImage.TYPE_INT_ARGB);Graphics2D g=b.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,36,32);g.dispose();return b;}
 @Test public void visualPlaceholdersAreDimmedClippedAndLeaveTheGraphicsStateUntouched(){
  BufferedImage target=new BufferedImage(200,120,BufferedImage.TYPE_INT_ARGB);Graphics2D g=target.createGraphics();Shape clip=new Rectangle(0,0,180,110);g.setClip(clip);Composite composite=g.getComposite();java.util.List<Integer> requested=new ArrayList<>();
  BankPlaceholderOverlay.paint(g,new Rectangle(10,20,100,50),0,new int[]{10,20,-1,30},id->{requested.add(id);return sprite();});
  assertEquals(Arrays.asList(10,20),requested);assertEquals(clip.getBounds(),g.getClipBounds());assertEquals(composite,g.getComposite());assertEquals(135,(target.getRGB(61,20)>>>24));assertEquals(0,target.getRGB(110,20));assertEquals(0,target.getRGB(61,19));g.dispose();
 }
 @Test public void scrolledOffscreenCellsDoNotRequestSpritesAndScrollIsAppliedToVisibleRows(){
  int[] cells=new int[24];Arrays.fill(cells,-1);cells[0]=10;cells[8]=20;cells[16]=30;java.util.List<Integer> requested=new ArrayList<>();BufferedImage target=new BufferedImage(160,80,BufferedImage.TYPE_INT_ARGB);Graphics2D g=target.createGraphics();
  BankPlaceholderOverlay.paint(g,new Rectangle(0,10,120,36),36,cells,id->{requested.add(id);return sprite();});assertEquals(Collections.singletonList(20),requested);assertEquals(135,target.getRGB(51,10)>>>24);assertEquals(0,target.getRGB(51,46));g.dispose();
 }
}
