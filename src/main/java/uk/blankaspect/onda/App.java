/*====================================================================*\

App.java

Application class.

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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import uk.blankaspect.common.build.BuildUtils;

import uk.blankaspect.common.cls.ClassUtils;

import uk.blankaspect.common.collection.ArraySet;

import uk.blankaspect.common.commandline.CommandLine;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.exception2.LocationException;

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


// APPLICATION CLASS


public class App
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		App		INSTANCE	= new App();

	public static final		String	SHORT_NAME	= "Onda";
	public static final		String	LONG_NAME	= "Onda lossless audio compressor";
	public static final		String	NAME_KEY	= "onda";

	private static final	String	BUILD_PROPERTIES_FILENAME	= "build.properties";

	private static final	String	PATHNAME_PREFIX	= "+";
	private static final	String	LIST_PREFIX		= "@";

	private static final	char	INFO_KIND_SEPARATOR_CHAR	= ',';

	private static final	String	EXCEPTION_CAUSE_PREFIX	= "- ";

	private static final	String	CONFIG_ERROR_STR	= "Configuration error";
	private static final	String	LAF_ERROR1_STR		= "Look-and-feel: ";
	private static final	String	LAF_ERROR2_STR		= "\nThe look-and-feel is not installed.";

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
	private static final	String	NUM_FILES_FOUND_STR			= "Number of files found = ";
	private static final	String	NUM_FILES_VALIDATED_STR		= "Number of files validated = ";
	private static final	String	NUM_FAILED_VALIDATION_STR	= "Number of files that failed validation = ";
	private static final	String	ALL_FILES_VALID_STR			= "All files were valid.";
	private static final	String	CQ_OPTION_STR				= "Continue or Quit (C/Q) ? ";
	private static final	String	RSQ_OPTION_STR				= "Replace, Skip or Quit (R/S/Q) ? ";

	private static final	String	SEPARATOR_STR	= "-".repeat(36);

	private static final	String	USAGE_STR	=
			"Usage: " + NAME_KEY + " command [options] [input-pathnames]\n\n" +
			"""
			Commands:
			  --compress
			      Compress the files specified by the input pathnames.
			      If an output directory is specified, the compressed files are written to
			      it; otherwise, each compressed file is written to the same directory as
			      its input file.
			  --expand
			      Expand the files specified by the input pathnames.
			      If an output directory is specified, the expanded files are written to
			      it; otherwise, each expanded file is written to the same directory as its
			      input file.
			  --help
			      Display help information.
			  --validate
			      Validate the files specified by the input pathnames.
			  --version
			      Display version information.

			Options:
			  --aiff-chunk-filter=(+|-)chunk-ids
			  --wave-chunk-filter=(+|-)chunk-ids
			      When compressing a file of the appropriate kind, preserve or discard
			      ancillary chunks with the identifiers specified by <chunk-ids>.  If this
			      option is not specified, all ancillary chunks are preserved.
			      <chunk-ids> must have either '+' or '-' prefixed to them:
			        If the prefix is '+', the filter is inclusive: a chunk is preserved if
			        its ID is in <chunk-ids>.
			        If the prefix is '-', the filter is exclusive: a chunk is preserved if
			        its ID is not in <chunk-ids>.
			      The first character after the '+' or '-' is taken to be the separator
			      between the identifiers listed in <chunk-ids>.  Trailing spaces are
			      assumed for any identifier that has fewer than four characters.
			      Examples:
			        --wave-chunk-filter=+/bext/cue  preserves the chunks in a WAVE file
			            with IDs 'bext' and 'cue ';
			        --aiff-chunk-filter=-  preserves all the ancillary chunks in an AIFF
			            file (ie, excludes none) (the default);
			        --wave-chunk-filter=+  discards all the ancillary chunks in a WAVE
			            file (ie, includes none).
			  --output-directory=pathname
			      The directory to which output files will be written.  If an input
			      pathname is a directory and the 'recursive' option is present, the
			      directory structure below the input directory will be reproduced in the
			      output directory.
			  --overwrite
			      Overwrite an existing file without seeking confirmation.
			  --recursive
			      Process the input directory recursively.
			  --show-info={none|title|log|result|all}
			      The kinds of information that will be written to standard output.
			      Multiple kinds may be specified, separated by ','.  The default value is
			      'log,result'.

			If an option takes an argument, the key and argument of the option may be
			separated either by whitespace or by a single '='.

			If an input pathname has the prefix '@', it denotes a file that contains a list
			of input pathnames and, optionally, output directories.  Each non-empty line of
			the file is expected to contain a single input pathname, which may be followed
			by one or more tab characters (U+0009) and the pathname of an output directory.

			The last component of an input pathname may contain the wildcards '?' and '*'.

			If an input pathname has the prefix '+', the prefix is ignored.  The prefix can
			be used to prevent the expansion of patterns such as '*' on the Java command
			line and to allow pathnames that start with a literal '@'.  A literal '+' at
			the start of a pathname must be escaped by prefixing '+' to it.

			A pathname may contain Java system properties or environment variables enclosed
			between '${' and '}'; eg, ${HOME}.  A Java system property takes precedence
			over an environment variable with the same name.  A Java system property can be
			specified by prefixing 'sys.' to it (eg, ${sys.user.home}), and an environment
			variable can be specified by prefixing 'env.' to it (eg, ${env.HOME}).
			""";

	private enum Command
	{
		COMPRESS,
		EXPAND,
		HELP,
		VALIDATE,
		VERSION
	}

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

	private App()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
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

	public static List<InputOutput> readListFile(File file)
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

	public void showWarningMessage(String titleStr,
								   Object message)
	{
		if (hasGui)
			showMessageDialog(titleStr, message, JOptionPane.WARNING_MESSAGE);
		else
			printMessage(titleStr, message);
	}

	//------------------------------------------------------------------

	public void showErrorMessage(String titleStr,
								 Object message)
	{
		if (hasGui)
			showMessageDialog(titleStr, message, JOptionPane.ERROR_MESSAGE);
		else
			printMessage(titleStr, message);
	}

	//------------------------------------------------------------------

	public void showMessageDialog(String titleStr,
								  Object message,
								  int    messageKind)
	{
		JOptionPane.showMessageDialog(mainWindow, message, titleStr, messageKind);
	}

	//------------------------------------------------------------------

	public void printMessage(String titleStr,
							 Object message)
	{
		System.err.println(titleStr);
		System.err.println(message);
		System.err.println(SEPARATOR_STR);
	}

	//------------------------------------------------------------------

	public void compress(List<InputOutput> inputsOutputs,
						 ChunkFilter[]     chunkFilters,
						 boolean           recursive)
		throws TaskCancelledException
	{
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
							setTotalFileLength(getTotalFileLength(inputsOutputs, getAudioFileFilter(),
																  recursive));
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

	public void expand(List<InputOutput> inputsOutputs,
					   boolean           recursive)
		throws TaskCancelledException
	{
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
						setTotalFileLength(getTotalFileLength(inputsOutputs, getCompressedFileFilter(),
															  recursive));
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

	public void validate(List<InputOutput> inputsOutputs,
						 boolean           recursive)
	{
		// Validate input files
		FileProcessor.ValidationResult result = new FileProcessor.ValidationResult();
		try
		{
			if (hasGui)
			{
				fileLengthOffset = 0;
				((TaskProgressDialog)Task.getProgressView()).
						setTotalFileLength(getTotalFileLength(inputsOutputs, getCompressedFileFilter(),
															  recursive));
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
		buffer.append(result.numFound);
		int messageKind = 0;
		int numFailed = result.numValidated - result.numValid;
		if ((result.numValidated == result.numFound) && (numFailed == 0))
		{
			messageKind = JOptionPane.INFORMATION_MESSAGE;
			if (result.numFound > 0)
			{
				buffer.append('\n');
				buffer.append(ALL_FILES_VALID_STR);
			}
		}
		else
		{
			messageKind = JOptionPane.WARNING_MESSAGE;
			if (result.numValidated < result.numFound)
			{
				buffer.append('\n');
				buffer.append(NUM_FILES_VALIDATED_STR);
				buffer.append(result.numValidated);
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

	private void init(String[] args)
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
			buildProperties = new ResourceProperties(ResourceUtils.normalisedPathname(getClass(), BUILD_PROPERTIES_FILENAME));
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
				showWarningMessage(SHORT_NAME + " : " + CONFIG_ERROR_STR,
								   LAF_ERROR1_STR + lookAndFeelName + LAF_ERROR2_STR);

			// Select all text when a text field gains focus
			if (config.isSelectTextOnFocusGained())
				TextFieldUtils.selectAllOnFocusGained();

			// Create main window
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					mainWindow = new MainWindow(LONG_NAME + " " + versionStr);
				}
			});
		}
		else
		{
			// Parse command line
			try
			{
				List<CommandLine.Element<Option>> commandLineElements =
														new CommandLine<>(Option.class, true, USAGE_STR).parse(args);
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

	private void parseCommandLine(List<CommandLine.Element<Option>> elements)
		throws AppException
	{
		// Initialise local variables
		List<Command> commands = new ArraySet<>();
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
					inputsOutputs.
							addAll(readListFile(new File(PathnameUtils.parsePathname(pathname))));
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

			switch (element.getOption().getKey())
			{
				case AIFF_CHUNK_FILTER:
					try
					{
						ChunkFilter aiffChunkFilter0 = new ChunkFilter(elementValue);
						if ((aiffChunkFilter != null) && !aiffChunkFilter.equals(aiffChunkFilter0))
							throw new UsageException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element.getOptionString());
						aiffChunkFilter = aiffChunkFilter0;
					}
					catch (IllegalArgumentException e)
					{
						throw new ArgumentException(ErrorId.INVALID_AIFF_CHUNK_FILTER, element);
					}
					break;

				case COMPRESS:
					commands.add(Command.COMPRESS);
					break;

				case EXPAND:
					commands.add(Command.EXPAND);
					break;

				case HELP:
					commands.add(Command.HELP);
					break;

				case OUTPUT_DIRECTORY:
				{
					if (elementValue.isEmpty())
						throw new ArgumentException(ErrorId.INVALID_OUTPUT_DIRECTORY, element);
					File outDirectory0 = new File(PathnameUtils.parsePathname(elementValue));
					if ((outDirectory != null) && !outDirectory.equals(outDirectory0))
						throw new UsageException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element.getOptionString());
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
					for (String key : StringUtils.split(elementValue, INFO_KIND_SEPARATOR_CHAR))
					{
						InfoKind infoKind = InfoKind.forKey(key);
						if (infoKind == null)
							throw new UsageException(ErrorId.INVALID_OPTION_ARGUMENT, key, element.getOptionString());
						infoKind.addTo(infoKinds);
					}
					if (infoKinds.contains(InfoKind.NONE) && (infoKinds.size() > 1))
						throw new ArgumentException(ErrorId.INCONSISTENT_INFO_KINDS, element);
					break;

				case VALIDATE:
					commands.add(Command.VALIDATE);
					break;

				case VERSION:
					commands.add(Command.VERSION);
					break;

				case WAVE_CHUNK_FILTER:
					try
					{
						ChunkFilter waveChunkFilter0 = new ChunkFilter(elementValue);
						if ((waveChunkFilter != null) && !waveChunkFilter.equals(waveChunkFilter0))
							throw new UsageException(ErrorId.CONFLICTING_OPTION_ARGUMENTS, element.getOptionString());
						waveChunkFilter = waveChunkFilter0;
					}
					catch (IllegalArgumentException e)
					{
						throw new ArgumentException(ErrorId.INVALID_WAVE_CHUNK_FILTER, element);
					}
					break;
			}
		}

		// Test for a single command
		if (commands.isEmpty())
			throw new UsageException(ErrorId.NO_COMMAND);
		if (commands.size() > 1)
			throw new UsageException(ErrorId.MULTIPLE_COMMANDS);

		// Set default values for missing options
		if (infoKinds.isEmpty())
		{
			infoKinds.add(InfoKind.LOG);
			infoKinds.add(InfoKind.RESULT);
		}
		if (aiffChunkFilter == null)
			aiffChunkFilter = ChunkFilter.INCLUDE_ALL;
		if (waveChunkFilter == null)
			waveChunkFilter = ChunkFilter.INCLUDE_ALL;

		// Perform command
		Log.INSTANCE.setShow(infoKinds.contains(InfoKind.LOG));
		switch (commands.get(0))
		{
			case COMPRESS:
				if (inputsOutputs.isEmpty())
					throw new UsageException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Compress(inputsOutputs, new ChunkFilter[] { aiffChunkFilter, waveChunkFilter },
										 recursive));
				break;

			case EXPAND:
				if (inputsOutputs.isEmpty())
					throw new UsageException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Expand(inputsOutputs, recursive));
				break;

			case HELP:
				showTitle();
				System.out.println();
				System.out.println(USAGE_STR);
				break;

			case VALIDATE:
				if (inputsOutputs.isEmpty())
					throw new UsageException(ErrorId.NO_INPUT_FILE_OR_DIRECTORY);
				if (infoKinds.contains(InfoKind.TITLE))
					showTitle();
				else
					titleShown = true;
				doTask(new Task.Validate(inputsOutputs, recursive));
				break;

			case VERSION:
				showTitle();
				break;
		}
	}

	//------------------------------------------------------------------

	private void showTitle()
	{
		if (!titleShown)
		{
			System.out.println(SHORT_NAME + " " + versionStr);
			titleShown = true;
		}
	}

	//------------------------------------------------------------------

	private void confirmContinue(AppException exception)
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

	private boolean confirmReplace(String titleStr,
								   File   file)
		throws TaskCancelledException
	{
		String messageStr = Utils.getPathname(file) + AppConstants.ALREADY_EXISTS_STR;
		if (hasGui)
		{
			String[] optionStrs = Utils.getOptionStrings(AppConstants.REPLACE_STR, SKIP_STR);
			int result = JOptionPane.showOptionDialog(mainWindow, messageStr, titleStr,
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

	private void doTask(Task task)
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

	private void compressDirectory(InputOutput   inputOutput,
								   ChunkFilter[] chunkFilters,
								   boolean       recursive)
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

	private void compressFile(InputOutput   inputOutput,
							  ChunkFilter[] chunkFilters)
		throws AppException
	{
		// Test for input file
		File inFile = inputOutput.input;
		if (!inFile.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, inFile);

		// Generate name of output file
		File outDirectory = inputOutput.getOutputDirectory();
		File outFile = new File(outDirectory, inFile.getName() + AppConstants.COMPRESSED_FILE_SUFFIX);

		// Write name of task to log
		Log.INSTANCE.appendLine(COMPRESSING_STR + Utils.getPathname(inFile) + ARROW_STR +
																			Utils.getPathname(outFile));

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

	private void expandDirectory(InputOutput inputOutput,
								 boolean     recursive)
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

	private void expandFile(InputOutput inputOutput)
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
		if (filename.endsWith(AppConstants.COMPRESSED_FILE_SUFFIX))
		{
			filename = StringUtils.removeSuffix(filename, AppConstants.COMPRESSED_FILE_SUFFIX);
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
			PrivateData privateData =
								OndaFileIff.getFileKind(inFile).createReader(inFile).readPrivateData();
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
		Log.INSTANCE.appendLine(EXPANDING_STR + Utils.getPathname(inFile) + ARROW_STR +
																			Utils.getPathname(outFile));

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

	private void validateDirectory(File                           directory,
								   boolean                        recursive,
								   FileProcessor.ValidationResult validationResult)
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

	private void validateFile(File                           file,
							  FileProcessor.ValidationResult validationResult)
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

	private long getFileLengths(File           directory,
								FilenameFilter filter,
								boolean        recursive)
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

	private long getTotalFileLength(List<InputOutput> inputsOutputs,
									FilenameFilter    filter,
									boolean           recursive)
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
		String[] patterns = new String[AppConstants.AUDIO_FILE_SUFFIXES.length];
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = "*" + AppConstants.AUDIO_FILE_SUFFIXES[i];
		return new FilenameFilter.MultipleFilter(patterns,
												 AppConfig.INSTANCE.isIgnoreFilenameCase());
	}

	//------------------------------------------------------------------

	private FilenameFilter getCompressedFileFilter()
	{
		return new FilenameFilter("*" + AppConstants.COMPRESSED_FILE_SUFFIX,
								  AppConfig.INSTANCE.isIgnoreFilenameCase());
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// COMMAND-LINE OPTIONS


	private enum Option
		implements CommandLine.IOption<Option>
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		AIFF_CHUNK_FILTER
		(
			"aiff-chunk-filter",
			true
		),

		COMPRESS
		(
			"compress",
			false
		),

		EXPAND
		(
			"expand",
			false
		),

		HELP
		(
			"help",
			false
		),

		OUTPUT_DIRECTORY
		(
			"output-directory",
			true
		),

		OVERWRITE
		(
			"overwrite",
			false
		),

		RECURSIVE
		(
			"recursive",
			false
		),

		SHOW_INFO
		(
			"show-info",
			true
		),

		VALIDATE
		(
			"validate",
			false
		),

		VERSION
		(
			"version",
			false
		),

		WAVE_CHUNK_FILTER
		(
			"wave-chunk-filter",
			true
		);

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	name;
		private	boolean	hasArgument;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Option(String  name,
					   boolean hasArgument)
		{
			this.name = name;
			this.hasArgument = hasArgument;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : CommandLine.IOption interface
	////////////////////////////////////////////////////////////////////

		@Override
		public Option getKey()
		{
			return this;
		}

		//--------------------------------------------------------------

		@Override
		public String getName()
		{
			return name;
		}

		//--------------------------------------------------------------

		@Override
		public boolean hasArgument()
		{
			return hasArgument;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		INVALID_OPTION_ARGUMENT
		("'%1' is not a valid argument for the '%2' option."),

		CONFLICTING_OPTION_ARGUMENTS
		("The '%1' option was specified more than once with different arguments."),

		NO_COMMAND
		("No command was specified."),

		MULTIPLE_COMMANDS
		("More than one command was specified."),

		NO_INPUT_FILE_OR_DIRECTORY
		("No input file or directory was specified."),

		INVALID_OUTPUT_DIRECTORY
		("The output directory is invalid."),

		INVALID_AIFF_CHUNK_FILTER
		("The AIFF chunk filter is invalid."),

		INVALID_WAVE_CHUNK_FILTER
		("The WAVE chunk filter is invalid."),

		INCONSISTENT_INFO_KINDS
		("The arguments of the 'show-info' option are inconsistent."),

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

		private ErrorId(String message)
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

		private ArgumentException(ErrorId                     id,
								  CommandLine.Element<Option> element)
		{
			super(id);
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


	// COMMAND-LINE USAGE EXCEPTION CLASS


	private static class UsageException
		extends AppException
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	prefix;
		private	String	suffix;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private UsageException(ErrorId id)
		{
			super(id);
			suffix = "\n" + USAGE_STR;
		}

		//--------------------------------------------------------------

		private UsageException(ErrorId   id,
							   String... strs)
		{
			super(id, strs);
			suffix = "\n" + USAGE_STR;
		}

		//--------------------------------------------------------------

		private UsageException(ErrorId                     id,
							   CommandLine.Element<Option> element)
		{
			this(id);
			prefix = element.getOptionString() + " " + element.getValue() + "\n";
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

		@Override
		protected String getSuffix()
		{
			return suffix;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
