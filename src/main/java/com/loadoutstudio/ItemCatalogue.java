/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Searchable real item names, excluding bank placeholders and noted copies. */
final class ItemCatalogue
{
    private final Map<Integer, StudioPanel.ItemInfo> items = new ConcurrentHashMap<>();
    private final Map<Integer,Integer> quality = new ConcurrentHashMap<>();
    private final Map<Integer,Integer> bankIds = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> definitions = ConcurrentHashMap.newKeySet();
    private volatile boolean ready;
    private static int preferred(String name) {
        switch(name.toLowerCase(Locale.ROOT)) {
            case "ultor ring": return net.runelite.api.gameval.ItemID.ULTOR_RING;
            case "venator ring": return net.runelite.api.gameval.ItemID.VENATOR_RING;
            case "magus ring": return net.runelite.api.gameval.ItemID.MAGUS_RING;
            case "bellator ring": return net.runelite.api.gameval.ItemID.BELLATOR_RING;
            case "void knight top": case "void top": return net.runelite.api.gameval.ItemID.PEST_VOID_KNIGHT_TOP;
            case "elite void top": return net.runelite.api.gameval.ItemID.ELITE_VOID_KNIGHT_TOP;
            case "elite void robe": return net.runelite.api.gameval.ItemID.ELITE_VOID_KNIGHT_ROBES;
            case "rune pouch": return net.runelite.api.gameval.ItemID.BH_RUNE_POUCH;
            case "divine rune pouch": return net.runelite.api.gameval.ItemID.DIVINE_RUNE_POUCH;
            case "dizana's quiver": return net.runelite.api.gameval.ItemID.DIZANAS_QUIVER_CHARGED;
            default:return -1;
        }
    }

    void add(int id, String membersName, boolean stackable, int noteTemplate, int placeholderTemplate)
    {
        if(id>0 && membersName!=null && !membersName.trim().isEmpty() && !membersName.equalsIgnoreCase("null"))definitions.add(id);
        if (id < 1 || id==net.runelite.api.gameval.ItemID.BETA_ITEM_2 || id==net.runelite.api.gameval.ItemID.BETA_ITEM_3 || id==net.runelite.api.gameval.ItemID.BETA_ITEM_4 || id==net.runelite.api.gameval.ItemID.BETA_ITEM_5 || id==net.runelite.api.gameval.ItemID.BR_ULTOR_RING || id==net.runelite.api.gameval.ItemID.BR_MAGUS_RING || noteTemplate != -1 || placeholderTemplate != -1 || membersName == null || membersName.trim().isEmpty() || membersName.equalsIgnoreCase("null")) return;
        items.put(id, new StudioPanel.ItemInfo(id, membersName, stackable, -1, null));
    }

    void bankId(int id,int canonical){if(id>0 && canonical>0)bankIds.put(id,canonical);}
    int bankId(int id){return bankIds.getOrDefault(id,id);}
    void indexed(){ready=true;}
    String exportError(com.google.gson.JsonObject portable){
        if(!ready)return "Game item definitions are still loading. Wait for the login screen before exporting to another plugin.";
        java.util.Set<Integer> ids=new java.util.LinkedHashSet<>();
        for(com.google.gson.JsonElement item:BankLayouts.items(portable))if(SetupCodec.id(item)>0)ids.add(SetupCodec.id(item));
        for(com.google.gson.JsonElement item:portable.getAsJsonArray("layout"))if(item.getAsInt()>0)ids.add(item.getAsInt());
        for(int id:ids)if(!definitions.contains(id))return "Item ID "+id+" is not a named item in this client. Replace it before exporting to another plugin. The draft is kept.";
        return null;
    }

    void add(int id,String name,boolean stackable,int note,int placeholder,int slot,boolean ge,boolean usable) {
        add(id,name,stackable,note,placeholder);
        if(items.containsKey(id)){items.put(id,new StudioPanel.ItemInfo(id,name,stackable,slot,null));quality.put(id,(ge?4:0)+(slot>=0?2:0)+(usable?1:0));}
    }
    static int selectionId(int id,String name){String key=name.toLowerCase(Locale.ROOT);return key.equals("ultor ring")||key.equals("magus ring")||key.equals("bellator ring")||key.equals("venator ring")?preferred(name):id;}
    static int legacyRingId(int id,String name){if(id==net.runelite.api.gameval.ItemID.BETA_ITEM_2||id==net.runelite.api.gameval.ItemID.BETA_ITEM_3||id==net.runelite.api.gameval.ItemID.BETA_ITEM_4||id==net.runelite.api.gameval.ItemID.BETA_ITEM_5||id==net.runelite.api.gameval.ItemID.BR_ULTOR_RING||id==net.runelite.api.gameval.ItemID.BR_MAGUS_RING)return selectionId(id,name);return id;}
    static String normalize(String name) {
        String result=name.replace('_',' ').trim().replaceAll("\\s+"," ");
        if(result.equalsIgnoreCase("Teleport to house (tablet)"))return "Teleport to house";
        if(result.equalsIgnoreCase("Void top"))return "Void knight top";
        return result;
    }
    StudioPanel.ItemInfo exact(String name)
    {
        StudioPanel.ItemInfo match = null;
        for (StudioPanel.ItemInfo item : items.values()) if (item.name.equalsIgnoreCase(name.trim()))
        {
            if (match != null) return null; // Actual variants still need a user choice.
            match = item;
        }
        return match;
    }

    /** Same stable order as search results: first exact name, then lowest Item ID. */
    StudioPanel.ItemInfo firstExact(String name)
    {
        StudioPanel.ItemInfo first = null;
        for (StudioPanel.ItemInfo item : items.values()) if (!AutomaticItemExclusions.contains(item.id) && item.name.equalsIgnoreCase(normalize(name)) && (first == null || rank(item,name)>rank(first,name) || rank(item,name)==rank(first,name)&&item.id < first.id)) first = item;
        if(first==null){String base=normalize(name);int dose=-1;for(StudioPanel.ItemInfo item:items.values()){if(AutomaticItemExclusions.contains(item.id))continue;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)^"+java.util.regex.Pattern.quote(base)+"\\s*\\(([1-4])\\)$").matcher(item.name);if(m.matches()){int candidate=Integer.parseInt(m.group(1));if(candidate>dose||candidate==dose&&(first==null||rank(item,name)>rank(first,name))){first=item;dose=candidate;}}}}
        return first;
    }
    private int rank(StudioPanel.ItemInfo item,String name){return (item.id==preferred(name)?100:0)+quality.getOrDefault(item.id,0);}

    List<StudioPanel.ItemInfo> search(String query)
    {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<StudioPanel.ItemInfo> result = new ArrayList<>();
        for (StudioPanel.ItemInfo item : items.values())
        {
            boolean match = true;
            for (String word : words) if (!item.name.toLowerCase(Locale.ROOT).contains(word) && !String.valueOf(item.id).equals(word)) match = false;
            if (match) result.add(item);
        }
        result.sort(Comparator.comparing((StudioPanel.ItemInfo i) -> i.name).thenComparingInt(i -> i.id));
        return result.subList(0, Math.min(100, result.size()));
    }

    int size() { return items.size(); }
    boolean matches(int id,String term) { StudioPanel.ItemInfo item=items.get(id);return item!=null && item.name.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)); }
    boolean isEmpty() { return items.isEmpty(); }
    void clear() { ready=false;items.clear();quality.clear();bankIds.clear();definitions.clear(); }
}
