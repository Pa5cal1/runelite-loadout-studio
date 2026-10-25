/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;
import java.util.function.Function;

/** Immutable, local observation. A Bank observation never claims account-wide ownership. */
final class OwnedGear
{
    final Map<Integer,Integer> amounts;
    final Map<Integer,StudioPanel.ItemInfo> items;
    final boolean bankOpen;
    final long observedAt;
    OwnedGear(Map<Integer,Integer> amounts,Map<Integer,StudioPanel.ItemInfo> items,long observedAt,boolean bankOpen)
    {
        this.amounts=Collections.unmodifiableMap(new LinkedHashMap<>(amounts));this.items=Collections.unmodifiableMap(new LinkedHashMap<>(items));this.observedAt=observedAt;this.bankOpen=bankOpen;
    }
    int count(int id){return amounts.getOrDefault(id,0);}
    static final class Proposal
    {
        final String container;final int index;final JsonElement before;final List<StudioPanel.ItemInfo> choices;final String reason;
        Proposal(String key,int index,JsonElement before,List<StudioPanel.ItemInfo> choices,String reason){this.container=key;this.index=index;this.before=before.deepCopy();this.choices=Collections.unmodifiableList(choices);this.reason=reason;}
    }
    /** Share one choice only when the same item has the same ranked replacements. */
    static List<List<Proposal>> grouped(List<Proposal> proposals)
    {
        Map<String,List<Proposal>> groups=new LinkedHashMap<>();
        for(Proposal proposal:proposals){
            StringBuilder key=new StringBuilder(proposal.container).append(':').append(SetupCodec.id(proposal.before));
            // Equipment positions remain independent; supplies can fill many Inventory slots.
            if(proposal.container.equals("eq"))key.append(':').append(proposal.index);
            for(StudioPanel.ItemInfo choice:proposal.choices)key.append(':').append(choice.id);
            groups.computeIfAbsent(key.toString(),ignored->new ArrayList<>()).add(proposal);
        }
        return new ArrayList<>(groups.values());
    }
    List<Proposal> alternatives(JsonObject portable,Function<String,StudioPanel.ItemInfo> resolve)
    {
        List<Proposal> result=new ArrayList<>();JsonObject setup=SetupCodec.setup(portable);
        for(String key:new String[]{"eq","inv"}){
            JsonArray array=setup.getAsJsonArray(key);
            for(int i=0;i<array.size();i++){
                JsonElement raw=array.get(i);int id=SetupCodec.id(raw);boolean pouchUpgrade=EquipmentVariants.runePouch(id)&&amounts.keySet().stream().anyMatch(other->EquipmentVariants.divinePouch(other)&&count(other)>0);if(id<1 || count(id)>0&&!pouchUpgrade)continue;
                JsonObject expected=raw.getAsJsonObject();if(!pouchUpgrade && expected.has("f") && expected.get("f").getAsBoolean() && amounts.keySet().stream().anyMatch(other->SetupVariationMapping.fuzzy(other)==SetupVariationMapping.fuzzy(id)))continue;
                LinkedHashMap<Integer,StudioPanel.ItemInfo> candidates=new LinkedHashMap<>();
                for(StudioPanel.ItemInfo owned:items.values())if(owned.id!=id&&count(owned.id)>0&&EquipmentVariants.alternative(owned.id,id)&&(!key.equals("eq")||owned.slot==i))candidates.put(owned.id,owned);
                if(expected.has("choices") && expected.get("choices").isJsonArray())for(JsonElement choice:expected.getAsJsonArray("choices")){
                    StudioPanel.ItemInfo item=resolve.apply(choice.getAsString());if(item!=null && count(item.id)>0 && item.id!=id && (!key.equals("eq") || item.slot==i))candidates.put(item.id,item);
                    // Equal-name variants may have different IDs; offer the one actually observed.
                    for(StudioPanel.ItemInfo owned:items.values())if((owned.name.equalsIgnoreCase(choice.getAsString())||item!=null&&EquipmentVariants.alternative(owned.id,item.id)) && count(owned.id)>0 && owned.id!=id && (!key.equals("eq") || owned.slot==i))candidates.put(owned.id,owned);
                }
                String reason="Wiki recommendation order (most to least effective); review source conditions";
                if(candidates.isEmpty() && (!expected.has("choices") || expected.getAsJsonArray("choices").size()==0)){
                    StudioPanel.ItemInfo missing=items.get(id);String name=expected.has("label")?expected.get("label").getAsString():missing==null?"":missing.name;
                    List<String> family=family(name);
                    for(String ranked:family){StudioPanel.ItemInfo rankedItem=resolve.apply(ranked);for(StudioPanel.ItemInfo owned:items.values())if(count(owned.id)>0 && owned.id!=id && (ranked.equals(owned.name.toLowerCase(Locale.ROOT))||rankedItem!=null&&EquipmentVariants.alternative(owned.id,rankedItem.id)) && (!key.equals("eq") || owned.slot==i))candidates.put(owned.id,owned);}
                    reason="Same gear/supply group; review requirements and healing";
                }
                if(!candidates.isEmpty())result.add(new Proposal(key,i,raw,new ArrayList<>(candidates.values()).subList(0,Math.min(5,candidates.size())),reason));
            }
        }return result;
    }
    /** Reserve all Inventory/Equipment slots using a replacement ID, not each choice in isolation. */
    void requireReplacementStock(JsonObject portable, Map<Proposal,StudioPanel.ItemInfo> selected)
    {
        Map<String,StudioPanel.ItemInfo> replacements = new HashMap<>();
        Map<Integer,StudioPanel.ItemInfo> targets = new LinkedHashMap<>();
        selected.forEach((proposal,item)->{replacements.put(proposal.container+":"+proposal.index,item);targets.put(item.id,item);});
        Map<Integer,Long> needed = new LinkedHashMap<>();
        JsonObject setup=SetupCodec.setup(portable);
        for(String key:new String[]{"eq","inv"}) {
            JsonArray slots=setup.getAsJsonArray(key);
            for(int i=0;i<slots.size();i++) {
                JsonElement before=slots.get(i);StudioPanel.ItemInfo replacement=replacements.get(key+":"+i);
                int id=replacement==null?SetupCodec.id(before):replacement.id;
                if(targets.containsKey(id))needed.merge(id,(long)Math.max(1,SetupCodec.quantity(before)),Long::sum);
            }
        }
        needed.forEach((id,total)->{if(total>count(id))throw new IllegalStateException("Not enough observed "+targets.get(id).name+": need "+total+" across Inventory and Equipment, seen "+count(id)+". Select fewer replacements or adjust quantities. The draft was kept.");});
    }
    private static List<String> family(String name)
    {
        String normalized=name.toLowerCase(Locale.ROOT);
        String[][] groups={
            {"torva platebody","bandos chestplate","fighter torso","obsidian platebody","rune platebody"},
            {"torva platelegs","bandos tassets","obsidian platelegs","dragon platelegs","rune platelegs"},
            {"torva full helm","neitiznot faceguard","serpentine helm","helm of neitiznot","berserker helm"},
            {"ultor ring","berserker ring (i)","berserker ring"},
            {"venator ring","archers ring (i)","archers ring"},
            {"magus ring","seers ring (i)","seers ring"},
            {"bellator ring","warrior ring (i)","warrior ring"},
            {"zaryte vambraces","barrows gloves","black d'hide vambraces"},
            {"tormented bracelet","barrows gloves","mystic gloves"},
            {"ferocious gloves","barrows gloves","dragon gloves","rune gloves"},
            {"primordial boots","dragon boots","rune boots","climbing boots"},
            {"ancestral robe top","virtus robe top","ahrim's robetop","bloodbark body","mystic robe top"},
            {"ancestral robe bottom","virtus robe bottom","ahrim's robeskirt","bloodbark legs","mystic robe bottom"},
            {"masori body (f)","masori body","armadyl chestplate","blessed d'hide body","black d'hide body","red d'hide body"},
            {"masori chaps (f)","masori chaps","armadyl chainskirt","blessed d'hide chaps","black d'hide chaps","red d'hide chaps"},
            {"amulet of torture","amulet of fury","amulet of glory","amulet of strength"},
            {"infernal cape","fire cape","obsidian cape"},
            {"manta ray","shark","monkfish","swordfish","lobster"}
        };
        for(String[] group:groups){List<String> names=Arrays.asList(group);if(names.contains(normalized))return names;}return Collections.emptyList();
    }
}
