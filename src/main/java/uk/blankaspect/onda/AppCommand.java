/*====================================================================*\

AppCommand.java

Application command enumeration.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;

import javax.swing.Action;

import uk.blankaspect.common.swing.action.Command;

//----------------------------------------------------------------------


// APPLICATION COMMAND ENUMERATION


enum AppCommand
	implements Action
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Commands

	IMPORT_FILES
	(
		"importFiles"
	),

	COMPRESS
	(
		"compress",
		"Compress" + AppConstants.ELLIPSIS_STR,
		KeyEvent.VK_C
	),

	EXPAND
	(
		"expand",
		"Expand" + AppConstants.ELLIPSIS_STR,
		KeyEvent.VK_E
	),

	VALIDATE
	(
		"validate",
		"Validate" + AppConstants.ELLIPSIS_STR,
		KeyEvent.VK_V
	),

	VIEW_LOG
	(
		"viewLog",
		"View log",
		KeyEvent.VK_L
	),

	EDIT_PREFERENCES
	(
		"editPreferences",
		"Preferences" + AppConstants.ELLIPSIS_STR,
		KeyEvent.VK_P
	),

	EXIT
	(
		"exit",
		"Exit",
		KeyEvent.VK_X
	);

	//------------------------------------------------------------------

	// Property keys
	interface Property
	{
		String	FILES	= "files";
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private AppCommand(String key)
	{
		command = new Command(this);
		putValue(Action.ACTION_COMMAND_KEY, key);
	}

	//------------------------------------------------------------------

	private AppCommand(String key,
					   String name,
					   int    mnemonicKey)
	{
		this(key);
		putValue(Action.NAME, name);
		putValue(Action.MNEMONIC_KEY, mnemonicKey);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void setAllEnabled(boolean enabled)
	{
		for (AppCommand command : values())
			command.setEnabled(enabled);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : Action interface
////////////////////////////////////////////////////////////////////////

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		command.addPropertyChangeListener(listener);
	}

	//------------------------------------------------------------------

	public Object getValue(String key)
	{
		return command.getValue(key);
	}

	//------------------------------------------------------------------

	public boolean isEnabled()
	{
		return command.isEnabled();
	}

	//------------------------------------------------------------------

	public void putValue(String key,
						 Object value)
	{
		command.putValue(key, value);
	}

	//------------------------------------------------------------------

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		command.removePropertyChangeListener(listener);
	}

	//------------------------------------------------------------------

	public void setEnabled(boolean enabled)
	{
		command.setEnabled(enabled);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		App.INSTANCE.getMainWindow().executeCommand(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void execute()
	{
		actionPerformed(null);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Command	command;

}

//----------------------------------------------------------------------
