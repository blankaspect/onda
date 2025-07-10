/*====================================================================*\

ChunkFilterListDialog.java

Chunk filter list dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.blankaspect.common.iff.ChunkFilter;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.list.SingleSelectionList;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// CHUNK FILTER LIST DIALOG CLASS


class ChunkFilterListDialog
	extends JDialog
	implements ActionListener, ChangeListener, ListSelectionListener
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private static final	int		MODIFIERS_MASK	=
			ActionEvent.ALT_MASK | ActionEvent.META_MASK | ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK;

	private static final	String	ADD_STR		= "Add";
	private static final	String	EDIT_STR	= "Edit";
	private static final	String	DELETE_STR	= "Delete";

	private static final	String	ADD_FILTER_STR		= "Add chunk filter";
	private static final	String	EDIT_FILTER_STR		= "Edit chunk filter";
	private static final	String	DELETE_FILTER_STR	= "Delete chunk filter";
	private static final	String	DELETE_MESSAGE_STR	= "Do you want to delete the selected filter?";

	// Commands
	private interface Command
	{
		String	ADD_FILTER				= "addFilter";
		String	EDIT_FILTER				= "editFilter";
		String	DELETE_FILTER			= "deleteFilter";
		String	CONFIRM_DELETE_FILTER	= "confirmDeleteFilter";
		String	MOVE_FILTER_UP			= "moveFilterUp";
		String	MOVE_FILTER_DOWN		= "moveFilterDown";
		String	MOVE_FILTER				= "moveFilter";
		String	ACCEPT					= "accept";
		String	CLOSE					= "close";
	}

	private static final	Map<String, String>	COMMAND_MAP;

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point	location;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean		accepted;
	private	FilterList	filterList;
	private	JScrollPane	filterListScrollPane;
	private	JButton		addButton;
	private	JButton		editButton;
	private	JButton		deleteButton;

////////////////////////////////////////////////////////////////////////
//  Static initialiser
////////////////////////////////////////////////////////////////////////

	static
	{
		COMMAND_MAP = new HashMap<>();
		COMMAND_MAP.put(SingleSelectionList.Command.EDIT_ELEMENT,      Command.EDIT_FILTER);
		COMMAND_MAP.put(SingleSelectionList.Command.DELETE_ELEMENT,    Command.CONFIRM_DELETE_FILTER);
		COMMAND_MAP.put(SingleSelectionList.Command.DELETE_EX_ELEMENT, Command.DELETE_FILTER);
		COMMAND_MAP.put(SingleSelectionList.Command.MOVE_ELEMENT_UP,   Command.MOVE_FILTER_UP);
		COMMAND_MAP.put(SingleSelectionList.Command.MOVE_ELEMENT_DOWN, Command.MOVE_FILTER_DOWN);
		COMMAND_MAP.put(SingleSelectionList.Command.DRAG_ELEMENT,      Command.MOVE_FILTER);
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private ChunkFilterListDialog(Window            owner,
								  String            title,
								  List<ChunkFilter> filters)
	{
		// Call superclass constructor
		super(owner, title, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Filter selection list

		// Selection list
		filterList = new FilterList(filters);
		filterList.addActionListener(this);
		filterList.addListSelectionListener(this);

		// Scroll pane: selection list
		filterListScrollPane = new JScrollPane(filterList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
											   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		filterListScrollPane.getVerticalScrollBar().setFocusable(false);
		filterListScrollPane.getVerticalScrollBar().getModel().addChangeListener(this);

		filterList.setViewport(filterListScrollPane.getViewport());


		//----  Filter button panel

		JPanel filterButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		// Button: add
		addButton = new FButton(ADD_STR + AppConstants.ELLIPSIS_STR);
		addButton.setMnemonic(KeyEvent.VK_A);
		addButton.setActionCommand(Command.ADD_FILTER);
		addButton.addActionListener(this);
		filterButtonPanel.add(addButton);

		// Button: edit
		editButton = new FButton(EDIT_STR + AppConstants.ELLIPSIS_STR);
		editButton.setMnemonic(KeyEvent.VK_E);
		editButton.setActionCommand(Command.EDIT_FILTER);
		editButton.addActionListener(this);
		filterButtonPanel.add(editButton);

		// Button: delete
		deleteButton = new FButton(DELETE_STR + AppConstants.ELLIPSIS_STR);
		deleteButton.setMnemonic(KeyEvent.VK_D);
		deleteButton.setActionCommand(Command.CONFIRM_DELETE_FILTER);
		deleteButton.addActionListener(this);
		filterButtonPanel.add(deleteButton);


		//----  OK/Cancel button panel

		JPanel okCancelButtonPanel = new JPanel(new GridLayout(0, 1, 0, 8));

		// Button: OK
		JButton okButton = new FButton(AppConstants.OK_STR);
		okButton.setActionCommand(Command.ACCEPT);
		okButton.addActionListener(this);
		okCancelButtonPanel.add(okButton);

		// Button: cancel
		JButton cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		okCancelButtonPanel.add(cancelButton);


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		controlPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		int gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(filterButtonPanel, gbc);
		controlPanel.add(filterButtonPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(16, 0, 0, 0);
		gridBag.setConstraints(okCancelButtonPanel, gbc);
		controlPanel.add(okCancelButtonPanel);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(filterListScrollPane, gbc);
		mainPanel.add(filterListScrollPane);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(0, 4, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		mainPanel.add(controlPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Update components
		updateComponents();

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

		// Show dialog
		setVisible(true);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static List<ChunkFilter> showDialog(Component         parent,
											   String            title,
											   List<ChunkFilter> filters)
	{
		return new ChunkFilterListDialog(GuiUtils.getWindow(parent), title, filters).getFilters();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();
		if (command.equals(Command.CONFIRM_DELETE_FILTER) &&
			 ((event.getModifiers() & MODIFIERS_MASK) == ActionEvent.SHIFT_MASK))
			command = Command.DELETE_FILTER;
		else if (COMMAND_MAP.containsKey(command))
			command = COMMAND_MAP.get(command);

		if (command.equals(Command.ADD_FILTER))
			onAddFilter();

		else if (command.equals(Command.EDIT_FILTER))
			onEditFilter();

		else if (command.equals(Command.DELETE_FILTER))
			onDeleteFilter();

		else if (command.equals(Command.CONFIRM_DELETE_FILTER))
			onConfirmDeleteFilter();

		else if (command.equals(Command.MOVE_FILTER_UP))
			onMoveFilterUp();

		else if (command.equals(Command.MOVE_FILTER_DOWN))
			onMoveFilterDown();

		else if (command.equals(Command.MOVE_FILTER))
			onMoveFilter();

		else if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ChangeListener interface
////////////////////////////////////////////////////////////////////////

	public void stateChanged(ChangeEvent event)
	{
		if (!filterListScrollPane.getVerticalScrollBar().getValueIsAdjusting() &&
			 !filterList.isDragging())
			filterList.snapViewPosition();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ListSelectionListener interface
////////////////////////////////////////////////////////////////////////

	public void valueChanged(ListSelectionEvent event)
	{
		if (!event.getValueIsAdjusting())
			updateComponents();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private List<ChunkFilter> getFilters()
	{
		return (accepted ? filterList.getElements() : null);
	}

	//------------------------------------------------------------------

	private void updateComponents()
	{
		addButton.setEnabled(filterList.getNumElements() < AppConfig.MAX_NUM_CHUNK_FILTERS);
		editButton.setEnabled(filterList.isSelection());
		deleteButton.setEnabled(filterList.isSelection());
	}

	//------------------------------------------------------------------

	private void onAddFilter()
	{
		ChunkFilter filter = ChunkFilterDialog.showDialog(this, ADD_FILTER_STR, null);
		if ((filter != null) && (filter.getNumIds() > 0))
		{
			filterList.addElement(filter);
			updateComponents();
		}
	}

	//------------------------------------------------------------------

	private void onEditFilter()
	{
		ChunkFilter filter = ChunkFilterDialog.showDialog(this, EDIT_FILTER_STR,
														  filterList.getSelectedElement());
		if (filter != null)
		{
			if (filter.getNumIds() == 0)
				onDeleteFilter();
			else
				filterList.setElement(filterList.getSelectedIndex(), filter);
		}
	}

	//------------------------------------------------------------------

	private void onDeleteFilter()
	{
		filterList.removeElement(filterList.getSelectedIndex());
		updateComponents();
	}

	//------------------------------------------------------------------

	private void onConfirmDeleteFilter()
	{
		String[] optionStrs = Utils.getOptionStrings(DELETE_STR);
		if (JOptionPane.showOptionDialog(this, DELETE_MESSAGE_STR, DELETE_FILTER_STR,
										 JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
										 optionStrs, optionStrs[1]) == JOptionPane.OK_OPTION)
			onDeleteFilter();
	}

	//------------------------------------------------------------------

	private void onMoveFilterUp()
	{
		int index = filterList.getSelectedIndex();
		filterList.moveElement(index, index - 1);
	}

	//------------------------------------------------------------------

	private void onMoveFilterDown()
	{
		int index = filterList.getSelectedIndex();
		filterList.moveElement(index, index + 1);
	}

	//------------------------------------------------------------------

	private void onMoveFilter()
	{
		int fromIndex = filterList.getSelectedIndex();
		int toIndex = filterList.getDragEndIndex();
		if (toIndex > fromIndex)
			--toIndex;
		filterList.moveElement(fromIndex, toIndex);
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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILTER SELECTION LIST CLASS


	private static class FilterList
		extends SingleSelectionList<ChunkFilter>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	NUM_COLUMNS	= 48;
		private static final	int	NUM_ROWS	= 16;

		private static final	int	VERTICAL_MARGIN	= 1;
		private static final	int	ICON_MARGIN		= 4;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilterList(List<ChunkFilter> filters)
		{
			super(NUM_COLUMNS, NUM_ROWS, AppFont.MAIN.getFont(), filters);
			setExtraWidth(ICON_MARGIN + Icons.INCLUDE.getIconWidth());
			FontMetrics fontMetrics = getFontMetrics(getFont());
			setRowHeight(2 * VERTICAL_MARGIN +
										Math.max(Icons.INCLUDE.getIconHeight(),
												 fontMetrics.getAscent() + fontMetrics.getDescent()));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String getElementText(int index)
		{
			return getElement(index).getIdString();
		}

		//--------------------------------------------------------------

		@Override
		protected void drawElement(Graphics gr,
								   int      index)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Draw icon
			int rowHeight = getRowHeight();
			int x = ICON_MARGIN;
			int y = index * rowHeight;
			ImageIcon icon = getElement(index).isInclude() ? Icons.INCLUDE : Icons.EXCLUDE;
			gr.drawImage(icon.getImage(), x, y + (rowHeight - icon.getIconHeight()) / 2, null);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints((Graphics2D)gr);

			// Get text and truncate it if it is too wide
			FontMetrics fontMetrics = gr.getFontMetrics();
			String text = truncateText(getElementText(index), fontMetrics, getMaxTextWidth());

			// Draw text
			x = getExtraWidth() + getHorizontalMargin();
			gr.setColor(getForegroundColour(index));
			gr.drawString(text, x, y + FontUtils.getBaselineOffset(rowHeight, fontMetrics));
		}

		//--------------------------------------------------------------

		@Override
		protected int getPopUpXOffset()
		{
			return getExtraWidth();
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
