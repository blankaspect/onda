/*====================================================================*\

PathnameComboBox.java

Pathname combo box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.ui.swing.combobox;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.FontMetrics;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import uk.blankaspect.common.property.Property;

import uk.blankaspect.ui.swing.font.FontKey;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextUtils;

import uk.blankaspect.ui.swing.textfield.PathnameField;

//----------------------------------------------------------------------


// PATHNAME COMBO BOX CLASS


public class PathnameComboBox
	extends JComboBox<String>
	implements FocusListener, PathnameField.IImportListener, PopupMenuListener
{

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// EDITOR CLASS


	private class Editor
		extends PathnameField
		implements ComboBoxEditor
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	VERTICAL_MARGIN		= 1;
		private static final	int	HORIZONTAL_MARGIN	= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Editor(int numColumns)
		{
			super(numColumns);
			setBorder(BorderFactory.createEmptyBorder(VERTICAL_MARGIN, HORIZONTAL_MARGIN,
													  VERTICAL_MARGIN, HORIZONTAL_MARGIN));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ComboBoxEditor interface
	////////////////////////////////////////////////////////////////////

		public Component getEditorComponent()
		{
			return this;
		}

		//--------------------------------------------------------------

		public Object getItem()
		{
			return getText();
		}

		//--------------------------------------------------------------

		public void setItem(Object obj)
		{
			setText((obj == null) ? null : obj.toString());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void setFile(File file)
		{
			super.setFile(file);
			updateList();
		}

		//--------------------------------------------------------------

		@Override
		public void processKeyEvent(KeyEvent event)
		{
			if ((event.getID() == KeyEvent.KEY_PRESSED) && (event.getKeyCode() == KeyEvent.VK_ENTER) &&
				 (event.getModifiersEx() == 0))
			{
				event.consume();
				accept();
			}
			super.processKeyEvent(event);
		}

		//--------------------------------------------------------------

		@Override
		public void propertyChanged(Property property)
		{
			// Update UNIX style
			super.propertyChanged(property);

			// Update pathnames in drop-down list
			List<String> pathnames = new ArrayList<>();
			for (String pathname : getPathnames())
				pathnames.add(convertPathname(pathname));
			setPathnames(pathnames);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getFieldWidth()
		{
			return getColumns() * getColumnWidth();
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// RENDERER CLASS


	private class Renderer
		extends ComboBoxRenderer<String>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Renderer(int maxTextWidth)
		{
			super(PathnameComboBox.this, maxTextWidth);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void reduceTextWidth(FontMetrics fontMetrics)
		{
			text = TextUtils.getLimitedWidthPathname(text, fontMetrics, maxTextWidth, getFileSeparatorChar());
			textWidth = fontMetrics.stringWidth(text);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * @throws IllegalArgumentException
	 */

	public PathnameComboBox(int maxNumPathnames,
							int numColumns)
	{
		_init(maxNumPathnames, numColumns);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public PathnameComboBox(int       maxNumPathnames,
							int       numColumns,
							String... pathnames)
	{
		super(pathnames);
		_init(maxNumPathnames, numColumns);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public PathnameComboBox(int          maxNumPathnames,
							int          numColumns,
							List<String> pathnames)
	{
		for (String pathname : pathnames)
			addItem(pathname);
		_init(maxNumPathnames, numColumns);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : FocusListener interface
////////////////////////////////////////////////////////////////////////

	public void focusGained(FocusEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void focusLost(FocusEvent event)
	{
		updateList();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : PathnameField.IImportListener interface
////////////////////////////////////////////////////////////////////////

	public void dataImported(PathnameField.ImportEvent event)
	{
		updateList();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : PopupMenuListener interface
////////////////////////////////////////////////////////////////////////

	public void popupMenuCanceled(PopupMenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
	{
		updateList();
	}

	//------------------------------------------------------------------

	public void popupMenuWillBecomeVisible(PopupMenuEvent event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public boolean isEmpty()
	{
		return getField().isEmpty();
	}

	//------------------------------------------------------------------

	public String getText()
	{
		return getField().getText();
	}

	//------------------------------------------------------------------

	public File getFile()
	{
		return getField().getFile();
	}

	//------------------------------------------------------------------

	public File getCanonicalFile()
	{
		return getField().getCanonicalFile();
	}

	//------------------------------------------------------------------

	public List<String> getPathnames()
	{
		List<String> items = new ArrayList<>();
		for (int i = 0; i < getItemCount(); i++)
			items.add(getItemAt(i));
		return items;
	}

	//------------------------------------------------------------------

	public void setText(String text)
	{
		getField().setText(text);
		updateList();
	}

	//------------------------------------------------------------------

	public void setFile(File file)
	{
		getField().setFile(file);
	}

	//------------------------------------------------------------------

	public void setCaretPosition(int index)
	{
		int length = getField().getText().length();
		getField().setCaretPosition((index < 0) ? length : Math.min(index, length));
	}

	//------------------------------------------------------------------

	public void setPathnames(String... pathnames)
	{
		removeAllItems();
		for (String pathname : pathnames)
			addItem(pathname);
		setSelectedIndex((pathnames.length == 0) ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void setPathnames(List<String> pathnames)
	{
		removeAllItems();
		for (String pathname : pathnames)
			addItem(pathname);
		setSelectedIndex(pathnames.isEmpty() ? -1 : 0);
	}

	//------------------------------------------------------------------

	public void setUnixStyle(boolean unixStyle)
	{
		getField().setUnixStyle(unixStyle);
	}

	//------------------------------------------------------------------

	public void updateList()
	{
		String str = getField().getText();
		if (!str.isEmpty())
		{
			insertItemAt(str, 0);
			for (int i = 1; i < getItemCount(); i++)
			{
				if (getItemAt(i).equals(str))
				{
					removeItemAt(i);
					break;
				}
			}
			while (getItemCount() > maxNumPathnames)
				removeItemAt(getItemCount() - 1);
			setSelectedIndex(0);
		}
	}

	//------------------------------------------------------------------

	public void setDefaultColours()
	{
		((ComboBoxRenderer<?>)getRenderer()).setDefaultColours();
	}

	//------------------------------------------------------------------

	protected char getFileSeparatorChar()
	{
		return getField().isUnixStyle() ? '/' : File.separatorChar;
	}

	//------------------------------------------------------------------

	protected Editor getField()
	{
		return (Editor)getEditor();
	}

	//------------------------------------------------------------------

	private void accept()
	{
		if (isPopupVisible())
		{
			if (isEmpty())
				getEditor().setItem(getSelectedItem());
			setPopupVisible(false);
		}
		else
			updateList();
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	private void _init(int maxNumPathnames,
					   int numColumns)
	{
		// Initialise instance variables
		this.maxNumPathnames = maxNumPathnames;

		// Set border
		FontUtils.setAppFont(FontKey.TEXT_FIELD, this);
		GuiUtils.setPaddedLineBorder(this, 1);

		// Set editor
		Editor editor = new Editor(numColumns);
		setEditor(editor);
		setEditable(true);

		// Set renderer
		setRenderer(new Renderer(editor.getFieldWidth()));

		// Add listeners
		editor.addFocusListener(this);
		editor.addImportListener(this);
		addPopupMenuListener(this);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int	maxNumPathnames;

}

//----------------------------------------------------------------------
