/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExistingSetupReaderTest
{
    static class Config implements ExistingSetupReader.Source
    {
        final Map<String,String> values=new LinkedHashMap<>();int reads;
        public List<String> keys(String group,String prefix){assertTrue(Arrays.asList("inventorysetups","banktags","banktaglayouts").contains(group));List<String> result=new ArrayList<>();for(String key:values.keySet())if(key.startsWith(group+"."+prefix))result.add(key.substring(group.length()+1));return result;}
        public String get(String group,String key){reads++;assertTrue(Arrays.asList("inventorysetups","banktags","banktaglayouts").contains(group));return values.get(group+"."+key);}
    }
    @Test public void readsInventorySetupsAndNativeBankTagsWithoutWritingOrAliasing()
    {
        Config config=new Config();JsonObject original=SetupCodec.blank("Boss Melee"),setup=SetupCodec.setup(original);setup.addProperty("fv",true);setup.addProperty("notes","Keep the original notes");setup.addProperty("sb",1);JsonObject weapon=SetupCodec.item(100,1);weapon.addProperty("f",true);setup.getAsJsonArray("eq").set(3,weapon);setup.getAsJsonArray("inv").set(0,SetupCodec.item(560,200000));
        config.values.put("inventorysetups.setupsV3_abc",RuneLiteAPI.GSON.toJson(setup));config.values.put("inventorysetups.sections","[{\"name\":\"PvM\",\"setups\":[\"Boss Melee\"]}]");config.values.put("banktags.layout__invsetup_abc","100,-1,560");config.values.put("banktags.tagtabs","boss");config.values.put("banktags.item_100","boss");config.values.put("banktags.item_-560","boss");config.values.put("banktags.layout_boss","100,-1,560,100");Map<String,String> before=new LinkedHashMap<>(config.values);
        ExistingSetupReader.Result result=new ExistingSetupReader(RuneLiteAPI.GSON,config).read();assertTrue(result.warnings.toString(),result.warnings.isEmpty());assertEquals(2,result.entries.size());ExistingSetupReader.Entry inventory=result.entries.get(0),bank=result.entries.get(1);assertEquals("PvM",inventory.section);assertTrue(inventory.favorite);assertEquals(setup,SetupCodec.setup(inventory.portable));assertEquals(100,inventory.portable.getAsJsonArray("layout").get(0).getAsInt());assertEquals(560,inventory.portable.getAsJsonArray("layout").get(2).getAsInt());assertEquals("Bank Tag",bank.source);assertEquals(100,bank.portable.getAsJsonArray("layout").get(3).getAsInt());assertTrue(SetupCodec.setup(bank.portable).getAsJsonArray("eq").get(3).isJsonNull());assertTrue(SetupCodec.setup(bank.portable).getAsJsonArray("inv").get(1).getAsJsonObject().get("f").getAsBoolean());SetupCodec.setup(inventory.portable).addProperty("name","Changed copy");assertEquals(before,config.values);
    }
    @Test public void malformedEntryDoesNotHideGoodSetupsAndLegacyLayoutsStayAvailable()
    {
        Config c=new Config();c.values.put("inventorysetups.setupsV3_bad","{broken");c.values.put("inventorysetups.setupsV3_good",RuneLiteAPI.GSON.toJson(SetupCodec.setup(SetupCodec.blank("Good"))));c.values.put("banktags.tagtabs","legacy,_invsetup_internal,loadoutstudio_private");c.values.put("banktaglayouts.layout_legacy","100:3,560:6");c.values.put("banktags.item_100","legacy");ExistingSetupReader.Result result=new ExistingSetupReader(RuneLiteAPI.GSON,c).read();assertEquals(2,result.entries.size());assertFalse(result.warnings.isEmpty());assertEquals(100,result.entries.get(1).portable.getAsJsonArray("layout").get(3).getAsInt());assertEquals(560,result.entries.get(1).portable.getAsJsonArray("layout").get(6).getAsInt());
    }
    @Test public void portableV2RemainsReadable()
    {
        Config c=new Config();c.values.put("inventorysetups.setupsV2","["+RuneLiteAPI.GSON.toJson(SetupCodec.setup(SetupCodec.blank("Older setup")))+"]");assertEquals("Older setup",SetupCodec.name(new ExistingSetupReader(RuneLiteAPI.GSON,c).read().entries.get(0).portable));
    }
    @Test public void disabledLegacyLayoutDoesNotDiscardTagMembership()
    {
        Config c=new Config();c.values.put("banktags.tagtabs","plain");c.values.put("banktags.item_100","plain");c.values.put("banktaglayouts.layout_plain","DISABLED");ExistingSetupReader.Result result=new ExistingSetupReader(RuneLiteAPI.GSON,c).read();assertTrue(result.warnings.toString(),result.warnings.isEmpty());assertEquals(1,result.entries.size());assertEquals(100,SetupCodec.id(SetupCodec.setup(result.entries.get(0).portable).getAsJsonArray("inv").get(0)));
    }

}
