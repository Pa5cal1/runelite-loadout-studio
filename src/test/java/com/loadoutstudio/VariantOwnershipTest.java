/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;

public class VariantOwnershipTest {
    @Test public void allBundledSlayerVariantsKeepTheirImbueCapability(){
        for(int id:ItemVariationMapping.getVariations(ItemID.SLAYER_HELM)){
            int base=EquipmentVariants.slayerBase(id);
            assertTrue(base==ItemID.SLAYER_HELM||base==ItemID.SLAYER_HELM_I);
            assertTrue(EquipmentVariants.alternative(id,base));
            assertFalse(EquipmentVariants.alternative(id,base==ItemID.SLAYER_HELM?ItemID.SLAYER_HELM_I:ItemID.SLAYER_HELM));
            assertTrue(EquipmentVariants.same(id,base));
        }
        assertEquals(ItemID.SLAYER_HELM_I,EquipmentVariants.slayerBase(ItemID.SLAYER_HELM_I_ARAXYTE));
        assertEquals(ItemID.SLAYER_HELM_I,EquipmentVariants.slayerBase(ItemID.SW_SLAYER_HELM_I_HOODED));
        assertEquals(ItemID.SLAYER_HELM_I,EquipmentVariants.slayerBase(ItemID.PVPA_LEAGUE_6_SLAYER_HELM2_I));
    }
    @Test public void lockedPouchesKeepExactIdentityAndUpgradeDirection(){
        assertTrue(EquipmentVariants.runePouch(ItemID.BH_RUNE_POUCH_TROUVER));
        assertTrue(EquipmentVariants.divinePouch(ItemID.DIVINE_RUNE_POUCH_TROUVER));
        assertTrue(EquipmentVariants.alternative(ItemID.DIVINE_RUNE_POUCH_TROUVER,ItemID.BH_RUNE_POUCH_TROUVER));
        assertFalse(EquipmentVariants.alternative(ItemID.BH_RUNE_POUCH_TROUVER,ItemID.DIVINE_RUNE_POUCH_TROUVER));
        assertTrue(EquipmentVariants.same(ItemID.DIVINE_RUNE_POUCH,ItemID.DIVINE_RUNE_POUCH_TROUVER));
    }
    @Test public void ownedColouredImbuedHelmetIsOfferedForTheWikiHelmet(){
        int owned=ItemID.SLAYER_HELM_I_VERZIK;
        JsonObject p=SetupCodec.blank("Helmet");SetupCodec.setup(p).getAsJsonArray("eq").set(0,SetupCodec.item(ItemID.SLAYER_HELM_I,1));
        StudioPanel.ItemInfo info=OwnedGearTest.item(owned,"Vampyric slayer helmet (i)",0);
        OwnedGear gear=new OwnedGear(Collections.singletonMap(owned,1),Collections.singletonMap(owned,info),1,true);
        assertEquals(owned,gear.alternatives(p,name->null).get(0).choices.get(0).id);
    }
    @Test public void bankFilteringPlacesAnOwnedImbuedColourInTheRequestedHelmetCell(){
        JsonObject p=SetupCodec.blank("Helmet filter");SetupCodec.setup(p).getAsJsonArray("eq").set(0,SetupCodec.item(ItemID.SLAYER_HELM_I,1));SetupCodec.rebuildLayout(p);
        BankFilterTest.FakeBank bank=new BankFilterTest.FakeBank();bank.open=true;StudioBankFilter filter=new StudioBankFilter(bank,"studio");filter.select(p);
        assertTrue(bank.matches.test(ItemID.SLAYER_HELM_I_VERZIK));assertTrue(bank.matches.test(ItemID.PVPA_SLAYER_HELM_I_ARAXYTE));
        assertFalse(bank.matches.test(ItemID.SLAYER_HELM_VERZIK));
        assertEquals(ItemID.SLAYER_HELM_I,filter.layoutItemId(ItemID.SLAYER_HELM_I_VERZIK));
    }
    @Test public void upstreamItemVariantsAreAvailableAsExplicitAlternatives(){
        assertTrue(EquipmentVariants.alternative(ItemID.DRAGON_SCIMITAR_ORNAMENT,ItemID.DRAGON_SCIMITAR));
        assertTrue(EquipmentVariants.alternative(ItemID.SKILLCAPE_MAX_INFERNALCAPE_TROUVER,ItemID.INFERNAL_CAPE));
        assertFalse(EquipmentVariants.alternative(ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.DIZANAS_QUIVER_INFINITE));
    }
    @Test public void builderUpgradesLockedOrdinaryToLockedDivineAndAutosavesExactId()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            OwnedGearPanelTest.Host host=new OwnedGearPanelTest.Host();int normal=ItemID.BH_RUNE_POUCH_TROUVER,divine=ItemID.DIVINE_RUNE_POUCH_TROUVER;
            Map<Integer,Integer> counts=new LinkedHashMap<>();counts.put(normal,1);counts.put(divine,1);
            Map<Integer,StudioPanel.ItemInfo> items=new HashMap<>();items.put(divine,OwnedGearTest.item(divine,"Divine rune pouch (l)",-1));
            host.owned=new OwnedGear(counts,items,1,true);
            JsonObject source=SetupCodec.blank("Locked pouch");SetupCodec.setup(source).getAsJsonArray("inv").set(0,SetupCodec.item(normal,1));SetupCodec.setup(source).getAsJsonArray("rp").set(0,SetupCodec.item(560,16001));SetupCodec.rebuildLayout(source);
            StudioPanel panel=new StudioPanel(host,RuneLiteAPI.GSON);panel.newDraft(source);panel.refreshAlternatives();
            JsonObject selected=SetupCodec.setup(host.selectedBankSetup);
            assertEquals(divine,SetupCodec.id(selected.getAsJsonArray("inv").get(0)));
            assertEquals("Divine rune pouch (l)",selected.getAsJsonArray("inv").get(0).getAsJsonObject().get("label").getAsString());
            assertEquals(16001,SetupCodec.quantity(selected.getAsJsonArray("rp").get(0)));assertTrue(panel.autoSaveNow());
            assertEquals(divine,SetupCodec.id(SetupCodec.setup(SetupLibrary.read(host.saved,RuneLiteAPI.GSON).setups.get(0).portable).getAsJsonArray("inv").get(0)));
            assertEquals(normal,SetupCodec.id(SetupCodec.setup(source).getAsJsonArray("inv").get(0)));panel.dispose();
        });
    }
}
