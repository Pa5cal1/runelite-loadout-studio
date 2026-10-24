/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads concrete Wiki templates. Ranked recommendation alternatives are not silently guessed. */
public final class WikiParser
{
	static final class Node
	{
		String kind;
		Map<String, String> fields = new LinkedHashMap<>();
		int start;
		int end;
	}
	public static List<String> split(String text, String delimiter)
	{
		List<String> result = new ArrayList<>(); int braces = 0, links = 0, tables = 0, start = 0;
		for (int i = 0; i < text.length(); i++)
		{
			if (text.startsWith("{{", i)) { braces++; i++; }
			else if (text.startsWith("}}", i)) { braces--; i++; }
			else if (text.startsWith("[[", i)) { links++; i++; }
			else if (text.startsWith("]]", i)) { links--; i++; }
			else if (text.startsWith("{|", i)) { tables++; i++; }
			else if (text.startsWith("|}", i)) { tables--; i++; }
			else if (braces == 0 && links == 0 && tables == 0 && text.startsWith(delimiter, i)) { result.add(text.substring(start, i)); i += delimiter.length() - 1; start = i + 1; }
		}
		result.add(text.substring(start)); return result;
	}
	static List<Node> nodes(String text)
	{
		List<Integer> stack = new ArrayList<>(); List<Node> result = new ArrayList<>();
		for (int i = 0; i < text.length() - 1; i++)
		{
			if (text.startsWith("{{", i)) { if (stack.size() >= 48) throw new IllegalArgumentException("Wiki nesting exceeds the supported limit."); stack.add(i++); }
			else if (text.startsWith("}}", i) && !stack.isEmpty())
			{
				int start = stack.remove(stack.size() - 1); Node node = new Node(); node.start = start; node.end = i + 2;
				List<String> fields = split(text.substring(start + 2, i), "|"); node.kind = fields.get(0).trim().replace('_', ' ').toLowerCase(Locale.ROOT); int positional = 1;
				for (String field : fields.subList(1, fields.size())) { List<String> pair = split(field, "="); if (pair.size() > 1) node.fields.put(pair.get(0).trim(), String.join("=", pair.subList(1, pair.size())).trim()); else node.fields.put(String.valueOf(positional++), field.trim()); }
				result.add(node); if (result.size() > 10000) throw new IllegalArgumentException("Too many Wiki templates."); i++;
			}
		}
		result.sort(java.util.Comparator.comparingInt(n -> n.start)); return result;
	}
	public static String plain(String text)
	{
		return text.replaceAll("(?s)<!--.*?-->", "").replaceAll("\\[\\[([^]|]+)\\|([^]]+)]]", "$2").replaceAll("\\[\\[([^]]+)]]", "$1").replaceAll("<[^>]+>", "").replace("'''", "").replace("''", "").replace("&amp;", "&").replace("&nbsp;", " ").trim();
	}
	private static boolean concrete(Node n) { return n.kind.equals("equipment") || n.kind.equals("inventory") || n.kind.equals("rune pouch"); }
    /** Bundled source order, deliberately independent of live market prices. */
    static String defaultName(JsonObject item)
    {
        if (item.has("choices") && item.get("choices").isJsonArray() && item.getAsJsonArray("choices").size() > 0) return item.getAsJsonArray("choices").get(0).getAsString();
        String name = item.has("label") ? item.get("label").getAsString() : "";
        String marker = name.trim().replace('_', ' ').replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (marker.matches("\\{\\{ ?cheap food ?}}")) return "Manta ray";
        if (marker.matches("\\{\\{ ?cheap prayer ?}}")) return "Prayer potion(4)";
        return name;
    }
	private static JsonElement item(String value, String explicit, boolean quantityRequired)
	{
		if (value == null || value.trim().isEmpty() || value.trim().matches("(?i)none|empty|n/a|-")) return JsonNull.INSTANCE;
		Matcher quantity = Pattern.compile("\\\\([0-9,]+)").matcher(value); String q = explicit;
		if (quantity.find()) q = quantity.group(1); String names = value.split("\\\\", 2)[0]; String[] parts = names.split(";");
		String name = itemName(parts[0]); for (int i = 1; i < parts.length; i++) if (!parts[i].matches("[ng]") && !parts[i].trim().isEmpty()) { name = itemName(parts[i]); break; }
		JsonObject item = SetupCodec.item(0, 1); item.addProperty("label", name);
        if(names.contains(" / ")){JsonArray options=new JsonArray();for(String option:names.split("\\s+/\\s+")){String candidate=itemName(option);if(!candidate.isBlank())options.add(candidate);}if(options.size()>1){item.add("choices",options);item.addProperty("label",options.get(0).getAsString());}}
		if (q != null && q.matches("[0-9,]+")) item.addProperty("q", SetupCodec.parseQuantity(q)); else if (quantityRequired) item.add("q", JsonNull.INSTANCE);
		return item;
	}
    static String itemName(String raw) {
        String value=plain(raw).replaceAll("(?s)<ref.*?</ref>","").trim();
        for(Node node:nodes(raw)) if(node.kind.equals("plink")||node.kind.equals("item")||node.kind.equals("icon")||node.kind.equals("rune")) {
            String first=node.fields.getOrDefault("1","");
            if(!first.isBlank()) {value=first;if(node.kind.equals("rune")&&!value.toLowerCase(Locale.ROOT).endsWith(" rune"))value+=" rune";break;}
        }
        value=value.replaceAll("(?i)\\s*\\((tablet|tab)\\)$", "").replaceAll("\\s*\\[.*?]", "").trim();
        return ItemCatalogue.normalize(value);
    }
    private static String namedVariant(String raw,String text,int offset,String fallback) {
        String label=plain(raw).trim();
        if(label.isEmpty()||label.matches("(?i)(?:wiki preset \\d+|(?:style|class|rowspan|colspan)\\s*=.*|item|slot|equipment|inventory)")) {
            String prefix=text.substring(0,Math.max(0,Math.min(offset,text.length())));
            Matcher headings=Pattern.compile("(?m)^={2,5}\\s*(.*?)\\s*={2,5}\\s*$").matcher(prefix);String heading="";
            while(headings.find())heading=plain(headings.group(1));
            label=heading.matches("(?i)(equipment|inventory|recommended equipment|requirements|suggested skills|strategy|strategies)")?"":heading;
        }
        return label.isEmpty()?fallback:label;
    }
	private static JsonObject fill(String title, String variant, String source, List<Node> nodes)
	{
		JsonObject p = SetupCodec.blank(title.isEmpty() ? variant : title), s = SetupCodec.setup(p); s.addProperty("notes", "Source: " + source + "\nPreset: " + variant + "\nReview Item IDs, quantities and Spellbook before use.");
		String[] keys = {"head", "cape", "neck", "weapon", "body", "shield", "", "legs", "", "hands", "feet", "", "ring", "ammo"};
		for (Node node : nodes)
		{
			if (node.kind.equals("inventory")) for (int i = 0; i < 28; i++) s.getAsJsonArray("inv").set(i, item(node.fields.get(String.valueOf(i + 1)), null, false));
			else if (node.kind.equals("equipment"))
			{
				for (int i = 0; i < 14; i++) if (!keys[i].isEmpty()) s.getAsJsonArray("eq").set(i, item(equipmentField(node, keys[i]), node.fields.get(keys[i] + "quantity"), i == 13));
				s.getAsJsonArray("qv").set(0, item(node.fields.get("ammo2"), node.fields.get("ammo2quantity"), true));
			}
			else if (node.kind.equals("rune pouch")) for (int i = 0; i < 4; i++) s.getAsJsonArray("rp").set(i, item(node.fields.get(String.valueOf(i + 1)), null, true));
		}
		SetupCodec.rebuildLayout(p); return p;
	}
	private static String equipmentField(Node node, String key)
	{
		if (node.fields.containsKey(key)) return node.fields.get(key);
		String alias = key.equals("body") ? "torso" : key.equals("hands") ? "gloves" : key.equals("feet") ? "boots" : key;
		return node.fields.get(alias);
	}
	private static String tabberVariant(String text, int offset, String fallback)
	{
		int start = text.lastIndexOf("<tabber", offset), closed = text.lastIndexOf("</tabber>", offset);
		if (start < 0 || closed > start) return fallback;
		Matcher labels = Pattern.compile("(?m)^([^\\r\\n|{}<>=]{1,100})=\\s*$").matcher(text.substring(start, offset));
		String label = fallback; while (labels.find()) label = plain(labels.group(1));
		return label;
	}
	public static String variant(JsonObject preset)
	{
		String notes = SetupCodec.setup(preset).get("notes").getAsString();
		for (String line : notes.split("\\R")) if (line.startsWith("Preset: ")) return line.substring(8);
		return SetupCodec.name(preset);
	}
	public List<JsonObject> parse(String text, String pageTitle, String source)
	{
		if (text == null || text.length() > 1_500_000) throw new IllegalArgumentException("Wiki text must be smaller than 1.5 MB.");
		String title = plain(pageTitle).replace('_', ' ').split("(?i)/Strategies", 2)[0]; if (title.length() > 50) title = title.substring(0, 50);
		List<JsonObject> result = new ArrayList<>(); List<int[]> used = new ArrayList<>();
		int search = 0;
		while ((search = text.indexOf("{|", search)) >= 0)
		{
			int end = search + 2, depth = 1;
			while (end < text.length() - 1 && depth > 0) { if (text.startsWith("{|", end)) { depth++; end++; } else if (text.startsWith("|}", end)) { depth--; end++; } end++; }
			if (depth != 0) break;
			Map<Integer, List<List<Node>>> columns = new LinkedHashMap<>(); Map<Integer, String> headers = new HashMap<>();
			String table = text.substring(search + 2, end - 2);
			for (String row : split(table, "\n|-"))
			{
                Matcher header=Pattern.compile("(?m)^!\\s*(.*)$").matcher(row);
                while(header.find()){int column=0;for(String cell:split(header.group(1),"!!")){List<String> parts=split(cell,"|");String name=plain(parts.size()>1&&parts.get(0).contains("=")?parts.get(parts.size()-1):cell);if(!name.isBlank())headers.putIfAbsent(column,name);column++;}}
                if(!row.contains("{{"))continue;
				List<String> cells = new ArrayList<>();
				for (String chunk : split("\n" + row, "\n|"))
				{
					if (chunk.trim().isEmpty() || chunk.startsWith("+") || !chunk.contains("{{")&&chunk.trim().matches("(?s)(?:class|style|rowspan|colspan)\\s*=.*")) continue;
					for (String cell : split(chunk, "||")) cells.addAll(split(cell, "\n!"));
				}

				for (int col = 0; col < cells.size(); col++)
				{
					String cell = cells.get(col); if (!cell.contains("{{") && !plain(cell).isEmpty() && plain(cell).length() < 80) headers.putIfAbsent(col, plain(cell));
					for (Node node : nodes(cell)) if (concrete(node))
					{
						List<List<Node>> groups = columns.computeIfAbsent(col, k -> new ArrayList<>()); if (groups.isEmpty() || groups.get(groups.size() - 1).stream().anyMatch(n -> n.kind.equals(node.kind))) groups.add(new ArrayList<>()); groups.get(groups.size() - 1).add(node);
					}
				}
			}
			for (Map.Entry<Integer, List<List<Node>>> column : columns.entrySet()) for (List<Node> group : column.getValue()) if (group.stream().anyMatch(n -> n.kind.equals("inventory")))
			{
				result.add(fill(title, namedVariant(headers.getOrDefault(column.getKey(), ""),text,search,tierName(result.size())), source, group)); used.add(new int[]{search, end});
			}
			search = end;
		}
		List<Node> free = new ArrayList<>(); for (Node node : nodes(text)) if (concrete(node) && used.stream().noneMatch(range -> range[0] <= node.start && node.start < range[1])) free.add(node);
		List<Node> group = new ArrayList<>(); String variant = "";
		for (Node node : free)
		{
			String nextVariant = tabberVariant(text, node.start, "");
			if (!group.isEmpty() && (!variant.equals(nextVariant) || group.stream().anyMatch(n -> n.kind.equals(node.kind))))
			{
				if (group.stream().anyMatch(n -> n.kind.equals("inventory") || n.kind.equals("equipment"))) result.add(fill(title, namedVariant(variant,text,node.start,tierName(result.size())), source, group));
				group = new ArrayList<>();
			}
			variant = nextVariant; group.add(node);
		}
		if (group.stream().anyMatch(n -> n.kind.equals("inventory") || n.kind.equals("equipment"))) result.add(fill(title, namedVariant(variant,text,text.length(),tierName(result.size())), source, group));
		if (result.size() > 100) throw new IllegalArgumentException("Too many presets on this page.");
		if(result.isEmpty())result.addAll(recommendations(text,title,source));
        if(result.isEmpty())throw new IllegalArgumentException("No Equipment, Inventory or Recommended equipment templates found. Use the Builder or import a setup.");
		for(JsonObject preset:result) {
            String variantName=variant(preset);String name=title+" ("+variantName+")";
            setupName(preset,name.length()>50?name.substring(0,50):name);
        }
        return result;
	}
    private static void setupName(JsonObject preset,String name){SetupCodec.setup(preset).addProperty("name",name);}
    private static String tierName(int index){return index==0?"Max gear":index==1?"High-tier gear":index==2?"Mid-game gear":index==3?"Budget gear":"Gear option "+(index+1);}
    /** The first listed recommendation is the default; conditions and alternatives remain editable. */
    private List<JsonObject> recommendations(String text,String title,String source)
    {
        List<JsonObject> result=new ArrayList<>();String[] keys={"head","cape","neck","weapon","body","shield","","legs","","hands","feet","","ring","ammo"};
        for(Node table:nodes(text))if(table.kind.equals("recommended equipment"))
        {
            if(result.size()>=100)throw new IllegalArgumentException("Too many recommendation tables.");
            String style=plain(table.fields.getOrDefault("style","Gear"));if(style.length()>80)style=style.substring(0,80);JsonObject p=SetupCodec.blank(title);JsonObject setup=SetupCodec.setup(p);StringBuilder notes=new StringBuilder("Source: "+source+"\nPreset: Recommended "+style+" (first choices)\nFirst listed items are selected by default. Gear alternatives only: Inventory, quantities and Spellbook must be reviewed. Conditions and combinations from the source are retained below.\n");
            int populated=0;
            for(int i=0;i<keys.length;i++)if(!keys[i].isEmpty())
            {
                JsonArray choices=new JsonArray();java.util.Set<String> names=new java.util.LinkedHashSet<>();StringBuilder hints=new StringBuilder();
                for(int rank=1;rank<=10;rank++)
                {
                    String value=table.fields.get(keys[i]+rank);if(value==null || value.isBlank() || value.matches("(?i)none|empty|-"))continue;
                    hints.append(keys[i]).append(" rank ").append(rank).append(": ").append(plain(value)).append('\n');
                    // The first component represents this slot; keep the full combination in Notes.
                    boolean combination=value.contains("+");int before=names.size();
                    List<Node> nested=nodes(value);
                    for(Node item:nested)if(item.kind.equals("plink") && nested.stream().noneMatch(parent->parent!=item && parent.start<item.start && parent.end>item.end && (parent.kind.equals("efn") || parent.kind.equals("refn"))))
                    {
                        String name=itemName(item.fields.getOrDefault("1",""));if(!name.isEmpty() && name.length()<150){names.add(name);if(combination)break;}
                    }
                    if(names.size()==before && !value.contains("{{")){for(String name:plain(value).split(combination?"\\s*\\+\\s*":"\\s+/\\s+")){if(!name.isBlank() && name.length()<150)names.add(name.trim());if(combination)break;}}
                }
                if(hints.length()==0)continue;
                for(String name:names)if(choices.size()<40)choices.add(name);
                JsonObject item=SetupCodec.item(0,1);item.addProperty("label",choices.size()>0?choices.get(0).getAsString():"Choose "+keys[i]+" · "+style);item.add("choices",choices);if(i==13)item.add("q",JsonNull.INSTANCE);setup.getAsJsonArray("eq").set(i,item);populated++;notes.append(hints);
            }
            if(populated>0){String detail=notes.toString();setup.addProperty("notes",detail.length()>10000?detail.substring(0,9900)+"\nSee the source page for remaining conditions.":detail);SetupCodec.rebuildLayout(p);result.add(p);}
        }
        return result;
    }

}
