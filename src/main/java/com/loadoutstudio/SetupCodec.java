/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Portable data only. Never reads or modifies another plugin's configuration. */
public final class SetupCodec
{
	public static final int MAX_TEXT = 2_000_000;
	private final Gson gson;

	public SetupCodec(Gson gson)
	{
		this.gson = gson;
	}

	public static JsonArray empty(int size)
	{
		JsonArray result = new JsonArray();
		for (int i = 0; i < size; i++) result.add(JsonNull.INSTANCE);
		return result;
	}

	public static JsonObject blank(String name)
	{
		JsonObject s = new JsonObject();
		s.addProperty("name", name);
		s.add("inv", empty(28));
		s.add("eq", empty(14));
		s.add("rp", empty(4));
		s.add("qv", empty(1));
		s.add("afi", new JsonObject());
		s.addProperty("hc", "#FFFF0000");
		s.addProperty("fb", true);
		s.addProperty("sb", 4);
		s.addProperty("notes", "");
		JsonObject p = new JsonObject();
		p.add("setup", s);
		rebuildLayout(p);
		return p;
	}

	public static JsonObject setup(JsonObject p) { return p.getAsJsonObject("setup"); }
	public static String name(JsonObject p) { return setup(p).get("name").getAsString(); }
	public static int id(JsonElement e) { return e == null || e.isJsonNull() ? -1 : e.getAsJsonObject().get("id").getAsInt(); }
	public static boolean unlimited(JsonElement e) { return e != null && e.isJsonObject() && e.getAsJsonObject().has("q") && !e.getAsJsonObject().get("q").isJsonNull() && e.getAsJsonObject().get("q").getAsInt() == 0; }
	public static int quantity(JsonElement e)
	{
		if (e == null || e.isJsonNull()) return 0;
		JsonElement q = e.getAsJsonObject().get("q");
		return q == null ? 1 : q.isJsonNull() ? 0 : q.getAsInt();
	}
	public static JsonObject item(int id, int quantity)
	{
		JsonObject item = new JsonObject(); item.addProperty("id", id);
		if (quantity != 1) item.addProperty("q", quantity);
		return item;
	}

	public static int parseQuantity(String text)
	{
		String value = text.trim().toLowerCase(java.util.Locale.ROOT).replace(",", "");
		if (!value.matches("[0-9]+(?:\\.[0-9]+)?k?")) throw new IllegalArgumentException("Enter a whole quantity, for example 15000 or 16k.");
		BigDecimal result = new BigDecimal(value.replace("k", ""));
		if (value.endsWith("k")) result = result.multiply(BigDecimal.valueOf(1000));
		try { int q = result.intValueExact(); if (q > 0) return q; }
		catch (ArithmeticException ignored) { }
		throw new IllegalArgumentException("Quantity must be a positive whole number within the supported range.");
	}

	public JsonElement parse(String text)
	{
		if (text == null || text.length() > MAX_TEXT) throw new IllegalArgumentException("Import must be smaller than 2 MB.");
		int depth = 0; boolean quoted = false, escaped = false;
		for (char c : text.toCharArray())
		{
			if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
			else if (c == '"') quoted = true;
			else if (c == '{' || c == '[') { if (++depth > 48) throw new IllegalArgumentException("Import nesting is excessive."); }
			else if (c == '}' || c == ']') depth--;
		}
		return gson.fromJson(text, JsonElement.class);
	}

	public List<JsonObject> importText(String text)
	{
		if (text == null || text.length() > MAX_TEXT || text.trim().isEmpty()) throw new IllegalArgumentException("Paste setup JSON or Bank Tag text.");
		String trimmed = text.trim();
		List<JsonObject> result = new ArrayList<>();
		if (trimmed.startsWith("{") || trimmed.startsWith("["))
		{
			JsonElement input = parse(trimmed);
			if (input.isJsonObject() && input.getAsJsonObject().has("bankTag")) return importText(input.getAsJsonObject().get("bankTag").getAsString());
			Iterable<JsonElement> entries = input.isJsonArray() ? input.getAsJsonArray() : Arrays.asList(input);
			for (JsonElement entry : entries)
			{
				if (result.size() >= 100) throw new IllegalArgumentException("Import at most 100 setups at once.");
				JsonObject p = entry.getAsJsonObject().deepCopy();
				validate(p, true);
				JsonObject s = setup(p);
				if (!s.has("rp") || s.get("rp").isJsonNull()) s.add("rp", empty(4));
				if (!s.has("qv") || s.get("qv").isJsonNull()) s.add("qv", empty(1));
				if (!s.has("afi") || s.get("afi").isJsonNull()) s.add("afi", new JsonObject());
				if (!s.has("notes")) s.addProperty("notes", "");
				if (!s.has("sb")) s.addProperty("sb", 0);
				result.add(p);
			}
		}
		else result.add(importBank(trimmed));
		if (result.isEmpty()) throw new IllegalArgumentException("No setups found.");
		return result;
	}

	public static void validate(JsonObject p, boolean complete)
	{
		if (p == null || !p.has("setup") || !p.get("setup").isJsonObject()) throw new IllegalArgumentException("Missing setup object.");
		JsonObject s = setup(p);
		if (!s.has("name") || !s.get("name").isJsonPrimitive() || !s.get("name").getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Missing setup name.");
		String name = s.get("name").getAsString();
		if (name.trim().isEmpty() || name.length() > 50) throw new IllegalArgumentException("Setup name must contain 1–50 characters.");
		if (s.has("notes") && !s.get("notes").isJsonNull() && s.get("notes").getAsString().length() > 10_000) throw new IllegalArgumentException("Notes are too long.");
		Map<String, Integer> limits = new LinkedHashMap<>(); limits.put("inv", 28); limits.put("eq", 14); limits.put("rp", 4); limits.put("qv", 1); limits.put("bp", 28);
		List<Integer> ids = new ArrayList<>();
		for (Map.Entry<String, Integer> limit : limits.entrySet())
		{
			String k = limit.getKey(); JsonElement value = s.get(k);
			if (value == null || value.isJsonNull()) { if (k.equals("inv") || k.equals("eq")) throw new IllegalArgumentException("Missing " + k); else continue; }
			if (!value.isJsonArray() || value.getAsJsonArray().size() > limit.getValue() || ((k.equals("inv") || k.equals("eq")) && value.getAsJsonArray().size() != limit.getValue())) throw new IllegalArgumentException("Invalid " + k + " slot count.");
			for (JsonElement item : value.getAsJsonArray()) validateItem(item, k.equals("rp"), complete, ids);
		}
		for (int unused : new int[]{6, 8, 11}) if (!s.getAsJsonArray("eq").get(unused).isJsonNull()) throw new IllegalArgumentException("Unused Equipment positions must be empty.");
		JsonElement extra = s.get("afi");
		if (extra != null && !extra.isJsonNull())
		{
			if (!extra.isJsonObject() || extra.getAsJsonObject().size() > 256) throw new IllegalArgumentException("Invalid Additional filtered items.");
			for (Map.Entry<String, JsonElement> entry : extra.getAsJsonObject().entrySet()) validateItem(entry.getValue(), false, complete, ids);
		}
		if (s.has("sb") && (integer(s.get("sb")) < 0 || integer(s.get("sb")) > 4)) throw new IllegalArgumentException("Invalid Spellbook.");
		if (!p.has("layout") || !p.get("layout").isJsonArray() || p.getAsJsonArray("layout").size() > 8192) throw new IllegalArgumentException("Missing or excessive Bank layout.");
		List<Integer> layout = new ArrayList<>();
		for (JsonElement position : p.getAsJsonArray("layout")) { int id = integer(position); if (id == 0 || id < -1) throw new IllegalArgumentException("Invalid Bank layout item ID."); layout.add(id); }
		if (complete && !layout.containsAll(ids)) throw new IllegalArgumentException("The Bank layout is missing setup items.");
	}

	private static int integer(JsonElement e)
	{
		try { if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new ArithmeticException(); return e.getAsBigDecimal().intValueExact(); }
		catch (RuntimeException ex) { throw new IllegalArgumentException("Expected a whole number."); }
	}
	private static void validateItem(JsonElement e, boolean rune, boolean complete, List<Integer> ids)
	{
		if (e == null || e.isJsonNull()) return;
		if (!e.isJsonObject() || !e.getAsJsonObject().has("id")) throw new IllegalArgumentException("Invalid item.");
		JsonObject item = e.getAsJsonObject(); int id = integer(item.get("id"));
		if (id < 0 || (complete && id == 0)) throw new IllegalArgumentException("Resolve every Item ID before exporting.");
		if (id > 0) ids.add(id);
		if (item.has("q"))
		{
			if (item.get("q").isJsonNull()) { if (complete) throw new IllegalArgumentException("Enter missing item quantities."); }
			else { int q = integer(item.get("q")); if (q < 0 || (rune && q > 16000)) throw new IllegalArgumentException(rune ? "Rune pouch stacks must contain 0–16000 runes (0 ignores quantity)." : "Invalid item quantity."); }
		}
		if (item.has("sc") && !Arrays.asList("None", "Standard", "Less_Than", "Greater_Than").contains(item.get("sc").getAsString())) throw new IllegalArgumentException("Invalid Stack comparison.");
	}

	public String inventory(JsonObject portable)
	{
		validate(portable, true);
		JsonObject copy = portable.deepCopy(), s = setup(copy);
		for (String key : Arrays.asList("inv", "eq", "rp", "qv", "bp")) if (s.has(key) && s.get(key).isJsonArray())
		{
			for (JsonElement item : s.getAsJsonArray(key)) { if (unlimited(item)) item.getAsJsonObject().addProperty("sc", "None"); cleanItem(item); }
		}
		if (s.has("afi")) for (Map.Entry<String, JsonElement> entry : s.getAsJsonObject("afi").entrySet()) cleanItem(entry.getValue());
		for (String key : Arrays.asList("rp", "qv", "bp")) if (s.has(key) && s.get(key).isJsonArray())
		{
			boolean populated = false; for (JsonElement e : s.getAsJsonArray(key)) if (!e.isJsonNull()) populated = true;
			if (!populated) s.remove(key);
		}
		return gson.toJson(copy);
	}
	private static void cleanItem(JsonElement e) { if (e != null && e.isJsonObject()) { e.getAsJsonObject().remove("label"); if (quantity(e) == 1) e.getAsJsonObject().remove("q"); } }

	public static void rebuildLayout(JsonObject p)
	{
		JsonObject s = setup(p); int[] layout = new int[56]; Arrays.fill(layout, -1);
		int[] eq = {1, 8, 9, 16, 17, 18, -1, 25, -1, 32, 33, -1, 34, 10};
		for (int i = 0; i < 14; i++) if (eq[i] >= 0) layout[eq[i]] = Math.max(-1, id(s.getAsJsonArray("eq").get(i)));
		for (int i = 0; i < 28; i++) layout[(i / 4) * 8 + 4 + i % 4] = Math.max(-1, id(s.getAsJsonArray("inv").get(i)));
		if (s.has("rp") && s.get("rp").isJsonArray()) for (int i = 0; i < Math.min(4, s.getAsJsonArray("rp").size()); i++) layout[40 + i] = Math.max(-1, id(s.getAsJsonArray("rp").get(i)));
		if (s.has("qv") && s.get("qv").isJsonArray() && s.getAsJsonArray("qv").size() > 0) layout[2] = Math.max(-1, id(s.getAsJsonArray("qv").get(0)));
		JsonArray result = new JsonArray(); for (int i : layout) result.add(i == 0 ? -1 : i);
		for (String key : Arrays.asList("afi", "bp"))
		{
			JsonElement extra = s.get(key); if (extra == null || extra.isJsonNull()) continue;
			Iterable<JsonElement> values = extra.isJsonArray() ? extra.getAsJsonArray() : extra.getAsJsonObject().entrySet().stream().map(Map.Entry::getValue).collect(java.util.stream.Collectors.toList());
			for (JsonElement item : values) if (id(item) > 0) result.add(id(item));
		}
		p.add("layout", result);
	}

	public String bank(JsonObject p, boolean legacy)
	{
		JsonObject s = setup(p); String name = name(p).replaceAll("[<>/:,\"\\r\\n]", " ").replaceAll("\\s+", " ").trim();
		if (name.isEmpty()) throw new IllegalArgumentException("Enter a Bank Tag name.");
		Map<Integer, Boolean> tags = new LinkedHashMap<>();
		for (String k : Arrays.asList("inv", "eq", "rp", "qv", "bp")) if (s.has(k) && s.get(k).isJsonArray()) for (JsonElement e : s.getAsJsonArray(k)) addTag(tags, e);
		if (s.has("afi")) for (Map.Entry<String, JsonElement> entry : s.getAsJsonObject("afi").entrySet()) addTag(tags, entry.getValue());
		JsonArray layout = p.getAsJsonArray("layout");
		if (layout == null || layout.size() > 8192) throw new IllegalArgumentException("Invalid Bank layout.");
		for (JsonElement e : layout) { int id = integer(e); if (id == 0 || id < -1) throw new IllegalArgumentException("Invalid layout ID."); if (id > 0) tags.putIfAbsent(id, false); }
		if (tags.isEmpty()) throw new IllegalArgumentException("Add an item first.");
		int icon = id(s.getAsJsonArray("eq").get(3)); if (icon < 1) icon = tags.keySet().iterator().next();
		StringBuilder out = new StringBuilder(legacy ? "banktaglayoutsplugin:" + name : "banktags,1," + name + "," + icon);
		if (legacy) for (int i = 0; i < layout.size(); i++) if (layout.get(i).getAsInt() > 0) out.append(',').append(layout.get(i).getAsInt()).append(':').append(i);
		if (legacy) out.append(",banktag:").append(name).append(',').append(icon);
		for (Map.Entry<Integer, Boolean> entry : tags.entrySet()) out.append(',').append(entry.getValue() ? -entry.getKey() : entry.getKey());
		if (!legacy && layout.size() > 0)
		{
			out.append(",layout"); for (int i = 0; i < layout.size(); i++) if (layout.get(i).getAsInt() > 0) out.append(',').append(i).append(',').append(layout.get(i).getAsInt());
		}
		return out.toString();
	}
	private static void addTag(Map<Integer, Boolean> tags, JsonElement e)
	{
		if (e == null || e.isJsonNull()) return; int id = id(e);
		if (id <= 0) throw new IllegalArgumentException("Resolve every Item ID before Bank Tag export.");
		boolean fuzzy = e.getAsJsonObject().has("f") && e.getAsJsonObject().get("f").getAsBoolean(); tags.put(id, tags.getOrDefault(id, false) || fuzzy);
	}

	private JsonObject importBank(String text)
	{
		List<String> fields = csv(text); boolean legacy = text.startsWith("banktaglayoutsplugin:"); int i = 0;
		String name; int icon; List<int[]> pairs = new ArrayList<>(); Map<Integer, Boolean> tags = new LinkedHashMap<>();
		if (legacy)
		{
			name = fields.get(i++).substring("banktaglayoutsplugin:".length());
			while (i < fields.size() && !fields.get(i).startsWith("banktag:")) { String[] pair = fields.get(i++).split(":"); if (pair.length != 2) throw new IllegalArgumentException("Invalid layout pair."); pairs.add(new int[]{number(pair[1]), number(pair[0])}); }
			if (i >= fields.size() || !fields.get(i++).equals("banktag:" + name)) throw new IllegalArgumentException("Bank Tag names do not match.");
		}
		else { if (fields.get(i).equals("banktags")) { i++; if (!fields.get(i++).equals("1")) throw new IllegalArgumentException("Unsupported Bank Tag version."); } name = fields.get(i++); }
		icon = number(fields.get(i++)); if (icon <= 0) throw new IllegalArgumentException("Invalid Bank Tag icon.");
		while (i < fields.size() && !fields.get(i).equals("layout")) { int id = number(fields.get(i++)); if (id == 0 || id == Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid Item ID."); tags.merge(Math.abs(id), id < 0, (a, b) -> a || b); }
		if (i < fields.size()) { i++; while (i < fields.size()) { if (i + 1 >= fields.size()) throw new IllegalArgumentException("Incomplete layout pair."); pairs.add(new int[]{number(fields.get(i++)), number(fields.get(i++))}); } }
		JsonObject p = blank(name.trim().substring(0, Math.min(50, name.trim().length()))), s = setup(p); JsonArray layout = new JsonArray();
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (int[] pair : pairs)
		{
			if (pair[0] < 0 || pair[0] > 8191 || pair[1] < 1 || !seen.add(pair[0])) throw new IllegalArgumentException("Invalid or duplicate Bank position.");
			while (layout.size() <= pair[0]) layout.add(-1); layout.set(pair[0], gson.toJsonTree(pair[1])); tags.putIfAbsent(pair[1], false);
		}
		if (tags.size() > 284) throw new IllegalArgumentException("Too many Bank Tag items.");
		int slot = 0; for (Map.Entry<Integer, Boolean> entry : tags.entrySet())
		{
			JsonObject item = item(entry.getKey(), 1); if (entry.getValue()) item.addProperty("f", true);
			if (slot < 28) s.getAsJsonArray("inv").set(slot++, item); else s.getAsJsonObject("afi").add(String.valueOf(entry.getKey()), item);
			boolean included = false; for (JsonElement position : layout) if (position.getAsInt() == entry.getKey()) included = true; if (!included) layout.add(entry.getKey());
		}
		p.add("layout", layout); s.addProperty("notes", "Imported from a Bank Tag. Equipment slots, quantities, Rune pouch contents and Spellbook are not stored in Bank Tags. Items start in Inventory, then Additional filtered items. Review before use.");
		validate(p, true); return p;
	}
	private static int number(String value) { try { return Integer.parseInt(value); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid Bank Tag number."); } }
	private static List<String> csv(String text)
	{
		List<String> fields = new ArrayList<>(); StringBuilder field = new StringBuilder(); boolean quoted = false;
		for (int i = 0; i < text.length(); i++) { char c = text.charAt(i); if (c == '"') { if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; } else quoted = !quoted; } else if (c == ',' && !quoted) { fields.add(field.toString()); field.setLength(0); } else field.append(c); }
		if (quoted) throw new IllegalArgumentException("Unclosed CSV quote."); fields.add(field.toString()); return fields;
	}
}
