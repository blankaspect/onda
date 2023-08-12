/*====================================================================*\

AudioFileKindDialog.java

Audio file kind dialog class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Dialog;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.blankaspect.common.stdin.InputUtils;

import uk.blankaspect.ui.swing.action.KeyAction;

import uk.blankaspect.ui.swing.button.FButton;

import uk.blankaspect.ui.swing.combobox.FComboBox;

import uk.blankaspect.ui.swing.label.FLabel;

import uk.blankaspect.ui.swing.misc.GuiUtils;

//----------------------------------------------------------------------


// AUDIO FILE KIND DIALOG CLASS


class AudioFileKindDialog
	extends JDialog
	implements ActionListener
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private static final	String	TITLE_STR	= "Select output file kind";
	private static final	String	OPTION_STR	= "AIFF, WAVE or Quit (A/W/Q) ?";

	private static final	String[]	PROMPT_STRS	=
	{
		"The kind of output file cannot be determined from the input file.",
		"Select the kind of output file"
	};

	// Commands
	private interface Command
	{
		String	ACCEPT	= "accept";
		String	CLOSE	= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private AudioFileKindDialog(Window owner)
	{

		// Call superclass constructor
		super(owner, TITLE_STR, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		setIconImages(owner.getIconImages());


		//----  Control panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel controlPanel = new JPanel(gridBag);
		GuiUtils.setPaddedLineBorder(controlPanel);

		int gridY = 0;

		// Labels: prompt
		for (String str : PROMPT_STRS)
		{
			JLabel label = new FLabel(str);

			gbc.gridx = 0;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = AppConstants.COMPONENT_INSETS;
			gridBag.setConstraints(label, gbc);
			controlPanel.add(label);
		}

		// Combo box: file kind
		fileKindComboBox = new FComboBox<>(AudioFileKind.values());
		fileKindComboBox.setSelectedValue(fileKind);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 6, 2, 6);
		gridBag.setConstraints(fileKindComboBox, gbc);
		controlPanel.add(fileKindComboBox);


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

		// Set location of dialog box
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

	public static AudioFileKind showDialog(Component parent)
	{
		return new AudioFileKindDialog(GuiUtils.getWindow(parent)).getFileKind();
	}

	//------------------------------------------------------------------

	public static AudioFileKind showPrompt()
	{
		for (String str : PROMPT_STRS)
			System.out.println(str);

		switch (InputUtils.readResponse(OPTION_STR, 'A', 'W', 'Q'))
		{
			case 'A':
				return AudioFileKind.AIFF;

			case 'W':
				return AudioFileKind.WAVE;
		}
		return null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();

		if (command.equals(Command.ACCEPT))
			onAccept();

		else if (command.equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public AudioFileKind getFileKind()
	{
		return (accepted ? fileKind : null);
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
		fileKind = fileKindComboBox.getSelectedValue();
		setVisible(false);
		dispose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point			location;
	private static	AudioFileKind	fileKind	= AudioFileKind.WAVE;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	boolean						accepted;
	private	FComboBox<AudioFileKind>	fileKindComboBox;

}

//----------------------------------------------------------------------
