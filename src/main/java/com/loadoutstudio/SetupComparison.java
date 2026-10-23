/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;
import java.util.function.IntUnaryOperator;

/** Pure comparison; never reads the game, mutates a draft or performs game actions. */
final class SetupComparison
{
    static boolean matches(JsonElement expected,JsonElement actual,boolean ignoreQuantity,IntUnaryOperator variation)
    {
        int wanted=SetupCodec.id(expected),present=SetupCodec.id(actual);
        if(wanted<1 || present<1)return wanted==present && wanted!=0;
        JsonObject item=expected.getAsJsonObject();
        if(item.has("f") && item.get("f").getAsBoolean()){wanted=variation.applyAsInt(wanted);present=variation.applyAsInt(present);}
        if(wanted!=present)return false;
        if(ignoreQuantity || SetupCodec.unlimited(expected))return true;
        if(item.has("q") && item.get("q").isJsonNull())return false;
        String mode=item.has("sc")?item.get("sc").getAsString():"None";
        int comparison=Integer.compare(SetupCodec.quantity(actual),SetupCodec.quantity(expected));
        switch(mode)
        {
            case "None":return true;
            case "Standard":return comparison==0;
            case "Less_Than":return comparison>=0;
            case "Greater_Than":return comparison<=0;
            default:return false;
        }
    }
    static Set<String> differences(JsonObject expected,JsonObject actual,boolean ignoreAmmo,IntUnaryOperator variation)
    {
        Set<String> result=new LinkedHashSet<>();JsonObject saved=SetupCodec.setup(expected),current=SetupCodec.setup(actual);
        for(String key:new String[]{"eq","inv","rp","qv","bp"})
        {
            if(key.equals("bp") && (!saved.has(key)||!saved.get(key).isJsonArray()))continue;
            int length=key.equals("eq")?14:key.equals("inv")?28:key.equals("rp")||key.equals("bp")?4:1;
            JsonArray wanted=saved.has(key)&&saved.get(key).isJsonArray()?saved.getAsJsonArray(key):new JsonArray();
            JsonArray present=current.has(key)&&current.get(key).isJsonArray()?current.getAsJsonArray(key):new JsonArray();
            if(key.equals("inv") && saved.has("uh") && saved.get("uh").getAsBoolean())
            {
                int[] owners=new int[length];Arrays.fill(owners,-1);
                // Maximum matching prevents a fuzzy slot stealing the only match for an exact slot.
                for(int i=0;i<length;i++)if(!at(wanted,i).isJsonNull() && !assign(i,wanted,present,owners,new boolean[length],variation))result.add(key+":"+i);
            }
            else for(int i=0;i<length;i++)if(!matches(at(wanted,i),at(present,i),ignoreAmmo&&(key.equals("qv") || key.equals("eq")&&i==13),variation))result.add(key+":"+i);
        }
        if(saved.has("sb") && saved.get("sb").getAsInt()!=4 && current.has("sb") && !saved.get("sb").equals(current.get("sb")))result.add("spellbook");
        if(saved.has("ao") && !saved.get("ao").isJsonNull() && !saved.get("ao").getAsString().isEmpty() && (!current.has("ao") || !saved.get("ao").equals(current.get("ao"))))result.add("attackOption");
        return result;
    }
    private static boolean assign(int expected,JsonArray wanted,JsonArray present,int[] owners,boolean[] seen,IntUnaryOperator variation)
    {
        for(int i=0;i<owners.length;i++)if(!seen[i] && matches(at(wanted,expected),at(present,i),false,variation))
        {
            seen[i]=true;
            if(owners[i]<0 || assign(owners[i],wanted,present,owners,seen,variation)){owners[i]=expected;return true;}
        }
        return false;
    }
    private static JsonElement at(JsonArray array,int i){return i<array.size()?array.get(i):JsonNull.INSTANCE;}
}
