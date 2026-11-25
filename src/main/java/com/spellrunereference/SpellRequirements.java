package com.spellrunereference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/**
 * Quest, diary and other unlocks each spell needs, besides its Magic level.
 * Taken from the OSRS Wiki spellbook and spell pages, November 2026.
 */
final class SpellRequirements
{
	private static final Requirement DESERT_TREASURE = Requirement.quest(Quest.DESERT_TREASURE_I);
	private static final Requirement LUNAR_DIPLOMACY = Requirement.quest(Quest.LUNAR_DIPLOMACY);
	private static final Requirement DREAM_MENTOR = Requirement.quest(Quest.DREAM_MENTOR);
	private static final Requirement KINGDOM_DIVIDED = Requirement.quest(Quest.A_KINGDOM_DIVIDED);
	private static final Requirement FREMENNIK_HARD = Requirement.diary("Hard Fremennik Diary", VarbitID.FREMENNIK_DIARY_HARD_COMPLETE);

	/** Spell name -> extra requirements, for spells on the Standard and Arceuus books (and a few Lunar ones). */
	private static final Map<String, List<Requirement>> BY_NAME = new HashMap<>();

	/** Lunar spells that also need Dream Mentor (marked * on the wiki's Lunar page). */
	private static final Set<String> DREAM_MENTOR_SPELLS = new HashSet<>(Arrays.asList(
		"Monster Examine", "Stat Spy", "Dream", "Humidify", "Hunter Kit", "Plank Make", "Spellbook Swap"));

	/** Arceuus spells that work without A Kingdom Divided; every other non-teleport Arceuus spell needs it. */
	private static final Set<String> ARCEUUS_NO_QUEST = new HashSet<>(Arrays.asList(
		"Ghostly Grasp", "Inferior Demonbane", "Resurrect Crops",
		"Basic Reanimation", "Adept Reanimation", "Expert Reanimation", "Master Reanimation"));

	static
	{
		// Standard spellbook
		put("Kourend Castle Teleport", Requirement.quest(Quest.CLIENT_OF_KOUREND));
		put("Ardougne Teleport", Requirement.quest(Quest.PLAGUE_CITY));
		put("Civitas illa Fortis Teleport", Requirement.quest(Quest.TWILIGHTS_PROMISE));
		put("Watchtower Teleport", Requirement.quest(Quest.WATCHTOWER));
		put("Trollheim Teleport", Requirement.quest(Quest.EADGARS_RUSE));
		put("Ape Atoll Teleport", Requirement.quest(Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI));
		put("Teleport to Boat", Requirement.quest(Quest.PANDEMONIUM),
			Requirement.note("Requires a greater teleport focus on your boat"));
		put("Summon Boat", Requirement.quest(Quest.PANDEMONIUM),
			Requirement.note("Requires a teleport focus on your boat"));
		put("Iban Blast", Requirement.quest(Quest.UNDERGROUND_PASS));
		put("Magic Dart", Requirement.skill(Skill.SLAYER, 55));
		put("Saradomin Strike", Requirement.quest(Quest.MAGE_ARENA_I),
			Requirement.note("Cast it 100 times in the Mage Arena to use it elsewhere"));
		put("Claws of Guthix", Requirement.quest(Quest.MAGE_ARENA_I),
			Requirement.note("Cast it 100 times in the Mage Arena to use it elsewhere"));
		put("Flames of Zamorak", Requirement.quest(Quest.MAGE_ARENA_I),
			Requirement.note("Cast it 100 times in the Mage Arena to use it elsewhere"));
		put("Charge", Requirement.quest(Quest.MAGE_ARENA_I));
		put("Bones to Peaches", Requirement.note("Unlock it at the Mage Training Arena"));

		// Arceuus teleports
		put("Salve Graveyard Teleport", Requirement.quest(Quest.PRIEST_IN_PERIL));
		put("Fenkenstrain's Castle Teleport", Requirement.quest(Quest.PRIEST_IN_PERIL));
		put("Barrows Teleport", Requirement.quest(Quest.PRIEST_IN_PERIL));
		put("West Ardougne Teleport", Requirement.quest(Quest.BIOHAZARD));
		put("Harmony Island Teleport", Requirement.quest(Quest.THE_GREAT_BRAIN_ROBBERY));

		// Lunar extras (Lunar Diplomacy itself is added for every Lunar spell below)
		put("Tan Leather", FREMENNIK_HARD);
		put("Recharge Dragonstone", FREMENNIK_HARD);
		put("Ourania Teleport", Requirement.note("Unlock it by talking to Baba Yaga with the Lunar spellbook"));
	}

	private SpellRequirements()
	{
	}

	private static void put(String spellName, Requirement... requirements)
	{
		BY_NAME.put(spellName, Arrays.asList(requirements));
	}

	static List<Requirement> of(Spellbook book, Spell spell)
	{
		String name = spell.getName();
		List<Requirement> result = new ArrayList<>();

		switch (book)
		{
			case ANCIENT:
				result.add(DESERT_TREASURE);
				break;
			case LUNAR:
				result.add(LUNAR_DIPLOMACY);
				if (DREAM_MENTOR_SPELLS.contains(name))
				{
					result.add(DREAM_MENTOR);
				}
				break;
			case ARCEUUS:
				if (name.equals("Ape Atoll Teleport"))
				{
					// The Arceuus version is unlocked by a different quest than the Standard one.
					result.add(Requirement.quest(Quest.MONKEY_MADNESS_I));
					return result;
				}
				if (SpellType.of(book, spell) != SpellType.TELEPORT && !ARCEUUS_NO_QUEST.contains(name))
				{
					result.add(KINGDOM_DIVIDED);
				}
				break;
			default:
				break;
		}

		List<Requirement> extra = BY_NAME.get(name);
		if (extra != null)
		{
			result.addAll(extra);
		}
		return result;
	}

	/** Every requirement the game can check, so the plugin can look them all up at once. */
	static Set<Requirement> allCheckable()
	{
		Set<Requirement> all = new LinkedHashSet<>();
		for (Spellbook book : Spellbook.values())
		{
			for (Spell spell : book.getSpells())
			{
				for (Requirement requirement : of(book, spell))
				{
					if (requirement.isCheckable())
					{
						all.add(requirement);
					}
				}
			}
		}
		return Collections.unmodifiableSet(all);
	}
}
