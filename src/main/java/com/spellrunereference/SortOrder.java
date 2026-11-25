package com.spellrunereference;

import java.util.Comparator;

/**
 * How the non-favorite spells are ordered.
 */
public enum SortOrder
{
	LEVEL_LOW_HIGH("Lvl 1-99", Comparator.comparingInt(Spell::getLevelNumber)),
	LEVEL_HIGH_LOW("Lvl 99-1", Comparator.comparingInt(Spell::getLevelNumber).reversed()),
	NAME("A-Z", Comparator.comparing(Spell::getName, String.CASE_INSENSITIVE_ORDER));

	private final String displayName;
	private final Comparator<Spell> comparator;

	SortOrder(String displayName, Comparator<Spell> comparator)
	{
		this.displayName = displayName;
		this.comparator = comparator;
	}

	Comparator<Spell> getComparator()
	{
		return comparator;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
