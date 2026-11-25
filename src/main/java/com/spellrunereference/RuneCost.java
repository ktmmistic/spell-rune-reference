package com.spellrunereference;

import lombok.Value;

/**
 * How many of one rune a spell needs, for example 5 Fire.
 */
@Value
public class RuneCost
{
	int amount;
	Rune rune;
}
