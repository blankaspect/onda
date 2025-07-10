/*====================================================================*\

ChunkFilterDialog.java

Chunk filter dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.blankaspect.common.iff.ChunkFilter;
import uk.blankaspect.common.iff.IffId;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.colour.Colours;

import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.misc.GuiConstants;
import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

import uk.blankaspect.ui.swing.textfield.ConstrainedTextField;

//----------------------------------------------------------------------


// CHUNK FILTER DIALOG CLASS


class ChunkFilterDialog
	extends JDialog
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private static final	int	ID_NUM_FIELDS	= AppConfig.MAX_NUM_CHUNK_FILTER_IDS;
	private static final	int	ID_NUM_COLUMNS	= 4;

	// Commands
	private interface Command
	{
		String	MOVE_UP		= "moveUp";
		String	MOVE_DOWN	= "moveDown";
		String	ACCEPT		= "accept";
		String	CLOSE		= "close";
	}

	private static final	KeyAction.KeyCommandPair[]	KEY_COMMANDS	=
	{
		new KeyAction.KeyCommandPair(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
									 Command.MOVE_UP),
		new KeyAction.KeyCommandPair(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
									 Command.MOVE_DOWN)
	};

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// FILTER KIND


	private enum FilterKind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		INCLUDE
		(
			"Include",
			new Color(0, 168, 0)
		),

		EXCLUDE
		(
			"Exclude",
			new Color(192, 48, 0)
		);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilterKind(String text,
						   Color  colour)
		{
			this.text = text;
			this.colour = colour;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public Color getColour()
		{
			return colour;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;
		private	Color	colour;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILTER KIND BUTTON CLASS


	private static class FilterKindButton
		extends JButton
		implements ActionListener
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		VERTICAL_MARGIN		= 5;
		private static final	int		HORIZONTAL_MARGIN	= 12;

		private static final	Color	TEXT_COLOUR				= Color.WHITE;
		private static final	Color	BORDER_COLOUR			= Colours.LINE_BORDER;
		private static final	Color	FOCUSED_BORDER_COLOUR1	= Color.WHITE;
		private static final	Color	FOCUSED_BORDER_COLOUR2	= Color.BLACK;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilterKindButton(ChunkFilter.Kind filterKind)
		{
			this.filterKind = (filterKind == ChunkFilter.Kind.INCLUDE) ? FilterKind.INCLUDE
																	   : FilterKind.EXCLUDE;
			setFont(AppFont.MAIN.getFont().deriveFont(Font.BOLD));
			setBorder(BorderFactory.createEmptyBorder(VERTICAL_MARGIN, HORIZONTAL_MARGIN,
													  VERTICAL_MARGIN, HORIZONTAL_MARGIN));
			FontMetrics fontMetrics = getFontMetrics(getFont());
			for (FilterKind kind : FilterKind.values())
			{
				int strWidth = fontMetrics.stringWidth(kind.toString());
				if (width < strWidth)
					width = strWidth;
			}
			width += getInsets().left + getInsets().right;
			height = getInsets().top + fontMetrics.getAscent() + fontMetrics.getDescent() + getInsets().bottom;
			addActionListener(this);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ActionListener interface
	////////////////////////////////////////////////////////////////////

		public void actionPerformed(ActionEvent event)
		{
			filterKind = (filterKind == FilterKind.INCLUDE) ? FilterKind.EXCLUDE : FilterKind.INCLUDE;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(width, height);
		}

		//--------------------------------------------------------------

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

			// Fill interior
			gr2d.setColor(filterKind.getColour());
			gr2d.fillRect(0, 0, width, height);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints(gr2d);

			// Draw text
			FontMetrics fontMetrics = gr2d.getFontMetrics();
			String str = filterKind.toString();
			gr2d.setColor(TEXT_COLOUR);
			gr2d.drawString(str, (width - fontMetrics.stringWidth(str)) / 2,
							FontUtils.getBaselineOffset(height, fontMetrics));

			// Draw border
			gr2d.setColor(BORDER_COLOUR);
			gr2d.drawRect(0, 0, width - 1, height - 1);
			if (isFocusOwner())
			{
				gr2d.setColor(FOCUSED_BORDER_COLOUR1);
				gr2d.drawRect(1, 1, width - 3, height - 3);

				gr2d.setStroke(GuiConstants.BASIC_DASH);
				gr2d.setColor(FOCUSED_BORDER_COLOUR2);
				gr2d.drawRect(1, 1, width - 3, height - 3);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public ChunkFilter.Kind getFilterKind()
		{
			return (filterKind == FilterKind.INCLUDE) ? ChunkFilter.Kind.INCLUDE : ChunkFilter.Kind.EXCLUDE;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	FilterKind	filterKind;
		private	int			width;
		private	int			height;

	}

	//==================================================================


	// ID FIELD CLASS


	private static class IdField
		extends ConstrainedTextField
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		FIELD_LENGTH	= 4;

		private static final	Color	FOREGROUND_COLOUR		= Color.BLACK;
		private static final	Color	BACKGROUND_COLOUR		= Colours.BACKGROUND;
		private static final	Color	EMPTY_BACKGROUND_COLOUR	= new Color(208, 208, 208);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private IdField(int   index,
						IffId id)
		{
			super(FIELD_LENGTH, (id == null) ? null : Utils.stripTrailingSpace(id.toString()));
			this.index = index;
			AppFont.TEXT_FIELD.apply(this);
			GuiUtils.setPaddedLineBorder(this, 2, 4);
			setForeground(FOREGROUND_COLOUR);
			setCaretColor(FOREGROUND_COLOUR);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Color getBackground()
		{
			return (isEmpty() ? EMPTY_BACKGROUND_COLOUR : BACKGROUND_COLOUR);
		}

		//--------------------------------------------------------------

		@Override
		protected int getColumnWidth()
		{
			return (FontUtils.getCharWidth('D', getFontMetrics(getFont())) + 1);
		}

		//--------------------------------------------------------------

		@Override
		protected boolean acceptCharacter(char ch,
										  int  index)
		{
			return ((ch >= IffId.MIN_CHAR) && (ch <= IffId.MAX_CHAR));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getIndex()
		{
			return index;
		}

		//--------------------------------------------------------------

		public IffId getId()
		{
			return (isEmpty() ? null : new IffId(getText() + "   "));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int	index;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private ChunkFilterDialog(Window      owner,
							  String      title,
							  ChunkFilter filter)
	{
		// Call superclass constructor
		super(owner, title, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Button: filter kind
		filterKindButton = new FilterKindButton((filter == null) ? ChunkFilter.Kind.INCLUDE : filter.getKind());

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(filterKindButton, gbc);
		controlPanel.add(filterKindButton);


		//----  ID panel

		JPanel idPanel = new JPanel(new GridLayout(0, ID_NUM_COLUMNS, 6, 4));
		KeyAction.create(idPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this, KEY_COMMANDS);

		idFields = new IdField[ID_NUM_FIELDS];
		int numIds = (filter == null) ? 0 : filter.getNumIds();
		for (int i = 0; i < ID_NUM_FIELDS; i++)
		{
			idFields[i] = new IdField(i, (i < numIds) ? filter.getId(i) : null);
			idPanel.add(idFields[i]);
		}

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(6, 0, 0, 0);
		gridBag.setConstraints(idPanel, gbc);
		controlPanel.add(idPanel);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 8, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: OK
		JButton okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		mainPanel.add(controlPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 0, 0, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		mainPanel.add(buttonPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				onClose();
			}
		});

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(okButton);

		// Set focus
		idFields[0].requestFocusInWindow();
		idFields[0].setCaretPosition(idFields[0].getText().length());

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static ChunkFilter showDialog(Component   parent,
										 String      title,
										 ChunkFilter filter)
	{
		return new ChunkFilterDialog(GuiUtils.getWindow(parent), title, filter).getFilter();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.MOVE_UP))
			onMoveUp();

		else if (command.equals(Command.MOVE_DOWN))
			onMoveDown();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private ChunkFilter getFilter()
	{
		ChunkFilter filter = null;
		if (accepted)
		{
			List<IffId> ids = new ArrayList<>();
			for (IdField idField : idFields)
			{
				IffId id = idField.getId();
				if ((id != null) && !ids.contains(id))
					ids.add(id);
			}
			filter = new ChunkFilter(filterKindButton.getFilterKind(), ids);
		}
		return filter;
	}

	//------------------------------------------------------------------

	private int getCurrentIdIndex()
	{
		Component component = getFocusOwner();
		return ((component != null) && (component instanceof IdField)) ? ((IdField)component).getIndex() : -1;
	}

	//------------------------------------------------------------------

	private void onMoveUp()
	{
		int index = getCurrentIdIndex();
		if (index >= 0)
			idFields[(index + ID_NUM_FIELDS - ID_NUM_COLUMNS) % ID_NUM_FIELDS].requestFocusInWindow();
	}

	//------------------------------------------------------------------

	private void onMoveDown()
	{
		int index = getCurrentIdIndex();
		if (index >= 0)
			idFields[(index + ID_NUM_COLUMNS) % ID_NUM_FIELDS].requestFocusInWindow();
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		accepted = true;
		onClose();
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		location = getLocation();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point	location;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean				accepted;
	private	FilterKindButton	filterKindButton;
	private	IdField[]			idFields;

}

//----------------------------------------------------------------------
