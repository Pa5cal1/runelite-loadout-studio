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

    @ConfigSection(name="Wiki",description="On-demand public sources; no startup or background requests",position=5,closedByDefault=true)
    String wiki="wiki";
    @ConfigSection(name="Editing",description="Chatbox and loadout editing",position=6,closedByDefault=true)
    String editing="editing";
    @ConfigItem(keyName="wikiFreshDays",name="Refresh source after days",description="On opening a source, refresh if its stored copy is older than this many days. No background polling",section=wiki,position=0)
    @Range(min=1,max=30)
    default int wikiFreshDays(){return 7;}
    @ConfigItem(keyName="autoAlternatives",name="Find owned alternatives",description="Determine local, setup-specific alternatives after opening a setup or Bank change; never contacts the Wiki",section=bank,position=1)
    default boolean autoAlternatives(){return true;}
    @ConfigItem(keyName="maxAlternatives",name="Alternatives per item",description="Maximum number of ranked source or bundled alternatives shown per missing item",section=bank,position=2)
    @Range(min=1,max=5)
    default int maxAlternatives(){return 3;}
    @ConfigItem(keyName="autoShowAlternatives",name="Show owned alternatives",description="Expand the compact alternatives list once per setup when compatible owned options exist",section=bank,position=3)
    default boolean autoShowAlternatives(){return true;}
    @ConfigItem(keyName="defaultHighlight",name="Default highlighting",description="Highlight differences in new setups; use each setup's marker button to override",section=defaults,position=6)
    default boolean defaultHighlight(){return true;}
    @ConfigItem(keyName="defaultHighlightColor",name="Default highlight color",description="Difference border color for new setups",section=defaults,position=7)
    default java.awt.Color defaultHighlightColor(){return java.awt.Color.RED;}
    @ConfigItem(keyName="defaultMissingOnly",name="Default missing items only",description="Highlight missing Inventory items without enforcing slot order in new setups",section=defaults,position=8)
    default boolean defaultMissingOnly(){return false;}
    @ConfigItem(keyName="emptyQuantityInput",name="Empty quantity input",description="Open amount entry empty so a new quantity can be typed immediately",section=editing,position=0)
    default boolean emptyQuantityInput(){return true;}
    @ConfigItem(keyName="showAttackOption",name="Show attack option",description="Show the saved attack option under the loadout title",section=sidebar,position=2)
    default boolean showAttackOption(){return true;}
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

    static boolean isPreferenceKey(String key){return java.util.Arrays.asList("libraryOrder","showSupportButton","ignoreAmmoQuantities","autoAlternatives","autoShowAlternatives","maxAlternatives","showAttackOption").contains(key);}
}
