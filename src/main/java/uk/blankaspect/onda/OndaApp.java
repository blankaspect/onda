/*====================================================================*\

OndaApp.java

Class: Onda application.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import uk.blankaspect.common.build.BuildUtils;

import uk.blankaspect.common.cls.ClassUtils;

import uk.blankaspect.common.commandline.CommandLine;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.exception2.LocationException;
import uk.blankaspect.common.exception2.UnexpectedRuntimeException;

import uk.blankaspect.common.filesystem.PathnameUtils;

import uk.blankaspect.common.iff.ChunkFilter;

import uk.blankaspect.common.logging.ErrorLogger;

import uk.blankaspect.common.misc.DirectoryFilter;
import uk.blankaspect.common.misc.FilenameFilter;
import uk.blankaspect.common.misc.TextFile;

import uk.blankaspect.common.resource.ResourceProperties;
import uk.blankaspect.common.resource.ResourceUtils;

import uk.blankaspect.common.stdin.InputUtils;

import uk.blankaspect.common.string.StringUtils;

import uk.blankaspect.ui.swing.text.TextRendering;

import uk.blankaspect.ui.swing.textfield.TextFieldUtils;

//----------------------------------------------------------------------


// CLASS: ONDA APPLICATION


public class OndaApp
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The single instance of the application. */
	public static final		OndaApp	INSTANCE	= new OndaApp();

	/** The short name of the application. */
	public static final		String	SHORT_NAME	= "Onda";

	/** The long name of the application. */
	public static final		String	LONG_NAME	= "Onda lossless audio compressor";

	/** The name of the application when used as a key. */
	public static final		String	NAME_KEY	= StringUtils.firstCharToLowerCase(SHORT_NAME);

	/** The name of the build-properties file. */
	private static final	String	BUILD_PROPERTIES_FILENAME	= "build.properties";

	/** The name of the usage-message file. */
	private static final	String	USAGE_MESSAGE_FILENAME	= "usageMessage.txt";

	/** The placeholder for the name of the application in the usage message. */
	private static final	String	APP_NAME_PLACEHOLDER	= "${appName}";

	/** The string that can be prefixed to an input pathname to disable other special characters in a command line. */
	private static final	String	PATHNAME_PREFIX	= "+";

	/** The prefix of the pathname of a file that contains a list of input pathnames and, optionally, corresponding
		output directories. */
	private static final	String	LIST_PREFIX	= "@";

	/** The prefix of a cause in the string representation of an exception. */
	private static final	String	EXCEPTION_CAUSE_PREFIX	= "- ";

	/** The separator after a message that is written to the standard output stream. */
	private static final	String	MESSAGE_SEPARATOR	= "-".repeat(36);

	/** Miscellaneous strings. */
	private static final	String	CONFIG_ERROR_STR			= "Configuration error";
	private static final	String	LAF_ERROR1_STR				= "Look-and-feel: ";
	private static final	String	LAF_ERROR2_STR				= "\nThe look-and-feel is not installed.";
	private static final	String	VALIDATE_STR				= "Validate";
	private static final	String	COMPRESS_FILE_STR			= "Compress file";
	private static final	String	EXPAND_FILE_STR				= "Expand file";
	private static final	String	COMPRESSING_STR				= "Compressing ";
	private static final	String	EXPANDING_STR				= "Expanding ";
	private static final	String	VALIDATING_STR				= "Validating ";
	private static final	String	ARROW_STR					= " --> ";
	private static final	String	SKIP_STR					= "Skip";
	private static final	String	CANCELLED_STR				= "The command was cancelled by the user.";
	private static final	String	NOT_REPLACED_STR			= "The existing file was not replaced.";
	private static final	String	NUM_FILES_FOUND_STR			= "Number of files found : ";
	private static final	String	NUM_FILES_VALIDATED_STR		= "Number of files validated : ";
	private static final	String	NUM_FAILED_VALIDATION_STR	= "Number of files that failed validation : ";
	private static final	String	ALL_FILES_VALID_STR			= "All files were valid.";
	private static final	String	CQ_OPTION_STR				= "Continue | Quit (C/Q) ? ";
	private static final	String	RSQ_OPTION_STR				= "Replace | Skip | Quit (R/S/Q) ? ";

	/** Values that may be returned by the application when it terminates. */
	private interface ExitCode
	{
		int	ERROR				= 1;
		int	TERMINATED_BY_USER	= 2;
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	ResourceProperties	buildProperties;
	private	String				versionStr;
	private	MainWindow			mainWindow;
	private	boolean				hasGui;
	private	boolean				titleShown;
	private	boolean				overwrite;
	private	Set<InfoKind>		infoKinds;
	private	long				fileLengthOffset;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private OndaApp()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void main(
		String[]	args)
	{
		INSTANCE.init(args);
	}

	//------------------------------------------------------------------

	public static Charset getCharEncoding()
	{
		String encodingName = AppConfig.INSTANCE.getCharacterEncoding();
		return (encodingName == null) ? Charset.defaultCharset() : Charset.forName(encodingName);
	}

	//------------------------------------------------------------------

	public static List<InputOutput> readListFile(
		File	file)
		throws AppException
	{
		// Test for file
		if (!file.isFile())
			throw new FileException(ErrorId.NOT_A_FILE, file);

		// Parse file
		List<InputOutput> inputsOutputs = new ArrayList<>();
		for (String line : TextFile.readLines(file, getCharEncoding()))
		{
			if (!line.isEmpty())
			{
				List<String> strs = StringUtils.split(line, '\t');
				if (!strs.isEmpty())
				{
					File inFile = new File(PathnameUtils.parsePathname(strs.get(0)));
					try
					{
						if (!inFile.isFile() && !inFile.isDirectory())
							throw new FileException(ErrorId.LIST_FILE_OR_DIRECTORY_DOES_NOT_EXIST, inFile);
					}
					catch (SecurityException e)
					{
						throw new FileException(ErrorId.FILE_OR_DIRECTORY_ACCESS_NOT_PERMITTED, inFile);
					}
					File outDirectory = null;
					if (strs.size() > 1)
					{
						outDirectory = new File(PathnameUtils.parsePathname(strs.get(1)));
						try
						{
							if (outDirectory.isFile())
								throw new FileException(ErrorId.LIST_FILE_PATHNAME_IS_A_FILE, outDirectory);
						}
						catch (SecurityException e)
						{
							throw new FileException(ErrorId.FILE_OR_DIRECTORY_ACCESS_NOT_PERMITTED, outDirectory);
						}
					}
					inputsOutputs.add(new InputOutput(inFile, outDirectory));
				}
			}
		}

		// Return list of input-output pairs
		return inputsOutputs;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public MainWindow getMainWindow()
	{
		return mainWindow;
	}

	//------------------------------------------------------------------

	public void showWarningMessage(
		String	title,
		Object	message)
	{
		if (hasGui)
			showMessageDialog(title, message, JOptionPane.WARNING_MESSAGE);
		else
			printMessage(title, message);
	}

	//------------------------------------------------------------------

	public void showErrorMessage(
		String	title,
		Object	message)
	{
		if (hasGui)
			showMessageDialog(title, message, JOptionPane.ERROR_MESSAGE);
		else
			printMessage(title, message);
	}

	//------------------------------------------------------------------

	public void showMessageDialog(
		String	title,
		Object	message,
		int		messageKind)
	{
		JOptionPane.showMessageDialog(mainWindow, message, title, messageKind);
	}

	//------------------------------------------------------------------

	public void printMessage(
		String	title,
		Object	message)
	{
		System.err.println(title);
		System.err.println(message);
		System.err.println(MESSAGE_SEPARATOR);
	}

	//------------------------------------------------------------------

	public void compress(
		List<InputOutput>	inputsOutputs,
		ChunkFilter[]		chunkFilters,
		boolean				recursive)
		throws TaskCancelledException
	{
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
						setTotalFileLength(getTotalFileLength(inputsOutputs, getAudioFileFilter(), recursive));
			}

			for (InputOutput inputOutput : inputsOutputs)
			{
				if (inputOutput.input.isDirectory())
				{
					inputOutput.updateRootDirectory();
					compressDirectory(inputOutput, chunkFilters, recursive);
				}
				else
				{
					try
					{
						compressFile(inputOutput, chunkFilters);
					}
					catch (TaskCancelledException e)
					{
						throw e;
					}
					catch (AppException e)
					{
						confirmContinue(e);
					}
				}
			}
		}
		catch (TaskCancelledException e)
		{
			Log.INSTANCE.appendLine(CANCELLED_STR);
			if (hasGui)
				throw e;
		}
	}

	//------------------------------------------------------------------

	public void expand(
		List<InputOutput>	inputsOutputs,
		boolean				recursive)
		throws TaskCancelledException
	{
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
						setTotalFileLength(getTotalFileLength(inputsOutputs, getCompressedFileFilter(), recursive));
			}

			for (InputOutput inputOutput : inputsOutputs)
			{
				if (inputOutput.input.isDirectory())
				{
					inputOutput.updateRootDirectory();
					expandDirectory(inputOutput, recursive);
				}
				else
				{
					try
					{
						expandFile(inputOutput);
					}
					catch (TaskCancelledException e)
					{
						throw e;
					}
					catch (AppException e)
					{
						confirmContinue(e);
					}
				}
			}
		}
		catch (TaskCancelledException e)
		{
			Log.INSTANCE.appendLine(CANCELLED_STR);
			if (hasGui)
				throw e;
		}
	}

	//------------------------------------------------------------------

	public void validate(
		List<InputOutput>	inputsOutputs,
		boolean				recursive)
	{
		// Validate input files
		FileProcessor.ValidationResult result = new FileProcessor.ValidationResult();
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
						setTotalFileLength(getTotalFileLength(inputsOutputs, getCompressedFileFilter(), recursive));
			}

			for (InputOutput inputOutput : inputsOutputs)
			{
				if (inputOutput.input.isDirectory())
					validateDirectory(inputOutput.input, recursive, result);
				else
				{
					try
					{
						validateFile(inputOutput.input, result);
					}
					catch (TaskCancelledException e)
					{
						throw e;
					}
					catch (AppException e)
					{
						Log.INSTANCE.appendException(e);
					}
				}
			}
		}
		catch (TaskCancelledException e)
		{
			Log.INSTANCE.appendLine(CANCELLED_STR);
		}

		// Display results
		StringBuilder buffer = new StringBuilder(256);
		buffer.append(NUM_FILES_FOUND_STR);
		buffer.append(result.foundCount);
		int messageKind = 0;
		int numFailed = result.validatedCount - result.validCount;
		if ((result.validatedCount == result.foundCount) && (numFailed == 0))
		{
			messageKind = JOptionPane.INFORMATION_MESSAGE;
			if (result.foundCount > 0)
			{
				buffer.append('\n');
				buffer.append(ALL_FILES_VALID_STR);
			}
		}
		else
		{
			messageKind = JOptionPane.WARNING_MESSAGE;
			if (result.validatedCount < result.foundCount)
			{
				buffer.append('\n');
				buffer.append(NUM_FILES_VALIDATED_STR);
				buffer.append(result.validatedCount);
			}
			if (numFailed > 0)
			{
				buffer.append('\n');
				buffer.append(NUM_FAILED_VALIDATION_STR);
				buffer.append(numFailed);
			}
		}
		if (hasGui)
			showMessageDialog(VALIDATE_STR, buffer, messageKind);
		else if (infoKinds.contains(InfoKind.RESULT))
			System.out.println(buffer);
	}

	//------------------------------------------------------------------

	private void init(
		String[]	args)
	{
		// Log stack trace of uncaught exception
		if (ClassUtils.isFromJar(getClass()))
		{
			Thread.setDefaultUncaughtExceptionHandler((thread, exception) ->
			{
				try
				{
					ErrorLogger.INSTANCE.write(exception);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			});
		}

		// Read build properties and initialise version string
		try
		{
			buildProperties =
					new ResourceProperties(ResourceUtils.normalisedPathname(getClass(), BUILD_PROPERTIES_FILENAME));
			versionStr = BuildUtils.versionString(getClass(), buildProperties);
		}
		catch (LocationException e)
		{
			e.printStackTrace();
		}

		// Initialise instance variables
		hasGui = (args.length == 0);
		infoKinds = EnumSet.noneOf(InfoKind.class);

		// Read configuration
		AppConfig config = AppConfig.INSTANCE;
		config.read();

		// Set UNIX style for pathnames in file exceptions
		ExceptionUtils.setUnixStyle(config.isShowUnixPathnames());

		// Start application
		if (hasGui)
		{
			// Set text antialiasing
			TextRendering.setAntialiasing(config.getTextAntialiasing());

			// Set look-and-feel
			String lookAndFeelName = config.getLookAndFeel();
			for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels())
			{
				if (lookAndFeelInfo.getName().equals(lookAndFeelName))
				{
					try
					{
						UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
					}
					catch (Exception e)
					{
						// ignore
					}
					lookAndFeelName = null;
					break;
				}
			}
			if (lookAndFeelName != null)
			{
				showWarningMessage(SHORT_NAME + " : " + CONFIG_ERROR_STR,
								   LAF_ERROR1_STR + lookAndFeelName + LAF_ERROR2_STR);
			}

			// Select all text when a text field gains focus
			if (config.isSelectTextOnFocusGained())
				TextFieldUtils.selectAllOnFocusGained();

			// Create main window
			SwingUtilities.invokeLater(() -> mainWindow = new MainWindow(LONG_NAME + " " + versionStr));
		}
		else
		{
			// Parse command line
			try
			{
				List<CommandLine.Element<Option>> commandLineElements =
						new CommandLine<>(Option.class, true, usageMessage()).parse(args);
				if (!commandLineElements.isEmpty())
					parseCommandLine(commandLineElements);
			}
			catch (TaskCancelledException e)
			{
				System.exit(ExitCode.TERMINATED_BY_USER);
			}
			catch (AppException e)
			{
				// Write title
				if (!titleShown)
					System.err.println(SHORT_NAME + " " + versionStr);

				// Write string representation of exception
				System.err.println(e);

				// Exit application
				System.exit(ExitCode.ERROR);
			}
			catch (CommandLine.CommandLineException e)
			{
				// Write title
				if (!titleShown)
					System.err.println(SHORT_NAME + " " + versionStr);

				// Write exception
				System.err.println(e);

				// Write causes of exception
				String causeStr = ExceptionUtils.getCompositeCauseString(e.getCause(), EXCEPTION_CAUSE_PREFIX);
				if (!causeStr.isEmpty())
					System.err.println(causeStr);

				// Exit application
				System.exit(ExitCode.ERROR);
			}
		}
	}

	//------------------------------------------------------------------

	private void parseCommandLine(
		List<CommandLine.Element<Option>>	elements)
		throws AppException
	{
		// Initialise local variables
		Set<Option> subcommands = EnumSet.noneOf(Option.class);
		List<InputOutput> inputsOutputs = new ArrayList<>();
		boolean recursive = false;
		ChunkFilter aiffChunkFilter = null;
		ChunkFilter waveChunkFilter = null;
		File outDirectory = null;

		// Parse command line
		for (CommandLine.Element<Option> element : elements)
		{
			String elementValue = element.getValue();

			if (element.getOption() == null)
			{
				if (element.getValue().startsWith(LIST_PREFIX))
				{
					String pathname = elementValue.substring(LIST_PREFIX.length());
					inputsOutputs.addAll(readListFile(new File(PathnameUtils.parsePathname(pathname))));
				}
				else
				{
					String pathname = elementValue.startsWith(PATHNAME_PREFIX)
													? elementValue.substring(PATHNAME_PREFIX.length())
													: elementValue;
					inputsOutputs.add(new InputOutput(new File(PathnameUtils.parsePathname(pathname)), outDirectory));
				}
				continue;
			}

			Option option = element.getOption().getKey();
			switch (option)
			{
				case COMPRESS, EXPAND, HELP, VALIDATE, VERSION:
					subcommands.add(option);
					break;

				case AIFF_CHUNK_FILTER:
					try
					{
						ChunkFilter aiffChunkFilter0 = new ChunkFilter(elementValue);
						if ((aiffChunkFilter != null) && !aiffChunkFilter.equals(aiffChunkFilter0))
							throw new OptionException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element);
						aiffChunkFilter = aiffChunkFilter0;
					}
					catch (IllegalArgumentException e)
					{
						throw new ArgumentException(ErrorId.INVALID_AIFF_CHUNK_FILTER, element);
					}
					break;

				case OUTPUT_DIRECTORY:
				{
					if (elementValue.isEmpty())
						throw new ArgumentException(ErrorId.INVALID_OUTPUT_DIRECTORY, element);
					File outDirectory0 = new File(PathnameUtils.parsePathname(elementValue));
					if ((outDirectory != null) && !outDirectory.equals(outDirectory0))
						throw new OptionException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element);
					outDirectory = outDirectory0;
					break;
				}

				case OVERWRITE:
					overwrite = true;
					break;

				case RECURSIVE:
					recursive = true;
					break;

				case SHOW_INFO:
						for (String key : StringUtils.split(elementValue, InfoKind.SEPARATOR_CHAR))
						{
							if (InfoKind.ALL_KEY.equals(key))
							{
								Arrays.stream(InfoKind.values())
										.filter(value -> value != InfoKind.NONE)
										.forEach(infoKinds::add);
							}
							else
							{
								InfoKind infoKind = InfoKind.forKey(key);
								if (infoKind == null)
									throw new ArgumentException(ErrorId.INVALID_INFO_KIND, element, key);
								infoKinds.add(infoKind);
							}
						}
						if (infoKinds.contains(InfoKind.NONE) && (infoKinds.size() > 1))
							throw new ArgumentException(ErrorId.INCONSISTENT_INFO_KINDS, element);
					break;

				case WAVE_CHUNK_FILTER:
					try
					{
						ChunkFilter waveChunkFilter0 = new ChunkFilter(elementValue);
						if ((waveChunkFilter != null) && !waveChunkFilter.equals(waveChunkFilter0))
							throw new OptionException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element);
						waveChunkFilter = waveChunkFilter0;
					}
					catch (IllegalArgumentException e)
					{
						throw new ArgumentException(ErrorId.INVALID_WAVE_CHUNK_FILTER, element);
					}
					break;
			}
		}

		// Test for a single subcommand
		if (subcommands.isEmpty())
			throw new AppException(ErrorId.NO_SUBCOMMAND);
		if (subcommands.size() > 1)
		{
			String names = subcommands.stream().map(Option::getName).collect(Collectors.joining(", "));
			throw new AppException(ErrorId.MULTIPLE_SUBCOMMANDS, names);
		}

		// Set default values for missing options
		if (infoKinds.isEmpty())
			infoKinds.addAll(InfoKind.DEFAULT_VALUES);
		if (aiffChunkFilter == null)
			aiffChunkFilter = ChunkFilter.INCLUDE_ALL;
		if (waveChunkFilter == null)
			waveChunkFilter = ChunkFilter.INCLUDE_ALL;

		// Perform subcommand
		Log.INSTANCE.setShow(infoKinds.contains(InfoKind.LOG));
		Option subcommand = subcommands.iterator().next();
		switch (subcommand)
		{
			case COMPRESS:
				if (inputsOutputs.isEmpty())
					throw new AppException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Compress(inputsOutputs, new ChunkFilter[] { aiffChunkFilter, waveChunkFilter },
										 recursive));
				break;

			case EXPAND:
				if (inputsOutputs.isEmpty())
					throw new AppException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Expand(inputsOutputs, recursive));
				break;

			case HELP:
				showTitle();
				System.out.println();
				System.out.print(usageMessage());
				break;

			case VALIDATE:
				if (inputsOutputs.isEmpty())
					throw new AppException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Validate(inputsOutputs, recursive));
				break;

			case VERSION:
				showTitle();
				break;

			default:
				throw new UnexpectedRuntimeException(subcommand.name);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Returns the usage message of this application.
	 *
	 * @return the usage message of this application.
	 */

	private String usageMessage()
	{
		try
		{
			return ResourceUtils.readText(getClass(), USAGE_MESSAGE_FILENAME).replace(APP_NAME_PLACEHOLDER, NAME_KEY);
		}
		catch (IOException e)
		{
			throw new UnexpectedRuntimeException(e);
		}
	}

	//------------------------------------------------------------------

	/**
	 * Writes the title of this application to the standard output stream if it has not already been written.
	 */

	private void showTitle()
	{
		if (!titleShown)
		{
			System.out.println(SHORT_NAME + " " + versionStr);
			titleShown = true;
		}
	}

	//------------------------------------------------------------------

	private void confirmContinue(
		AppException	exception)
		throws TaskCancelledException
	{
		// Clear exception from task
		Task.setException(null, true);

		// Append exception to log
		Log.INSTANCE.appendException(exception);

		// Display error message and ask user whether to continue or to cancel
		if (hasGui)
		{
			String[] optionStrs = Utils.getOptionStrings(AppConstants.CONTINUE_STR);
			if (JOptionPane.showOptionDialog(mainWindow, exception, SHORT_NAME, JOptionPane.OK_CANCEL_OPTION,
											 JOptionPane.ERROR_MESSAGE, null, optionStrs,
											 optionStrs[1]) != JOptionPane.OK_OPTION)
				throw new TaskCancelledException();
		}
		else
		{
			if (InputUtils.readResponse(CQ_OPTION_STR, 'C', 'Q') == 'Q')
				throw new TaskCancelledException();
		}
	}

	//------------------------------------------------------------------

	private boolean confirmReplace(
		String	title,
		File	file)
		throws TaskCancelledException
	{
		String messageStr = Utils.getPathname(file) + AppConstants.ALREADY_EXISTS_STR;
		if (hasGui)
		{
			String[] optionStrs = Utils.getOptionStrings(AppConstants.REPLACE_STR, SKIP_STR);
			int result = JOptionPane.showOptionDialog(mainWindow, messageStr, title,
													  JOptionPane.YES_NO_CANCEL_OPTION,
													  JOptionPane.WARNING_MESSAGE, null, optionStrs, optionStrs[1]);
			if (result == JOptionPane.YES_OPTION)
				return true;
			if (result == JOptionPane.NO_OPTION)
			{
				Log.INSTANCE.appendLine(NOT_REPLACED_STR);
				return false;
			}
			throw new TaskCancelledException();
		}
		else
		{
			if (overwrite)
				return true;

			System.out.println(messageStr);
			switch (InputUtils.readResponse(RSQ_OPTION_STR, 'R', 'S', 'Q'))
			{
				case 'R':
					return true;

				case 'S':
					return false;

				default:
					throw new TaskCancelledException();
			}
		}
	}

	//------------------------------------------------------------------

	private void doTask(
		Task	task)
		throws AppException
	{
		Task.setException(null, true);
		Task.setCancelled(false);
		task.start();
		while (Task.getNumThreads() > 0)
		{
			try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				// ignore
			}
		}
		Task.throwIfException();
	}

	//------------------------------------------------------------------

	private void compressDirectory(
		InputOutput		inputOutput,
		ChunkFilter[]	chunkFilters,
		boolean			recursive)
		throws TaskCancelledException
	{
		// Process files
		File directory = inputOutput.input;
		try
		{
			File[] files = directory.listFiles(getAudioFileFilter());
			if (files == null)
				throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
			Arrays.sort(files);
			for (File file : files)
			{
				try
				{
					compressFile(new InputOutput(file, inputOutput), chunkFilters);
				}
				catch (TaskCancelledException e)
				{
					throw e;
				}
				catch (AppException e)
				{
					confirmContinue(e);
				}
			}
		}
		catch (TaskCancelledException e)
		{
			throw e;
		}
		catch (AppException e)
		{
			confirmContinue(e);
		}

		// Process subdirectories
		if (recursive)
		{
			try
			{
				File[] files = directory.listFiles(DirectoryFilter.INSTANCE);
				if (files == null)
					throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
				Arrays.sort(files);
				for (File file : files)
					compressDirectory(new InputOutput(file, inputOutput), chunkFilters, true);
			}
			catch (TaskCancelledException e)
			{
				throw e;
			}
			catch (AppException e)
			{
				confirmContinue(e);
			}
		}
	}

	//------------------------------------------------------------------

	private void compressFile(
		InputOutput		inputOutput,
		ChunkFilter[]	chunkFilters)
		throws AppException
	{
		// Test for input file
		File inFile = inputOutput.input;
		if (!inFile.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);

		// Generate name of output file
		File outDirectory = inputOutput.getOutputDirectory();
		File outFile = new File(outDirectory, inFile.getName() + AppConstants.COMPRESSED_FILENAME_EXTENSION);

		// Write name of task to log
		Log.INSTANCE.appendLine(COMPRESSING_STR + Utils.getPathname(inFile) + ARROW_STR + Utils.getPathname(outFile));

		// Create output directory
		if ((outDirectory != null) && !outDirectory.exists() && !outDirectory.mkdirs())
			throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, outDirectory);

		// Compress file
		if (!outFile.exists() || confirmReplace(COMPRESS_FILE_STR, outFile))
		{
			if (hasGui)
			{
				long fileLength = inFile.length();
				((TaskProgressDialog)Task.getProgressView()).setFileLength(fileLength, fileLengthOffset);
				fileLengthOffset += fileLength;
			}
			new FileProcessor().compress(inFile, outFile, chunkFilters);
		}
	}

	//------------------------------------------------------------------

	private void expandDirectory(
		InputOutput	inputOutput,
		boolean		recursive)
		throws TaskCancelledException
	{
		// Process files
		File directory = inputOutput.input;
		try
		{
			File[] files = directory.listFiles(getCompressedFileFilter());
			if (files == null)
				throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
			Arrays.sort(files);
			for (File file : files)
			{
				try
				{
					expandFile(new InputOutput(file, inputOutput));
				}
				catch (TaskCancelledException e)
				{
					throw e;
				}
				catch (AppException e)
				{
					confirmContinue(e);
				}
			}
		}
		catch (TaskCancelledException e)
		{
			throw e;
		}
		catch (AppException e)
		{
			confirmContinue(e);
		}

		// Process subdirectories
		if (recursive)
		{
			try
			{
				File[] files = directory.listFiles(DirectoryFilter.INSTANCE);
				if (files == null)
					throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
				Arrays.sort(files);
				for (File file : files)
					expandDirectory(new InputOutput(file, inputOutput), true);
			}
			catch (TaskCancelledException e)
			{
				throw e;
			}
			catch (AppException e)
			{
				confirmContinue(e);
			}
		}
	}

	//------------------------------------------------------------------

	private void expandFile(
		InputOutput	inputOutput)
		throws AppException
	{
		// Test for input file
		File inFile = inputOutput.input;
		if (!inFile.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);

		// Generate name of output file
		File outDirectory = inputOutput.getOutputDirectory();
		File outFile = null;
		String filename = inFile.getName();
		if (filename.endsWith(AppConstants.COMPRESSED_FILENAME_EXTENSION))
		{
			filename = StringUtils.removeSuffix(filename, AppConstants.COMPRESSED_FILENAME_EXTENSION);
			outFile = new File(outDirectory, filename);
		}
		else
		{
			String[] filenameParts = StringUtils.splitAtFirst(filename, '.', StringUtils.SplitMode.SUFFIX);
			filename = filenameParts[0];
			int index = 1;
			while (true)
			{
				outFile = new File(outDirectory, filename + "-" + index + filenameParts[1]);
				if (!outFile.exists())
					break;
				++index;
			}
		}

		// Determine kind of output file
		AudioFileKind audioFileKind = AudioFileKind.forFilename(filename);
		if (audioFileKind == null)
		{
			// Read input file to get source file kind from private data
			PrivateData privateData = OndaFileIff.getFileKind(inFile).createReader(inFile).readPrivateData();
			if (privateData != null)
				audioFileKind = privateData.getSourceKind();
			if (audioFileKind == null)
			{
				// Ask user for kind of output file
				audioFileKind = hasGui ? AudioFileKindDialog.showDialog(mainWindow) : AudioFileKindDialog.showPrompt();
				if (audioFileKind == null)
					throw new TaskCancelledException();
			}
		}

		// Write name of task to log
		Log.INSTANCE.appendLine(EXPANDING_STR + Utils.getPathname(inFile) + ARROW_STR + Utils.getPathname(outFile));

		// Create output directory
		if ((outDirectory != null) && !outDirectory.exists() && !outDirectory.mkdirs())
			throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, outDirectory);

		// Expand file
		if (!outFile.exists() || confirmReplace(EXPAND_FILE_STR, outFile))
		{
			if (hasGui)
			{
				long fileLength = inFile.length();
				((TaskProgressDialog)Task.getProgressView()).setFileLength(fileLength, fileLengthOffset);
				fileLengthOffset += fileLength;
			}
			new FileProcessor().expand(inFile, outFile, audioFileKind);
		}
	}

	//------------------------------------------------------------------

	private void validateDirectory(
		File							directory,
		boolean							recursive,
		FileProcessor.ValidationResult	validationResult)
		throws TaskCancelledException
	{
		// Process files
		try
		{
			File[] files = directory.listFiles(getCompressedFileFilter());
			if (files == null)
				throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
			Arrays.sort(files);
			for (File file : files)
			{
				try
				{
					validateFile(file, validationResult);
				}
				catch (TaskCancelledException e)
				{
					throw e;
				}
				catch (AppException e)
				{
					Log.INSTANCE.appendException(e);
				}
			}
		}
		catch (TaskCancelledException e)
		{
			throw e;
		}
		catch (AppException e)
		{
			confirmContinue(e);
		}

		// Process subdirectories
		if (recursive)
		{
			try
			{
				File[] files = directory.listFiles(DirectoryFilter.INSTANCE);
				if (files == null)
					throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
				Arrays.sort(files);
				for (File file : files)
					validateDirectory(file, true, validationResult);
			}
			catch (TaskCancelledException e)
			{
				throw e;
			}
			catch (AppException e)
			{
				confirmContinue(e);
			}
		}
	}

	//------------------------------------------------------------------

	private void validateFile(
		File							file,
		FileProcessor.ValidationResult	validationResult)
		throws AppException
	{
		// Write name of task to log
		Log.INSTANCE.appendLine(VALIDATING_STR + Utils.getPathname(file));

		// Validate file
		if (hasGui)
		{
			long fileLength = file.length();
			((TaskProgressDialog)Task.getProgressView()).setFileLength(fileLength, fileLengthOffset);
			fileLengthOffset += fileLength;
		}
		new FileProcessor().validate(file, validationResult);
	}

	//------------------------------------------------------------------

	private long getFileLengths(
		File			directory,
		FilenameFilter	filter,
		boolean			recursive)
		throws AppException
	{
		// Process files
		File[] files = directory.listFiles(filter);
		if (files == null)
			throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
		long length = 0;
		for (File file : files)
			length += file.length();

		// Process subdirectories
		if (recursive)
		{
			files = directory.listFiles(DirectoryFilter.INSTANCE);
			if (files == null)
				throw new FileException(ErrorId.FAILED_TO_LIST_DIRECTORY_ENTRIES, directory);
			for (File file : files)
				length += getFileLengths(file, filter, true);
		}

		return length;
	}

	//------------------------------------------------------------------

	private long getTotalFileLength(
		List<InputOutput>	inputsOutputs,
		FilenameFilter		filter,
		boolean				recursive)
		throws TaskCancelledException
	{
		long length = 0;
		try
		{
			for (InputOutput inputOutput : inputsOutputs)
			{
				if (inputOutput.input.isDirectory())
					length += getFileLengths(inputOutput.input, filter, recursive);
				else if (filter.accept(inputOutput.input))
					length += inputOutput.input.length();
			}
		}
		catch (AppException e)
		{
			confirmContinue(e);
		}
		return length;
	}

	//------------------------------------------------------------------

	private FilenameFilter getAudioFileFilter()
	{
		String[] patterns = new String[AppConstants.AUDIO_FILENAME_EXTENSIONS.length];
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = "*" + AppConstants.AUDIO_FILENAME_EXTENSIONS[i];
		return new FilenameFilter.MultipleFilter(AppConfig.INSTANCE.isIgnoreFilenameCase(), patterns);
	}

	//------------------------------------------------------------------

	private FilenameFilter getCompressedFileFilter()
	{
		return new FilenameFilter("*" + AppConstants.COMPRESSED_FILENAME_EXTENSION,
								  AppConfig.INSTANCE.isIgnoreFilenameCase());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: COMMAND-LINE OPTIONS


	private enum Option
		implements CommandLine.IOption<Option>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		AIFF_CHUNK_FILTER
		(
			"aiff-chunk-filter",
			false,
			true
		),

		COMPRESS
		(
			"compress",
			true,
			false
		),

		EXPAND
		(
			"expand",
			true,
			false
		),

		HELP
		(
			"help",
			true,
			false
		),

		OUTPUT_DIRECTORY
		(
			"output-directory",
			false,
			true
		),

		OVERWRITE
		(
			"overwrite",
			false,
			false
		),

		RECURSIVE
		(
			"recursive",
			false,
			false
		),

		SHOW_INFO
		(
			"show-info",
			false,
			true
		),

		VALIDATE
		(
			"validate",
			true,
			false
		),

		VERSION
		(
			"version",
			true,
			false
		),

		WAVE_CHUNK_FILTER
		(
			"wave-chunk-filter",
			false,
			true
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The name of this option. */
		private	String	name;

		/** Flag: if {@code true}, this option is a subcommand. */
		private	boolean	subcommand;

		/** Flag: if {@code true}, this option requires an argument. */
		private	boolean	requiresArgument;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an enumeration constant for a command-line option of the application.
		 *
		 * @param name
		 *          the name of the option.
		 * @param subcommand
		 *          if {@code true}, the option is a subcommand.
		 * @param requiresArgument
		 *          if {@code true}, the option requires an argument.
		 */

		private Option(
			String	name,
			boolean	subcommand,
			boolean	requiresArgument)
		{
			// Initialise instance variables
			this.name = name;
			this.subcommand = subcommand;
			this.requiresArgument = requiresArgument;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : CommandLine.IOption interface
	////////////////////////////////////////////////////////////////////

		/**
		 * {@inheritDoc}
		 */

		@Override
		public Option getKey()
		{
			return this;
		}

		//--------------------------------------------------------------

		/**
		 * {@inheritDoc}
		 */

		@Override
		public String getName()
		{
			return name;
		}

		//--------------------------------------------------------------

		/**
		 * {@inheritDoc}
		 */

		@Override
		public boolean isSubcommand()
		{
			return subcommand;
		}

		//--------------------------------------------------------------

		/**
		 * {@inheritDoc}
		 */

		@Override
		public boolean requiresArgument()
		{
			return requiresArgument;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return getPrefixedName();
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ENUMERATION: KINDS OF INFORMATION


	/**
	 * This is an enumeration of kinds of information that may be written by the application to the standard output
	 * stream.
	 */

	private enum InfoKind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/**
		 * No information.
		 */
		NONE
		(
			"none"
		),

		/**
		 * The title of the application.
		 */
		TITLE
		(
			"title"
		),

		/**
		 * Logged output.
		 */
		LOG
		(
			"log"
		),

		/**
		 * The result of a subcommand.
		 */
		RESULT
		(
			"result"
		);

		/** The separator between kinds of information in the argument of the 'show-info' command-line option. */
		private static final	char	SEPARATOR_CHAR	= ',';

		/** The key that denotes all kinds of information. */
		private static final	String	ALL_KEY	= "all";

		/** The default kinds of information. */
		private static final	Set<InfoKind>	DEFAULT_VALUES	= EnumSet.of(LOG, RESULT);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The key that is associated with this kind of information. */
		private	String	key;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an enumeration constant for a kind of information that may be written to the
		 * standard output stream.
		 *
		 * @param key
		 *          the key that will be associated with this kind of information.
		 */

		private InfoKind(
			String	key)
		{
			// Initialise instance variables
			this.key = key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the kind of information that is associated with the specified key.
		 *
		 * @param  key
		 *           the key whose associated kind of information is desired.
		 * @return the kind of information that is associated {@code key}, or {@code null} if there is no such kind of
		 *         information.
		 */

		private static InfoKind forKey(
			String	key)
		{
			return Arrays.stream(values()).filter(value -> value.key.equalsIgnoreCase(key)).findFirst().orElse(null);
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

		CONFLICTING_OPTION_ARGUMENTS
		("The '%1' option occurs more than once with conflicting arguments."),

		NO_SUBCOMMAND
		("No subcommand was specified."),

		MULTIPLE_SUBCOMMANDS
		("More than one subcommand was specified: %1."),

		NO_INPUT_FILE_OR_DIRECTORY
		("No input file or directory was specified."),

		INVALID_OUTPUT_DIRECTORY
		("The output directory is invalid."),

		INVALID_AIFF_CHUNK_FILTER
		("The AIFF chunk filter is invalid."),

		INVALID_WAVE_CHUNK_FILTER
		("The WAVE chunk filter is invalid."),

		INVALID_INFO_KIND
		("'%1' is not a valid kind of information."),

		INCONSISTENT_INFO_KINDS
		("The arguments of the '" + Option.SHOW_INFO + "' option are inconsistent."),

		LIST_FILE_OR_DIRECTORY_DOES_NOT_EXIST
		("The file or directory denoted by this pathname in the list file does not exist."),

		LIST_FILE_PATHNAME_IS_A_FILE
		("The output pathname in the list file denotes a file."),

		FILE_OR_DIRECTORY_ACCESS_NOT_PERMITTED
		("Access to the file or directory specified in the list file was not permitted."),

		NOT_A_FILE
		("The pathname does not denote a normal file."),

		FILE_DOES_NOT_EXIST
		("The file does not exist."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory."),

		FAILED_TO_LIST_DIRECTORY_ENTRIES
		("Failed to get a list of directory entries.");

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


	// CLASS: COMMAND-LINE OPTION EXCEPTION


	/**
	 * This class implements an exception that relates to a command-line option.
	 */

	private static class OptionException
		extends AppException
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an exception that has the detail message that is associated with the specified
		 * identifier and relates to the option that is associated with the specified command-line element.
		 *
		 * @param id
		 *          the identifier of the exception.
		 * @param element
		 *          the command-line element to whose associated option the exception relates.
		 */

		private OptionException(
			ErrorId						id,
			CommandLine.Element<Option>	element)
		{
			// Call superclass constructor
			super(id, element.getOptionString());
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// COMMAND-LINE ARGUMENT EXCEPTION CLASS


	private static class ArgumentException
		extends AppException
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	prefix;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ArgumentException(
			ErrorId						id,
			CommandLine.Element<Option>	element,
			CharSequence...				replacements)
		{
			super(id, replacements);
			prefix = element.getOptionString() + "=" + element.getValue() + "\n";
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		protected String getPrefix()
		{
			return prefix;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
