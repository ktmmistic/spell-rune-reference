package com.spellrunereference;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SpellRuneReferenceConfig.GROUP)
public interface SpellRuneReferenceConfig extends Config
{
	String GROUP = "spellrunereference";
	String SHOW_RUNE_ICONS = "showRuneIcons";
	String LOCKED_SPELLS = "lockedSpells";
	/** Followed by the spellbook, e.g. "favorites_LUNAR". Saved from the panel's stars, not shown in settings. */
	String FAVORITES_PREFIX = "favorites_";
	String FAVORITES_COLLAPSED = "favoritesCollapsed";
	/** The panel's Type and Sort dropdowns, remembered between sessions. */
	String TYPE_FILTER = "typeFilter";
	String SORT_ORDER = "sortOrder";

	@ConfigItem(
		keyName = SHOW_RUNE_ICONS,
		name = "Show rune icons",
		description = "Show each spell's runes as in-game rune sprites with amounts, instead of as text",
		position = 1
	)
	default boolean showRuneIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = LOCKED_SPELLS,
		name = "Spells I can't cast yet",
		description = "Show, dim or hide spells above your Magic level or locked behind a quest or diary you haven't finished. "
			+ "Checked once you're logged in. Favorites are never hidden.",
		position = 2
	)
	default LockedSpellMode lockedSpells()
	{
		return LockedSpellMode.SHOW;
	}
}
