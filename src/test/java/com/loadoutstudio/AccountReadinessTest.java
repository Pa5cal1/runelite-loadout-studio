/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.BiConsumer;
import javax.swing.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccountReadinessTest {
    private static class Host extends PanelTest.FakeHost {
        final Map<String,String> libraries=new HashMap<>();
        BiConsumer<List<String>,String> pending;
        @Override public String read(String scope){return libraries.get(scope);}
        @Override public void save(String scope,String text){super.save(scope,text);libraries.put(scope,text);}
        @Override public void searchWiki(String query,BiConsumer<List<String>,String> callback){pending=callback;}
    }
    @Test public void threeCharactersKeepTheirGearNamesAndFavoritesSeparate() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);
            String[] accounts={"maxed-main","mid-iron","new-hardcore"};
            String[] names={"Main raid","Iron slayer","Hardcore starter"};
            int[] items={net.runelite.api.gameval.ItemID.ULTOR_RING,net.runelite.api.gameval.ItemID.BANDOS_CHESTPLATE,net.runelite.api.gameval.ItemID.BRONZE_SWORD};
            for(int i=0;i<accounts.length;i++){
                host.scope=accounts[i];panel.reloadScope();JsonObject setup=SetupCodec.blank(names[i]);SetupCodec.setup(setup).getAsJsonArray("inv").set(0,SetupCodec.item(items[i],1));SetupCodec.rebuildLayout(setup);panel.newDraft(setup);OwnedGearPanelTest.click(panel,"Save");
                SetupLibrary library=SetupLibrary.read(host.read(accounts[i]),RuneLiteAPI.GSON);assertEquals(1,library.setups.size());OwnedGearPanelTest.click(panel,"libraryFavorite:"+library.setups.get(0).id);
            }
            for(int i=0;i<accounts.length;i++){
                host.scope=accounts[i];panel.reloadScope();SetupLibrary library=SetupLibrary.read(host.read(accounts[i]),RuneLiteAPI.GSON);assertEquals(1,library.setups.size());SetupLibrary.Entry entry=library.setups.get(0);assertEquals(names[i],SetupCodec.name(entry.portable));assertTrue(entry.favorite);panel.openSaved(entry.id);assertEquals(items[i],SetupCodec.id(SetupCodec.setup(host.selectedBankSetup).getAsJsonArray("inv").get(0)));
            }
            panel.dispose();
        });
    }
    @Test public void previousCharactersDelayedSearchCannotPopulateNewLibrary() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            Host host=new Host();StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);OwnedGearPanelTest.click(panel,"nav:Wiki");((JTextField)OwnedGearPanelTest.find(panel,"wikiSearch")).setText("Vorkath");OwnedGearPanelTest.click(panel,"Search Wiki");assertNotNull(host.pending);
            host.scope="new-hardcore";panel.reloadScope();host.pending.accept(Collections.singletonList("Vorkath"),null);assertEquals(0,((JList<?>)OwnedGearPanelTest.find(panel,"wikiSuggestions")).getModel().getSize());assertNull(host.selectedBankSetup);assertNull(host.read(host.scope));panel.dispose();
        });
    }
    @Test public void slayerRecommendationsRemainDiscoverableWhileTechnicalSubpagesAreHidden(){
        List<String> titles=WikiClient.activityTitles(Arrays.asList("Slayer task/Aquanites","Slayer task/Aquanites/Strategies","Slayer task/Aquanites/Table","Vorkath/Strategies","Vorkath","Fractured archive miniquest"));
        assertEquals(Arrays.asList("Slayer task/Aquanites","Vorkath"),titles);
    }
}
