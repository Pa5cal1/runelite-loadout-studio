/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemCatalogueTest
{
    @Test public void notedAndPlaceholderCopiesDoNotMakeRealItemAmbiguous()
    {
        ItemCatalogue catalogue = new ItemCatalogue();
        catalogue.add(22978, "Dragon hunter lance", false, -1, -1);
        catalogue.add(22979, "Dragon hunter lance", true, 799, -1);
        catalogue.add(22980, "Dragon hunter lance", false, -1, 14401);
        assertEquals(1, catalogue.size());
        assertEquals(22978, catalogue.exact("dragon hunter lance").id);
        assertEquals(22978, catalogue.search("hunter lance").get(0).id);
    }

    @Test public void genuineSameNameVariantsAreNotGuessedAndSearchLimitDoesNotHideAmbiguity()
    {
        ItemCatalogue catalogue = new ItemCatalogue();
        for (int i = 1; i <= 110; i++) catalogue.add(i, "Rune pouch", false, -1, -1);
        assertEquals(100, catalogue.search("Rune pouch").size());
        assertNull(catalogue.exact("Rune pouch"));
        assertNull(catalogue.exact("{{Cheap food}}"));
    }

    @Test public void sourceMembersNameIsUsableAndInvalidDefinitionsAreExcluded()
    {
        ItemCatalogue catalogue = new ItemCatalogue();
        catalogue.add(26382, "Torva full helm", false, -1, -1);
        catalogue.add(0, "Unsupported zero ID", false, -1, -1);
        catalogue.add(1, "null", false, -1, -1);
        catalogue.add(2, null, false, -1, -1);
        assertEquals(26382, catalogue.exact("Torva full helm").id);
        assertEquals(1, catalogue.size());
    }
}
