/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2019, dillydill123 <https://github.com/dillydill123>
 * Mapping table adapted from InventorySetupsVariationMapping.
 * Redistribution conditions and disclaimer: bundled inventory-setups-LICENSE.txt.
 */
package com.loadoutstudio;
import java.util.Map;
import java.util.HashMap;
import net.runelite.client.game.ItemVariationMapping;
import static net.runelite.api.gameval.ItemID.*;

final class SetupVariationMapping
{
    private static final Map<Integer,Integer> mappings=new HashMap<>();
    static int map(int id){int base=ItemVariationMapping.map(id);return base==id?mappings.getOrDefault(id,id):base;}
    /** Fuzzy matching also includes supported combat-equivalent combined capes. */
    static int fuzzy(int id){
        int combat=EquipmentVariants.combatBase(id);
        if(combat==SLAYER_HELM_I)return SLAYER_HELM_I;
        int base=ItemVariationMapping.map(combat);
        return mappings.getOrDefault(combat,mappings.getOrDefault(base,base));
    }
    static
    {
		// Granite Cannonball -> Cannonball
		mappings.put(GRANITE_CANNONBALL, MCANNONBALL);

		// Smith Gloves (i) act as ice gloves
		mappings.put(SMITHING_UNIFORM_GLOVES_ICE, ICE_GLOVES);

		// Divine rune pouch -> Rune Pouch
		mappings.put(DIVINE_RUNE_POUCH, BH_RUNE_POUCH);

		// Make god capes the same
		final int itemIDGodCape = 1000000001;
		mappings.put(SARADOMIN_CAPE, itemIDGodCape);
		mappings.put(GUTHIX_CAPE, itemIDGodCape);
		mappings.put(ZAMORAK_CAPE, itemIDGodCape);
		final int itemIDImbuedGodCape = 1000000002;
		mappings.put(MA2_SARADOMIN_CAPE, itemIDImbuedGodCape);
		mappings.put(MA2_GUTHIX_CAPE, itemIDImbuedGodCape);
		mappings.put(MA2_ZAMORAK_CAPE, itemIDImbuedGodCape);
		mappings.put(MA2_SARADOMIN_CAPE_TROUVER, itemIDImbuedGodCape);
		mappings.put(MA2_GUTHIX_CAPE_TROUVER, itemIDImbuedGodCape);
		mappings.put(MA2_ZAMORAK_CAPE_TROUVER, itemIDImbuedGodCape);
		final int itemIDGodMaxCape = 1000000003;
		mappings.put(SKILLCAPE_MAX_SARADOMIN, itemIDGodMaxCape);
		mappings.put(SKILLCAPE_MAX_GUTHIX, itemIDGodMaxCape);
		mappings.put(SKILLCAPE_MAX_ZAMORAK, itemIDGodMaxCape);
		final int itemIDImbuedGodMaxCape = 1000000004;
		mappings.put(SKILLCAPE_MAX_SARADOMIN2, itemIDImbuedGodMaxCape);
		mappings.put(SKILLCAPE_MAX_GUTHIX2, itemIDImbuedGodMaxCape);
		mappings.put(SKILLCAPE_MAX_ZAMORAK2, itemIDImbuedGodMaxCape);
		mappings.put(SKILLCAPE_MAX_SARADOMIN2_TROUVER, itemIDImbuedGodMaxCape);
		mappings.put(SKILLCAPE_MAX_GUTHIX2_TROUVER, itemIDImbuedGodMaxCape);
		mappings.put(SKILLCAPE_MAX_ZAMORAK2_TROUVER, itemIDImbuedGodMaxCape);

		// Make god d'hides the same
		final int itemIDGodCoif = 1000000005;
		mappings.put(TRAIL_ANCIENT_COIF, itemIDGodCoif);
		mappings.put(TRAIL_ARMADYL_COIF, itemIDGodCoif);
		mappings.put(TRAIL_BANDOS_COIF, itemIDGodCoif);
		mappings.put(TRAIL_GUTHIX_COIF, itemIDGodCoif);
		mappings.put(TRAIL_SARADOMIN_COIF, itemIDGodCoif);
		mappings.put(TRAIL_ZAMORAK_COIF, itemIDGodCoif);

		final int itemIDGodDhideBody = 1000000006;
		mappings.put(TRAIL_ANCIENT_CHEST, itemIDGodDhideBody);
		mappings.put(TRAIL_ARMADYL_CHEST , itemIDGodDhideBody);
		mappings.put(TRAIL_BANDOS_CHEST, itemIDGodDhideBody);
		mappings.put(TRAIL_GUTHIX_CHEST, itemIDGodDhideBody);
		mappings.put(TRAIL_SARADOMIN_CHEST, itemIDGodDhideBody);
		mappings.put(TRAIL_ZAMORAK_CHEST, itemIDGodDhideBody);

		final int itemIDGodChaps = 1000000007;
		mappings.put(TRAIL_ANCIENT_CHAPS, itemIDGodChaps);
		mappings.put(TRAIL_ARMADYL_CHAPS, itemIDGodChaps);
		mappings.put(TRAIL_BANDOS_CHAPS, itemIDGodChaps);
		mappings.put(TRAIL_GUTHIX_CHAPS, itemIDGodChaps);
		mappings.put(TRAIL_SARADOMIN_CHAPS, itemIDGodChaps);
		mappings.put(TRAIL_ZAMORAK_CHAPS, itemIDGodChaps);

		final int itemIDGodBracers = 1000000008;
		mappings.put(TRAIL_ANCIENT_VAMBRACES, itemIDGodBracers);
		mappings.put(TRAIL_ARMADYL_VAMBRACES, itemIDGodBracers);
		mappings.put(TRAIL_BANDOS_VAMBRACES, itemIDGodBracers);
		mappings.put(TRAIL_GUTHIX_VAMBRACES, itemIDGodBracers);
		mappings.put(TRAIL_SARADOMIN_VAMBRACES, itemIDGodBracers);
		mappings.put(TRAIL_ZAMORAK_VAMBRACES, itemIDGodBracers);

		final int itemIDGodDhideBoots = 1000000009;
		mappings.put(BLESSED_BOOTS_ANCIENT, itemIDGodDhideBoots);
		mappings.put(BLESSED_BOOTS_ARMADYL, itemIDGodDhideBoots);
		mappings.put(BLESSED_BOOTS_BANDOS, itemIDGodDhideBoots);
		mappings.put(BLESSED_BOOTS_GUTHIX, itemIDGodDhideBoots);
		mappings.put(BLESSED_BOOTS_SARADOMIN, itemIDGodDhideBoots);
		mappings.put(BLESSED_BOOTS_ZAMORAK, itemIDGodDhideBoots);

		final int itemIDGodDhideShield = 1000000010;
		mappings.put(BLESSED_DHIDE_SHIELD_ANCIENT, itemIDGodDhideShield);
		mappings.put(BLESSED_DHIDE_SHIELD_ARMADYL, itemIDGodDhideShield);
		mappings.put(BLESSED_DHIDE_SHIELD_BANDOS, itemIDGodDhideShield);
		mappings.put(BLESSED_DHIDE_SHIELD_GUTHIX, itemIDGodDhideShield);
		mappings.put(BLESSED_DHIDE_SHIELD_SARADOMIN, itemIDGodDhideShield);
		mappings.put(BLESSED_DHIDE_SHIELD_ZAMORAK, itemIDGodDhideShield);

		// Twisted Ancestral -> Regular Ancestral
		mappings.put(ANCESTRAL_HAT_TWISTED, ANCESTRAL_HAT);
		mappings.put(ANCESTRAL_ROBE_BOTTOM_TWISTED, ANCESTRAL_ROBE_BOTTOM);
		mappings.put(ANCESTRAL_ROBE_TOP_TWISTED, ANCESTRAL_ROBE_TOP);

		// Golden Prospectors -> Regular Prospectors
		mappings.put(MOTHERLODE_REWARD_BOOTS_GOLD, MOTHERLODE_REWARD_BOOTS);
		mappings.put(MOTHERLODE_REWARD_HAT_GOLD, MOTHERLODE_REWARD_HAT);
		mappings.put(MOTHERLODE_REWARD_TOP_GOLD, MOTHERLODE_REWARD_TOP);
		mappings.put(MOTHERLODE_REWARD_LEGS_GOLD, MOTHERLODE_REWARD_LEGS);

		// Spirit Anglers -> Regular Anglers
		mappings.put(SPIRIT_ANGLER_BOOTS, TRAWLER_REWARD_BOOTS);
		mappings.put(SPIRIT_ANGLER_HAT, TRAWLER_REWARD_HAT);
		mappings.put(SPIRIT_ANGLER_TOP, TRAWLER_REWARD_TOP);
		mappings.put(SPIRIT_ANGLER_LEGS, TRAWLER_REWARD_LEGS);
		
		// ToB ornament kits -> base version
		mappings.put(SCYTHE_OF_VITUR_BL, SCYTHE_OF_VITUR);
		mappings.put(SCYTHE_OF_VITUR_OR, SCYTHE_OF_VITUR);
		mappings.put(SANGUINESTI_STAFF_OR, SANGUINESTI_STAFF);
		mappings.put(GHRAZI_RAPIER_OR, GHRAZI_RAPIER);

		mappings.put(INFERNAL_DEFENDER_GHOMMAL_5, INFERNAL_DEFENDER);
		mappings.put(INFERNAL_DEFENDER_GHOMMAL_5_TROUVER, INFERNAL_DEFENDER);
		mappings.put(INFERNAL_DEFENDER_GHOMMAL_6 , INFERNAL_DEFENDER);
		mappings.put(INFERNAL_DEFENDER_GHOMMAL_6_TROUVER, INFERNAL_DEFENDER);

		// Blazing blowpipe -> toxic blowpipe
		mappings.put(TOXIC_BLOWPIPE_ORNAMENT, TOXIC_BLOWPIPE);
		mappings.put(TOXIC_BLOWPIPE_LOADED_ORNAMENT, TOXIC_BLOWPIPE_LOADED);

		// Locked fire/infernal cape -> regular capes
		mappings.put(TZHAAR_CAPE_FIRE_TROUVER, TZHAAR_CAPE_FIRE);
		mappings.put(INFERNAL_CAPE_TROUVER, INFERNAL_CAPE);

		// Blood Torva -> Regular Torva
		mappings.put(TORVA_HELM_SANGUINE, TORVA_HELM);
		mappings.put(TORVA_CHEST_SANGUINE, TORVA_CHEST);
		mappings.put(TORVA_LEGS_SANGUINE, TORVA_LEGS);

    }
}
