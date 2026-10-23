/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import net.runelite.client.config.*;

@ConfigGroup(LoadoutStudioConfig.GROUP)
public interface LoadoutStudioConfig extends Config
{
    String GROUP = "loadout-studio";
    @ConfigSection(name="Default loadouts",description="Defaults for new blank setups and captured gear; saved and imported setups keep their own values",position=0,closedByDefault=true)
    String defaults="defaults";
    @ConfigSection(name="Comparison",description="General gear comparison preferences",position=1,closedByDefault=true)
    String comparison="comparison";
    @ConfigSection(name="Sidebar",description="Library and sidebar presentation",position=2,closedByDefault=true)
    String sidebar="sidebar";
    @ConfigSection(name="Hotkeys",description="Keyboard shortcuts",position=3,closedByDefault=true)
    String hotkeys="hotkeys";
    @ConfigSection(name="Bank",description="Bank menu preferences",position=4,closedByDefault=true)
    String bank="bank";

    enum LibraryOrder {
        ALPHABETICAL("Alphabetical"),RECENT("Recent"),FAVORITES("Favorites first"),MANUAL("Manual");
        private final String label;LibraryOrder(String label){this.label=label;}public String toString(){return label;}
    }
    enum DefaultLayout { PRESET,ZIGZAG;public String toString(){return this==PRESET?"Preset":"Zigzag";} }
    enum StackRule {
        NONE("None","Ignore amount"),EXACT("Standard","Exactly"),MINIMUM("Less_Than","At least"),MAXIMUM("Greater_Than","At most");
        final String code,label;StackRule(String code,String label){this.code=code;this.label=label;}public String toString(){return label;}
    }
    @ConfigItem(keyName="defaultLayout",name="Default layout",description="Initial Bank layout for new blank or captured setups",section=defaults,position=0)
    default DefaultLayout defaultLayout(){return DefaultLayout.PRESET;}
    @ConfigItem(keyName="defaultZigzagBottom",name="Zigzag bottom row first",description="Starting direction for new Zigzag layouts",section=defaults,position=1)
    default boolean defaultZigzagBottom(){return false;}
    @ConfigItem(keyName="defaultFilterBank",name="Default Filter Bank",description="Enable Bank filtering for new blank and captured setups",section=defaults,position=2)
    default boolean defaultFilterBank(){return true;}
    @ConfigItem(keyName="defaultFuzzy",name="Default Fuzzy",description="Allow supported item variants in newly captured gear; imports keep their own rules",section=defaults,position=3)
    default boolean defaultFuzzy(){return false;}
    @ConfigItem(keyName="defaultStackRule",name="Default amount rule",description="Initial quantity comparison rule for newly captured items",section=defaults,position=4)
    default StackRule defaultStackRule(){return StackRule.NONE;}
    @ConfigItem(keyName="captureAttackOption",name="Capture attack option",description="Include the current attack option in new gear captures; existing setups are unchanged",section=defaults,position=5)
    default boolean captureAttackOption(){return true;}
    @ConfigItem(keyName="ignoreAmmoQuantities",name="Ignore Ammo quantities",description="Ignore worn Ammo and Quiver stack sizes when comparing or exporting. Rune Pouch amounts remain checked",section=comparison,position=0)
    default boolean ignoreAmmoQuantities(){return false;}
    @ConfigItem(keyName="checkAlternativeStock",name="Check replacement stock",description="Check combined Inventory and Equipment amounts before applying owned replacements. Bank observations may be outdated; other storage is not included",section=comparison,position=1)
    default boolean checkAlternativeStock(){return true;}
    @ConfigItem(keyName="libraryOrder",name="Library order",description="Choose alphabetical, recent, favorite-first or manual library order",section=sidebar,position=0)
    default LibraryOrder libraryOrder(){return LibraryOrder.ALPHABETICAL;}
    @ConfigItem(keyName="showSupportButton",name="Show coffee button",description="Show the optional support link in the sidebar header",section=sidebar,position=1)
    default boolean showSupportButton(){return true;}
    @ConfigItem(keyName="filterHotkey",name="Toggle Bank filter",description="Toggle the active Loadout Studio filter while the Bank is open",section=hotkeys,position=0)
    default Keybind filterHotkey(){return Keybind.NOT_SET;}
    @ConfigItem(keyName="bankMenu",name="Bank setup shortcuts",description="Show up to five favorite or recent setups on the Bank worn-items button",section=bank,position=0)
    default boolean bankMenu(){return true;}

    static boolean isPreferenceKey(String key){return java.util.Arrays.asList("libraryOrder","showSupportButton","ignoreAmmoQuantities").contains(key);}
}
