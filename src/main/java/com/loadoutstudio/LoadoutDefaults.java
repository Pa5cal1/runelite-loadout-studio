/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;

/** Applied at creation, including generated Wiki loadouts; explicit imports keep their values. */
final class LoadoutDefaults
{
    static JsonObject blank(LoadoutStudioConfig settings){return captured(SetupCodec.blank("New loadout"),settings);}
    static JsonObject captured(JsonObject source,LoadoutStudioConfig settings)
    {
        JsonObject copy=created(source,settings);if(!settings.captureAttackOption())SetupCodec.setup(copy).remove("ao");return copy;
    }
    static JsonObject created(JsonObject source,LoadoutStudioConfig settings)
    {
        JsonObject copy=source.deepCopy(),setup=SetupCodec.setup(copy);setup.addProperty("fb",settings.defaultFilterBank());
        setup.addProperty("hd",settings.defaultHighlight());setup.addProperty("hc",String.format("#FF%06X",settings.defaultHighlightColor().getRGB()&0xFFFFFF));setup.addProperty("uh",settings.defaultMissingOnly());
        for(JsonElement item:BankLayouts.items(copy)){
            newItem(item.getAsJsonObject(),settings);
        }
        copy.add("layout",BankLayouts.generate(copy,settings.defaultLayout()==LoadoutStudioConfig.DefaultLayout.ZIGZAG?BankLayouts.Mode.ZIGZAG:BankLayouts.Mode.PRESET,settings.defaultZigzagBottom()));return copy;
    }
    static void newItem(JsonObject item,LoadoutStudioConfig settings){item.addProperty("f",settings.defaultFuzzy());item.addProperty("sc",settings.defaultStackRule().code);}
}
