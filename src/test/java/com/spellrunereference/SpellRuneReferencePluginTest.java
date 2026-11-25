package com.spellrunereference;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SpellRuneReferencePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SpellRuneReferencePlugin.class);
		RuneLite.main(args);
	}
}
