package com.spellrunereference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Value;

/**
 * One spell and what it costs to cast. Names and rune costs come from the OSRS Wiki.
 */
@Value
public class Spell
{
	/** Magic level needed, as text so spells with no level can show "N/A". */
	String level;
	String name;
	/** Written the way the wiki lists it, e.g. "5 Fire, 1 Death (+ Iban's staff)". */
	String runes;

	/**
	 * The level as a number. "N/A" (home and minigame teleports) counts as 0, since anyone can cast them.
	 */
	public int getLevelNumber()
	{
		try
		{
			return Integer.parseInt(level);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	/**
	 * The runes as separate amounts, so they can be drawn as sprites. Empty for spells that cost nothing.
	 */
	public List<RuneCost> getRuneCosts()
	{
		String main = runes;
		int extraStart = main.indexOf("(+");
		if (extraStart >= 0)
		{
			main = main.substring(0, extraStart);
		}

		List<RuneCost> costs = new ArrayList<>();
		for (String part : main.split(","))
		{
			String[] amountAndRune = part.trim().split(" ", 2);
			if (amountAndRune.length != 2)
			{
				continue;
			}

			Rune rune = Rune.fromName(amountAndRune[1].trim());
			if (rune == null)
			{
				continue;
			}

			try
			{
				costs.add(new RuneCost(Integer.parseInt(amountAndRune[0]), rune));
			}
			catch (NumberFormatException ignored)
			{
				// Not an amount, so not a rune cost.
			}
		}
		return Collections.unmodifiableList(costs);
	}

	/**
	 * Anything needed besides runes, e.g. "+ Iban's staff", or null if nothing else is needed.
	 */
	public String getExtraRequirement()
	{
		int start = runes.indexOf("(+");
		int end = runes.lastIndexOf(')');
		if (start < 0 || end <= start)
		{
			return null;
		}
		return runes.substring(start + 1, end).trim();
	}
}
