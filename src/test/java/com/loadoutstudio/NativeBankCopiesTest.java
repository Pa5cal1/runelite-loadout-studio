/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
/** Native-slot contract fixture; never attaches to or controls a game client. */
public class NativeBankCopiesTest {
 @Test public void hiddenSourceWithoutLabelsStillGetsTheExistingNativeWithdrawOperations(){String[] labels=BankViewPlan.bankActions(4,50,null);assertEquals("Withdraw-All",labels[0]);assertEquals("Withdraw-1",labels[1]);assertEquals("Withdraw-5",labels[2]);assertEquals("Withdraw-10",labels[3]);assertEquals("Withdraw-X",labels[5]);assertEquals("Examine",labels[10]);assertNull(labels[8]);assertNull(labels[9]);}
 static abstract class WidgetStub implements Widget {
  public int getId(){throw new UnsupportedOperationException("Unexpected Widget call: getId");}
  public int getType(){throw new UnsupportedOperationException("Unexpected Widget call: getType");}
  public void setType(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setType");}
  public int getContentType(){throw new UnsupportedOperationException("Unexpected Widget call: getContentType");}
  public net.runelite.api.widgets.Widget setContentType(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setContentType");}
  public int getClickMask(){throw new UnsupportedOperationException("Unexpected Widget call: getClickMask");}
  public net.runelite.api.widgets.Widget setClickMask(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setClickMask");}
  public net.runelite.api.widgets.Widget getParent(){throw new UnsupportedOperationException("Unexpected Widget call: getParent");}
  public int getParentId(){throw new UnsupportedOperationException("Unexpected Widget call: getParentId");}
  public net.runelite.api.widgets.Widget getChild(int a0){throw new UnsupportedOperationException("Unexpected Widget call: getChild");}
  public net.runelite.api.widgets.Widget[] getChildren(){throw new UnsupportedOperationException("Unexpected Widget call: getChildren");}
  public void setChildren(net.runelite.api.widgets.Widget[] a0){throw new UnsupportedOperationException("Unexpected Widget call: setChildren");}
  public net.runelite.api.widgets.Widget[] getDynamicChildren(){throw new UnsupportedOperationException("Unexpected Widget call: getDynamicChildren");}
  public net.runelite.api.widgets.Widget[] getStaticChildren(){throw new UnsupportedOperationException("Unexpected Widget call: getStaticChildren");}
  public net.runelite.api.widgets.Widget[] getNestedChildren(){throw new UnsupportedOperationException("Unexpected Widget call: getNestedChildren");}
  public int getRelativeX(){throw new UnsupportedOperationException("Unexpected Widget call: getRelativeX");}
  public void setRelativeX(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setRelativeX");}
  public int getRelativeY(){throw new UnsupportedOperationException("Unexpected Widget call: getRelativeY");}
  public void setRelativeY(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setRelativeY");}
  public void setForcedPosition(int a0,int a1){throw new UnsupportedOperationException("Unexpected Widget call: setForcedPosition");}
  public java.lang.String getText(){throw new UnsupportedOperationException("Unexpected Widget call: getText");}
  public net.runelite.api.widgets.Widget setText(java.lang.String a0){throw new UnsupportedOperationException("Unexpected Widget call: setText");}
  public int getTextColor(){throw new UnsupportedOperationException("Unexpected Widget call: getTextColor");}
  public net.runelite.api.widgets.Widget setTextColor(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setTextColor");}
  public int getOpacity(){throw new UnsupportedOperationException("Unexpected Widget call: getOpacity");}
  public net.runelite.api.widgets.Widget setOpacity(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setOpacity");}
  public java.lang.String getName(){throw new UnsupportedOperationException("Unexpected Widget call: getName");}
  public net.runelite.api.widgets.Widget setName(java.lang.String a0){throw new UnsupportedOperationException("Unexpected Widget call: setName");}
  public int getModelId(){throw new UnsupportedOperationException("Unexpected Widget call: getModelId");}
  public net.runelite.api.widgets.Widget setModelId(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setModelId");}
  public int getModelType(){throw new UnsupportedOperationException("Unexpected Widget call: getModelType");}
  public net.runelite.api.widgets.Widget setModelType(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setModelType");}
  public int getAnimationId(){throw new UnsupportedOperationException("Unexpected Widget call: getAnimationId");}
  public net.runelite.api.widgets.Widget setAnimationId(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setAnimationId");}
  public int getRotationX(){throw new UnsupportedOperationException("Unexpected Widget call: getRotationX");}
  public net.runelite.api.widgets.Widget setRotationX(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setRotationX");}
  public int getRotationY(){throw new UnsupportedOperationException("Unexpected Widget call: getRotationY");}
  public net.runelite.api.widgets.Widget setRotationY(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setRotationY");}
  public int getRotationZ(){throw new UnsupportedOperationException("Unexpected Widget call: getRotationZ");}
  public net.runelite.api.widgets.Widget setRotationZ(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setRotationZ");}
  public int getModelZoom(){throw new UnsupportedOperationException("Unexpected Widget call: getModelZoom");}
  public net.runelite.api.widgets.Widget setModelZoom(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setModelZoom");}
  public int getSpriteId(){throw new UnsupportedOperationException("Unexpected Widget call: getSpriteId");}
  public boolean getSpriteTiling(){throw new UnsupportedOperationException("Unexpected Widget call: getSpriteTiling");}
  public net.runelite.api.widgets.Widget setSpriteTiling(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setSpriteTiling");}
  public net.runelite.api.widgets.Widget setSpriteId(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setSpriteId");}
  public boolean isHidden(){throw new UnsupportedOperationException("Unexpected Widget call: isHidden");}
  public boolean isSelfHidden(){throw new UnsupportedOperationException("Unexpected Widget call: isSelfHidden");}
  public net.runelite.api.widgets.Widget setHidden(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setHidden");}
  public int getIndex(){throw new UnsupportedOperationException("Unexpected Widget call: getIndex");}
  public net.runelite.api.Point getCanvasLocation(){throw new UnsupportedOperationException("Unexpected Widget call: getCanvasLocation");}
  public int getWidth(){throw new UnsupportedOperationException("Unexpected Widget call: getWidth");}
  public void setWidth(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setWidth");}
  public int getHeight(){throw new UnsupportedOperationException("Unexpected Widget call: getHeight");}
  public void setHeight(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setHeight");}
  public java.awt.Rectangle getBounds(){throw new UnsupportedOperationException("Unexpected Widget call: getBounds");}
  public int getItemId(){throw new UnsupportedOperationException("Unexpected Widget call: getItemId");}
  public net.runelite.api.widgets.Widget setItemId(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setItemId");}
  public int getItemQuantity(){throw new UnsupportedOperationException("Unexpected Widget call: getItemQuantity");}
  public net.runelite.api.widgets.Widget setItemQuantity(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setItemQuantity");}
  public boolean contains(net.runelite.api.Point a0){throw new UnsupportedOperationException("Unexpected Widget call: contains");}
  public int getScrollX(){throw new UnsupportedOperationException("Unexpected Widget call: getScrollX");}
  public net.runelite.api.widgets.Widget setScrollX(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setScrollX");}
  public int getScrollY(){throw new UnsupportedOperationException("Unexpected Widget call: getScrollY");}
  public net.runelite.api.widgets.Widget setScrollY(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setScrollY");}
  public int getScrollWidth(){throw new UnsupportedOperationException("Unexpected Widget call: getScrollWidth");}
  public net.runelite.api.widgets.Widget setScrollWidth(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setScrollWidth");}
  public int getScrollHeight(){throw new UnsupportedOperationException("Unexpected Widget call: getScrollHeight");}
  public net.runelite.api.widgets.Widget setScrollHeight(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setScrollHeight");}
  public int getOriginalX(){throw new UnsupportedOperationException("Unexpected Widget call: getOriginalX");}
  public net.runelite.api.widgets.Widget setOriginalX(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setOriginalX");}
  public int getOriginalY(){throw new UnsupportedOperationException("Unexpected Widget call: getOriginalY");}
  public net.runelite.api.widgets.Widget setOriginalY(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setOriginalY");}
  public net.runelite.api.widgets.Widget setPos(int a0,int a1){throw new UnsupportedOperationException("Unexpected Widget call: setPos");}
  public net.runelite.api.widgets.Widget setPos(int a0,int a1,int a2,int a3){throw new UnsupportedOperationException("Unexpected Widget call: setPos");}
  public int getOriginalHeight(){throw new UnsupportedOperationException("Unexpected Widget call: getOriginalHeight");}
  public net.runelite.api.widgets.Widget setOriginalHeight(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setOriginalHeight");}
  public int getOriginalWidth(){throw new UnsupportedOperationException("Unexpected Widget call: getOriginalWidth");}
  public net.runelite.api.widgets.Widget setOriginalWidth(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setOriginalWidth");}
  public net.runelite.api.widgets.Widget setSize(int a0,int a1){throw new UnsupportedOperationException("Unexpected Widget call: setSize");}
  public net.runelite.api.widgets.Widget setSize(int a0,int a1,int a2,int a3){throw new UnsupportedOperationException("Unexpected Widget call: setSize");}
  public java.lang.String[] getActions(){throw new UnsupportedOperationException("Unexpected Widget call: getActions");}
  public java.lang.String[][] getSubOps(){throw new UnsupportedOperationException("Unexpected Widget call: getSubOps");}
  public net.runelite.api.widgets.Widget createChild(int a0,int a1){throw new UnsupportedOperationException("Unexpected Widget call: createChild");}
  public net.runelite.api.widgets.Widget createChild(int a0){throw new UnsupportedOperationException("Unexpected Widget call: createChild");}
  public void deleteAllChildren(){throw new UnsupportedOperationException("Unexpected Widget call: deleteAllChildren");}
  public void setAction(int a0,java.lang.String a1){throw new UnsupportedOperationException("Unexpected Widget call: setAction");}
  public void setSubOp(int a0,int a1,java.lang.String a2){throw new UnsupportedOperationException("Unexpected Widget call: setSubOp");}
  public void clearActions(){throw new UnsupportedOperationException("Unexpected Widget call: clearActions");}
  public void setOnOpListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnOpListener");}
  public void setOnDialogAbortListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnDialogAbortListener");}
  public void setOnKeyListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnKeyListener");}
  public void setOnMouseOverListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnMouseOverListener");}
  public void setOnMouseRepeatListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnMouseRepeatListener");}
  public void setOnMouseLeaveListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnMouseLeaveListener");}
  public void setOnTimerListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnTimerListener");}
  public void setOnTargetEnterListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnTargetEnterListener");}
  public void setOnTargetLeaveListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnTargetLeaveListener");}
  public boolean hasListener(){throw new UnsupportedOperationException("Unexpected Widget call: hasListener");}
  public net.runelite.api.widgets.Widget setHasListener(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setHasListener");}
  public boolean isIf3(){throw new UnsupportedOperationException("Unexpected Widget call: isIf3");}
  public void revalidate(){throw new UnsupportedOperationException("Unexpected Widget call: revalidate");}
  public void revalidateScroll(){throw new UnsupportedOperationException("Unexpected Widget call: revalidateScroll");}
  public java.lang.Object[] getOnOpListener(){throw new UnsupportedOperationException("Unexpected Widget call: getOnOpListener");}
  public java.lang.Object[] getOnKeyListener(){throw new UnsupportedOperationException("Unexpected Widget call: getOnKeyListener");}
  public java.lang.Object[] getOnLoadListener(){throw new UnsupportedOperationException("Unexpected Widget call: getOnLoadListener");}
  public java.lang.Object[] getOnInvTransmitListener(){throw new UnsupportedOperationException("Unexpected Widget call: getOnInvTransmitListener");}
  public int getFontId(){throw new UnsupportedOperationException("Unexpected Widget call: getFontId");}
  public net.runelite.api.widgets.Widget setFontId(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setFontId");}
  public int getBorderType(){throw new UnsupportedOperationException("Unexpected Widget call: getBorderType");}
  public void setBorderType(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setBorderType");}
  public boolean isFlippedVertically(){throw new UnsupportedOperationException("Unexpected Widget call: isFlippedVertically");}
  public void setFlippedVertically(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setFlippedVertically");}
  public boolean isFlippedHorizontally(){throw new UnsupportedOperationException("Unexpected Widget call: isFlippedHorizontally");}
  public void setFlippedHorizontally(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setFlippedHorizontally");}
  public boolean getTextShadowed(){throw new UnsupportedOperationException("Unexpected Widget call: getTextShadowed");}
  public net.runelite.api.widgets.Widget setTextShadowed(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setTextShadowed");}
  public int getDragDeadZone(){throw new UnsupportedOperationException("Unexpected Widget call: getDragDeadZone");}
  public void setDragDeadZone(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setDragDeadZone");}
  public int getDragDeadTime(){throw new UnsupportedOperationException("Unexpected Widget call: getDragDeadTime");}
  public void setDragDeadTime(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setDragDeadTime");}
  public int getItemQuantityMode(){throw new UnsupportedOperationException("Unexpected Widget call: getItemQuantityMode");}
  public net.runelite.api.widgets.Widget setItemQuantityMode(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setItemQuantityMode");}
  public int getXPositionMode(){throw new UnsupportedOperationException("Unexpected Widget call: getXPositionMode");}
  public net.runelite.api.widgets.Widget setXPositionMode(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setXPositionMode");}
  public int getYPositionMode(){throw new UnsupportedOperationException("Unexpected Widget call: getYPositionMode");}
  public net.runelite.api.widgets.Widget setYPositionMode(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setYPositionMode");}
  public int getLineHeight(){throw new UnsupportedOperationException("Unexpected Widget call: getLineHeight");}
  public net.runelite.api.widgets.Widget setLineHeight(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setLineHeight");}
  public int getXTextAlignment(){throw new UnsupportedOperationException("Unexpected Widget call: getXTextAlignment");}
  public net.runelite.api.widgets.Widget setXTextAlignment(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setXTextAlignment");}
  public int getYTextAlignment(){throw new UnsupportedOperationException("Unexpected Widget call: getYTextAlignment");}
  public net.runelite.api.widgets.Widget setYTextAlignment(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setYTextAlignment");}
  public int getWidthMode(){throw new UnsupportedOperationException("Unexpected Widget call: getWidthMode");}
  public net.runelite.api.widgets.Widget setWidthMode(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setWidthMode");}
  public int getHeightMode(){throw new UnsupportedOperationException("Unexpected Widget call: getHeightMode");}
  public net.runelite.api.widgets.Widget setHeightMode(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setHeightMode");}
  public net.runelite.api.FontTypeFace getFont(){throw new UnsupportedOperationException("Unexpected Widget call: getFont");}
  public boolean isFilled(){throw new UnsupportedOperationException("Unexpected Widget call: isFilled");}
  public net.runelite.api.widgets.Widget setFilled(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setFilled");}
  public java.lang.String getTargetVerb(){throw new UnsupportedOperationException("Unexpected Widget call: getTargetVerb");}
  public void setTargetVerb(java.lang.String a0){throw new UnsupportedOperationException("Unexpected Widget call: setTargetVerb");}
  public int getTargetPriority(){throw new UnsupportedOperationException("Unexpected Widget call: getTargetPriority");}
  public void setTargetPriority(int a0){throw new UnsupportedOperationException("Unexpected Widget call: setTargetPriority");}
  public boolean getNoClickThrough(){throw new UnsupportedOperationException("Unexpected Widget call: getNoClickThrough");}
  public void setNoClickThrough(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setNoClickThrough");}
  public boolean getNoScrollThrough(){throw new UnsupportedOperationException("Unexpected Widget call: getNoScrollThrough");}
  public void setNoScrollThrough(boolean a0){throw new UnsupportedOperationException("Unexpected Widget call: setNoScrollThrough");}
  public int[] getVarTransmitTrigger(){throw new UnsupportedOperationException("Unexpected Widget call: getVarTransmitTrigger");}
  public void setVarTransmitTrigger(int... a0){throw new UnsupportedOperationException("Unexpected Widget call: setVarTransmitTrigger");}
  public void setOnClickListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnClickListener");}
  public void setOnHoldListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnHoldListener");}
  public void setOnReleaseListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnReleaseListener");}
  public void setOnDragCompleteListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnDragCompleteListener");}
  public void setOnDragListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnDragListener");}
  public void setOnScrollWheelListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnScrollWheelListener");}
  public net.runelite.api.widgets.Widget getDragParent(){throw new UnsupportedOperationException("Unexpected Widget call: getDragParent");}
  public net.runelite.api.widgets.Widget setDragParent(net.runelite.api.widgets.Widget a0){throw new UnsupportedOperationException("Unexpected Widget call: setDragParent");}
  public java.lang.Object[] getOnVarTransmitListener(){throw new UnsupportedOperationException("Unexpected Widget call: getOnVarTransmitListener");}
  public void setOnVarTransmitListener(java.lang.Object... a0){throw new UnsupportedOperationException("Unexpected Widget call: setOnVarTransmitListener");}
 }
 static class NativeWidget extends WidgetStub {
  final int index;final boolean serverConfigured;int id,qty,mode,x=400,y=500,opacity,border;boolean hidden=true;String name="Original";String[] actions=new String[11];Object[] op={99};Object[] drag;
  NativeWidget(int index,boolean configured,int id){this.index=index;serverConfigured=configured;this.id=id;qty=15;actions[0]="Original operation";}
  public int getIndex(){return index;}public int getType(){return net.runelite.api.widgets.WidgetType.GRAPHIC;}public int getOriginalHeight(){return 32;}
  public int getItemId(){return id;}public Widget setItemId(int v){id=v;return this;}public int getItemQuantity(){return qty;}public Widget setItemQuantity(int v){qty=v;return this;}public int getItemQuantityMode(){return mode;}public Widget setItemQuantityMode(int v){mode=v;return this;}
  public int getOriginalX(){return x;}public Widget setOriginalX(int v){x=v;return this;}public int getOriginalY(){return y;}public Widget setOriginalY(int v){y=v;return this;}public int getOpacity(){return opacity;}public Widget setOpacity(int v){opacity=v;return this;}public int getBorderType(){return border;}public void setBorderType(int v){border=v;}
  public boolean isSelfHidden(){return hidden;}public Widget setHidden(boolean v){hidden=v;return this;}public String getName(){return name;}public Widget setName(String v){name=v;return this;}public String[] getActions(){return actions;}public void clearActions(){actions=new String[11];}public void setAction(int index,String v){actions[index]=v;}
  public Object[] getOnOpListener(){return op;}public void setOnOpListener(Object... v){op=v;}public void setOnDragCompleteListener(Object... v){drag=v;}public void revalidate(){}
  // The server's operation permissions are separate from the static widget mask.
  public int getClickMask(){return 0;}public Widget setClickMask(int v){throw new AssertionError("Do not replace native server permissions with a static mask");}
  boolean hasMenuAction(String operation){if(!serverConfigured)return false;for(String action:actions)if(operation.equals(action))return true;return false;}
 }
 @Test public void duplicatesReuseConfiguredNativeSlotsAndRestoreTheHiddenBankItems(){
  NativeWidget original=new NativeWidget(0,true,391);original.actions=StudioPotionStorage.actions(0,50);original.name="Manta ray";original.qty=1499;NativeWidget spare=new NativeWidget(1,true,995);Widget[] children={original,spare};IndependentBankView view=new IndependentBankView(null,null,null,id->id);
  Widget copy=view.proxy(children,new int[]{391,-1},original,6,391,1499,"Manta ray",original.actions);assertSame(spare,copy);assertEquals(1,copy.getIndex());assertEquals(391,copy.getItemId());assertTrue(spare.hasMenuAction("Withdraw-1"));assertTrue(spare.hasMenuAction("Withdraw-5"));assertEquals(0,spare.getClickMask());assertEquals(1499,copy.getItemQuantity());
  assertNull(view.proxy(children,new int[]{391,-1},original,7,391,1499,"Manta ray",original.actions));view.removeCopies();assertEquals(995,spare.id);assertEquals(15,spare.qty);assertEquals("Original",spare.name);assertEquals("Original operation",spare.actions[0]);assertTrue(spare.hidden);assertEquals(400,spare.x);assertEquals(500,spare.y);assertEquals(391,original.id);assertEquals(1499,original.qty);
 }
 @Test public void copiedLabelsDoNotGiveNewlyAppendedWidgetsNativeServerPermissions(){
  NativeWidget unconfigured=new NativeWidget(1001,false,391);unconfigured.actions=StudioPotionStorage.actions(0,50);assertFalse(unconfigured.hasMenuAction("Withdraw-1"));assertFalse(unconfigured.hasMenuAction("Withdraw-5"));NativeWidget configured=new NativeWidget(1,true,391);configured.actions=StudioPotionStorage.actions(0,50);assertTrue(configured.hasMenuAction("Withdraw-1"));
 }
}
