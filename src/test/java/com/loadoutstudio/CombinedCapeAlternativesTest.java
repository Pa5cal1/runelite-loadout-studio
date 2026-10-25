/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class CombinedCapeAlternativesTest {
 private static JsonObject setup(int wanted,int slot){JsonObject p=SetupCodec.blank("Cosmetic test");SetupCodec.setup(p).getAsJsonArray("eq").set(slot,SetupCodec.item(wanted,1));SetupCodec.rebuildLayout(p);return p;}
 private static OwnedGear owned(int id,String name,int slot){return new OwnedGear(Collections.singletonMap(id,1),Collections.singletonMap(id,OwnedGearTest.item(id,name,slot)),1,true);}
 @Test public void usableMaxCapesAndOrnamentsAreOfferedWithTheirExactOwnedIds(){
  int[][] pairs={{ItemID.INFERNAL_CAPE,ItemID.SKILLCAPE_MAX_INFERNALCAPE},{ItemID.INFERNAL_CAPE,ItemID.SKILLCAPE_MAX_INFERNALCAPE_TROUVER},{ItemID.TZHAAR_CAPE_FIRE,ItemID.SKILLCAPE_MAX_FIRECAPE},{ItemID.TZHAAR_CAPE_FIRE,ItemID.SKILLCAPE_MAX_FIRECAPE_TROUVER},{ItemID.DIZANAS_QUIVER_CHARGED,ItemID.SKILLCAPE_MAX_DIZANAS},{ItemID.DIZANAS_QUIVER_INFINITE,ItemID.SKILLCAPE_MAX_DIZANAS_TROUVER},{ItemID.AVAS_ASSEMBLER,ItemID.SKILLCAPE_MAX_ASSEMBLER_MASORI},{ItemID.MA2_GUTHIX_CAPE,ItemID.SKILLCAPE_MAX_GUTHIX2_TROUVER},{ItemID.VOIDWAKER,ItemID.DEADMAN_VOIDWAKER}};
  for(int[] pair:pairs){int slot=pair[0]==ItemID.VOIDWAKER?3:1;JsonObject p=setup(pair[0],slot),before=p.deepCopy();OwnedGear gear=owned(pair[1],"Owned cosmetic",slot);List<OwnedGear.Proposal> choices=gear.alternatives(p,n->null);assertEquals(1,choices.size());assertEquals(pair[1],choices.get(0).choices.get(0).id);assertEquals(before,p);BankFilterTest.FakeBank bank=new BankFilterTest.FakeBank();bank.open=true;StudioBankFilter filter=new StudioBankFilter(bank,"studio");filter.select(p);assertTrue(bank.matches.test(pair[0]));SetupCodec.setup(p).getAsJsonArray("eq").set(slot,SetupCodec.item(pair[1],1));SetupCodec.rebuildLayout(p);SetupCodec codec=new SetupCodec(RuneLiteAPI.GSON);assertEquals(pair[1],SetupCodec.id(SetupCodec.setup(codec.importText(codec.inventory(p)).get(0)).getAsJsonArray("eq").get(slot)));}
 }
 @Test public void rankedDowngradeCanUseAFireMaxCapeWithoutChangingWikiOrder(){
  JsonObject p=setup(ItemID.INFERNAL_CAPE,1);JsonArray choices=new JsonArray();choices.add("Infernal cape");choices.add("Fire cape");SetupCodec.setup(p).getAsJsonArray("eq").get(1).getAsJsonObject().add("choices",choices);
  List<OwnedGear.Proposal> proposals=owned(ItemID.SKILLCAPE_MAX_FIRECAPE,"Fire max cape",1).alternatives(p,n->OwnedGearTest.item(n.equals("Fire cape")?ItemID.TZHAAR_CAPE_FIRE:ItemID.INFERNAL_CAPE,n,1));assertEquals(ItemID.SKILLCAPE_MAX_FIRECAPE,proposals.get(0).choices.get(0).id);
 }
 @Test public void chargeStatesBrokenCapesAndOrdinaryMaxCapeAreNotEquivalent(){
  assertFalse(EquipmentVariants.same(ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.SKILLCAPE_MAX_DIZANAS));assertTrue(EquipmentVariants.alternative(ItemID.SKILLCAPE_MAX_DIZANAS,ItemID.DIZANAS_QUIVER_UNCHARGED));assertFalse(EquipmentVariants.alternative(ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.SKILLCAPE_MAX_DIZANAS));assertFalse(EquipmentVariants.same(ItemID.SCYTHE_OF_VITUR_UNCHARGED_OR,ItemID.SCYTHE_OF_VITUR));
  for(int id:new int[]{ItemID.SKILLCAPE_MAX,ItemID.SKILLCAPE_MAX_INFERNALCAPE_BROKEN,ItemID.SKILLCAPE_MAX_INFERNALCAPE_TROUVER_MANGLED})assertTrue(owned(id,"Unusable or different cape",1).alternatives(setup(ItemID.INFERNAL_CAPE,1),n->null).isEmpty());
 }
 @Test public void wrongEquipmentSlotIsStillRejected(){assertTrue(owned(ItemID.DEADMAN_VOIDWAKER,"Voidwaker",1).alternatives(setup(ItemID.VOIDWAKER,3),n->null).isEmpty());}
 @Test public void jewelleryAndWeaponOrnamentKitsOfferTheObservedVariant(){
  int[][] pairs={{ItemID.ENCHANTED_ONYX_AMULET,ItemID.FURY_ORNAMENT,2},{ItemID.ZENYTE_AMULET_ENCHANTED,ItemID.ZENYTE_AMULET_ORNAMENT,2},{ItemID.ZENYTE_NECKLACE_ENCHANTED,ItemID.ZENYTE_NECKLACE_ORNAMENT,2},{ItemID.ZENYTE_BRACELET_ENCHANTED,ItemID.ZENYTE_BRACELET_ENCHANTED_ORNAMENT,9},{ItemID.OCCULT_NECKLACE,ItemID.OCCULT_NECKLACE_ORNAMENT,2},{ItemID.ELIDINIS_WARD_FORTIFIED,ItemID.ELIDINIS_WARD_ORNAMENT,5},{ItemID.DRAGON_SCIMITAR,ItemID.DRAGON_SCIMITAR_ORNAMENT,3},{ItemID.DRAGON_CLAWS,ItemID.DRAGON_CLAWS_ORNAMENT,3}};
  for(int[] pair:pairs)assertEquals(pair[1],owned(pair[1],"Ornamented item",pair[2]).alternatives(setup(pair[0],pair[2]),n->null).get(0).choices.get(0).id);
  assertFalse(EquipmentVariants.alternative(ItemID.ELIDINIS_WARD_ORNAMENT,ItemID.ELIDINIS_WARD));
 }
}
