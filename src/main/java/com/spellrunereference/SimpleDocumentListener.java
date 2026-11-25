package com.spellrunereference;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Runs one action whenever the text in a field changes, so the list filters as you type.
 */
class SimpleDocumentListener implements DocumentListener
{
	private final Runnable onChange;

	SimpleDocumentListener(Runnable onChange)
	{
		this.onChange = onChange;
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		onChange.run();
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		onChange.run();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		onChange.run();
	}
}
