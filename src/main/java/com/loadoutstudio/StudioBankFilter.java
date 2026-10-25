/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/** Client-thread controller. Only closes its own active tag; the gateway owns the RuneLite API calls. */
public final class StudioBankFilter
{
	interface Gateway
	{
		boolean bankOpen();
		int capacity();
		String activeTag();
        default String competingTag(){return activeTag();}
        default boolean respectOtherFilters(){return false;}
        default void pauseOwnedView(){}
		default int[] currentLayout() { return null; }
		int canonicalize(int id);
		int variation(int id);
		void register(String tag, IntPredicate matches);
		void open(String tag, int[] layout);
        default void title(String title) { }
		void close();
		void unregister(String tag);
		void discardLayout(String tag);
	}
	private final Gateway gateway;
	private final String tag;
	private JsonObject selected;
	private boolean registered;
	private boolean waiting;
	public boolean waiting() { return waiting; }
    int layoutItemId(int actual){int id=gateway.canonicalize(actual);if(selected==null)return id;for(JsonElement item:BankLayouts.items(selected))if(SetupCodec.id(item)>0&&gateway.canonicalize(SetupCodec.id(item))==id)return id;for(JsonElement item:BankLayouts.items(selected))if(SetupCodec.id(item)>0&&item.getAsJsonObject().has("f")&&item.getAsJsonObject().get("f").getAsBoolean()&&gateway.variation(gateway.canonicalize(SetupCodec.id(item)))==gateway.variation(id))return gateway.canonicalize(SetupCodec.id(item));return id;}
	JsonArray readChangedLayout()
	{
		if(selected==null || !tag.equals(gateway.activeTag()))return null;
		int[] cells=gateway.currentLayout();if(cells==null || cells.length>8192)return null;
		Set<Integer> allowed=new HashSet<>();for(JsonElement item:BankLayouts.items(selected))if(SetupCodec.id(item)>0)allowed.add(gateway.canonicalize(SetupCodec.id(item)));
		JsonArray layout=new JsonArray();for(int id:cells){int mapped=id>0?layoutItemId(id):id;if(mapped!=-1&&!allowed.contains(mapped))return null;layout.add(mapped);}
		JsonArray previous=new JsonArray();for(JsonElement e:selected.getAsJsonArray("layout"))previous.add(e.getAsInt()>0?gateway.canonicalize(e.getAsInt()):-1);
		while(previous.size()>0 && previous.get(previous.size()-1).getAsInt()==-1)previous.remove(previous.size()-1);
		while(layout.size()>0 && layout.get(layout.size()-1).getAsInt()==-1)layout.remove(layout.size()-1);
		if(layout.equals(previous))return null;
		// Missing items must remain available as placeholders after a manual move.
		for(int id:allowed)if(id>0 && !layout.contains(new com.google.gson.JsonPrimitive(id)))layout.add(id);
		selected.add("layout",layout.deepCopy());return layout;
	}
	StudioBankFilter(Gateway gateway, String tag) { this.gateway = gateway; this.tag = tag; }
	public String select(JsonObject portable){return select(portable,false);}
    public String select(JsonObject portable,boolean explicit)
	{
		clear();
		if (portable == null || !SetupCodec.setup(portable).has("fb") || !SetupCodec.setup(portable).get("fb").getAsBoolean()) return "Bank filter off.";
		SetupCodec.validate(portable, false);
		if (BankLayouts.items(portable).isEmpty()) return "Add items before filtering the Bank.";
		if(BankLayouts.items(portable).stream().noneMatch(item->SetupCodec.id(item)>0))return "No resolved items yet. Choose an item to start Bank filtering.";
		selected = portable.deepCopy();
        selected.add("layout",BankLayouts.generate(selected,BankLayouts.Mode.CUSTOM,false));
        return apply(explicit);
	}
	public String apply(){return apply(false);}
    public String apply(boolean explicit)
	{
		if (selected == null) return "Bank filter off.";
		if (!gateway.bankOpen()) { waiting = true; return "Bank filter ready. Open your Bank to apply it."; }
        String competing=gateway.competingTag();
        if(!explicit&&gateway.respectOtherFilters()&&competing!=null&&!competing.isEmpty()&&!tag.equals(competing)){
            if(registered){gateway.unregister(tag);registered=false;}gateway.pauseOwnedView();waiting=true;
            return "Another Bank filter is active. Use Show worn items > Loadout Studio > Filter Bank to switch.";
        }
		Set<Integer> exact = new HashSet<>(), fuzzy = new HashSet<>();
		for (JsonElement e : BankLayouts.items(selected))
		{
			if(SetupCodec.id(e)<=0)continue;int id = gateway.canonicalize(SetupCodec.id(e)); exact.add(id);
			if (e.getAsJsonObject().has("f") && e.getAsJsonObject().get("f").getAsBoolean()) fuzzy.add(gateway.variation(id));
		}
		IntPredicate matches = id -> { if (id <= 0) return false; int canonical = gateway.canonicalize(id); return exact.contains(canonical) || fuzzy.contains(gateway.variation(canonical)); };
		JsonArray source = selected.getAsJsonArray("layout"); int last = -1;
		for (int i = 0; i < source.size(); i++) if (source.get(i).getAsInt() > 0 && matches.test(source.get(i).getAsInt())) last = i;
		if (last >= gateway.capacity()) throw new IllegalArgumentException("This Bank layout exceeds the current Bank display capacity. Select Preset or Zigzag to rebuild it.");
		int[] layout = new int[last + 1]; java.util.Arrays.fill(layout, -1);
		for (int i = 0; i <= last; i++) if (source.get(i).getAsInt() > 0 && matches.test(source.get(i).getAsInt())) layout[i] = gateway.canonicalize(source.get(i).getAsInt());
		gateway.register(tag, matches); registered = true; gateway.open(tag, layout);
		waiting = false; refreshTitle();
		long unresolved=BankLayouts.items(selected).stream().filter(item->SetupCodec.id(item)<=0).count();return "Bank filtered: "+SetupCodec.name(selected)+(unresolved>0?" · "+unresolved+" unresolved slot(s) skipped":"")+". Bank quantities show real stock.";
	}
    public void refreshTitle()
    {
        if(selected!=null && gateway.bankOpen() && tag.equals(gateway.activeTag())) gateway.title("Loadout: " + SetupCodec.name(selected).replaceAll("<[^>]*>", ""));
    }
	public void clear()
	{
		selected = null;
		waiting = false;
		try { if (tag.equals(gateway.activeTag())) gateway.close(); }
		finally
		{
			try { if (registered) { gateway.unregister(tag); registered = false; } }
			finally { gateway.discardLayout(tag); }
		}
	}
}
