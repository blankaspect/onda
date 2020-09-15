/*====================================================================*\

TaskProgressDialog.java

Task progress dialog box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
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

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.number.NumberUtils;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.common.swing.action.KeyAction;

import uk.blankaspect.common.swing.button.FButton;

import uk.blankaspect.common.swing.label.FLabel;

import uk.blankaspect.common.swing.misc.GuiUtils;

import uk.blankaspect.common.swing.text.TextRendering;
import uk.blankaspect.common.swing.text.TextUtils;

import uk.blankaspect.common.ui.progress.IProgressView;

//----------------------------------------------------------------------


// TASK PROGRESS DIALOG BOX CLASS


class TaskProgressDialog
	extends JDialog
	implements ActionListener, IProgressView
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	INFO_FIELD_WIDTH	= 480;

	private static final	int	PROGRESS_BAR_WIDTH		= INFO_FIELD_WIDTH;
	private static final	int	PROGRESS_BAR_HEIGHT		= 15;
	private static final	int	PROGRESS_BAR_MAX_VALUE	= 10000;

	private static final	String	TIME_ELAPSED_STR	= "Time elapsed";
	private static final	String	TIME_REMAINING_STR	= "Estimated time remaining";

	// Commands
	private interface Command
	{
		String	CLOSE	= "close";
	}

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// INFORMATION FIELD CLASS


	private static class InfoField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private InfoField()
		{
			AppFont.MAIN.apply(this);
			setPreferredSize(new Dimension(INFO_FIELD_WIDTH,
										   getFontMetrics(getFont()).getHeight()));
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Draw background
			gr.setColor(getBackground());
			gr.fillRect(0, 0, getWidth(), getHeight());

			// Draw text
			if (text != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Draw text
				gr.setColor(Color.BLACK);
				gr.drawString(text, 0, gr.getFontMetrics().getAscent());
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void setText(String text)
		{
			if (!StringUtils.equal(text, this.text))
			{
				this.text = text;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	}

	//==================================================================


	// TIME FIELD CLASS


	private static class TimeField
		extends JComponent
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	MIN_TIME	= 0;
		private static final	int	MAX_TIME	= 100 * 60 * 60 * 1000 - 1;

		private static final	String	SEPARATOR_STR		= ":";
		private static final	String	PROTOTYPE_STR		= "00" + SEPARATOR_STR + "00" +
																					SEPARATOR_STR + "00";
		private static final	String	OUT_OF_RANGE_STR	= "--";

		private static final	Color	TEXT_COLOUR	= new Color(0, 0, 144);

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private TimeField()
		{
			AppFont.MAIN.apply(this);
			FontMetrics fontMetrics = getFontMetrics(getFont());
			setPreferredSize(new Dimension(fontMetrics.stringWidth(PROTOTYPE_STR),
										   fontMetrics.getAscent() + fontMetrics.getDescent()));
			setOpaque(true);
			setFocusable(false);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected void paintComponent(Graphics gr)
		{
			// Create copy of graphics context
			gr = gr.create();

			// Draw background
			gr.setColor(getBackground());
			gr.fillRect(0, 0, getWidth(), getHeight());

			// Draw text
			if (text != null)
			{
				// Set rendering hints for text antialiasing and fractional metrics
				TextRendering.setHints((Graphics2D)gr);

				// Draw text
				FontMetrics fontMetrics = gr.getFontMetrics();
				gr.setColor(TEXT_COLOUR);
				gr.drawString(text, getWidth() - fontMetrics.stringWidth(text),
							  fontMetrics.getAscent());
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void setTime(int milliseconds)
		{
			String str = OUT_OF_RANGE_STR;
			if ((milliseconds >= MIN_TIME) && (milliseconds <= MAX_TIME))
			{
				int seconds = milliseconds / 1000;
				int minutes = seconds / 60;
				int hours = minutes / 60;
				str = ((hours == 0) ? Integer.toString(minutes)
									: Integer.toString(hours) + SEPARATOR_STR +
												NumberUtils.uIntToDecString(minutes % 60, 2, '0')) +
									SEPARATOR_STR + NumberUtils.uIntToDecString(seconds % 60, 2, '0');
			}
			setText(str);
		}

		//--------------------------------------------------------------

		public void setText(String text)
		{
			if (!StringUtils.equal(text, this.text))
			{
				this.text = text;
				repaint();
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// SET INFO CLASS


	private class DoSetInfo
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private DoSetInfo(String str,
						  File   file)
		{
			this.str = str;
			this.file = file;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			if (file == null)
				infoField.setText(str);
			else
			{
				FontMetrics fontMetrics = infoField.getFontMetrics(infoField.getFont());
				int maxWidth = infoField.getWidth() -
												((str == null) ? 0 : fontMetrics.stringWidth(str + " "));
				String pathname = TextUtils.getLimitedWidthPathname(Utils.getPathname(file),
																	fontMetrics, maxWidth,
																	Utils.getFileSeparatorChar());
				infoField.setText((str == null) ? pathname : str + " " + pathname);
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	str;
		private	File	file;

	}

	//==================================================================


	// SET PROGRESS CLASS


	private class DoSetProgress
		implements Runnable
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int	UPDATE_INTERVAL	= 500;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private DoSetProgress(double value)
		{
			this.value = value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Runnable interface
	////////////////////////////////////////////////////////////////////

		public void run()
		{
			if (value < 0.0)
			{
				fileProgressBar.setIndeterminate(true);

				timeElapsedField.setText(null);
				timeRemainingField.setText(null);
			}
			else
			{
				if (fileProgressBar.isIndeterminate())
					fileProgressBar.setIndeterminate(false);
				fileProgressBar.setValue((int)Math.round(value * (double)PROGRESS_BAR_MAX_VALUE));

				boolean reset = (value == 0.0);
				if (overallProgressBar != null)
				{
					reset = reset && (fileLengthOffset == 0);
					value = (value * (double)fileLength + (double)fileLengthOffset) * fileLengthFactor;
					overallProgressBar.
									setValue((int)Math.round(value * (double)PROGRESS_BAR_MAX_VALUE));
				}

				if (reset)
				{
					startTime = System.currentTimeMillis();
					timeElapsedField.setTime(0);
					timeRemainingField.setText(null);
				}
				else
				{
					long currentTime = System.currentTimeMillis();
					if (currentTime >= updateTime)
					{
						long timeElapsed = currentTime - startTime;
						timeElapsedField.setTime((int)timeElapsed);
						timeRemainingField.setTime((int)Math.round((1.0 / value - 1.0) *
																			(double)timeElapsed) + 500);
						updateTime = currentTime + UPDATE_INTERVAL;
					}
				}
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	double	value;

	}

	//==================================================================


	// WINDOW EVENT HANDLER CLASS


	private class WindowEventHandler
		extends WindowAdapter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private WindowEventHandler()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void windowOpened(WindowEvent event)
		{
			Task.setProgressView((TaskProgressDialog)event.getWindow());
			Task.setException(null, true);
			Task.setCancelled(false);
			task.start();
		}

		//--------------------------------------------------------------

		@Override
		public void windowClosing(WindowEvent event)
		{
			location = getLocation();
			if (stopped)
				dispose();
			else
				Task.setCancelled(true);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private TaskProgressDialog(Window  owner,
							   String  titleStr,
							   Task    task,
							   boolean showOverallProgress)
		throws AppException
	{

		// Call superclass constructor
		super(owner, titleStr, Dialog.ModalityType.APPLICATION_MODAL);

		// Set icons
		if (owner != null)
			setIconImages(owner.getIconImages());

		// Initialise instance variables
		this.task = task;


		//----  Info field

		infoField = new InfoField();


		//----  Progress bars

		fileProgressBar = new JProgressBar(0, PROGRESS_BAR_MAX_VALUE);
		fileProgressBar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));

		if (showOverallProgress)
		{
			overallProgressBar = new JProgressBar(0, PROGRESS_BAR_MAX_VALUE);
			overallProgressBar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));
		}


		//----  Time panel

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel timePanel = new JPanel(gridBag);

		int gridY = 0;

		// Label: time elapsed
		JLabel timeElapsedLabel = new FLabel(TIME_ELAPSED_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(timeElapsedLabel, gbc);
		timePanel.add(timeElapsedLabel);

		// Field: time elapsed
		timeElapsedField = new TimeField();

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 4, 0, 0);
		gridBag.setConstraints(timeElapsedField, gbc);
		timePanel.add(timeElapsedField);

		// Label: time remaining
		JLabel timeRemainingLabel = new FLabel(TIME_REMAINING_STR);

		gbc.gridx = 0;
		gbc.gridy = gridY;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(1, 0, 0, 0);
		gridBag.setConstraints(timeRemainingLabel, gbc);
		timePanel.add(timeRemainingLabel);

		// Field: time remaining
		timeRemainingField = new TimeField();

		gbc.gridx = 1;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(1, 4, 0, 0);
		gridBag.setConstraints(timeRemainingField, gbc);
		timePanel.add(timeRemainingField);


		//----  Button panel

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 0, 0));

		// Button: cancel
		cancelButton = new FButton(AppConstants.CANCEL_STR);
		cancelButton.setActionCommand(Command.CLOSE);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		//----  Bottom panel

		JPanel bottomPanel = new JPanel(gridBag);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gridBag.setConstraints(timePanel, gbc);
		bottomPanel.add(timePanel);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 24, 0, 0);
		gridBag.setConstraints(buttonPanel, gbc);
		bottomPanel.add(buttonPanel);


		//----  Main panel

		JPanel mainPanel = new JPanel(gridBag);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

		gridY = 0;

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(8, 2, 8, 2);
		gridBag.setConstraints(infoField, gbc);
		mainPanel.add(infoField);

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 0, 0, 0);
		gridBag.setConstraints(fileProgressBar, gbc);
		mainPanel.add(fileProgressBar);

		if (showOverallProgress)
		{
			gbc.gridx = 0;
			gbc.gridy = gridY++;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(6, 0, 0, 0);
			gridBag.setConstraints(overallProgressBar, gbc);
			mainPanel.add(overallProgressBar);
		}

		gbc.gridx = 0;
		gbc.gridy = gridY++;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 0, 0, 0);
		gridBag.setConstraints(bottomPanel, gbc);
		mainPanel.add(bottomPanel);

		// Add commands to action map
		KeyAction.create(mainPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						 KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Command.CLOSE, this);


		//----  Window

		// Set content pane
		setContentPane(mainPanel);

		// Dispose of window explicitly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// Handle window events
		addWindowListener(new WindowEventHandler());

		// Prevent dialog from being resized
		setResizable(false);

		// Resize dialog to its preferred size
		pack();

		// Set location of dialog box
		if (location == null)
			location = GuiUtils.getComponentLocation(this, owner);
		setLocation(location);

		// Set default button
		getRootPane().setDefaultButton(cancelButton);

		// Show dialog
		setVisible(true);

		// Throw any exception from task thread
		Task.throwIfException();

	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void showDialog(Component parent,
								  String    titleStr,
								  Task      task)
		throws AppException
	{
		showDialog(parent, titleStr, task, false);
	}

	//------------------------------------------------------------------

	public static void showDialog(Component parent,
								  String    titleStr,
								  Task      task,
								  boolean   showOverallProgress)
		throws AppException
	{
		new TaskProgressDialog(GuiUtils.getWindow(parent), titleStr, task, showOverallProgress);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : ActionListener interface
////////////////////////////////////////////////////////////////////////

	public void actionPerformed(ActionEvent event)
	{
		if (event.getActionCommand().equals(Command.CLOSE))
			onClose();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IProgressView interface
////////////////////////////////////////////////////////////////////////

	public void setInfo(String str)
	{
		setInfo(str, null);
	}

	//------------------------------------------------------------------

	public void setInfo(String str,
						File   file)
	{
		SwingUtilities.invokeLater(new DoSetInfo(str, file));
	}

	//------------------------------------------------------------------

	public void setProgress(int    index,
							double value)
	{
		SwingUtilities.invokeLater(new DoSetProgress(value));
	}

	//------------------------------------------------------------------

	public void waitForIdle()
	{
		EventQueue eventQueue = getToolkit().getSystemEventQueue();
		while (eventQueue.peekEvent() != null)
		{
			// do nothing
		}
	}

	//------------------------------------------------------------------

	public void close()
	{
		stopped = true;
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void setFileLength(long fileLength,
							  long fileLengthOffset)
	{
		// Set instance variables
		this.fileLength = fileLength;
		this.fileLengthOffset = fileLengthOffset;

		// Update progress bars
		fileProgressBar.setValue(0);
		if (overallProgressBar != null)
			overallProgressBar.setValue((int)Math.round((double)fileLengthOffset * fileLengthFactor *
																		(double)PROGRESS_BAR_MAX_VALUE));
	}

	//------------------------------------------------------------------

	public void setTotalFileLength(long totalFileLength)
	{
		fileLengthFactor = (totalFileLength == 0.0) ? 0.0 : 1.0 / (double)totalFileLength;
	}

	//------------------------------------------------------------------

	private void onClose()
	{
		cancelButton.setEnabled(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class variables
////////////////////////////////////////////////////////////////////////

	private static	Point	location;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	long			fileLength;
	private	long			fileLengthOffset;
	private	double			fileLengthFactor;
	private	Task			task;
	private	boolean			stopped;
	private	long			startTime;
	private	long			updateTime;
	private	InfoField		infoField;
	private	JProgressBar	fileProgressBar;
	private	JProgressBar	overallProgressBar;
	private	TimeField		timeElapsedField;
	private	TimeField		timeRemainingField;
	private	JButton			cancelButton;

}

//----------------------------------------------------------------------
