/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;
import net.runelite.client.util.Text;
import net.runelite.api.gameval.ItemID;

/** Optional read-only import bridge. The interface deliberately has no write methods. */
final class ExistingSetupReader
{
    interface Source { List<String> keys(String group,String prefix); String get(String group,String key); }
    static final class Entry
    {
        final String source,section;final JsonObject portable;final boolean favorite;
        Entry(String source,String section,JsonObject portable,boolean favorite){this.source=source;this.section=section;this.portable=portable.deepCopy();this.favorite=favorite;}
        public String toString(){return SetupCodec.name(portable)+" · "+source;}
    }
    static final class Result
    {
        final List<Entry> entries=new ArrayList<>();final List<String> warnings=new ArrayList<>();
    }
    private int remaining=10_000_000;
    private final SetupCodec codec;private final Source source;
    ExistingSetupReader(Gson gson,Source source){this.codec=new SetupCodec(gson);this.source=source;}
    Result read()
    {
        Result result=new Result();Map<String,String> sections=sections(result);
        List<String> keys=new ArrayList<>(source.keys("inventorysetups","setupsV3_"));Collections.sort(keys);
        if(keys.size()>500){result.warnings.add("Only the first 500 Inventory Setups are listed.");keys=keys.subList(0,500);}
        for(String key:keys)try{String hash=key.substring("setupsV3_".length());addInventory(parse(read("inventorysetups",key)),hash,sections,result);}catch(RuntimeException ex){result.warnings.add("An incompatible Inventory Setup was skipped; its original is unchanged.");}
        if(keys.isEmpty())
        {
            String old=read("inventorysetups","setupsV2");
            if(old!=null)try{JsonArray list=parse(old).getAsJsonArray();if(list.size()>500)throw new IllegalArgumentException();for(JsonElement setup:list)addInventory(setup,null,sections,result);}catch(RuntimeException ex){result.warnings.add("Legacy Inventory Setups could not all be read. Use their JSON export if needed.");}
            else if(read("inventorysetups","setups")!=null)result.warnings.add("Legacy V1 Inventory Setups need migration in Inventory Setups or a portable JSON export.");
        }
        Map<String,List<Integer>> membership=new TreeMap<>();
        for(String tag:csv(read("banktags","tagtabs")))if(publicTag(tag))membership.putIfAbsent(tag,new ArrayList<>());
        List<String> itemKeys=source.keys("banktags","item_");
        if(itemKeys.size()>20000){result.warnings.add("Bank Tag data is unusually large; import a specific exported tag instead.");return result;}
        for(String key:itemKeys)try
        {
            int id=Integer.parseInt(key.substring(5));if(id==0 || id==Integer.MIN_VALUE)continue;
            for(String tag:csv(read("banktags",key)))if(publicTag(tag))membership.computeIfAbsent(tag,k->new ArrayList<>()).add(id);
        }catch(RuntimeException ex){result.warnings.add("An invalid Bank Tag item entry was skipped.");}
        for(String key:source.keys("banktaglayouts","layout_")){String tag=key.substring(7);if(publicTag(tag))membership.putIfAbsent(tag,new ArrayList<>());}
        int count=0;
        for(Map.Entry<String,List<Integer>> tag:membership.entrySet())
        {
            if(++count>500){result.warnings.add("Only the first 500 Bank Tags are listed.");break;}
            try
            {
                String name=tag.getKey(),icon=read("banktags","icon_"+name);int iconId=icon==null?ItemID.SPADE:Integer.parseInt(icon);if(iconId<1)iconId=ItemID.SPADE;
                String core=read("banktags","layout_"+name),legacy=read("banktaglayouts","layout_"+name);StringBuilder text=new StringBuilder();
                if(core==null && legacy!=null && !legacy.isEmpty() && !legacy.equalsIgnoreCase("disabled"))
                {
                    text.append("banktaglayoutsplugin:").append(name).append(',').append(legacy).append(",banktag:").append(name).append(',').append(iconId);
                }
                else text.append("banktags,1,").append(quote(name)).append(',').append(iconId);
                for(int id:tag.getValue())text.append(',').append(id);
                if(core!=null && !core.isEmpty())
                {
                    List<String> cells=csv(core);if(cells.size()>8192)throw new IllegalArgumentException();text.append(",layout");
                    for(int i=0;i<cells.size();i++){int id=Integer.parseInt(cells.get(i));if(id>0)text.append(',').append(i).append(',').append(id);else if(id!=-1)throw new IllegalArgumentException();}
                }
                JsonObject portable=codec.importText(text.toString()).get(0);result.entries.add(new Entry("Bank Tag",null,portable,false));
            }
            catch(RuntimeException ex){result.warnings.add("An incompatible Bank Tag was skipped; its original is unchanged.");}
        }
        return result;
    }
    private void addInventory(JsonElement raw,String hash,Map<String,String> sections,Result result)
    {
        JsonObject portable;
        if(raw.getAsJsonObject().has("setup"))portable=raw.getAsJsonObject().deepCopy();
        else
        {
            portable=new JsonObject();portable.add("setup",raw.deepCopy());JsonObject setup=SetupCodec.setup(portable);
            if(!setup.has("sb") || setup.get("sb").isJsonNull())setup.addProperty("sb",0);
            if(!setup.has("hc") || setup.get("hc").isJsonNull())setup.addProperty("hc","#FFFF0000");
            for(String key:new String[]{"rp","qv"})if(!setup.has(key)||setup.get(key).isJsonNull())setup.add(key,SetupCodec.empty(key.equals("rp")?4:1));
            if(!setup.has("afi")||setup.get("afi").isJsonNull())setup.add("afi",new JsonObject());
            if(!setup.has("notes")||setup.get("notes").isJsonNull())setup.addProperty("notes","");
            SetupCodec.rebuildLayout(portable);
            if(hash!=null)
            {
                String layout=read("banktags","layout__invsetup_"+hash);
                if(layout!=null){JsonArray cells=new JsonArray();for(String cell:csv(layout))cells.add(Integer.parseInt(cell));portable.add("layout",cells);portable.add("layout",BankLayouts.generate(portable,BankLayouts.Mode.CUSTOM,false));}
                else
                {
                    String escaped=SetupCodec.name(portable).replace("&","&amp;").replace(":","&#58;");String legacy=read("banktaglayouts","inventory_setups_layout_"+escaped);
                    if(legacy!=null && !legacy.isEmpty() && !legacy.equalsIgnoreCase("disabled")){JsonArray cells=new JsonArray();for(String pair:legacy.split(",")){String[] p=pair.split(":");if(p.length!=2)throw new IllegalArgumentException();int id=Integer.parseInt(p[0]),index=Integer.parseInt(p[1]);if(index<0 || index>8191 || id<1)throw new IllegalArgumentException();while(cells.size()<=index)cells.add(-1);cells.set(index,new JsonPrimitive(id));}portable.add("layout",cells);portable.add("layout",BankLayouts.generate(portable,BankLayouts.Mode.CUSTOM,false));}
                }
            }
        }
        JsonObject setup=SetupCodec.setup(portable);
        for(String key:new String[]{"rp","qv"})if(!setup.has(key)||setup.get(key).isJsonNull())setup.add(key,SetupCodec.empty(key.equals("rp")?4:1));
        if(!setup.has("afi")||setup.get("afi").isJsonNull())setup.add("afi",new JsonObject());
        if(!setup.has("notes")||setup.get("notes").isJsonNull())setup.addProperty("notes","");
        if(!setup.has("sb")||setup.get("sb").isJsonNull())setup.addProperty("sb",0);
        SetupCodec.validate(portable,false);boolean favorite=setup.has("fv")&&!setup.get("fv").isJsonNull()&&setup.get("fv").getAsBoolean();
        result.entries.add(new Entry("Inventory Setups",sections.get(SetupCodec.name(portable)),portable,favorite));
    }
    private Map<String,String> sections(Result result)
    {
        Map<String,String> resultMap=new HashMap<>();String value=read("inventorysetups","sections");if(value==null)return resultMap;
        try{JsonArray groups=parse(value).getAsJsonArray();if(groups.size()>500)throw new IllegalArgumentException();for(JsonElement e:groups){JsonObject group=e.getAsJsonObject();String name=group.get("name").getAsString();if(name.isEmpty()||name.length()>60)continue;JsonArray setups=group.getAsJsonArray("setups");if(setups.size()>500)continue;for(JsonElement setup:setups)resultMap.putIfAbsent(setup.getAsString(),name);}}
        catch(RuntimeException ex){result.warnings.add("Inventory Setups sections could not be read; setups can still be copied.");}return resultMap;
    }
    private JsonElement parse(String value){return codec.parse(value);}
    private String read(String group,String key){String value=source.get(group,key);if(value!=null && (value.length()>SetupCodec.MAX_TEXT || (remaining-=value.length())<0))throw new IllegalArgumentException("Stored setup is too large.");return value;}
    private static List<String> csv(String value){return value==null || value.isEmpty()?Collections.emptyList():Text.fromCSV(value);}
    private static String quote(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private static boolean publicTag(String value){return value!=null&&!value.isEmpty()&&value.length()<=50&&!value.startsWith("_invsetup_")&&!value.startsWith("loadoutstudio_");}
}
