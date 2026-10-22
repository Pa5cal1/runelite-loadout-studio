/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Deterministic eight-column layouts; no game state or plugin configuration is accessed. */
public final class BankLayouts
{
	public enum Mode
	{
		PRESET("Preset"), ZIGZAG("Zigzag"), CUSTOM("Imported / custom");
		private final String label;
		Mode(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}
	private BankLayouts() { }
	public static List<JsonElement> items(JsonObject portable)
	{
		JsonObject s = SetupCodec.setup(portable); List<JsonElement> result = new ArrayList<>();
		for (String key : new String[]{"eq", "inv", "rp", "qv", "bp"}) if (s.has(key) && s.get(key).isJsonArray()) for (JsonElement e : s.getAsJsonArray(key)) if (!e.isJsonNull()) result.add(e);
		if (s.has("afi") && s.get("afi").isJsonObject()) for (java.util.Map.Entry<String, JsonElement> e : s.getAsJsonObject("afi").entrySet()) if (!e.getValue().isJsonNull()) result.add(e.getValue());
		return result;
	}
	private static void put(JsonArray layout, int position, int id)
	{
		if (id <= 0) return; while (layout.size() <= position) layout.add(-1); layout.set(position, new com.google.gson.JsonPrimitive(id));
	}
	private static void row(JsonArray layout, JsonArray items, int start)
	{
		if (items != null) for (int i = 0; i < items.size(); i++) put(layout, start + i, SetupCodec.id(items.get(i)));
	}
	private static int zigzag(JsonArray layout, JsonArray items, int start, boolean bottom)
	{
		int count = 0;
		for (JsonElement item : items) if (SetupCodec.id(item) > 0)
		{
			put(layout, zigzagPosition(count++, start, bottom), SetupCodec.id(item));
		}
		return zigzagPosition(count, start, bottom);
	}
	static int zigzagPosition(int count, int start, boolean bottom)
	{
		return start + (count / 16) * 16 + (count % 16) / 2 + (((count % 2 == 0) == bottom) ? 8 : 0);
	}
	public static JsonArray generate(JsonObject portable, Mode mode, boolean bottom)
	{
		JsonObject s = SetupCodec.setup(portable); JsonArray layout = new JsonArray();
		if (mode == Mode.CUSTOM)
		{
			java.util.Set<Integer> ids = new java.util.HashSet<>(); for (JsonElement e : items(portable)) if (SetupCodec.id(e) > 0) ids.add(SetupCodec.id(e));
			for (JsonElement e : portable.getAsJsonArray("layout")) layout.add(ids.contains(e.getAsInt()) ? e.getAsInt() : -1);
			for (JsonElement e : items(portable)) if (SetupCodec.id(e) > 0 && !layout.contains(new com.google.gson.JsonPrimitive(SetupCodec.id(e)))) layout.add(SetupCodec.id(e));
			return layout;
		}
		while (layout.size() < 56) layout.add(-1);
		if (mode == Mode.PRESET)
		{
			int[] equipment = {1, 8, 9, 16, 17, 18, -1, 25, -1, 32, 33, -1, 34, 10};
			for (int i = 0; i < 14; i++) if (equipment[i] >= 0) put(layout, equipment[i], SetupCodec.id(s.getAsJsonArray("eq").get(i)));
			for (int i = 0; i < 28; i++) put(layout, (i / 4) * 8 + 4 + i % 4, SetupCodec.id(s.getAsJsonArray("inv").get(i)));
			row(layout, s.getAsJsonArray("qv"), 2); row(layout, s.getAsJsonArray("rp"), 40); row(layout, s.getAsJsonArray("bp"), 48);
		}
		else
		{
			int quiver = zigzag(layout, s.getAsJsonArray("eq"), 0, bottom); row(layout, s.getAsJsonArray("qv"), quiver);
			zigzag(layout, s.getAsJsonArray("inv"), 16, bottom); row(layout, s.getAsJsonArray("rp"), 48); row(layout, s.getAsJsonArray("bp"), 52);
		}
		int extra = 56;
		if (s.has("afi") && s.get("afi").isJsonObject()) for (java.util.Map.Entry<String, JsonElement> e : s.getAsJsonObject("afi").entrySet()) if (SetupCodec.id(e.getValue()) > 0) put(layout, extra++, SetupCodec.id(e.getValue()));
		// Larger imported bolt pouches must never overwrite Inventory or additional items.
		for (JsonElement e : items(portable)) if (SetupCodec.id(e) > 0 && !layout.contains(new com.google.gson.JsonPrimitive(SetupCodec.id(e)))) layout.add(SetupCodec.id(e));
		return layout;
	}
	public static Mode infer(JsonObject portable)
	{
		JsonArray actual = portable.getAsJsonArray("layout");
		if (generate(portable, Mode.PRESET, false).equals(actual)) return Mode.PRESET;
		if (generate(portable, Mode.ZIGZAG, false).equals(actual) || generate(portable, Mode.ZIGZAG, true).equals(actual)) return Mode.ZIGZAG;
		return Mode.CUSTOM;
	}
	public static boolean inferBottom(JsonObject portable) { return infer(portable) == Mode.ZIGZAG && generate(portable, Mode.ZIGZAG, true).equals(portable.getAsJsonArray("layout")); }
}
