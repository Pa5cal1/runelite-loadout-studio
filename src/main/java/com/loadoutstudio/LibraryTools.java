/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;

/** Source-only library operations, applied to validated copies by the panel. */
final class LibraryTools
{
    static boolean matches(SetupLibrary.Entry entry,String query,java.util.function.BiPredicate<Integer,String> names)
    {
        String term=query.toLowerCase(Locale.ROOT);JsonObject setup=SetupCodec.setup(entry.portable);
        if(term.startsWith("notes:"))return setup.has("notes") && !setup.get("notes").isJsonNull() && setup.get("notes").getAsString().toLowerCase(Locale.ROOT).contains(term.substring(6).trim());
        if(term.startsWith("item:")){String item=term.substring(5).trim();for(JsonElement e:BankLayouts.items(entry.portable)){JsonObject obj=e.getAsJsonObject();if(String.valueOf(SetupCodec.id(e)).equals(item) || names.test(SetupCodec.id(e),item) || obj.has("label")&&obj.get("label").getAsString().toLowerCase(Locale.ROOT).contains(item))return true;}return false;}
        return SetupCodec.name(entry.portable).toLowerCase(Locale.ROOT).contains(term);
    }
    static int replace(JsonObject portable,int oldId,int newId,String name)
    {
        if(oldId<1||newId<1||oldId==newId)throw new IllegalArgumentException("Choose two different valid items.");
        JsonObject extras=SetupCodec.setup(portable).has("afi")&&SetupCodec.setup(portable).get("afi").isJsonObject()?SetupCodec.setup(portable).getAsJsonObject("afi"):new JsonObject();if(extras.has(String.valueOf(oldId))&&extras.has(String.valueOf(newId)))throw new IllegalArgumentException("Additional filtered items already contain the replacement.");
        int changed=0;
        for(JsonElement e:BankLayouts.items(portable))if(SetupCodec.id(e)==oldId){e.getAsJsonObject().addProperty("id",newId);e.getAsJsonObject().addProperty("label",name);changed++;}
        JsonObject normalized=new JsonObject();for(Map.Entry<String,JsonElement> entry:extras.entrySet())if(!entry.getValue().isJsonNull())normalized.add(String.valueOf(SetupCodec.id(entry.getValue())),entry.getValue());SetupCodec.setup(portable).add("afi",normalized);
        JsonArray layout=portable.getAsJsonArray("layout");for(int i=0;i<layout.size();i++)if(layout.get(i).getAsInt()==oldId)layout.set(i,new JsonPrimitive(newId));
        SetupCodec.validate(portable,false);return changed;
    }
    static JsonObject bundle(SetupLibrary source,Set<String> ids,Gson gson)
    {
        SetupLibrary copy=new SetupLibrary();Set<String> sections=new HashSet<>();
        for(SetupLibrary.Entry e:source.setups)if(ids.contains(e.id)){copy.setups.add(gson.fromJson(gson.toJson(e),SetupLibrary.Entry.class));if(e.section!=null)sections.add(e.section);}
        if(copy.setups.isEmpty())throw new IllegalArgumentException("Choose at least one saved setup.");
        for(SetupLibrary.Section section:source.sections)if(sections.contains(section.id))copy.sections.add(gson.fromJson(gson.toJson(section),SetupLibrary.Section.class));
        JsonObject result=gson.toJsonTree(copy).getAsJsonObject();result.addProperty("libraryBundle",1);copy.validate();return result;
    }
    static void merge(SetupLibrary target,SetupLibrary incoming,Gson gson)
    {
        incoming.validate();Map<String,String> mapping=new HashMap<>();
        for(SetupLibrary.Section section:incoming.sections){SetupLibrary.Section same=target.sections.stream().filter(e->e.name.equals(section.name)).findFirst().orElse(null);if(same==null){same=new SetupLibrary.Section(section.name);target.sections.add(same);}mapping.put(section.id,same.id);}
        for(SetupLibrary.Entry e:incoming.setups){SetupLibrary.Entry copy=gson.fromJson(gson.toJson(e),SetupLibrary.Entry.class);copy.id=UUID.randomUUID().toString();copy.section=mapping.get(e.section);copy.lastOpened=0;target.setups.add(copy);}target.validate();
    }
}
