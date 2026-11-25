package com.spellrunereference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Every rune a spell can cost, linked to its in-game item so RuneLite can draw its sprite.
 */
@Getter
@RequiredArgsConstructor
public enum Rune
{
	AIR("Air", ItemID.AIRRUNE),
	WATER("Water", ItemID.WATERRUNE),
	EARTH("Earth", ItemID.EARTHRUNE),
	FIRE("Fire", ItemID.FIRERUNE),
	MIND("Mind", ItemID.MINDRUNE),
	BODY("Body", ItemID.BODYRUNE),
	COSMIC("Cosmic", ItemID.COSMICRUNE),
	CHAOS("Chaos", ItemID.CHAOSRUNE),
	NATURE("Nature", ItemID.NATURERUNE),
	LAW("Law", ItemID.LAWRUNE),
	DEATH("Death", ItemID.DEATHRUNE),
	ASTRAL("Astral", ItemID.ASTRALRUNE),
	BLOOD("Blood", ItemID.BLOODRUNE),
	SOUL("Soul", ItemID.SOULRUNE),
	WRATH("Wrath", ItemID.WRATHRUNE);

	private final String displayName;
	private final int itemId;

	static Rune fromName(String name)
	{
		for (Rune rune : values())
		{
			if (rune.displayName.equalsIgnoreCase(name))
			{
				return rune;
			}
		}
		return null;
	}
}
