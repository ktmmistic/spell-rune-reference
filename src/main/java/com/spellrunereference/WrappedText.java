package com.spellrunereference;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 * Plain text that wraps onto extra lines at a fixed width, in any font.
 * It only draws text: unlike a text box it has no cursor, so it never makes the panel scroll.
 */
class WrappedText extends JComponent
{
	private final String text;
	private final List<String> lines = new ArrayList<>();

	WrappedText(String text, Font font, Color color, int width)
	{
		this.text = text;
		setFont(font);
		setForeground(color);
		setOpaque(false);

		FontMetrics metrics = getFontMetrics(font);
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(candidate) > width)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}

		Dimension size = new Dimension(width, Math.max(1, lines.size()) * metrics.getHeight());
		setPreferredSize(size);
		setMinimumSize(size);
	}

	String getText()
	{
		return text;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		// Draw text the same way the rest of the Mac's interface does.
		Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
		if (hints instanceof Map)
		{
			g2.addRenderingHints((Map<?, ?>) hints);
		}
		else
		{
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		g2.setFont(getFont());
		g2.setColor(getForeground());
		FontMetrics metrics = g2.getFontMetrics();
		int y = metrics.getAscent();
		for (String line : lines)
		{
			g2.drawString(line, 0, y);
			y += metrics.getHeight();
		}
		g2.dispose();
	}
}
