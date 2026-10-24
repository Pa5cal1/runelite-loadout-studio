/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;

final class ConversionReview
{
    static int count(JsonObject portable,String key)
    {
        JsonElement value=SetupCodec.setup(portable).get(key);if(value==null || value.isJsonNull())return 0;int count=0;
        Iterable<JsonElement> items=value.isJsonArray()?value.getAsJsonArray():value.getAsJsonObject().entrySet().stream().map(java.util.Map.Entry::getValue).collect(java.util.stream.Collectors.toList());
        for(JsonElement item:items)if(SetupCodec.id(item)>0)count++;return count;
    }
    static String inventory(JsonObject p){return "Inventory Setup: "+count(p,"inv")+" Inventory, "+count(p,"eq")+" Equipment, "+count(p,"rp")+" Rune Pouch items. Slots, quantities, notes and matching flags are retained.";}
    static String bank(JsonObject p)
    {
        JsonObject s=SetupCodec.setup(p);return "Bank Tag keeps item membership and Bank layout with normal Bank IDs. RuneLite variant groups can differ from Inventory Setups custom Fuzzy groups. It cannot retain "+count(p,"eq")+" worn slots, "+count(p,"rp")+" Rune Pouch entries, quantities, Spellbook"+(s.has("notes")&&!s.get("notes").isJsonNull()&&!s.get("notes").getAsString().isEmpty()?", Notes":"")+" or Inventory slot assignments. Converting back needs manual review.";
    }
    static String imported(JsonObject p,String origin)
    {
        if(origin.contains("Bank"))return origin+" · "+SetupCodec.name(p)+"\nWorn slots, quantities, Rune Pouch contents and Spellbook are unknown. Review them manually.\n"+count(p,"inv")+" Inventory items; "+count(p,"afi")+" additional items.";
        return origin+" · "+SetupCodec.name(p)+"\n"+count(p,"inv")+" Inventory · "+count(p,"eq")+" Equipment · "+count(p,"rp")+" Rune Pouch items.\nSlots, amounts, Notes and flags are copied.";
    }
}
