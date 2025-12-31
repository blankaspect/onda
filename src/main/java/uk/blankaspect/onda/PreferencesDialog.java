/*====================================================================*\

PreferencesDialog.java

Preferences dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.iff.ChunkFilter;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.border.TitledBorder;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.combobox.BooleanComboBox;
import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.font.FontEx;
import uk.blankaspect.ui.swing.font.FontStyle;
import uk.blankaspect.ui.swing.font.FontUtils;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.spinner.FIntegerSpinner;
import uk.blankaspect.ui.swing.spinner.IntegerSpinner;

import uk.blankaspect.ui.swing.tabbedpane.FTabbedPane;

import uk.blankaspect.ui.swing.text.TextRendering;

import uk.blankaspect.ui.swing.textfield.IntegerValueField;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// PREFERENCES DIALOG CLASS


class PreferencesDialog
	extends JDialog
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Main panel
	private static final	String	TITLE_STR				= "Preferences";
	private static final	String	SAVE_CONFIGURATION_STR	= "Save configuration";
	private static final	String	SAVE_CONFIG_FILE_STR	= "Save configuration file";
	private static final	String	WRITE_CONFIG_FILE_STR	= "Write configuration file";

	// General panel
	private static final	String	CHARACTER_ENCODING_STR			= "Character encoding";
	private static final	String	IGNORE_FILENAME_CASE_STR		= "Ignore case of filenames";
	private static final	String	SELECT_TEXT_ON_FOCUS_GAINED_STR	= "Select text when focus is gained";
	private static final	String	SAVE_MAIN_WINDOW_LOCATION_STR	= "Save location of main window";
	private static final	String	DEFAULT_ENCODING_STR			= "<default encoding>";

	// Appearance panel
	private static final	String	LOOK_AND_FEEL_STR			= "Look-and-feel";
	private static final	String	TEXT_ANTIALIASING_STR		= "Text antialiasing";
	private static final	String	SHOW_OVERALL_PROGRESS_STR	= "Show overall progress";
	private static final	String	NO_LOOK_AND_FEELS_STR		= "<no look-and-feels>";

	// Compression panel
	private static final	int		BLOCK_LENGTH_FIELD_LENGTH	= 5;

	private static final	String	BLOCK_LENGTH_STR	= "Block length";

	// Chunk filters panel
	private static final	Insets	EDIT_BUTTON_MARGINS	= new Insets(2, 4, 2, 4);

	private static final	String	CHUNK_FILTERS_STR	= "Chunk filters";
	private static final	String	FILTER_STR			= "Filter";
	private static final	String	EDIT_FILTERS_STR	= "Edit filters";

	// Fonts panel
	private static final	String	PT_STR	= "pt";

	// Commands
	private interface Command
	{
		String	EDIT_FILTERS		= "editFilters";
		String	SAVE_CONFIGURATION	= "saveConfiguration";
		String	ACCEPT				= "accept";
		String	CLOSE				= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point	location;
	private static	int		tabIndex;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	// Main panel
	private	boolean										accepted;
	private	JTabbedPane									tabbedPanel;

	// General panel
	private	FComboBox<String>							characterEncodingComboBox;
	private	BooleanComboBox								ignoreFilenameCaseComboBox;
	private	BooleanComboBox								selectTextOnFocusGainedComboBox;
	private	BooleanComboBox								saveMainWindowLocationComboBox;

	// Appearance panel
	private	FComboBox<String>							lookAndFeelComboBox;
	private	FComboBox<TextRendering.Antialiasing>		textAntialiasingComboBox;
	private	BooleanComboBox								showOverallProgressComboBox;

	// Compression panel
	private	FIntegerSpinner								blockLengthSpinner;

	// Chunk filters panel
	private	Map<AudioFileKind, JComboBox<ChunkFilter>>	chunkFilterComboBoxes;

	// Fonts panel
	private	FontPanel[]									fontPanels;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private PreferencesDialog(Window owner)
	{
		// Call superclass constructor
		super(owner, TITLE_STR, ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Tabbed panel

		tabbedPanel = new FTabbedPane();
		for (Tab tab : Tab.values())
			tabbedPanel.addTab(tab.text, tab.createPanel(this));
		tabbedPanel.setSelectedIndex(tabIndex);


		//----  Button panel: save configuration

		JPanel saveButtonPanel = new JPanel(new GridLayout(1, 0, 8, 0));

		// Button: save configuration
		JButton saveButton = new FButton(SAVE_CONFIGURATION_STR + AppConstants.ELLIPSIS_STR);
		saveButton.setActionCommand(Command.SAVE_CONFIGURATION);
		saveButton.addActionListener(this);
		saveButtonPanel.add(saveButton);


		//----  Button panel: OK, cancel

		JPanel okCancelButtonPanel = new JPanel(new GridLayout(1, 0, 8, 0));

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


		//----  Button panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel buttonPanel = new JPanel(gridBag);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 24, 3, 24));

		int gridX = 0;

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 12);
		gridBag.setConstraints(saveButtonPanel, gbc);
		buttonPanel.add(saveButtonPanel);

		gbc.gridx = gridX++;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 12, 0, 0);
		gridBag.setConstraints(okCancelButtonPanel, gbc);
		buttonPanel.add(okCancelButtonPanel);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		int gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(tabbedPanel, gbc);
		mainPanel.add(tabbedPanel);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
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

		// Handle window events
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(
				WindowEvent	event)
			{
				// WORKAROUND for a bug that has been observed on Linux/GNOME whereby a window is displaced downwards
				// when its location is set.  The error in the y coordinate is the height of the title bar of the
				// window.  The workaround is to set the location of the window again with an adjustment for the error.
				LinuxWorkarounds.fixWindowYCoord(event.getWindow(), location);
			}

			@Override
			public void windowClosing(
				WindowEvent	event)
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

	public static boolean showDialog(Component parent)
	{
		return new PreferencesDialog(GuiUtils.getWindow(parent)).accepted;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.startsWith(Command.EDIT_FILTERS))
			onEditFilters(StringUtils.removePrefix(command, Command.EDIT_FILTERS));
		else
		{
			switch (command)
			{
				case Command.SAVE_CONFIGURATION -> onSaveConfiguration();
				case Command.ACCEPT             -> onAccept();
				case Command.CLOSE              -> onClose();
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	private void validatePreferences()
		throws AppException
	{
		for (Tab tab : Tab.values())
			tab.validatePreferences(this);
	}

	//------------------------------------------------------------------

	private void setPreferences()
	{
		for (Tab tab : Tab.values())
			tab.setPreferences(this);
	}

	//------------------------------------------------------------------

	private void onEditFilters(String str)
	{
		AudioFileKind fileKind = AudioFileKind.forKey(str);
		JComboBox<ChunkFilter> comboBox = chunkFilterComboBoxes.get(fileKind);
		List<ChunkFilter> filters = new ArrayList<>();
		for (int i = -AppConfig.MIN_CHUNK_FILTER_INDEX; i < comboBox.getItemCount(); i++)
			filters.add((ChunkFilter)comboBox.getItemAt(i));
		String title = CHUNK_FILTERS_STR + " : " + fileKind;
		filters = ChunkFilterListDialog.showDialog(this, title, filters);
		if (filters != null)
		{
			int index = comboBox.getSelectedIndex();
			comboBox.removeAllItems();
			for (ChunkFilter filter : AppConfig.GENERIC_FILTERS)
				comboBox.addItem(filter);
			for (ChunkFilter filter : filters)
				comboBox.addItem(filter);
			if (index >= comboBox.getItemCount())
				index = 0;
			comboBox.setSelectedIndex(index);
		}
	}

	//------------------------------------------------------------------

	private void onSaveConfiguration()
	{
		try
		{
			validatePreferences();

			File file = AppConfig.INSTANCE.chooseFile(this);
			if (file != null)
			{
				String[] optionStrs = Utils.getOptionStrings(AppConstants.REPLACE_STR);
				if (!file.exists() ||
					 (JOptionPane.showOptionDialog(this, Utils.getPathname(file) +
																			AppConstants.ALREADY_EXISTS_STR,
												   SAVE_CONFIG_FILE_STR, JOptionPane.OK_CANCEL_OPTION,
												   JOptionPane.WARNING_MESSAGE, null, optionStrs,
												   optionStrs[1]) == JOptionPane.OK_OPTION))
				{
					setPreferences();
					accepted = true;
					TaskProgressDialog.showDialog(this, WRITE_CONFIG_FILE_STR, new Task.WriteConfig(file));
				}
			}
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, OndaApp.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
		if (accepted)
			onClose();
	}

	//------------------------------------------------------------------

	private void onAccept()
	{
		try
		{
			validatePreferences();
			setPreferences();
			accepted = true;
			onClose();
		}
		catch (AppException e)
		{
			JOptionPane.showMessageDialog(this, e, OndaApp.SHORT_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		location = getLocation();
		tabIndex = tabbedPanel.getSelectedIndex();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

	private JPanel createPanelGeneral()
	{
		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		AppConfig config = AppConfig.INSTANCE;

		// Label: character encoding
		JLabel characterEncodingLabel = new FLabel(CHARACTER_ENCODING_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(characterEncodingLabel, gbc);
		controlPanel.add(characterEncodingLabel);

		// Combo box: character encoding
		characterEncodingComboBox = new FComboBox<>();
		characterEncodingComboBox.addItem(DEFAULT_ENCODING_STR);
		for (String key : Charset.availableCharsets().keySet())
			characterEncodingComboBox.addItem(key);
		String encodingName = config.getCharacterEncoding();
		if (encodingName == null)
			characterEncodingComboBox.setSelectedIndex(0);
		else
			characterEncodingComboBox.setSelectedValue(encodingName);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(characterEncodingComboBox, gbc);
		controlPanel.add(characterEncodingComboBox);

		// Label: ignore filename case
		JLabel ignoreFilenameCaseLabel = new FLabel(IGNORE_FILENAME_CASE_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(ignoreFilenameCaseLabel, gbc);
		controlPanel.add(ignoreFilenameCaseLabel);

		// Combo box: ignore filename case
		ignoreFilenameCaseComboBox = new BooleanComboBox(config.isIgnoreFilenameCase());

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(ignoreFilenameCaseComboBox, gbc);
		controlPanel.add(ignoreFilenameCaseComboBox);

		// Label: select text on focus gained
		JLabel selectTextOnFocusGainedLabel = new FLabel(SELECT_TEXT_ON_FOCUS_GAINED_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(selectTextOnFocusGainedLabel, gbc);
		controlPanel.add(selectTextOnFocusGainedLabel);

		// Combo box: select text on focus gained
		selectTextOnFocusGainedComboBox = new BooleanComboBox(config.isSelectTextOnFocusGained());

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(selectTextOnFocusGainedComboBox, gbc);
		controlPanel.add(selectTextOnFocusGainedComboBox);

		// Label: save main window location
		JLabel saveMainWindowLocationLabel = new FLabel(SAVE_MAIN_WINDOW_LOCATION_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(saveMainWindowLocationLabel, gbc);
		controlPanel.add(saveMainWindowLocationLabel);

		// Combo box: save main window location
		saveMainWindowLocationComboBox = new BooleanComboBox(config.isMainWindowLocation());

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(saveMainWindowLocationComboBox, gbc);
		controlPanel.add(saveMainWindowLocationComboBox);


		//----  Outer panel

		JPanel outerPanel = new JPanel(gridBag);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		outerPanel.add(controlPanel);

		return outerPanel;
	}

	//------------------------------------------------------------------

	private JPanel createPanelAppearance()
	{
		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		AppConfig config = AppConfig.INSTANCE;

		// Label: look-and-feel
		JLabel lookAndFeelLabel = new FLabel(LOOK_AND_FEEL_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lookAndFeelLabel, gbc);
		controlPanel.add(lookAndFeelLabel);

		// Combo box: look-and-feel
		lookAndFeelComboBox = new FComboBox<>();

		UIManager.LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
		if (lookAndFeelInfos.length == 0)
		{
			lookAndFeelComboBox.addItem(NO_LOOK_AND_FEELS_STR);
			lookAndFeelComboBox.setSelectedIndex(0);
			lookAndFeelComboBox.setEnabled(false);
		}
		else
		{
			String[] lookAndFeelNames = new String[lookAndFeelInfos.length];
			for (int i = 0; i < lookAndFeelInfos.length; i++)
			{
				lookAndFeelNames[i] = lookAndFeelInfos[i].getName();
				lookAndFeelComboBox.addItem(lookAndFeelNames[i]);
			}
			lookAndFeelComboBox.setSelectedValue(config.getLookAndFeel());
		}

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(lookAndFeelComboBox, gbc);
		controlPanel.add(lookAndFeelComboBox);

		// Label: text antialiasing
		JLabel textAntialiasingLabel = new FLabel(TEXT_ANTIALIASING_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(textAntialiasingLabel, gbc);
		controlPanel.add(textAntialiasingLabel);

		// Combo box: text antialiasing
		textAntialiasingComboBox = new FComboBox<>(TextRendering.Antialiasing.values());
		textAntialiasingComboBox.setSelectedValue(config.getTextAntialiasing());

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(textAntialiasingComboBox, gbc);
		controlPanel.add(textAntialiasingComboBox);

		// Label: show overall progress
		JLabel showOverallProgressLabel = new FLabel(SHOW_OVERALL_PROGRESS_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(showOverallProgressLabel, gbc);
		controlPanel.add(showOverallProgressLabel);

		// Combo box: show overall progress
		showOverallProgressComboBox = new BooleanComboBox(config.isShowOverallProgress());

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(showOverallProgressComboBox, gbc);
		controlPanel.add(showOverallProgressComboBox);


		//----  Outer panel

		JPanel outerPanel = new JPanel(gridBag);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		outerPanel.add(controlPanel);

		return outerPanel;
	}

	//------------------------------------------------------------------

	private JPanel createPanelCompression()
	{
		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		AppConfig config = AppConfig.INSTANCE;

		// Label: block length
		JLabel blockLengthLabel = new FLabel(BLOCK_LENGTH_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(blockLengthLabel, gbc);
		controlPanel.add(blockLengthLabel);

		// Spinner: block length
		blockLengthSpinner = new FIntegerSpinner(config.getBlockLength(), OndaFile.MIN_BLOCK_LENGTH,
												 OndaFile.MAX_BLOCK_LENGTH, BLOCK_LENGTH_FIELD_LENGTH);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(blockLengthSpinner, gbc);
		controlPanel.add(blockLengthSpinner);


		//----  Outer panel

		JPanel outerPanel = new JPanel(gridBag);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		outerPanel.add(controlPanel);

		return outerPanel;
	}

	//------------------------------------------------------------------

	private JPanel createPanelChunkFilters()
	{
		//----  Outer panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel outerPanel = new JPanel(gridBag);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));


		//----  Filter panels

		int gridY = 0;

		AppConfig config = AppConfig.INSTANCE;

		chunkFilterComboBoxes = new EnumMap<>(AudioFileKind.class);
		for (AudioFileKind fileKind : AudioFileKind.values())
		{
			JPanel filterPanel = new JPanel(gridBag);
			TitledBorder.setPaddedBorder(filterPanel, fileKind.toString());

			gridY = 0;

			// Label: filter
			JLabel filterLabel = new FLabel(FILTER_STR);

			gbc.gridx = 0;
			gbc.gridy = gridY;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_END;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(filterLabel, gbc);
			filterPanel.add(filterLabel);

			// Combo box: filter
			JComboBox<ChunkFilter> filterComboBox = new JComboBox<>(AppConfig.GENERIC_FILTERS);
			chunkFilterComboBoxes.put(fileKind, filterComboBox);
			AppFont.COMBO_BOX.apply(filterComboBox);
			filterComboBox.setRenderer(new FilterComboBoxRenderer());
			for (ChunkFilter filter : config.getChunkFilters(fileKind))
				filterComboBox.addItem(filter);
			filterComboBox.setSelectedItem(config.getChunkFilter(fileKind));

			gbc.gridx = 1;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(filterComboBox, gbc);
			filterPanel.add(filterComboBox);

			// Button: edit filters
			JButton editButton = new FButton(EDIT_FILTERS_STR + AppConstants.ELLIPSIS_STR);
			editButton.setMargin(EDIT_BUTTON_MARGINS);
			editButton.setActionCommand(Command.EDIT_FILTERS + fileKind.getKey());
			editButton.addActionListener(this);

			gbc.gridx = 0;
			gbc.gridy = gridY++;
			gbc.gridwidth = 2;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(6, 0, 2, 0);
			gridBag.setConstraints(editButton, gbc);
			filterPanel.add(editButton);

			// Add filter panel to outer panel
			gbc.gridx = 0;
			gbc.gridy = fileKind.ordinal();
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets((fileKind.ordinal() == 0) ? 0 : 3, 0, 0, 0);
			gridBag.setConstraints(filterPanel, gbc);
			outerPanel.add(filterPanel);
		}

		return outerPanel;
	}

	//------------------------------------------------------------------

	private JPanel createPanelFonts()
	{
		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		fontPanels = new FontPanel[AppFont.getNumFonts()];
		for (int i = 0; i < fontPanels.length; i++)
		{
			FontEx fontEx = AppConfig.INSTANCE.getFont(i);
			fontPanels[i] = new FontPanel(fontEx, fontNames);

			int gridX = 0;

			// Label: font
			JLabel fontLabel = new FLabel(AppFont.values()[i].toString());

			gbc.gridx = gridX++;
			gbc.gridy = i;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_END;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(fontLabel, gbc);
			controlPanel.add(fontLabel);

			// Combo box: font name
			gbc.gridx = gridX++;
			gbc.gridy = i;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(fontPanels[i].nameComboBox, gbc);
			controlPanel.add(fontPanels[i].nameComboBox);

			// Combo box: font style
			gbc.gridx = gridX++;
			gbc.gridy = i;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(fontPanels[i].styleComboBox, gbc);
			controlPanel.add(fontPanels[i].styleComboBox);

			// Panel: font size
			JPanel sizePanel = new JPanel(gridBag);

			gbc.gridx = gridX++;
			gbc.gridy = i;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(sizePanel, gbc);
			controlPanel.add(sizePanel);

			// Spinner: font size
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 0, 0, 0);
			gridBag.setConstraints(fontPanels[i].sizeSpinner, gbc);
			sizePanel.add(fontPanels[i].sizeSpinner);

			// Label: "pt"
			JLabel ptLabel = new FLabel(PT_STR);

			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 4, 0, 0);
			gridBag.setConstraints(ptLabel, gbc);
			sizePanel.add(ptLabel);
		}


		//----  Outer panel

		JPanel outerPanel = new JPanel(gridBag);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(controlPanel, gbc);
		outerPanel.add(controlPanel);

		return outerPanel;
	}

	//------------------------------------------------------------------

	private void validatePreferencesGeneral()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	private void validatePreferencesAppearance()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	private void validatePreferencesCompression()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	private void validatePreferencesChunkFilters()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	private void validatePreferencesFonts()
	{
		// do nothing
	}

	//------------------------------------------------------------------

	private void setPreferencesGeneral()
	{
		AppConfig config = AppConfig.INSTANCE;
		config.setCharacterEncoding((characterEncodingComboBox.getSelectedIndex() <= 0)
														? null
														: characterEncodingComboBox.getSelectedValue());
		config.setIgnoreFilenameCase(ignoreFilenameCaseComboBox.getSelectedValue());
		config.setSelectTextOnFocusGained(selectTextOnFocusGainedComboBox.getSelectedValue());
		if (saveMainWindowLocationComboBox.getSelectedValue() != config.isMainWindowLocation())
			config.setMainWindowLocation(saveMainWindowLocationComboBox.getSelectedValue() ? new Point() : null);
	}

	//------------------------------------------------------------------

	private void setPreferencesAppearance()
	{
		AppConfig config = AppConfig.INSTANCE;
		if (lookAndFeelComboBox.isEnabled() && (lookAndFeelComboBox.getSelectedIndex() >= 0))
			config.setLookAndFeel(lookAndFeelComboBox.getSelectedValue());
		config.setTextAntialiasing(textAntialiasingComboBox.getSelectedValue());
		config.setShowOverallProgress(showOverallProgressComboBox.getSelectedValue());
	}

	//------------------------------------------------------------------

	private void setPreferencesCompression()
	{
		AppConfig config = AppConfig.INSTANCE;
		config.setBlockLength(blockLengthSpinner.getIntValue());
	}

	//------------------------------------------------------------------

	private void setPreferencesChunkFilters()
	{
		AppConfig config = AppConfig.INSTANCE;
		for (AudioFileKind fileKind : AudioFileKind.values())
		{
			List<ChunkFilter> filters = new ArrayList<>();
			JComboBox<ChunkFilter> comboBox = chunkFilterComboBoxes.get(fileKind);
			for (int i = AppConfig.GENERIC_FILTERS.length; i < comboBox.getItemCount(); i++)
				filters.add((ChunkFilter)comboBox.getItemAt(i));
			int index = comboBox.getSelectedIndex();
			if (index < 0)
				index = AppConfig.MIN_CHUNK_FILTER_INDEX;
			else
				index += AppConfig.MIN_CHUNK_FILTER_INDEX;
			config.setFilterList(fileKind, filters, index);
		}
	}

	//------------------------------------------------------------------

	private void setPreferencesFonts()
	{
		for (int i = 0; i < fontPanels.length; i++)
		{
			if (fontPanels[i].nameComboBox.getSelectedIndex() >= 0)
				AppConfig.INSTANCE.setFont(i, fontPanels[i].getFont());
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// TABS


	private enum Tab
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		GENERAL
		(
			"General"
		)
		{
			@Override
			protected JPanel createPanel(PreferencesDialog dialog)
			{
				return dialog.createPanelGeneral();
			}

			//----------------------------------------------------------

			@Override
			protected void validatePreferences(PreferencesDialog dialog)
				throws AppException
			{
				dialog.validatePreferencesGeneral();
			}

			//----------------------------------------------------------

			@Override
			protected void setPreferences(PreferencesDialog dialog)
			{
				dialog.setPreferencesGeneral();
			}

			//----------------------------------------------------------
		},

		APPEARANCE
		(
			"Appearance"
		)
		{
			@Override
			protected JPanel createPanel(PreferencesDialog dialog)
			{
				return dialog.createPanelAppearance();
			}

			//----------------------------------------------------------

			@Override
			protected void validatePreferences(PreferencesDialog dialog)
				throws AppException
			{
				dialog.validatePreferencesAppearance();
			}

			//----------------------------------------------------------

			@Override
			protected void setPreferences(PreferencesDialog dialog)
			{
				dialog.setPreferencesAppearance();
			}

			//----------------------------------------------------------
		},

		COMPRESSION
		(
			"Compression"
		)
		{
			@Override
			protected JPanel createPanel(PreferencesDialog dialog)
			{
				return dialog.createPanelCompression();
			}

			//----------------------------------------------------------

			@Override
			protected void validatePreferences(PreferencesDialog dialog)
				throws AppException
			{
				dialog.validatePreferencesCompression();
			}

			//----------------------------------------------------------

			@Override
			protected void setPreferences(PreferencesDialog dialog)
			{
				dialog.setPreferencesCompression();
			}

			//----------------------------------------------------------
		},

		CHUNK_FILTERS
		(
			"Pattern"
		)
		{
			@Override
			protected JPanel createPanel(PreferencesDialog dialog)
			{
				return dialog.createPanelChunkFilters();
			}

			//----------------------------------------------------------

			@Override
			protected void validatePreferences(PreferencesDialog dialog)
				throws AppException
			{
				dialog.validatePreferencesChunkFilters();
			}

			//----------------------------------------------------------

			@Override
			protected void setPreferences(PreferencesDialog dialog)
			{
				dialog.setPreferencesChunkFilters();
			}

			//----------------------------------------------------------
		},

		FONTS
		(
			"Fonts"
		)
		{
			@Override
			protected JPanel createPanel(PreferencesDialog dialog)
			{
				return dialog.createPanelFonts();
			}

			//----------------------------------------------------------

			@Override
			protected void validatePreferences(PreferencesDialog dialog)
				throws AppException
			{
				dialog.validatePreferencesFonts();
			}

			//----------------------------------------------------------

			@Override
			protected void setPreferences(PreferencesDialog dialog)
			{
				dialog.setPreferencesFonts();
			}

			//----------------------------------------------------------
		};

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Tab(String text)
		{
			this.text = text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Abstract methods
	////////////////////////////////////////////////////////////////////

		protected abstract JPanel createPanel(PreferencesDialog dialog);

		//--------------------------------------------------------------

		protected abstract void validatePreferences(PreferencesDialog dialog)
			throws AppException;

		//--------------------------------------------------------------

		protected abstract void setPreferences(PreferencesDialog dialog);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILTER COMBO BOX RENDERER CLASS


	private class FilterComboBoxRenderer
		extends JComponent
		implements ListCellRenderer<ChunkFilter>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	NUM_COLUMNS	= 48;

		private static final	int	TOP_MARGIN		= 1;
		private static final	int	BOTTOM_MARGIN	= TOP_MARGIN;
		private static final	int	LEADING_MARGIN	= 3;
		private static final	int	TRAILING_MARGIN	= 5;
		private static final	int	ICON_TEXT_GAP	= 4;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int			maxTextWidth;
		private	int			textWidth;
		private	int			textHeight;
		private	ImageIcon	icon;
		private	String		text;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public FilterComboBoxRenderer()
		{
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ListCellRenderer interface
	////////////////////////////////////////////////////////////////////

		public Component getListCellRendererComponent(JList<? extends ChunkFilter> list,
													  ChunkFilter                  value,
													  int                          index,
													  boolean                      isSelected,
													  boolean                      cellHasFocus)
		{
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

			ChunkFilter filter = (ChunkFilter)value;
			icon = (filter == null) ? null
									: filter.isIncludeAll()
											? Icons.INCLUDE
											: filter.isExcludeAll()
													? Icons.EXCLUDE
													: filter.isInclude()
															? Icons.INCLUDE
															: Icons.EXCLUDE;
			text = (filter == null) ? "" : filter.getIdString();

			FontMetrics fontMetrics = getFontMetrics(list.getFont());
			maxTextWidth = NUM_COLUMNS * FontUtils.getCharWidth('0', fontMetrics);
			textWidth = fontMetrics.stringWidth(text);
			if (textWidth > maxTextWidth)
			{
				int maxWidth = maxTextWidth - fontMetrics.stringWidth(AppConstants.ELLIPSIS_STR);
				char[] chars = text.toCharArray();
				int length = chars.length;
				while ((length > 0) && (textWidth > maxWidth))
					textWidth -= fontMetrics.charWidth(chars[--length]);
				text = new String(chars, 0, length) + AppConstants.ELLIPSIS_STR;
			}
			textHeight = fontMetrics.getHeight();

			return this;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public Dimension getPreferredSize()
		{
			int width = LEADING_MARGIN + icon.getIconWidth() + ICON_TEXT_GAP + maxTextWidth + TRAILING_MARGIN;
			int height = TOP_MARGIN + Math.max(icon.getIconHeight(), textHeight) + BOTTOM_MARGIN;
			return new Dimension(width, height);
		}

		//--------------------------------------------------------------

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			Graphics2D gr2d = GuiUtils.copyGraphicsContext(gr);

			// Fill background
			Rectangle rect = gr2d.getClipBounds();
			gr2d.setColor(getBackground());
			gr2d.fillRect(rect.x, rect.y, rect.width, rect.height);

			// Draw icon
			int x = LEADING_MARGIN;
			int y = (getHeight() - icon.getIconHeight()) / 2;
			gr2d.drawImage(icon.getImage(), x, y, null);

			// Set rendering hints for text antialiasing and fractional metrics
			TextRendering.setHints(gr2d);

			// Draw text
			FontMetrics fontMetrics = gr2d.getFontMetrics();
			x += icon.getIconWidth() + ICON_TEXT_GAP;
			y = (getHeight() - textHeight) / 2 + fontMetrics.getAscent();
			gr2d.setColor(getForeground());
			gr2d.drawString(text, x, y);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// FONT PANEL CLASS


	private static class FontPanel
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		MIN_SIZE	= 0;
		private static final	int		MAX_SIZE	= 99;

		private static final	int		SIZE_FIELD_LENGTH	= 2;

		private static final	String	DEFAULT_FONT_STR	= "<default font>";

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	FComboBox<String>		nameComboBox;
		private	FComboBox<FontStyle>	styleComboBox;
		private	SizeSpinner				sizeSpinner;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FontPanel(FontEx   font,
						  String[] fontNames)
		{
			nameComboBox = new FComboBox<>();
			nameComboBox.addItem(DEFAULT_FONT_STR);
			for (String fontName : fontNames)
				nameComboBox.addItem(fontName);
			nameComboBox.setSelectedIndex(Utils.indexOf(font.getName(), fontNames) + 1);

			styleComboBox = new FComboBox<>(FontStyle.values());
			styleComboBox.setSelectedValue(font.getStyle());

			sizeSpinner = new SizeSpinner(font.getSize());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public FontEx getFont()
		{
			String name = (nameComboBox.getSelectedIndex() <= 0) ? null : nameComboBox.getSelectedValue();
			return new FontEx(name, styleComboBox.getSelectedValue(), sizeSpinner.getIntValue());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Member classes : non-inner classes
	////////////////////////////////////////////////////////////////////


		// SIZE SPINNER CLASS


		private static class SizeSpinner
			extends IntegerSpinner
		{

		////////////////////////////////////////////////////////////////
		//  Constructors
		////////////////////////////////////////////////////////////////

			private SizeSpinner(int value)
			{
				super(value, MIN_SIZE, MAX_SIZE, SIZE_FIELD_LENGTH);
				AppFont.TEXT_FIELD.apply(this);
			}

			//----------------------------------------------------------

		////////////////////////////////////////////////////////////////
		//  Instance methods : overriding methods
		////////////////////////////////////////////////////////////////

			/**
			 * @throws NumberFormatException
			 */

			@Override
			protected int getEditorValue()
			{
				IntegerValueField field = (IntegerValueField)getEditor();
				return field.isEmpty() ? 0 : field.getValue();
			}

			//----------------------------------------------------------

			@Override
			protected void setEditorValue(int value)
			{
				IntegerValueField field = (IntegerValueField)getEditor();
				if (value == 0)
					field.setText(null);
				else
					field.setValue(value);
			}

			//----------------------------------------------------------

		}

		//==============================================================

	}

	//==================================================================

}

//----------------------------------------------------------------------
