/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import com.google.gson.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Small source-shape regression from Slayer_task/Aquanites; no network calls. */
public class RankedWikiAlternativesTest {
 private static String source(){return "<tabber>\nMelee Slash/Stab=\n"+
  "{{Equipment|head=Slayer helmet (i)|torso=Oathplate chest|weapon=Soulreaper axe}}\n"+
  "{{Recommended equipment|style=Melee switch|head1={{plink|Slayer helmet (i)}} / {{plink|Torva full helm}}|head2={{plink|Neitiznot faceguard}} / {{plink|Oathplate helm}}"+
  "|body1={{plink|Oathplate chest}}{{efn|If using a soulreaper axe or scythe of Vitur.|name=oath}} / {{plink|Torva platebody}}"+
  "|body2={{plink|Bandos chestplate}} /<br>{{plink|Blood Moon chestplate}} /<br>{{plink|Fighter torso}}|body3={{plink|Sunfire fanatic cuirass}}|body4={{plink|Proselyte hauberk}}"+
  "|weapon1={{plink|Soulreaper axe}} / {{plink|Scythe of Vitur}}|weapon2={{plink|Ghrazi rapier}} / {{plink|Blade of Saeldor}}{{efn|On the stab attack style.|name=stab}}}}\n"+
  "==Inventory==\n{{Inventory|Dragon claws|Abyssal whip|Prayer potion(4)}}{{Rune pouch|1=Nature rune|2=Fire rune}}\n|-|\nRange=\n"+
  "{{Equipment|head=Slayer helmet (i)|torso=Masori body (f)|weapon=Toxic blowpipe}}\n"+
  "{{Recommended equipment|style=Ranged|body1={{plink|Masori body (f)}}|body2={{plink|Crystal body}}{{efn|If using crystal armour}}|body3={{plink|Karil's leathertop}}|body4={{plink|Black d'hide body}}|weapon1={{plink|Toxic blowpipe}} > {{plink|Twisted bow}}}}\n"+
  "{{Inventory|Shark}}\n</tabber>";}
 private static List<JsonObject> presets(){return new WikiParser().parse(source(),"Slayer task/Aquanites","https://oldschool.runescape.wiki/w/Slayer_task/Aquanites");}
 private static JsonObject body(JsonObject p){return SetupCodec.setup(p).getAsJsonArray("eq").get(4).getAsJsonObject();}
 private static List<String> names(JsonObject item){List<String> n=new ArrayList<>();for(JsonElement e:item.getAsJsonArray("choices"))n.add(e.getAsString());return n;}
 @Test public void concreteSetupsRetainAdjacentRankedTablesAndInventory(){List<JsonObject> p=presets();assertEquals(2,p.size());assertEquals("Melee Slash/Stab",WikiParser.variant(p.get(0)));assertEquals(Arrays.asList("Oathplate chest","Torva platebody","Bandos chestplate","Blood Moon chestplate","Fighter torso","Sunfire fanatic cuirass","Proselyte hauberk"),names(body(p.get(0))));assertEquals("Dragon claws",SetupCodec.setup(p.get(0)).getAsJsonArray("inv").get(0).getAsJsonObject().get("label").getAsString());assertEquals("Nature rune",SetupCodec.setup(p.get(0)).getAsJsonArray("rp").get(0).getAsJsonObject().get("label").getAsString());assertTrue(SetupCodec.setup(p.get(0)).get("notes").getAsString().contains("If using a soulreaper axe"));}
 @Test public void variantsDoNotShareAlternativesEvenForSameSlot(){List<JsonObject> p=presets();assertEquals(Arrays.asList("Masori body (f)","Crystal body","Karil's leathertop","Black d'hide body"),names(body(p.get(1))));assertFalse(names(body(p.get(0))).contains("Crystal body"));assertFalse(names(body(p.get(1))).contains("Torva platebody"));}
 @Test public void observedChoicesFollowWikiOrderRatherThanBankOrder(){JsonObject p=presets().get(0);body(p).addProperty("id",1);Map<Integer,Integer> counts=new LinkedHashMap<>();Map<Integer,StudioPanel.ItemInfo> items=new LinkedHashMap<>();for(int id:new int[]{5,4,3,2,9}){counts.put(id,1);String name=id==2?"Torva platebody":id==3?"Bandos chestplate":id==4?"Blood Moon chestplate":id==5?"Fighter torso":"Ancestral robe top";items.put(id,new StudioPanel.ItemInfo(id,name,false,4,null));}List<OwnedGear.Proposal> proposals=new OwnedGear(counts,items,123,true).alternatives(p,name->null);assertEquals(1,proposals.size());List<Integer> ids=new ArrayList<>();for(StudioPanel.ItemInfo i:proposals.get(0).choices)ids.add(i.id);assertEquals(Arrays.asList(2,3,4,5),ids);assertTrue(proposals.get(0).reason.startsWith("Wiki recommendation order"));}
 @Test public void authoritativeTableDoesNotFallBackToUnlistedFamilyGear(){JsonObject p=presets().get(1);JsonObject b=body(p);b.addProperty("id",1);b.addProperty("label","Masori body (f)");Map<Integer,StudioPanel.ItemInfo> items=Collections.singletonMap(2,new StudioPanel.ItemInfo(2,"Armadyl chestplate",false,4,null));assertTrue(new OwnedGear(Collections.singletonMap(2,1),items,123,true).alternatives(p,name->null).isEmpty());}
 @Test public void selectedConcreteItemIsNotReplacedByTopRecommendation(){String source="{{Equipment|torso=Bandos chestplate}}{{Recommended equipment|body1={{plink|Torva platebody}}|body2={{plink|Bandos chestplate}}}}{{Inventory|Shark}}";JsonObject b=body(new WikiParser().parse(source,"Boss","Fixture").get(0));assertEquals("Bandos chestplate",WikiParser.defaultName(b));assertEquals("Torva platebody",names(b).get(0));}
 @Test public void tableColumnsKeepTheirOwnRecommendationLists(){String text="{|\n! Melee !! Ranged\n|-\n| {{Equipment|torso=Torva platebody}}{{Recommended equipment|body1={{plink|Torva platebody}}|body2={{plink|Bandos chestplate}}}}{{Inventory|Shark}}\n| {{Equipment|torso=Masori body (f)}}{{Recommended equipment|body1={{plink|Masori body (f)}}|body2={{plink|Black d'hide body}}}}{{Inventory|Shark}}\n|}";List<JsonObject> p=new WikiParser().parse(text,"Boss","Fixture");assertEquals(2,p.size());assertEquals(Arrays.asList("Torva platebody","Bandos chestplate"),names(body(p.get(0))));assertEquals(Arrays.asList("Masori body (f)","Black d'hide body"),names(body(p.get(1))));}
}
