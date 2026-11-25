package com.spellrunereference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The three spell categories the OSRS Wiki uses on every spellbook page.
 */
public enum SpellType
{
	COMBAT("Combat"),
	TELEPORT("Teleport"),
	UTILITY("Utility");

	private final String displayName;

	SpellType(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	/**
	 * Spells the wiki lists under "Combat spells" (including support and curse spells) on the
	 * Standard, Lunar and Arceuus pages. Every non-teleport Ancient spell is combat.
	 */
	private static final Set<String> COMBAT_SPELLS = new HashSet<>(Arrays.asList(
		// Standard: offensive
		"Wind Strike", "Water Strike", "Earth Strike", "Fire Strike",
		"Wind Bolt", "Water Bolt", "Earth Bolt", "Fire Bolt", "Crumble Undead",
		"Wind Blast", "Water Blast", "Earth Blast", "Fire Blast", "Iban Blast", "Magic Dart",
		"Saradomin Strike", "Claws of Guthix", "Flames of Zamorak",
		"Wind Wave", "Water Wave", "Earth Wave", "Fire Wave",
		"Wind Surge", "Water Surge", "Earth Surge", "Fire Surge",
		// Standard: support and curses
		"Charge", "Confuse", "Weaken", "Curse", "Bind", "Snare", "Vulnerability",
		"Enfeeble", "Entangle", "Stun", "Tele Block",
		// Lunar
		"Monster Examine", "Cure Other", "Cure Me", "Cure Group", "Stat Spy", "Dream",
		"Stat Restore Pot Share", "Boost Potion Share", "Energy Transfer", "Heal Other",
		"Vengeance Other", "Vengeance", "Heal Group",
		// Arceuus
		"Ghostly Grasp", "Skeletal Grasp", "Undead Grasp",
		"Inferior Demonbane", "Superior Demonbane", "Dark Demonbane",
		"Lesser Corruption", "Greater Corruption",
		"Resurrect Lesser Ghost / Skeleton / Zombie", "Resurrect Superior Ghost / Skeleton / Zombie",
		"Resurrect Greater Ghost / Skeleton / Zombie",
		"Dark Lure", "Mark of Darkness", "Ward of Arceuus"
	));

	static SpellType of(Spellbook book, Spell spell)
	{
		String name = spell.getName();
		// "Tele Block" doesn't contain "Teleport", so it stays a combat (curse) spell.
		if (name.contains("Teleport") || name.startsWith("Tele Group") || name.startsWith("Teleother"))
		{
			return TELEPORT;
		}
		if (book == Spellbook.ANCIENT || COMBAT_SPELLS.contains(name))
		{
			return COMBAT;
		}
		return UTILITY;
	}
}
