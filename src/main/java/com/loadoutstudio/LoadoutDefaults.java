/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;

/** Applied only at creation, never to existing library entries or imported source data. */
final class LoadoutDefaults
{
    static JsonObject blank(LoadoutStudioConfig settings){return captured(SetupCodec.blank("New loadout"),settings);}
    static JsonObject captured(JsonObject source,LoadoutStudioConfig settings)
    {
        JsonObject copy=source.deepCopy(),setup=SetupCodec.setup(copy);setup.addProperty("fb",settings.defaultFilterBank());
        setup.addProperty("hd",settings.defaultHighlight());setup.addProperty("hc",String.format("#FF%06X",settings.defaultHighlightColor().getRGB()&0xFFFFFF));setup.addProperty("uh",settings.defaultMissingOnly());
        if(!settings.captureAttackOption())setup.remove("ao");
        for(JsonElement item:BankLayouts.items(copy)){
            if(settings.defaultFuzzy())item.getAsJsonObject().addProperty("f",true);
            if(settings.defaultStackRule()!=LoadoutStudioConfig.StackRule.NONE)item.getAsJsonObject().addProperty("sc",settings.defaultStackRule().code);
        }
        copy.add("layout",BankLayouts.generate(copy,settings.defaultLayout()==LoadoutStudioConfig.DefaultLayout.ZIGZAG?BankLayouts.Mode.ZIGZAG:BankLayouts.Mode.PRESET,settings.defaultZigzagBottom()));return copy;
    }
}
