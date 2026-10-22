/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonArray;
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
	private static JsonObject item(String value, String explicit, boolean quantityRequired)
	{
		if (value == null || value.trim().isEmpty() || value.trim().matches("(?i)none|empty|n/a|-")) return null;
		Matcher quantity = Pattern.compile("\\\\([0-9,]+)").matcher(value); String q = explicit;
		if (quantity.find()) q = quantity.group(1); String names = value.split("\\\\", 2)[0]; String[] parts = names.split(";");
		String name = plain(parts[0]); for (int i = 1; i < parts.length; i++) if (!parts[i].matches("[ng]") && !parts[i].trim().isEmpty()) { name = plain(parts[i]); break; }
		JsonObject item = SetupCodec.item(0, 1); item.addProperty("label", name);
		if (q != null && q.matches("[0-9,]+")) item.addProperty("q", SetupCodec.parseQuantity(q)); else if (quantityRequired) item.add("q", JsonNull.INSTANCE);
		return item;
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
				for (int i = 0; i < 14; i++) if (!keys[i].isEmpty()) s.getAsJsonArray("eq").set(i, item(node.fields.get(keys[i]), node.fields.get(keys[i] + "quantity"), i == 13));
				s.getAsJsonArray("qv").set(0, item(node.fields.get("ammo2"), node.fields.get("ammo2quantity"), true));
			}
			else if (node.kind.equals("rune pouch")) for (int i = 0; i < 4; i++) s.getAsJsonArray("rp").set(i, item(node.fields.get(String.valueOf(i + 1)), null, true));
		}
		SetupCodec.rebuildLayout(p); return p;
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
				List<String> cells = new ArrayList<>();
				for (String chunk : split("\n" + row, "\n|"))
				{
					if (chunk.trim().isEmpty() || chunk.startsWith("+")) continue;
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
				result.add(fill(title, headers.getOrDefault(column.getKey(), "Wiki preset " + (result.size() + 1)), source, group)); used.add(new int[]{search, end});
			}
			search = end;
		}
		List<Node> free = new ArrayList<>(); for (Node node : nodes(text)) if (concrete(node) && used.stream().noneMatch(range -> range[0] <= node.start && node.start < range[1])) free.add(node);
		List<Node> invs = new ArrayList<>(), eqs = new ArrayList<>(), rps = new ArrayList<>(); for (Node n : free) { if (n.kind.equals("inventory")) invs.add(n); else if (n.kind.equals("equipment")) eqs.add(n); else rps.add(n); }
		for (int i = 0; i < Math.max(invs.size(), eqs.size()); i++) { List<Node> group = new ArrayList<>(); if (i < invs.size()) group.add(invs.get(i)); if (i < eqs.size()) group.add(eqs.get(i)); if (i < rps.size()) group.add(rps.get(i)); result.add(fill(title, "Wiki preset " + (result.size() + 1), source, group)); }
		if (result.size() > 100) throw new IllegalArgumentException("Too many presets on this page.");
		if (result.isEmpty()) throw new IllegalArgumentException("No concrete Equipment/Inventory templates found. Ranked Recommended equipment tables are not expanded in this test version; use the Builder or import a setup.");
		return result;
	}
}
