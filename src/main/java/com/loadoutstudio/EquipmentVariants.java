/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import static net.runelite.api.gameval.ItemID.*;
/** Usable cosmetic/combat equivalence; exact stored/exported IDs are never rewritten. */
final class EquipmentVariants {
    static int combatBase(int id){switch(id){
        case SKILLCAPE_MAX_INFERNALCAPE:case SKILLCAPE_MAX_INFERNALCAPE_TROUVER:case INFERNAL_CAPE_TROUVER:return INFERNAL_CAPE;
        case SKILLCAPE_MAX_FIRECAPE:case SKILLCAPE_MAX_FIRECAPE_TROUVER:case TZHAAR_CAPE_FIRE_TROUVER:return TZHAAR_CAPE_FIRE;
        case SKILLCAPE_MAX_ANMA:return ANMA_50_REWARD;
        case SKILLCAPE_MAX_ARDY:return ARDY_CAPE_ELITE;
        case SKILLCAPE_MAX_MYTHICAL:return MYTHICAL_CAPE;
        case SKILLCAPE_MAX_ASSEMBLER:case SKILLCAPE_MAX_ASSEMBLER_TROUVER:
        case SKILLCAPE_MAX_ASSEMBLER_MASORI:case SKILLCAPE_MAX_ASSEMBLER_MASORI_TROUVER:
        case AVAS_ASSEMBLER_MASORI:case AVAS_ASSEMBLER_MASORI_TROUVER:case AVAS_ASSEMBLER_TROUVER:return AVAS_ASSEMBLER;
        case SKILLCAPE_MAX_SARADOMIN:return SARADOMIN_CAPE;
        case SKILLCAPE_MAX_GUTHIX:return GUTHIX_CAPE;
        case SKILLCAPE_MAX_ZAMORAK:return ZAMORAK_CAPE;
        case SKILLCAPE_MAX_SARADOMIN2:case SKILLCAPE_MAX_SARADOMIN2_TROUVER:case MA2_SARADOMIN_CAPE_TROUVER:return MA2_SARADOMIN_CAPE;
        case SKILLCAPE_MAX_GUTHIX2:case SKILLCAPE_MAX_GUTHIX2_TROUVER:case MA2_GUTHIX_CAPE_TROUVER:return MA2_GUTHIX_CAPE;
        case SKILLCAPE_MAX_ZAMORAK2:case SKILLCAPE_MAX_ZAMORAK2_TROUVER:case MA2_ZAMORAK_CAPE_TROUVER:return MA2_ZAMORAK_CAPE;
        case SKILLCAPE_MAX_DIZANAS:case SKILLCAPE_MAX_DIZANAS_TROUVER:
        case DIZANAS_QUIVER_INFINITE_TROUVER:case DIZANAS_QUIVER_CHARGED:case DIZANAS_QUIVER_CHARGED_TROUVER:return DIZANAS_QUIVER_INFINITE;
        case DIZANAS_QUIVER_UNCHARGED_TROUVER:return DIZANAS_QUIVER_UNCHARGED;
        case DEADMAN_VOIDWAKER:return VOIDWAKER;
        case FURY_ORNAMENT:return ENCHANTED_ONYX_AMULET;
        case ZENYTE_AMULET_ORNAMENT:return ZENYTE_AMULET_ENCHANTED;
        case ZENYTE_NECKLACE_ORNAMENT:return ZENYTE_NECKLACE_ENCHANTED;
        case ZENYTE_BRACELET_ENCHANTED_ORNAMENT:return ZENYTE_BRACELET_ENCHANTED;
        case OCCULT_NECKLACE_ORNAMENT:return OCCULT_NECKLACE;
        case ELIDINIS_WARD_ORNAMENT:return ELIDINIS_WARD_FORTIFIED;
        case DRAGON_SCIMITAR_ORNAMENT:return DRAGON_SCIMITAR;
        case DRAGON_CLAWS_ORNAMENT:return DRAGON_CLAWS;
        case ANCESTRAL_HAT_TWISTED:return ANCESTRAL_HAT;
        case ANCESTRAL_ROBE_TOP_TWISTED:return ANCESTRAL_ROBE_TOP;
        case ANCESTRAL_ROBE_BOTTOM_TWISTED:return ANCESTRAL_ROBE_BOTTOM;
        case GHRAZI_RAPIER_OR:return GHRAZI_RAPIER;
        case SANGUINESTI_STAFF_OR:return SANGUINESTI_STAFF;
        case SCYTHE_OF_VITUR_OR:case SCYTHE_OF_VITUR_BL:return SCYTHE_OF_VITUR;
        case SCYTHE_OF_VITUR_UNCHARGED_OR:case SCYTHE_OF_VITUR_UNCHARGED_BL:return SCYTHE_OF_VITUR_UNCHARGED;
        case TOXIC_BLOWPIPE_LOADED_ORNAMENT:return TOXIC_BLOWPIPE_LOADED;
        case TOXIC_BLOWPIPE_ORNAMENT:return TOXIC_BLOWPIPE;
        case TORVA_HELM_SANGUINE:return TORVA_HELM;
        case TORVA_CHEST_SANGUINE:return TORVA_CHEST;
        case TORVA_LEGS_SANGUINE:return TORVA_LEGS;
        case RADIANT_OATHPLATE_HELM:return OATHPLATE_HELM;
        case RADIANT_OATHPLATE_CHEST:return OATHPLATE_CHEST;
        case RADIANT_OATHPLATE_LEGS:return OATHPLATE_LEGS;
        default:return id;
    }}
    /** Charged/blessed may replace an uncharged quiver, never the reverse. */
    static boolean alternative(int owned,int expected){return owned>0&&expected>0&&(owned==DIVINE_RUNE_POUCH&&expected==BH_RUNE_POUCH||combatBase(owned)==combatBase(expected)||combatBase(expected)==DIZANAS_QUIVER_UNCHARGED&&combatBase(owned)==DIZANAS_QUIVER_INFINITE);}
    /** Locked cape requirements remain exact unless the player chooses a replacement. */
    static int base(int id){switch(id){
        case TORVA_HELM_SANGUINE:return TORVA_HELM;
        case TORVA_CHEST_SANGUINE:return TORVA_CHEST;
        case TORVA_LEGS_SANGUINE:return TORVA_LEGS;
        case RADIANT_OATHPLATE_HELM:return OATHPLATE_HELM;
        case RADIANT_OATHPLATE_CHEST:return OATHPLATE_CHEST;
        case RADIANT_OATHPLATE_LEGS:return OATHPLATE_LEGS;
        case DEADMAN_VOIDWAKER:return VOIDWAKER;
        default:return id;
    }}
    static boolean same(int a,int b){return a>0&&b>0&&base(a)==base(b);}
}
