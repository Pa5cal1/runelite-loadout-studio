/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SetupLibrary
{
	public int version = 1;
	public List<Section> sections = new ArrayList<>();
	public List<Entry> setups = new ArrayList<>();
	public static final class Section
	{
		public String id = UUID.randomUUID().toString();
		public String name;
		public Section(String name) { this.name = name; }
	}
	public static final class Entry
	{
		public String id = UUID.randomUUID().toString();
		public String section;
		public boolean favorite;
		public boolean archived;
		public long lastOpened;
		public BankLayouts.Mode bankLayout;
		public boolean zigzagBottomToTop;
		public JsonObject portable;
		public Entry(JsonObject p) { portable = p.deepCopy(); }
	}
	public void validate()
	{
		if (version != 1 || sections == null || setups == null || sections.size() > 100 || setups.size() > 200) throw new IllegalArgumentException("Unsupported or excessive library.");
		Set<String> ids = new HashSet<>(), sectionIds = new HashSet<>();
		for (Section section : sections) { if (section == null || section.id == null || !ids.add(section.id) || section.name == null || section.name.trim().isEmpty() || section.name.length() > 60) throw new IllegalArgumentException("Invalid section."); sectionIds.add(section.id); }
		for (Entry entry : setups) { if (entry == null || entry.id == null || !ids.add(entry.id) || (entry.section != null && !sectionIds.contains(entry.section))) throw new IllegalArgumentException("Invalid saved setup."); SetupCodec.validate(entry.portable, false); }
	}
	public String serialize(Gson gson) { validate(); String text = gson.toJson(this); if (text.length() > 1_000_000) throw new IllegalArgumentException("Library is full. Export a backup."); return text; }
	public static SetupLibrary read(String text, Gson gson)
	{
		if (text == null || text.isEmpty()) return new SetupLibrary();
		new SetupCodec(gson).parse(text);
		SetupLibrary result = gson.fromJson(text, SetupLibrary.class); if (result == null) throw new IllegalArgumentException("Invalid library."); result.validate(); return result;
	}
	public void removeSection(String id) { sections.removeIf(s -> s.id.equals(id)); for (Entry entry : setups) if (id.equals(entry.section)) entry.section = null; }
	public boolean removeArchived(String id) { Entry entry = find(id); return entry != null && entry.archived && setups.remove(entry); }
	public Entry find(String id) { return setups.stream().filter(e -> e.id.equals(id)).findFirst().orElse(null); }
}
