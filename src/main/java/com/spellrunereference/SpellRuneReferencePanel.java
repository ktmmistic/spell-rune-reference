package com.spellrunereference;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * The side panel: pick a spellbook, optionally search, and see each spell's level and runes,
 * either as text or as rune sprites with amounts.
 */
class SpellRuneReferencePanel extends PluginPanel
{
	/** Panel width minus the panel's side padding (10 + 10) and each row's side padding (8 + 8). */
	private static final int ROW_TEXT_WIDTH = PANEL_WIDTH - 36;
	private static final int MAX_FAVORITES = 3;
	private static final String FAVORITE_SEPARATOR = "|";
	private static final String FAVORITE_SEPARATOR_REGEX = "\\|";
	/** The RuneScape fonts have no star or arrow characters, so those use a standard font. */
	private static final Font SYMBOL_FONT = new Font(Font.DIALOG, Font.PLAIN, 14);
	private static final Color STAR_DISABLED = new Color(80, 80, 80);
	private static final String ALL_TYPES = "All types";
	private static final String ALL_BOOKS = "All spellbooks";
	/** Used before login and after logout, when the player's Magic level isn't known. */
	static final int UNKNOWN_LEVEL = 0;
	/** Laid over a dimmed row; the same shade as the row background, mostly opaque. */
	private static final Color DIM_OVERLAY = new Color(30, 30, 30, 170);
	/** A soft red for the "Needs ..." lines, readable on the dark rows. */
	private static final Color LOCKED_TEXT = new Color(230, 110, 100);

	private int magicLevel = UNKNOWN_LEVEL;
	/** Keys of the quests/diaries/skills the player has met, or null when not logged in (unknown). */
	private Set<String> metRequirements;

	private final ItemManager itemManager;
	private final SpellRuneReferenceConfig config;
	private final ConfigManager configManager;

	private final JComboBox<Object> spellbookPicker = new JComboBox<>(new Object[]{
		ALL_BOOKS, Spellbook.STANDARD, Spellbook.ANCIENT, Spellbook.LUNAR, Spellbook.ARCEUUS});
	private final IconTextField searchBar = new IconTextField();
	private final JComboBox<Object> typePicker = new JComboBox<>(new Object[]{
		ALL_TYPES, SpellType.COMBAT, SpellType.TELEPORT, SpellType.UTILITY});
	private final JComboBox<SortOrder> sortPicker = new JComboBox<>(SortOrder.values());
	private final JCheckBox runeIconsToggle = new JCheckBox("Show rune icons");
	private final JPanel spellList = new JPanel();

	SpellRuneReferencePanel(ItemManager itemManager, SpellRuneReferenceConfig config, ConfigManager configManager)
	{
		super();
		this.itemManager = itemManager;
		this.config = config;
		this.configManager = configManager;

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 8));

		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 6));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Spell Rune Lookup");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		controls.add(title);

		spellbookPicker.setFocusable(false);
		spellbookPicker.setToolTipText("Pick \"All spellbooks\" to search every book at once");
		spellbookPicker.setSelectedItem(Spellbook.STANDARD);
		spellbookPicker.addActionListener(e -> rebuildListFromTop());
		controls.add(spellbookPicker);

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addClearListener(this::rebuildListFromTop);
		searchBar.getDocument().addDocumentListener(new SimpleDocumentListener(this::rebuildListFromTop));
		controls.add(searchBar);

		// Type filter and sort order, side by side. Restore what was picked last time.
		typePicker.setFocusable(false);
		typePicker.setToolTipText("Show only one type of spell");
		typePicker.setSelectedItem(savedType());
		typePicker.addActionListener(e ->
		{
			Object choice = typePicker.getSelectedItem();
			configManager.setConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.TYPE_FILTER,
				choice instanceof SpellType ? ((SpellType) choice).name() : ALL_TYPES);
			rebuildListFromTop();
		});

		sortPicker.setFocusable(false);
		sortPicker.setToolTipText("Sort order (favorites keep their own order)");
		sortPicker.setSelectedItem(savedSortOrder());
		sortPicker.addActionListener(e ->
		{
			SortOrder choice = (SortOrder) sortPicker.getSelectedItem();
			if (choice != null)
			{
				configManager.setConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.SORT_ORDER, choice.name());
			}
			rebuildListFromTop();
		});

		JPanel filterRow = new JPanel(new GridLayout(1, 2, 6, 0));
		filterRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filterRow.add(typePicker);
		filterRow.add(sortPicker);
		controls.add(filterRow);

		runeIconsToggle.setBackground(ColorScheme.DARK_GRAY_COLOR);
		runeIconsToggle.setForeground(ColorScheme.TEXT_COLOR);
		runeIconsToggle.setFocusable(false);
		runeIconsToggle.setSelected(config.showRuneIcons());
		// Saving the choice to RuneLite's settings means it's remembered next time.
		// The plugin hears the change and calls refreshFromConfig, which redraws the list.
		runeIconsToggle.addActionListener(e -> configManager.setConfiguration(
			SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.SHOW_RUNE_ICONS, runeIconsToggle.isSelected()));
		controls.add(runeIconsToggle);

		spellList.setLayout(new BoxLayout(spellList, BoxLayout.Y_AXIS));
		spellList.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(controls, BorderLayout.NORTH);
		add(spellList, BorderLayout.CENTER);

		rebuildList();
	}

	/**
	 * Called when the setting changes, from this panel's checkbox or from RuneLite's settings screen.
	 */
	void refreshFromConfig()
	{
		runeIconsToggle.setSelected(config.showRuneIcons());
		rebuildList();
	}

	/**
	 * Called by the plugin on login, on level-up, and with UNKNOWN_LEVEL on logout.
	 */
	void setMagicLevel(int level)
	{
		if (level != magicLevel)
		{
			magicLevel = level;
			rebuildList();
		}
	}

	/**
	 * Called by the plugin after it checks quests and diaries, and with null on logout.
	 */
	void setMetRequirements(Set<String> met)
	{
		if (!Objects.equals(met, metRequirements))
		{
			metRequirements = met;
			rebuildList();
		}
	}

	private boolean isAboveMyLevel(Spell spell)
	{
		return magicLevel != UNKNOWN_LEVEL && spell.getLevelNumber() > magicLevel;
	}

	/** Quests, diaries or skills this spell needs that the player hasn't done. Empty when logged out. */
	private List<Requirement> unmetRequirements(Spellbook book, Spell spell)
	{
		List<Requirement> unmet = new ArrayList<>();
		if (metRequirements == null)
		{
			return unmet;
		}
		for (Requirement requirement : SpellRequirements.of(book, spell))
		{
			if (requirement.isCheckable() && !metRequirements.contains(requirement.getKey()))
			{
				unmet.add(requirement);
			}
		}
		return unmet;
	}

	/** True when the player can't cast this yet: Magic level too low, or a quest/diary/skill missing. */
	private boolean isLocked(Spellbook book, Spell spell)
	{
		return isAboveMyLevel(spell) || !unmetRequirements(book, spell).isEmpty();
	}

	/**
	 * Rebuilds the list and jumps back to the top. Used when you pick a spellbook, type, sort or search,
	 * because the old scroll position no longer means anything. Starring a spell or collapsing
	 * Favorites uses plain rebuildList, so the list stays where you are.
	 */
	private void rebuildListFromTop()
	{
		rebuildList();
		// Wait until Swing has laid out the new list, then scroll the panel to its top.
		SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
	}

	private void rebuildList()
	{
		spellList.removeAll();

		// One spellbook, or all four when "All spellbooks" is picked.
		Object bookChoice = spellbookPicker.getSelectedItem();
		boolean allBooks = !(bookChoice instanceof Spellbook);
		List<Spellbook> books = new ArrayList<>();
		if (allBooks)
		{
			books.addAll(Arrays.asList(Spellbook.values()));
		}
		else
		{
			books.add((Spellbook) bookChoice);
		}

		String query = searchBar.getText() == null ? "" : searchBar.getText().trim().toLowerCase(Locale.ROOT);
		boolean showIcons = config.showRuneIcons();
		LockedSpellMode lockedMode = config.lockedSpells();
		boolean dimLocked = lockedMode != LockedSpellMode.SHOW;
		int shown = 0;

		// Favorites are saved per spellbook (3 each). "All spellbooks" shows every book's favorites together.
		Map<Spellbook, List<String>> favoritesByBook = new EnumMap<>(Spellbook.class);
		int favoriteTotal = 0;
		for (Spellbook book : books)
		{
			List<String> favorites = getFavorites(book);
			favoritesByBook.put(book, favorites);
			favoriteTotal += favorites.size();
		}

		if (favoriteTotal > 0)
		{
			boolean collapsed = isFavoritesCollapsed();
			String count = allBooks ? "(" + favoriteTotal + ")" : "(" + favoriteTotal + "/" + MAX_FAVORITES + ")";
			spellList.add(createSectionHeader("Favorites " + count, true, collapsed));

			if (!collapsed)
			{
				for (Spellbook book : books)
				{
					List<String> favorites = favoritesByBook.get(book);
					// Favorites keep the order you starred them in.
					for (String favoriteName : favorites)
					{
						Spell spell = findSpell(book, favoriteName);
						if (spell != null && matches(spell, query))
						{
							// Favorites are never hidden, only dimmed, because you picked them on purpose.
							boolean dim = dimLocked && isLocked(book, spell);
							spellList.add(createRow(book, spell, showIcons, true, favorites.size(), dim, allBooks));
							shown++;
						}
					}
				}
			}
		}

		// The Type and Sort dropdowns only change this part of the list; favorites stay in starred order.
		Object typeChoice = typePicker.getSelectedItem();
		SpellType typeFilter = typeChoice instanceof SpellType ? (SpellType) typeChoice : null;
		SortOrder sortOrder = (SortOrder) sortPicker.getSelectedItem();

		List<BookSpell> others = new ArrayList<>();
		int hiddenLocked = 0;
		for (Spellbook book : books)
		{
			for (Spell spell : book.getSpells())
			{
				// Favorites move to the top, so they aren't listed twice.
				if (favoritesByBook.get(book).contains(spell.getName()) || !matches(spell, query))
				{
					continue;
				}
				if (typeFilter != null && SpellType.of(book, spell) != typeFilter)
				{
					continue;
				}
				if (lockedMode == LockedSpellMode.HIDE && isLocked(book, spell))
				{
					hiddenLocked++;
					continue;
				}
				others.add(new BookSpell(book, spell));
			}
		}
		if (sortOrder != null)
		{
			// A stable sort, so spells with the same level keep the wiki's order
			// (and, for "All spellbooks", the Standard, Ancient, Lunar, Arceuus order).
			Comparator<Spell> bySpell = sortOrder.getComparator();
			others.sort((a, b) -> bySpell.compare(a.spell, b.spell));
		}

		String heading = (typeFilter == null ? "All" : typeFilter.toString()) + " spells (" + others.size() + ")";
		spellList.add(createSectionHeader(heading, false, false));

		// A short note so it's never a mystery why spells are dimmed or missing.
		if (lockedMode != LockedSpellMode.SHOW && magicLevel == UNKNOWN_LEVEL && metRequirements == null)
		{
			spellList.add(createNote("Log in to " + lockedMode.toString().toLowerCase(Locale.ROOT)
				+ " spells you can't cast yet."));
		}
		else if (hiddenLocked > 0)
		{
			spellList.add(createNote(hiddenLocked + " hidden that you can't cast yet (Magic level or quests)."));
		}

		for (BookSpell entry : others)
		{
			boolean dim = dimLocked && isLocked(entry.book, entry.spell);
			int favoriteCount = favoritesByBook.get(entry.book).size();
			spellList.add(createRow(entry.book, entry.spell, showIcons, false, favoriteCount, dim, allBooks));
			shown++;
		}

		if (shown == 0)
		{
			JLabel empty = new JLabel("No spells match.", SwingConstants.CENTER);
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
			spellList.add(empty);
		}

		spellList.revalidate();
		spellList.repaint();
	}

	private Object savedType()
	{
		String saved = configManager.getConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.TYPE_FILTER);
		for (SpellType type : SpellType.values())
		{
			if (type.name().equals(saved))
			{
				return type;
			}
		}
		return ALL_TYPES;
	}

	private SortOrder savedSortOrder()
	{
		String saved = configManager.getConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.SORT_ORDER);
		for (SortOrder order : SortOrder.values())
		{
			if (order.name().equals(saved))
			{
				return order;
			}
		}
		return SortOrder.LEVEL_LOW_HIGH;
	}

	private static boolean matches(Spell spell, String query)
	{
		return query.isEmpty()
			|| spell.getName().toLowerCase(Locale.ROOT).contains(query)
			|| spell.getRunes().toLowerCase(Locale.ROOT).contains(query);
	}

	private static Spell findSpell(Spellbook book, String name)
	{
		for (Spell spell : book.getSpells())
		{
			if (spell.getName().equals(name))
			{
				return spell;
			}
		}
		return null;
	}

	// ---- Favorites: saved in RuneLite's settings, one list per spellbook ----

	private List<String> getFavorites(Spellbook book)
	{
		String saved = configManager.getConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.FAVORITES_PREFIX + book.name());
		List<String> names = new ArrayList<>();
		if (saved == null || saved.isEmpty())
		{
			return names;
		}
		for (String name : saved.split(FAVORITE_SEPARATOR_REGEX))
		{
			// Skip anything that no longer matches a spell, e.g. if a spell is renamed in a future update.
			if (findSpell(book, name) != null && !names.contains(name) && names.size() < MAX_FAVORITES)
			{
				names.add(name);
			}
		}
		return names;
	}

	private void toggleFavorite(Spellbook book, Spell spell)
	{
		List<String> favorites = getFavorites(book);
		if (favorites.contains(spell.getName()))
		{
			favorites.remove(spell.getName());
		}
		else if (favorites.size() < MAX_FAVORITES)
		{
			favorites.add(spell.getName());
		}
		else
		{
			return;
		}

		String key = SpellRuneReferenceConfig.FAVORITES_PREFIX + book.name();
		if (favorites.isEmpty())
		{
			configManager.unsetConfiguration(SpellRuneReferenceConfig.GROUP, key);
		}
		else
		{
			configManager.setConfiguration(SpellRuneReferenceConfig.GROUP, key, String.join(FAVORITE_SEPARATOR, favorites));
		}
		rebuildList();
	}

	private boolean isFavoritesCollapsed()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.FAVORITES_COLLAPSED));
	}

	private void toggleFavoritesCollapsed()
	{
		configManager.setConfiguration(SpellRuneReferenceConfig.GROUP, SpellRuneReferenceConfig.FAVORITES_COLLAPSED, !isFavoritesCollapsed());
		rebuildList();
	}

	/**
	 * A small heading above a group of spells. The Favorites heading can be clicked to collapse it.
	 */
	private JPanel createSectionHeader(String text, boolean collapsible, boolean collapsed)
	{
		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

		JLabel title = new JLabel(text);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(collapsible ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
		header.add(title, BorderLayout.CENTER);

		if (collapsible)
		{
			JLabel arrow = new JLabel(collapsed ? "▸" : "▾");
			arrow.setFont(SYMBOL_FONT);
			arrow.setForeground(ColorScheme.BRAND_ORANGE);
			header.add(arrow, BorderLayout.WEST);

			header.setToolTipText(collapsed ? "Show favorites" : "Hide favorites");
			header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			header.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					toggleFavoritesCollapsed();
				}
			});
		}

		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
		return header;
	}

	/**
	 * The star on each spell. Filled orange = favorite. Clicking adds or removes it.
	 */
	private JLabel createStar(Spellbook book, Spell spell, boolean isFavorite, int favoriteCount)
	{
		boolean full = !isFavorite && favoriteCount >= MAX_FAVORITES;
		Color normal = isFavorite ? ColorScheme.BRAND_ORANGE : (full ? STAR_DISABLED : ColorScheme.LIGHT_GRAY_COLOR);

		JLabel star = new JLabel(isFavorite ? "★" : "☆");
		star.setFont(SYMBOL_FONT);
		star.setForeground(normal);
		star.setVerticalAlignment(SwingConstants.TOP);
		star.setToolTipText(isFavorite ? "Remove from favorites"
			: full ? "Favorites are full (" + MAX_FAVORITES + " max). Unstar one first."
			: "Add to favorites");

		if (!full)
		{
			star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			star.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					toggleFavorite(book, spell);
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					star.setForeground(ColorScheme.BRAND_ORANGE);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					star.setForeground(normal);
				}
			});
		}
		return star;
	}

	private JPanel createNote(String text)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		wrapper.add(wrappingText(text, FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR, PANEL_WIDTH - 20),
			BorderLayout.CENTER);
		wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height));
		return wrapper;
	}

	private JPanel createRow(Spellbook book, Spell spell, boolean showIcons, boolean isFavorite, int favoriteCount, boolean dimmed,
		boolean showBook)
	{
		JPanel row = dimmed ? new DimmedPanel() : new JPanel();
		row.setLayout(new BorderLayout(0, 2));
		row.setToolTipText(requirementsTooltip(book, spell));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)));

		JLabel level = new JLabel("Lvl " + spell.getLevel());
		level.setFont(FontManager.getRunescapeSmallFont());
		level.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		level.setVerticalAlignment(SwingConstants.TOP);

		JPanel levelAndStar = new JPanel(new BorderLayout(4, 0));
		levelAndStar.setOpaque(false);
		if (showBook)
		{
			// With "All spellbooks", say which book each spell is in, right under its level.
			JLabel bookTag = new JLabel(shortName(book));
			bookTag.setFont(FontManager.getRunescapeSmallFont());
			bookTag.setForeground(ColorScheme.BRAND_ORANGE);
			bookTag.setHorizontalAlignment(SwingConstants.RIGHT);
			level.setHorizontalAlignment(SwingConstants.RIGHT);

			JPanel levelAndBook = new JPanel(new GridLayout(0, 1));
			levelAndBook.setOpaque(false);
			levelAndBook.add(level);
			levelAndBook.add(bookTag);

			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			top.add(levelAndBook, BorderLayout.NORTH);
			levelAndStar.add(top, BorderLayout.CENTER);
		}
		else
		{
			levelAndStar.add(level, BorderLayout.CENTER);
		}
		levelAndStar.add(createStar(book, spell, isFavorite, favoriteCount), BorderLayout.EAST);

		int nameWidth = ROW_TEXT_WIDTH - levelAndStar.getPreferredSize().width - 6;
		WrappedText name = wrappingText(spell.getName(), FontManager.getRunescapeBoldFont(), ColorScheme.TEXT_COLOR, nameWidth);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.add(name, BorderLayout.CENTER);
		header.add(levelAndStar, BorderLayout.EAST);

		row.add(header, BorderLayout.NORTH);
		row.add(showIcons ? createRuneIcons(spell) : createRuneText(spell), BorderLayout.CENTER);
		JPanel requirementLines = createRequirementLines(book, spell);
		if (requirementLines != null)
		{
			row.add(requirementLines, BorderLayout.SOUTH);
		}
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private WrappedText createRuneText(Spell spell)
	{
		return wrappingText(spell.getRunes(), FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE, ROW_TEXT_WIDTH);
	}

	private JPanel createRuneIcons(Spell spell)
	{
		List<RuneCost> costs = spell.getRuneCosts();
		if (costs.isEmpty())
		{
			// Home teleports and similar spells cost nothing, so there's no sprite to show.
			JPanel wrapper = new JPanel(new BorderLayout());
			wrapper.setOpaque(false);
			wrapper.add(createRuneText(spell), BorderLayout.CENTER);
			return wrapper;
		}

		JPanel icons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		icons.setOpaque(false);
		for (RuneCost cost : costs)
		{
			JLabel icon = new JLabel();
			icon.setToolTipText(cost.getAmount() + " " + cost.getRune().getDisplayName());
			// "stackable" draws the amount on the sprite in yellow, like a stack in your inventory.
			AsyncBufferedImage image = itemManager.getImage(cost.getRune().getItemId(), cost.getAmount(), true);
			if (image != null)
			{
				image.addTo(icon);
			}
			else
			{
				icon.setText(cost.getAmount() + " " + cost.getRune().getDisplayName());
			}
			icons.add(icon);
		}

		JPanel wrapper = new JPanel(new BorderLayout(0, 2));
		wrapper.setOpaque(false);
		// Nudge the sprites left so they line up with the spell name above.
		icons.setBorder(BorderFactory.createEmptyBorder(0, -2, 0, 0));
		wrapper.add(icons, BorderLayout.NORTH);

		String extra = spell.getExtraRequirement();
		if (extra != null)
		{
			wrapper.add(wrappingText(extra, FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR, ROW_TEXT_WIDTH),
				BorderLayout.CENTER);
		}
		return wrapper;
	}

	/**
	 * Small lines under the runes: what's still locked (in red, once logged in) and any unlock
	 * RuneLite can't check (in grey, always). Returns null when there's nothing to say.
	 */
	private JPanel createRequirementLines(Spellbook book, Spell spell)
	{
		JPanel lines = new JPanel();
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.setOpaque(false);

		List<String> missing = new ArrayList<>();
		for (Requirement requirement : unmetRequirements(book, spell))
		{
			missing.add(requirement.getDescription());
		}
		if (!missing.isEmpty())
		{
			lines.add(wrappingText("Needs " + String.join(", ", missing), FontManager.getRunescapeSmallFont(),
				LOCKED_TEXT, ROW_TEXT_WIDTH));
		}

		for (Requirement requirement : SpellRequirements.of(book, spell))
		{
			if (!requirement.isCheckable())
			{
				lines.add(wrappingText(requirement.getDescription(), FontManager.getRunescapeSmallFont(),
					ColorScheme.LIGHT_GRAY_COLOR, ROW_TEXT_WIDTH));
			}
		}
		return lines.getComponentCount() == 0 ? null : lines;
	}

	/** Hover text listing everything the spell needs, so requirements are visible even when logged out. */
	private String requirementsTooltip(Spellbook book, Spell spell)
	{
		List<String> parts = new ArrayList<>();
		parts.add("Magic level " + spell.getLevel()
			+ (magicLevel != UNKNOWN_LEVEL ? " (you have " + magicLevel + ")" : ""));
		for (Requirement requirement : SpellRequirements.of(book, spell))
		{
			if (requirement.isCheckable())
			{
				parts.add(requirement.getDescription());
			}
		}
		return "Requires: " + String.join(", ", parts);
	}

	/** "Ancient Magicks" is too long for the tag on each row. */
	private static String shortName(Spellbook book)
	{
		return book == Spellbook.ANCIENT ? "Ancient" : book.getDisplayName();
	}

	/** A spell plus the spellbook it belongs to, for lists that mix books. */
	private static final class BookSpell
	{
		private final Spellbook book;
		private final Spell spell;

		private BookSpell(Spellbook book, Spell spell)
		{
			this.book = book;
			this.spell = spell;
		}
	}

	/**
	 * A spell row drawn faded, for spells above your Magic level. It lays a see-through dark layer
	 * over everything in the row, so the rune sprites fade too. The star still works underneath.
	 */
	private static class DimmedPanel extends JPanel
	{
		@Override
		public void paint(Graphics g)
		{
			super.paint(g);
			// Leave the bottom border (the gap between rows) untouched.
			int bottomGap = getInsets().bottom - 6;
			g.setColor(DIM_OVERLAY);
			g.fillRect(0, 0, getWidth(), getHeight() - Math.max(bottomGap, 0));
		}
	}

	/**
	 * Text that wraps onto extra lines instead of being cut off, and keeps the RuneScape font.
	 */
	private static WrappedText wrappingText(String text, Font font, Color color, int width)
	{
		return new WrappedText(text, font, color, width);
	}
}
