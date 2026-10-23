/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.List;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiFirstChoiceTest
{
    private static class Host extends PanelTest.FakeHost
    {
        ItemCatalogue catalogue = new ItemCatalogue();
        int requests;
        @Override public StudioPanel.ItemInfo defaultItem(String name) { return catalogue.firstExact(name); }
        @Override public void fetchWiki(String url, java.util.function.BiConsumer<String,String> callback) { requests++; }
    }
    @Test public void duplicateNamesUseFirstCatalogueVariantInStableOrder()
    {
        ItemCatalogue catalogue=new ItemCatalogue();
        catalogue.add(900,"Rune pouch",false,-1,-1);catalogue.add(100,"Rune pouch",false,-1,-1);
        catalogue.add(1,"Rune pouch",false,799,-1);catalogue.add(2,"Rune pouch",false,-1,14401);
        assertNull(catalogue.exact("Rune pouch"));assertEquals(100,catalogue.firstExact("rune pouch").id);
        assertEquals(catalogue.search("Rune pouch").get(0).id,catalogue.firstExact("Rune pouch").id);
    }
    @Test public void rankedGearDefaultsAndExistingUnresolvedCopiesResolveWithoutHttp() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();host.catalogue.add(26382,"Torva full helm",false,-1,-1);host.catalogue.add(12931,"Serpentine helm",false,-1,-1);host.catalogue.add(8839,"Void knight top",false,-1,-1);
            JsonObject source=new WikiParser().parse("{{Recommended equipment|style=Melee|head1={{plink|Torva full helm}} / {{plink|Serpentine helm}}|body1={{plink|Void knight top}} + {{plink|Void knight robe}}}}","Vorkath","fixture").get(0);
            SetupLibrary.Entry entry=new SetupLibrary.Entry(source);StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.open(entry);
            JsonObject setup=SetupCodec.setup(host.selectedBankSetup);assertEquals(26382,SetupCodec.id(setup.getAsJsonArray("eq").get(0)));assertEquals(8839,SetupCodec.id(setup.getAsJsonArray("eq").get(4)));
            assertEquals("Torva full helm",setup.getAsJsonArray("eq").get(0).getAsJsonObject().getAsJsonArray("choices").get(0).getAsString());
            assertEquals(0,SetupCodec.id(SetupCodec.setup(source).getAsJsonArray("eq").get(0)));assertEquals(0,host.requests);assertNull(host.saved);panel.dispose();
        });
    }
    @Test public void commonInventoryTemplatesUseFirstBundledOptionAndKeepSlotsAndUnknownAmounts() throws Exception
    {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();host.catalogue.add(391,"Manta ray",false,-1,-1);host.catalogue.add(2434,"Prayer potion(4)",false,-1,-1);host.catalogue.add(560,"Death rune",true,-1,-1);
            JsonObject source=new WikiParser().parse("{{Inventory|1={{Cheap food}}|3={{Cheap prayer}}|4={{Unsupported choice}}}}{{Rune pouch|1=Death rune}}","Vorkath","fixture").get(0);
            JsonObject fixed=SetupCodec.item(892,200000);fixed.addProperty("label","{{Cheap food}}");SetupCodec.setup(source).getAsJsonArray("inv").set(4,fixed);
            StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(source);JsonObject setup=SetupCodec.setup(host.selectedBankSetup);
            assertEquals(391,SetupCodec.id(setup.getAsJsonArray("inv").get(0)));assertTrue(setup.getAsJsonArray("inv").get(1).isJsonNull());assertEquals(2434,SetupCodec.id(setup.getAsJsonArray("inv").get(2)));
            assertEquals(0,SetupCodec.id(setup.getAsJsonArray("inv").get(3)));assertEquals(560,SetupCodec.id(setup.getAsJsonArray("rp").get(0)));assertTrue(setup.getAsJsonArray("rp").get(0).getAsJsonObject().get("q").isJsonNull());
            assertEquals(892,SetupCodec.id(setup.getAsJsonArray("inv").get(4)));assertEquals(200000,SetupCodec.quantity(setup.getAsJsonArray("inv").get(4)));
            assertEquals(28,setup.getAsJsonArray("inv").size());assertEquals(0,host.requests);panel.dispose();
        });
    }
}
