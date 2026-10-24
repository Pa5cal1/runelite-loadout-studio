/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class PluginPreferencesTest
{
    static class Settings implements LoadoutStudioConfig {
        boolean ignore;
        public DefaultLayout defaultLayout(){return DefaultLayout.ZIGZAG;}
        public boolean defaultZigzagBottom(){return true;}
        public boolean defaultFilterBank(){return false;}
        public boolean defaultFuzzy(){return true;}
        public StackRule defaultStackRule(){return StackRule.MINIMUM;}
        public boolean captureAttackOption(){return false;}
        public boolean ignoreAmmoQuantities(){return ignore;}
    }
    static class Host extends PanelTest.FakeHost {final Settings settings=new Settings();public LoadoutStudioConfig settings(){return settings;}}
    @Test public void defaultsApplyToCapturedCopyWithoutChangingSource()
    {
        JsonObject source=SetupCodec.blank("Capture");SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(100,50));SetupCodec.setup(source).addProperty("ao","Chop");JsonObject before=source.deepCopy(),copy=LoadoutDefaults.captured(source,new Settings());
        assertEquals(before,source);JsonObject setup=SetupCodec.setup(copy);assertFalse(setup.get("fb").getAsBoolean());assertFalse(setup.has("ao"));JsonObject item=setup.getAsJsonArray("inv").get(0).getAsJsonObject();assertTrue(item.get("f").getAsBoolean());assertEquals("Less_Than",item.get("sc").getAsString());assertEquals(50,SetupCodec.quantity(item));assertEquals(BankLayouts.generate(copy,BankLayouts.Mode.ZIGZAG,true),copy.getAsJsonArray("layout"));
    }
    @Test public void blankSetupKeepsConfiguredDirectionEvenWithNoItems() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);OwnedGearPanelTest.click(panel,"libraryTool:New setup");OwnedGearPanelTest.click(panel,"Save");SetupLibrary.Entry entry=SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0);assertEquals(BankLayouts.Mode.ZIGZAG,entry.bankLayout);assertTrue(entry.zigzagBottomToTop);assertFalse(SetupCodec.setup(entry.portable).get("fb").getAsBoolean());panel.dispose();});
    }
    @Test public void globalPreferencesDoNotRewriteSavedSetupsOrInterruptNotes() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject source=SetupCodec.blank("Imported");SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(100,1));SetupCodec.rebuildLayout(source);JsonObject before=source.deepCopy();panel.open(new SetupLibrary.Entry(source));JTextArea notes=(JTextArea)OwnedGearPanelTest.find(panel,"loadoutNotes");notes.setText("Keep my typing");host.settings.ignore=true;panel.preferencesChanged();assertSame(notes,OwnedGearPanelTest.find(panel,"loadoutNotes"));assertEquals("Keep my typing",notes.getText());OwnedGearPanelTest.click(panel,"Save");JsonObject saved=SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable;assertEquals(before.getAsJsonObject("setup").get("fb"),SetupCodec.setup(saved).get("fb"));assertFalse(SetupCodec.setup(saved).getAsJsonArray("inv").get(0).getAsJsonObject().has("f"));assertEquals(before.get("layout"),saved.get("layout"));panel.dispose();});
    }
    @Test public void ammoPreferenceUpdatesComparisonButDoesNotIgnoreRunePouch() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);JsonObject source=SetupCodec.blank("Amounts"),actual=SetupCodec.blank("Actual");JsonObject arrow=SetupCodec.item(100,100);arrow.addProperty("sc","Standard");SetupCodec.setup(source).getAsJsonArray("eq").set(13,arrow);SetupCodec.setup(actual).getAsJsonArray("eq").set(13,SetupCodec.item(100,50));panel.open(new SetupLibrary.Entry(source));panel.acceptLiveGear(actual);assertEquals("1 gear difference(s)",((JLabel)OwnedGearPanelTest.find(panel,"gearReadiness")).getText());host.settings.ignore=true;panel.preferencesChanged();assertEquals("Gear matches setup",((JLabel)OwnedGearPanelTest.find(panel,"gearReadiness")).getText());panel.dispose();});
        JsonObject source=SetupCodec.blank("Runes"),actual=SetupCodec.blank("Actual");JsonObject runes=SetupCodec.item(560,100);runes.addProperty("sc","Standard");SetupCodec.setup(source).getAsJsonArray("rp").set(0,runes);SetupCodec.setup(actual).getAsJsonArray("rp").set(0,SetupCodec.item(560,50));assertTrue(SetupComparison.differences(source,actual,true,id->id).contains("rp:0"));
    }
    @Test public void sidebarOmitsGeneralSettingsAndRedundantActions() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(new SetupLibrary.Entry(SetupCodec.blank("Clean")));assertNull(OwnedGearPanelTest.find(panel,"librarySort"));assertNull(OwnedGearPanelTest.find(panel,"Ignore Ammo quantities"));assertNull(OwnedGearPanelTest.find(panel,"Apply filter"));assertNull(OwnedGearPanelTest.find(panel,"Compare gear"));assertNull(OwnedGearPanelTest.find(panel,"Export setup"));assertNotNull(OwnedGearPanelTest.find(panel,"quickCompareGear"));assertNotNull(OwnedGearPanelTest.find(panel,"Save copy"));panel.preferencesChanged();assertTrue(OwnedGearPanelTest.find(panel,"supportCoffee").isVisible());panel.dispose();});
    }
}
