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

    void add(int id, String membersName, boolean stackable, int noteTemplate, int placeholderTemplate)
    {
        if (id < 1 || noteTemplate != -1 || placeholderTemplate != -1 || membersName == null || membersName.trim().isEmpty() || membersName.equalsIgnoreCase("null")) return;
        items.put(id, new StudioPanel.ItemInfo(id, membersName, stackable, -1, null));
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
    boolean isEmpty() { return items.isEmpty(); }
    void clear() { items.clear(); }
}
