/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import static net.runelite.api.gameval.ItemID.*;
/** Usable cosmetic/combat equivalence; exact stored/exported IDs are never rewritten. */
final class EquipmentVariants {
    static boolean runePouch(int id){return net.runelite.client.game.ItemVariationMapping.map(id)==BH_RUNE_POUCH;}
    static boolean divinePouch(int id){return net.runelite.client.game.ItemVariationMapping.map(id)==DIVINE_RUNE_POUCH;}
    /** Separate imbued combat functionality from cosmetic colours and imbue sources. */
    static int slayerBase(int id){switch(id){
        case SLAYER_HELM_I:
        case SLAYER_HELM_I_BLACK:
        case SLAYER_HELM_I_GREEN:
        case SLAYER_HELM_I_RED:
        case SLAYER_HELM_I_PURPLE:
        case SLAYER_HELM_I_TURQUOISE:
        case SLAYER_HELM_I_HYDRA:
        case SLAYER_HELM_I_TWISTED:
        case SW_SLAYER_HELM_I:
        case SW_SLAYER_HELM_I_BLACK:
        case SW_SLAYER_HELM_I_GREEN:
        case SW_SLAYER_HELM_I_RED:
        case SW_SLAYER_HELM_I_PURPLE:
        case SW_SLAYER_HELM_I_TURQUOISE:
        case SW_SLAYER_HELM_I_HYDRA:
        case SW_SLAYER_HELM_I_TWISTED:
        case SLAYER_HELM_I_JAD:
        case SW_SLAYER_HELM_I_JAD:
        case SLAYER_HELM_I_VERZIK:
        case SW_SLAYER_HELM_I_VERZIK:
        case SLAYER_HELM_I_ZUK:
        case SW_SLAYER_HELM_I_ZUK:
        case PVPA_SLAYER_HELM_I:
        case PVPA_SLAYER_HELM_I_BLACK:
        case PVPA_SLAYER_HELM_I_GREEN:
        case PVPA_SLAYER_HELM_I_RED:
        case PVPA_SLAYER_HELM_I_PURPLE:
        case PVPA_SLAYER_HELM_I_TURQUOISE:
        case PVPA_SLAYER_HELM_I_HYDRA:
        case PVPA_SLAYER_HELM_I_TWISTED:
        case PVPA_SLAYER_HELM_I_JAD:
        case PVPA_SLAYER_HELM_I_VERZIK:
        case PVPA_SLAYER_HELM_I_ZUK:
        case SLAYER_HELM_I_ARAXYTE:
        case SW_SLAYER_HELM_I_ARAXYTE:
        case PVPA_SLAYER_HELM_I_ARAXYTE:
        case SLAYER_HELM_I_HOODED:
        case SW_SLAYER_HELM_I_HOODED:
        case PVPA_SLAYER_HELM_I_HOODED:
        case LEAGUE_6_SLAYER_HELM1_I:
        case SW_LEAGUE_6_SLAYER_HELM1_I:
        case PVPA_LEAGUE_6_SLAYER_HELM1_I:
        case LEAGUE_6_SLAYER_HELM2_I:
        case SW_LEAGUE_6_SLAYER_HELM2_I:
        case PVPA_LEAGUE_6_SLAYER_HELM2_I:
            return SLAYER_HELM_I;
        default:return net.runelite.client.game.ItemVariationMapping.map(id)==SLAYER_HELM?SLAYER_HELM:id;
    }}

    static int combatBase(int id){int slayer=slayerBase(id);if(slayer!=id)return slayer;switch(id){
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
    static boolean alternative(int owned,int expected){
        if(owned<1||expected<1)return false;
        if(divinePouch(owned)&&runePouch(expected))return true;
        int actual=combatBase(owned),wanted=combatBase(expected);
        if(actual==wanted)return true;
        if(actual==SLAYER_HELM||actual==SLAYER_HELM_I||wanted==SLAYER_HELM||wanted==SLAYER_HELM_I)return false;
        if(actual==ELIDINIS_WARD||wanted==ELIDINIS_WARD)return false;
        if(wanted==DIZANAS_QUIVER_UNCHARGED&&actual==DIZANAS_QUIVER_INFINITE)return true;
        if(wanted==DIZANAS_QUIVER_INFINITE&&actual==DIZANAS_QUIVER_UNCHARGED)return false;
        return net.runelite.client.game.ItemVariationMapping.map(actual)==net.runelite.client.game.ItemVariationMapping.map(wanted);
    }
    /** Locked cape requirements remain exact unless the player chooses a replacement. */
    static int base(int id){int slayer=slayerBase(id);if(slayer!=id)return slayer;if(runePouch(id))return BH_RUNE_POUCH;if(divinePouch(id))return DIVINE_RUNE_POUCH;switch(id){
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
