/*====================================================================*\

MainWindow.java

Class: main window.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.iff.ChunkFilter;

import uk.blankaspect.common.misc.SystemUtils;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.checkbox.FCheckBox;

import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.container.PathnamePanel;

import uk.blankaspect.ui.swing.dialog.NonEditableTextPaneDialog;

import uk.blankaspect.ui.swing.filechooser.FileChooserUtils;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.menu.FMenuItem;

import uk.blankaspect.ui.swing.misc.GuiUtils;

import uk.blankaspect.ui.swing.transfer.DataImporter;

import uk.blankaspect.ui.swing.workaround.LinuxWorkarounds;

//----------------------------------------------------------------------


// CLASS: MAIN WINDOW


class MainWindow
	extends JFrame
	implements ActionListener, MouseListener
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	Insets	ARROW_BUTTON_MARGINS	= new Insets(2, 4, 2, 4);

	private static final	String	INPUT_MODE_STR						= "Input mode";
	private static final	String	RECURSIVE_STR						= "Recursive";
	private static final	String	INPUT_FILE_OR_DIRECTORY_STR			= "Input file or directory";
	private static final	String	OUTPUT_DIRECTORY_STR				= "Output directory";
	private static final	String	COMPRESS_STR						= "Compress";
	private static final	String	EXPAND_STR							= "Expand";
	private static final	String	VALIDATE_STR						= "Validate";
	private static final	String	COMPRESS_FILE_STR					= "Compress file";
	private static final	String	EXPAND_FILE_STR						= "Expand file";
	private static final	String	VALIDATE_FILE_STR					= "Validate file";
	private static final	String	PROCEED_STR							= "Proceed";
	private static final	String	SELECT_INPUT_PATHNAME_TOOLTIP_STR	= "Select input file or directory";
	private static final	String	SELECT_OUTPUT_DIRECTORY_TOOLTIP_STR	= "Select output directory";
	private static final	String	COPY_IN_TO_OUT_TOOLTIP_STR			= "Copy input pathname to output pathname";
	private static final	String	COPY_OUT_TO_IN_TOOLTIP_STR			= "Copy output pathname to input pathname";
	private static final	String	INPUT_FILE_OR_DIRECTORY_TITLE_STR	= "Input file or directory";
	private static final	String	OUTPUT_DIRECTORY_TITLE_STR			= "Output directory";
	private static final	String	SELECT_STR							= "Select";
	private static final	String	SELECT_FILE_OR_DIRECTORY_STR		= "Select file or directory";
	private static final	String	SELECT_DIRECTORY_STR				= "Select directory";
	private static final	String	LOG_STR								= "Log";
	private static final	String	NUM_FILES_TO_COMPRESS				= "Number of files to compress : ";
	private static final	String	NUM_FILES_TO_EXPAND					= "Number of files to expand : ";
	private static final	String	PROCEED_MESSAGE_STR					= "Do you want to proceed with the task?";

	// Commands
	private interface Command
	{
		String	CHOOSE_INPUT_PATHNAME					= "chooseInputPathname";
		String	CHOOSE_OUTPUT_DIRECTORY					= "chooseOutputDirectory";
		String	COPY_INPUT_PATHNAME_TO_OUTPUT_PATHNAME	= "copyInputPathnameToOutputPathname";
		String	COPY_OUTPUT_PATHNAME_TO_INPUT_PATHNAME	= "copyOutputPathnameToInputPathname";
		String	SHOW_CONTEXT_MENU						= "showContextMenu";
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	FComboBox<InputMode>	inputModeComboBox;
	private	JCheckBox				recursiveCheckBox;
	private	FPathnameComboBox		inPathnameComboBox;
	private	FPathnameComboBox		outDirectoryComboBox;
	private	JFileChooser			inPathnameChooser;
	private	JFileChooser			outDirectoryChooser;
	private	JFileChooser			compressFileChooser;
	private	JFileChooser			expandFileChooser;
	private	JFileChooser			validateFileChooser;
	private	JPopupMenu				contextMenu;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public MainWindow(
		String	title)
	{
		// Call superclass constructor
		super(title);

		// Set icons
		setIconImages(Images.APP_ICON_IMAGES);

		// Initialise instance variables
		inPathnameChooser = new JFileChooser(SystemUtils.userHomeDirectoryPathname());
		inPathnameChooser.setDialogTitle(INPUT_FILE_OR_DIRECTORY_TITLE_STR);
		inPathnameChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		inPathnameChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		inPathnameChooser.setApproveButtonToolTipText(SELECT_FILE_OR_DIRECTORY_STR);

		outDirectoryChooser = new JFileChooser(SystemUtils.userHomeDirectoryPathname());
		outDirectoryChooser.setDialogTitle(OUTPUT_DIRECTORY_TITLE_STR);
		outDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		outDirectoryChooser.setApproveButtonMnemonic(KeyEvent.VK_S);
		outDirectoryChooser.setApproveButtonToolTipText(SELECT_DIRECTORY_STR);

		AppConfig config = AppConfig.INSTANCE;

		compressFileChooser = new JFileChooser(config.getCompressDirectory());
		compressFileChooser.setDialogTitle(COMPRESS_FILE_STR);
		compressFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileChooserUtils.setFilter(compressFileChooser, AppConstants.AUDIO_FILE_FILTER);

		expandFileChooser = new JFileChooser(config.getExpandDirectory());
		expandFileChooser.setDialogTitle(EXPAND_FILE_STR);
		expandFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileChooserUtils.setFilter(expandFileChooser, AppConstants.COMPRESSED_FILE_FILTER);

		validateFileChooser = new JFileChooser(config.getValidateDirectory());
		validateFileChooser.setDialogTitle(VALIDATE_FILE_STR);
		validateFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileChooserUtils.setFilter(validateFileChooser, AppConstants.COMPRESSED_FILE_FILTER);


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Label: input mode
		JLabel inputModeLabel = new FLabel(INPUT_MODE_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inputModeLabel, gbc);
		controlPanel.add(inputModeLabel);

		// Panel: input mode
		JPanel inputModePanel = new JPanel(gridBag);

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inputModePanel, gbc);
		controlPanel.add(inputModePanel);

		// Combo box: input mode
		inputModeComboBox = new FComboBox<>(InputMode.values());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(inputModeComboBox, gbc);
		inputModePanel.add(inputModeComboBox);

		// Check box: recursive
		recursiveCheckBox = new FCheckBox(RECURSIVE_STR);
		recursiveCheckBox.setMnemonic(KeyEvent.VK_R);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 24, 0, 0);
		gridBag.setConstraints(recursiveCheckBox, gbc);
		inputModePanel.add(recursiveCheckBox);

		// Label: input pathname
		JLabel inPathnameLabel = new FLabel(INPUT_FILE_OR_DIRECTORY_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inPathnameLabel, gbc);
		controlPanel.add(inPathnameLabel);

		// Panel: input pathname
		inPathnameComboBox = new FPathnameComboBox();
		FPathnameComboBox.addObserver(inPathnameComboBox);
		PathnamePanel inPathnamePanel = new PathnamePanel(inPathnameComboBox, Command.CHOOSE_INPUT_PATHNAME, this);
		inPathnamePanel.setButtonTooltipText(SELECT_INPUT_PATHNAME_TOOLTIP_STR);

		gbc.gridx = 1;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(inPathnamePanel, gbc);
		controlPanel.add(inPathnamePanel);

		// Button: copy input pathname to output pathname
		JButton copyInPathToOutPathButton = new JButton(Icons.ARROW_DOWN);
		copyInPathToOutPathButton.setMargin(ARROW_BUTTON_MARGINS);
		copyInPathToOutPathButton.setToolTipText(COPY_IN_TO_OUT_TOOLTIP_STR);
		copyInPathToOutPathButton.setActionCommand(Command.COPY_INPUT_PATHNAME_TO_OUTPUT_PATHNAME);
		copyInPathToOutPathButton.addActionListener(this);

		gbc.gridx = 2;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(copyInPathToOutPathButton, gbc);
		controlPanel.add(copyInPathToOutPathButton);

		// Label: output directory
		JLabel outDirectoryLabel = new FLabel(OUTPUT_DIRECTORY_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outDirectoryLabel, gbc);
		controlPanel.add(outDirectoryLabel);

		// Panel: output directory
		outDirectoryComboBox = new FPathnameComboBox();
		FPathnameComboBox.addObserver(outDirectoryComboBox);
		PathnamePanel outDirectoryPanel =
				new PathnamePanel(outDirectoryComboBox, Command.CHOOSE_OUTPUT_DIRECTORY, this);
		outDirectoryPanel.setButtonTooltipText(SELECT_OUTPUT_DIRECTORY_TOOLTIP_STR);

		gbc.gridx = 1;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(outDirectoryPanel, gbc);
		controlPanel.add(outDirectoryPanel);

		// Button: copy output pathname to input pathname
		JButton copyOutPathToInPathButton = new JButton(Icons.ARROW_UP);
		copyOutPathToInPathButton.setMargin(ARROW_BUTTON_MARGINS);
		copyOutPathToInPathButton.setToolTipText(COPY_OUT_TO_IN_TOOLTIP_STR);
		copyOutPathToInPathButton.setActionCommand(Command.COPY_OUTPUT_PATHNAME_TO_INPUT_PATHNAME);
		copyOutPathToInPathButton.addActionListener(this);

		gbc.gridx = 2;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = AppConstants.COMPONENT_INSETS;
		gridBag.setConstraints(copyOutPathToInPathButton, gbc);
		controlPanel.add(copyOutPathToInPathButton);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(0, 3, 8, 6));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

		// Button: compress
		JButton compressButton = new FButton(AppCommand.COMPRESS);
		buttonPanel.add(compressButton);

		// Button: expand
		buttonPanel.add(new FButton(AppCommand.EXPAND));

		// Button: validate
		buttonPanel.add(new FButton(AppCommand.VALIDATE));

		// Button: view log
		buttonPanel.add(new FButton(AppCommand.VIEW_LOG));

		// Button: preferences
		buttonPanel.add(new FButton(AppCommand.EDIT_PREFERENCES));

		// Button: exit
		buttonPanel.add(new FButton(AppCommand.EXIT));


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

		// Set transfer handler on main panel
		mainPanel.setTransferHandler(new FileTransferHandler());

		// Add listener
		mainPanel.addMouseListener(this);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), Command.SHOW_CONTEXT_MENU, this);

		// Update commands
		updateCommands();


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(
				WindowEvent	event)
			{
				AppCommand.EXIT.execute();
			}
		});

		// Prevent window from being resized
		setResizable(false);

		// Resize window to its preferred size
		pack();

		// Set location of window
		Point location = config.getMainWindowLocation();
		location = (location == null)
							? GuiUtils.getComponentLocation(this)
							: GuiUtils.getLocationWithinScreen(this, location);
		setLocation(location);

		// Set focus
		inPathnameComboBox.requestFocusInWindow();

		// Make window visible
		setVisible(true);

		// WORKAROUND for a bug that has been observed on Linux/GNOME whereby a window is displaced downwards when its
		// location is set.  The error in the y coordinate is the height of the title bar of the window.  The workaround
		// is to set the location of the window again with an adjustment for the error.
		LinuxWorkarounds.fixWindowYCoord(this, location);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void actionPerformed(ActionEvent event)
	{
		switch (event.getActionCommand())
		{
			case Command.CHOOSE_INPUT_PATHNAME                  -> onChooseInputPathname();
			case Command.CHOOSE_OUTPUT_DIRECTORY                -> onChooseOutputDirectory();
			case Command.COPY_INPUT_PATHNAME_TO_OUTPUT_PATHNAME -> onCopyInputPathnameToOutputPathname();
			case Command.COPY_OUTPUT_PATHNAME_TO_INPUT_PATHNAME -> onCopyOutputPathnameToInputPathname();
			case Command.SHOW_CONTEXT_MENU                      -> onShowContextMenu();
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : MouseListener interface
////////////////////////////////////////////////////////////////////////

	@Override
	public void mouseClicked(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseEntered(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mouseExited(
		MouseEvent	event)
	{
		// do nothing
	}

	//------------------------------------------------------------------

	@Override
	public void mousePressed(
		MouseEvent	event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

	@Override
	public void mouseReleased(
		MouseEvent	event)
	{
		showContextMenu(event);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void executeCommand(
		AppCommand	command)
	{
		try
		{
			switch (command)
			{
				case IMPORT_FILES:
					onImportFiles();
					break;

				case COMPRESS:
					onCompress();
					break;

				case EXPAND:
					onExpand();
					break;

				case VALIDATE:
					onValidate();
					break;

				case VIEW_LOG:
					onViewLog();
					break;

				case EDIT_PREFERENCES:
					onEditPreferences();
					break;

				case EXIT:
					onExit();
					break;
			}
		}
		catch (AppException e)
		{
			OndaApp.INSTANCE.showErrorMessage(OndaApp.SHORT_NAME, e);
		}

		updateCommands();
	}

	//------------------------------------------------------------------

	private InputMode getInputMode()
	{
		return inputModeComboBox.getSelectedValue();
	}

	//------------------------------------------------------------------

	private boolean isRecursive()
	{
		return recursiveCheckBox.isSelected();
	}

	//------------------------------------------------------------------

	private void updateCommands()
	{
		AppCommand.setAllEnabled(true);

		AppCommand.VIEW_LOG.setEnabled(!Log.INSTANCE.isEmpty());
	}

	//------------------------------------------------------------------

	private void validateInPathname()
		throws AppException
	{
		try
		{
			if (!inPathnameComboBox.isEmpty())
			{
				File file = inPathnameComboBox.getFile();
				if (!file.exists())
					throw new FileException(ErrorId.INPUT_FILE_OR_DIRECTORY_DOES_NOT_EXIST, file);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(inPathnameComboBox);
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void validateOutDirectory()
		throws AppException
	{
		try
		{
			if (!outDirectoryComboBox.isEmpty())
			{
				File directory = outDirectoryComboBox.getFile();
				if (directory.exists() && !directory.isDirectory())
					throw new FileException(ErrorId.NOT_A_DIRECTORY, directory);
			}
		}
		catch (AppException e)
		{
			GuiUtils.setFocus(outDirectoryComboBox);
			throw e;
		}
	}

	//------------------------------------------------------------------

	private ChunkFilter[] getChunkFilters()
		throws AppException
	{
		AppConfig config = AppConfig.INSTANCE;
		ChunkFilter[] chunkFilters = new ChunkFilter[AudioFileKind.values().length];
		for (AudioFileKind fileKind : AudioFileKind.values())
		{
			ChunkFilter chunkFilter = config.getChunkFilter(fileKind);
			if (chunkFilter == null)
				throw new AppException(ErrorId.NO_CHUNK_FILTER, fileKind.toString());
			chunkFilters[fileKind.ordinal()] = chunkFilter;
		}
		return chunkFilters;
	}

	//------------------------------------------------------------------

	private File getOutDirectory()
	{
		return outDirectoryComboBox.isEmpty() ? null : outDirectoryComboBox.getFile();
	}

	//------------------------------------------------------------------

	private boolean compressFiles(
		List<InputOutput>	inputsOutputs,
		ChunkFilter[]		chunkFilters)
		throws AppException
	{
		try
		{
			TaskProgressDialog.showDialog(this, COMPRESS_STR,
										  new Task.Compress(inputsOutputs, chunkFilters, isRecursive()),
										  AppConfig.INSTANCE.isShowOverallProgress());
		}
		catch (TaskCancelledException e)
		{
			return false;
		}
		return true;
	}

	//------------------------------------------------------------------

	private boolean expandFiles(
		List<InputOutput>	inputsOutputs)
		throws AppException
	{
		try
		{
			TaskProgressDialog.showDialog(this, EXPAND_STR, new Task.Expand(inputsOutputs, isRecursive()),
										  AppConfig.INSTANCE.isShowOverallProgress());
		}
		catch (TaskCancelledException e)
		{
			return false;
		}
		return true;
	}

	//------------------------------------------------------------------

	private void showContextMenu(
		MouseEvent	event)
	{
		if ((event == null) || event.isPopupTrigger())
		{
			// Create context menu
			if (contextMenu == null)
			{
				contextMenu = new JPopupMenu();
				contextMenu.add(new FMenuItem(AppCommand.COMPRESS));
				contextMenu.add(new FMenuItem(AppCommand.EXPAND));
				contextMenu.add(new FMenuItem(AppCommand.VALIDATE));
				contextMenu.add(new FMenuItem(AppCommand.VIEW_LOG));
				contextMenu.add(new FMenuItem(AppCommand.EDIT_PREFERENCES));
				contextMenu.add(new FMenuItem(AppCommand.EXIT));
			}

			// Update commands for menu items
			updateCommands();

			// Display menu
			if (event == null)
				contextMenu.show(getContentPane(), 0, 0);
			else
				contextMenu.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	//------------------------------------------------------------------

	private void updateConfiguration()
	{
		// Save location of main window
		AppConfig config = AppConfig.INSTANCE;
		if (config.isMainWindowLocation())
		{
			Point location = GuiUtils.getFrameLocation(this);
			if (location != null)
				config.setMainWindowLocation(location);
		}

		// Save pathnames
		config.setCompressPathname(Utils.getPathname(compressFileChooser.getCurrentDirectory()));
		config.setExpandPathname(Utils.getPathname(expandFileChooser.getCurrentDirectory()));
		config.setValidatePathname(Utils.getPathname(validateFileChooser.getCurrentDirectory()));

		// Write configuration file
		config.write();
	}

	//------------------------------------------------------------------

	private void onChooseInputPathname()
	{
		if (!inPathnameComboBox.isEmpty())
			inPathnameChooser.setCurrentDirectory(inPathnameComboBox.getCanonicalFile());
		inPathnameChooser.rescanCurrentDirectory();
		if (inPathnameChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			inPathnameComboBox.setFile(inPathnameChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onChooseOutputDirectory()
	{
		if (!outDirectoryComboBox.isEmpty())
			outDirectoryChooser.setCurrentDirectory(outDirectoryComboBox.getCanonicalFile());
		outDirectoryChooser.rescanCurrentDirectory();
		if (outDirectoryChooser.showDialog(this, SELECT_STR) == JFileChooser.APPROVE_OPTION)
			outDirectoryComboBox.setFile(outDirectoryChooser.getSelectedFile());
	}

	//------------------------------------------------------------------

	private void onCopyInputPathnameToOutputPathname()
	{
		outDirectoryComboBox.setText(inPathnameComboBox.getText());
	}

	//------------------------------------------------------------------

	private void onCopyOutputPathnameToInputPathname()
	{
		inPathnameComboBox.setText(outDirectoryComboBox.getText());
	}

	//------------------------------------------------------------------

	private void onShowContextMenu()
	{
		showContextMenu(null);
	}

	//------------------------------------------------------------------

	private void onImportFiles()
		throws AppException
	{
		// Validate output directory
		validateOutDirectory();

		// Create lists of files to compress or expand
		@SuppressWarnings("unchecked")
		List<File> files = (List<File>)AppCommand.IMPORT_FILES.getValue(AppCommand.Property.FILES);
		File outDirectory = getOutDirectory();
		List<InputOutput> compressInputsOutputs = new ArrayList<>();
		List<InputOutput> expandInputsOutputs = new ArrayList<>();
		for (File file : files)
		{
			if (file.isFile())
			{
				if (file.getName().endsWith(AppConstants.COMPRESSED_FILENAME_EXTENSION))
					expandInputsOutputs.add(new InputOutput(file, outDirectory));
				else
					compressInputsOutputs.add(new InputOutput(file, outDirectory));
			}
		}

		// Test for files
		int numFilesToCompress = compressInputsOutputs.size();
		int numFilesToExpand = expandInputsOutputs.size();
		if (numFilesToCompress + numFilesToExpand == 0)
			throw new AppException(ErrorId.NO_FILES_TRANSFERRED);

		// Confirm operation
		StringBuilder buffer = new StringBuilder(128);
		if (numFilesToCompress > 0)
		{
			buffer.append(NUM_FILES_TO_COMPRESS);
			buffer.append(numFilesToCompress);
			buffer.append('\n');
		}
		if (numFilesToExpand > 0)
		{
			buffer.append(NUM_FILES_TO_EXPAND);
			buffer.append(numFilesToExpand);
			buffer.append('\n');
		}
		buffer.append(PROCEED_MESSAGE_STR);
		String[] optionStrs = Utils.getOptionStrings(PROCEED_STR);
		if (JOptionPane.showOptionDialog(this, buffer, OndaApp.SHORT_NAME, JOptionPane.OK_CANCEL_OPTION,
										 JOptionPane.QUESTION_MESSAGE, null, optionStrs,
										 optionStrs[1]) == JOptionPane.OK_OPTION)

			// Compress and expand files
			if (compressFiles(compressInputsOutputs, getChunkFilters()))
				expandFiles(expandInputsOutputs);
	}

	//------------------------------------------------------------------

	private boolean onCompress()
		throws AppException
	{
		// Validate input pathname
		validateInPathname();

		// Validate output directory
		validateOutDirectory();

		// Get chunk filters
		ChunkFilter[] chunkFilters = getChunkFilters();

		// Choose file
		File outDirectory = getOutDirectory();
		List<InputOutput> inputsOutputs = new ArrayList<>();
		if (inPathnameComboBox.isEmpty())
		{
			compressFileChooser.rescanCurrentDirectory();
			compressFileChooser.setSelectedFile(new File(""));
			if (compressFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return false;
			inputsOutputs.add(new InputOutput(compressFileChooser.getSelectedFile(), outDirectory));
		}
		else
		{
			switch (getInputMode())
			{
				case DIRECT:
					inputsOutputs.add(new InputOutput(inPathnameComboBox.getFile(), outDirectory));
					break;

				case LIST:
					inputsOutputs = OndaApp.readListFile(inPathnameComboBox.getFile());
					break;
			}
		}

		// Compress files
		return compressFiles(inputsOutputs, chunkFilters);
	}

	//------------------------------------------------------------------

	private boolean onExpand()
		throws AppException
	{
		// Validate input pathname
		validateInPathname();

		// Validate output directory
		validateOutDirectory();

		// Choose file
		File outDirectory = getOutDirectory();
		List<InputOutput> inputsOutputs = new ArrayList<>();
		if (inPathnameComboBox.isEmpty())
		{
			expandFileChooser.rescanCurrentDirectory();
			expandFileChooser.setSelectedFile(new File(""));
			if (expandFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return false;
			inputsOutputs.add(new InputOutput(expandFileChooser.getSelectedFile(), outDirectory));
		}
		else
		{
			switch (getInputMode())
			{
				case DIRECT:
					inputsOutputs.add(new InputOutput(inPathnameComboBox.getFile(), outDirectory));
					break;

				case LIST:
					inputsOutputs = OndaApp.readListFile(inPathnameComboBox.getFile());
					break;
			}
		}

		// Expand files
		return expandFiles(inputsOutputs);
	}

	//------------------------------------------------------------------

	private void onValidate()
		throws AppException
	{
		// Validate input pathname
		validateInPathname();

		// Display file selection dialog
		List<InputOutput> inputsOutputs = new ArrayList<>();
		if (inPathnameComboBox.isEmpty())
		{
			validateFileChooser.rescanCurrentDirectory();
			validateFileChooser.setSelectedFile(new File(""));
			if (validateFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return;
			inputsOutputs.add(new InputOutput(validateFileChooser.getSelectedFile()));
		}
		else
		{
			switch (getInputMode())
			{
				case DIRECT:
					inputsOutputs.add(new InputOutput(inPathnameComboBox.getFile()));
					break;

				case LIST:
					inputsOutputs = OndaApp.readListFile(inPathnameComboBox.getFile());
					break;
			}
		}

		// Validate files
		TaskProgressDialog.showDialog(this, VALIDATE_STR,
									  new Task.Validate(inputsOutputs, isRecursive()),
									  AppConfig.INSTANCE.isShowOverallProgress());
	}

	//------------------------------------------------------------------

	private void onViewLog()
	{
		Log log = Log.INSTANCE;
		if (!log.isEmpty())
		{
			LogDialog dialog = LogDialog.showDialog(this, LOG_STR, log.getLines());
			if (dialog.isCleared())
				log.clear();
		}
	}

	//------------------------------------------------------------------

	private void onEditPreferences()
	{
		if (PreferencesDialog.showDialog(this))
			ExceptionUtils.setUnixStyle(AppConfig.INSTANCE.isShowUnixPathnames());
	}

	//------------------------------------------------------------------

	private void onExit()
	{
		// Update configuration
		updateConfiguration();

		// Destroy window
		setVisible(false);
		dispose();

		// Exit application
		System.exit(0);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: INPUT MODE


	private enum InputMode
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		DIRECT  ("Direct"),
		LIST    ("List");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private InputMode(
			String	text)
		{
			this.text = text;
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

	}

	//==================================================================


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE_TRANSFER_NOT_SUPPORTED
		("File transfer is not supported."),

		ERROR_TRANSFERRING_DATA
		("An error occurred while transferring data."),

		NO_FILES_TRANSFERRED
		("The transfer did not include any files."),

		INPUT_FILE_OR_DIRECTORY_DOES_NOT_EXIST
		("The input file or directory does not exist."),

		NOT_A_DIRECTORY
		("The output pathname does not denote a directory."),

		NO_CHUNK_FILTER
		("No %1 chunk filter has been specified.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(
			String	message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		@Override
		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: LOG DIALOG


	private static class LogDialog
		extends NonEditableTextPaneDialog
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		NUM_COLUMNS	= 72;
		private static final	int		NUM_ROWS	= 24;

		private static final	String	KEY	= LogDialog.class.getCanonicalName();

		private static final	String	ERROR_STYLE_KEY		= "error";
		private static final	Color	ERROR_STYLE_COLOUR	= new Color(208, 0, 0);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	List<Log.Line>	lines;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private LogDialog(
			Window			owner,
			String			title,
			List<Log.Line>	lines)
		{
			// Call superclass constructor
			super(owner, title, KEY, NUM_COLUMNS, NUM_ROWS, true);

			// Initialise instance variables
			this.lines = lines;

			// Add error style
			Style style = addStyle(ERROR_STYLE_KEY);
			StyleConstants.setForeground(style, ERROR_STYLE_COLOUR);

			// Set text
			for (Log.Line line : lines)
			{
				Paragraph paragraph = new Paragraph(StyleContext.DEFAULT_STYLE);
				paragraph.add(new Span(line.text(), (line.kind() == Log.LineKind.INFO)
																	? StyleContext.DEFAULT_STYLE
																	: ERROR_STYLE_KEY));
				append(paragraph);
			}
			setCaretToEnd();

			// Show dialog
			setVisible(true);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static LogDialog showDialog(
			Component		parent,
			String			title,
			List<Log.Line>	lines)
		{
			return new LogDialog(GuiUtils.getWindow(parent), title, lines);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getText()
		{
			StringBuilder buffer = new StringBuilder(lines.size() * 80);
			for (Log.Line line : lines)
			{
				if (line.kind() == Log.LineKind.ERROR)
					buffer.append(Log.ERROR_PREFIX);
				buffer.append(line.text());
				buffer.append('\n');
			}
			return buffer.toString();
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: FILE-TRANSFER HANDLER


	private class FileTransferHandler
		extends TransferHandler
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public FileTransferHandler()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			AppCommand.IMPORT_FILES.execute();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean canImport(
			TransferHandler.TransferSupport	support)
		{
			boolean supported = !support.isDrop() || ((support.getSourceDropActions() & COPY) == COPY);
			if (supported)
				supported = DataImporter.isFileList(support.getDataFlavors());
			if (support.isDrop() && supported)
				support.setDropAction(COPY);
			return supported;
		}

		//--------------------------------------------------------------

		@Override
		public boolean importData(
			TransferHandler.TransferSupport	support)
		{
			if (canImport(support))
			{
				try
				{
					try
					{
						List<File> files = DataImporter.getFiles(support.getTransferable());
						if (!files.isEmpty())
						{
							toFront();
							AppCommand.IMPORT_FILES.putValue(AppCommand.Property.FILES, files);
							SwingUtilities.invokeLater(this);
							return true;
						}
					}
					catch (UnsupportedFlavorException e)
					{
						throw new AppException(ErrorId.FILE_TRANSFER_NOT_SUPPORTED);
					}
					catch (IOException e)
					{
						throw new AppException(ErrorId.ERROR_TRANSFERRING_DATA);
					}
				}
				catch (AppException e)
				{
					OndaApp.INSTANCE.showErrorMessage(OndaApp.SHORT_NAME, e);
				}
			}
			return false;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
