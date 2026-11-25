package com.spellrunereference;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Spell Rune Lookup",
	description = "Shows which runes every spell needs, for all four spellbooks",
	tags = {"spell", "rune", "magic", "spellbook", "bank", "reference", "quest"}
)
public class SpellRuneReferencePlugin extends Plugin
{
	/** How often to recheck quests and diaries while logged in: 100 game ticks is about one minute. */
	private static final int UNLOCK_CHECK_INTERVAL_TICKS = 100;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SpellRuneReferenceConfig config;

	private SpellRuneReferencePanel panel;
	private NavigationButton navButton;
	/** Counts down to the next quest check; 0 means "check on the next tick". */
	private int ticksUntilUnlockCheck;

	@Override
	protected void startUp()
	{
		panel = new SpellRuneReferencePanel(itemManager, config, configManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Spell Rune Lookup")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// If the plugin is turned on while already logged in, read the Magic level now.
		// Game data has to be read on the game's own thread.
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				sendMagicLevel(client.getRealSkillLevel(Skill.MAGIC));
			}
		});
		ticksUntilUnlockCheck = 0;
		log.debug("Spell Rune Lookup started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		log.debug("Spell Rune Lookup stopped");
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Fires on login and whenever you level up. getLevel() is the real level, so potions don't count.
		if (event.getSkill() == Skill.MAGIC)
		{
			sendMagicLevel(event.getLevel());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Quest progress loads with the player, so check on the next tick.
			ticksUntilUnlockCheck = 0;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Logged out: level and quests are unknown again, so stop dimming or hiding.
			sendMagicLevel(SpellRuneReferencePanel.UNKNOWN_LEVEL);
			sendUnlocks(null);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Runs on the game thread, which is where quest progress has to be read.
		if (client.getGameState() != GameState.LOGGED_IN || --ticksUntilUnlockCheck > 0)
		{
			return;
		}
		ticksUntilUnlockCheck = UNLOCK_CHECK_INTERVAL_TICKS;

		Set<String> met = new HashSet<>();
		for (Requirement requirement : SpellRequirements.allCheckable())
		{
			if (requirement.isMet(client))
			{
				met.add(requirement.getKey());
			}
		}
		sendUnlocks(met);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SpellRuneReferenceConfig.GROUP.equals(event.getGroup()) || panel == null)
		{
			return;
		}
		// Keeps the panel in sync when a setting is changed from RuneLite's settings screen.
		SpellRuneReferencePanel current = panel;
		SwingUtilities.invokeLater(current::refreshFromConfig);
	}

	/** Game events arrive on the game thread; the panel must be updated on Swing's thread. */
	private void sendMagicLevel(int level)
	{
		SpellRuneReferencePanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setMagicLevel(level));
		}
	}

	/** @param met keys of the requirements the player meets, or null when logged out (unknown). */
	private void sendUnlocks(Set<String> met)
	{
		SpellRuneReferencePanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setMetRequirements(met));
		}
	}

	@Provides
	SpellRuneReferenceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpellRuneReferenceConfig.class);
	}
}
