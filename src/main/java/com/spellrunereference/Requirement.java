package com.spellrunereference;

import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Something besides Magic level that a spell needs: a quest, an achievement diary, another skill,
 * or (when RuneLite can't check it) just a note.
 */
final class Requirement
{
	private enum Kind
	{
		QUEST, DIARY, SKILL, NOTE
	}

	private final Kind kind;
	private final String description;
	private final Quest quest;
	private final int varbit;
	private final Skill skill;
	private final int level;

	private Requirement(Kind kind, String description, Quest quest, int varbit, Skill skill, int level)
	{
		this.kind = kind;
		this.description = description;
		this.quest = quest;
		this.varbit = varbit;
		this.skill = skill;
		this.level = level;
	}

	static Requirement quest(Quest quest)
	{
		return new Requirement(Kind.QUEST, quest.getName(), quest, -1, null, 0);
	}

	/** An achievement diary tier, checked with the game variable that marks it complete. */
	static Requirement diary(String name, int completeVarbit)
	{
		return new Requirement(Kind.DIARY, name, null, completeVarbit, null, 0);
	}

	static Requirement skill(Skill skill, int level)
	{
		return new Requirement(Kind.SKILL, level + " " + skill.getName(), null, -1, skill, level);
	}

	/** An unlock RuneLite can't detect. Shown as a reminder, never used to dim or hide. */
	static Requirement note(String text)
	{
		return new Requirement(Kind.NOTE, text, null, -1, null, 0);
	}

	/** Text shown to the player, e.g. "Desert Treasure I" or "55 Slayer". */
	String getDescription()
	{
		return description;
	}

	boolean isCheckable()
	{
		return kind != Kind.NOTE;
	}

	/** A stable name for this requirement, used to remember which ones are met. */
	String getKey()
	{
		return kind + ":" + description;
	}

	/**
	 * Whether the logged-in player meets this. Must run on the game's own thread.
	 */
	boolean isMet(Client client)
	{
		switch (kind)
		{
			case QUEST:
				return quest.getState(client) == QuestState.FINISHED;
			case DIARY:
				return client.getVarbitValue(varbit) == 1;
			case SKILL:
				return client.getRealSkillLevel(skill) >= level;
			default:
				return true;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Requirement && ((Requirement) o).getKey().equals(getKey());
	}

	@Override
	public int hashCode()
	{
		return getKey().hashCode();
	}
}
