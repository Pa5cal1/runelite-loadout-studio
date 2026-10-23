/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;

/** Versioned, bounded snapshot in this plugin's own profile-scoped configuration. */
final class DraftRecovery
{
    static String encode(JsonObject portable,String editingId,String section,boolean favorite,BankLayouts.Mode mode,boolean bottom,boolean ignoreAmmo,Gson gson)
    {
        JsonObject check=portable.deepCopy();SetupCodec.setup(check).addProperty("name","Recovery validation");SetupCodec.validate(check,false);
        JsonObject value=new JsonObject();value.addProperty("version",1);value.addProperty("savedAt",System.currentTimeMillis());value.add("portable",portable.deepCopy());value.addProperty("editingId",editingId);value.addProperty("section",section);value.addProperty("favorite",favorite);value.addProperty("layout",mode.name());value.addProperty("bottom",bottom);value.addProperty("ignoreAmmo",ignoreAmmo);
        String text=gson.toJson(value);if(text.length()>200000)throw new IllegalArgumentException("Draft recovery exceeds its size limit. Save or export the setup.");return text;
    }
    static JsonObject decode(String text,Gson gson)
    {
        if(text==null || text.isEmpty())return null;
        if(text.length()>200000)throw new IllegalArgumentException("Draft recovery is too large.");
        JsonObject value=new SetupCodec(gson).parse(text).getAsJsonObject();if(value.get("version").getAsInt()!=1)throw new IllegalArgumentException("Unsupported draft recovery.");
        for(String flag:new String[]{"favorite","bottom","ignoreAmmo"})SetupCodec.validateBoolean(value,flag,true);
        JsonObject copy=value.getAsJsonObject("portable").deepCopy();JsonObject setup=SetupCodec.setup(copy);
        // A transient empty/long name may occur while typing. Validate the remainder without changing the snapshot.
        setup.addProperty("name","Recovery validation");SetupCodec.validate(copy,false);
        String name=SetupCodec.name(value.getAsJsonObject("portable"));if(name.length()>1000)throw new IllegalArgumentException("Invalid recovery name.");
        BankLayouts.Mode.valueOf(value.get("layout").getAsString());return value;
    }
    static String optional(JsonObject object,String key){return !object.has(key)||object.get(key).isJsonNull()?null:object.get(key).getAsString();}
}
