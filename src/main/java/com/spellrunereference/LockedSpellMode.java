package com.spellrunereference;

/**
 * What to do with spells you can't cast yet, because your Magic level is too low or you haven't
 * finished a quest or diary they need. Chosen in the plugin's settings.
 */
public enum LockedSpellMode
{
	SHOW("Show"),
	DIM("Dim"),
	HIDE("Hide");

	private final String displayName;

	LockedSpellMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
