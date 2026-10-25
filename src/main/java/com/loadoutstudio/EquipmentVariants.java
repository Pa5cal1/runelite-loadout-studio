/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import static net.runelite.api.gameval.ItemID.*;
/** Cosmetic armour equivalence only; exact stored/exported IDs are never rewritten. */
final class EquipmentVariants {
    static int base(int id){switch(id){
        case TORVA_HELM_SANGUINE:return TORVA_HELM;
        case TORVA_CHEST_SANGUINE:return TORVA_CHEST;
        case TORVA_LEGS_SANGUINE:return TORVA_LEGS;
        case RADIANT_OATHPLATE_HELM:return OATHPLATE_HELM;
        case RADIANT_OATHPLATE_CHEST:return OATHPLATE_CHEST;
        case RADIANT_OATHPLATE_LEGS:return OATHPLATE_LEGS;
        default:return id;
    }}
    static boolean same(int a,int b){return a>0&&b>0&&base(a)==base(b);}
}
