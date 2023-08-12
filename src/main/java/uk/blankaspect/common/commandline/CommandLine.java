/*====================================================================*\

CommandLine.java

Class: command line.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.commandline;

//----------------------------------------------------------------------


// IMPORTS


import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import uk.blankaspect.common.filesystem.PathnameUtils;

import uk.blankaspect.common.misc.Tokeniser;

//----------------------------------------------------------------------


// CLASS: COMMAND LINE


/**
 * This class provides a means of parsing the command line of a program.  A command line consists of
 * <ul>
 *   <li><i>options</i>, which have the prefix "--" and may have an argument, and</li>
 *   <li><i>non-option arguments</i> such as the pathnames of input files.</li>
 * </ul>
 * A command line may optionally be required to conform to POSIX order in which all options must precede all non-option
 * arguments.  If an argument starts with "@", the remainder of the argument may optionally be interpreted as the
 * pathname of a file that contains arguments that will replace the original argument.
 */

public class CommandLine<E extends Enum<E> & CommandLine.IOption<E>>
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The prefix of a command-line option. */
	public static final		String	OPTION_PREFIX	= "--";

	/** The prefix of the pathname of a file that contains additional arguments. */
	public static final		String	ARGUMENT_FILE_PREFIX	= "@";

	/** The character that separates the name and argument of an option. */
	private static final	char	OPTION_ARGUMENT_SEPARATOR_CHAR	= '=';

	/** The states of the finite state machine that parses a command line. */
	private enum State
	{
		ARGUMENT,
		OPTION,
		OPTION_ARGUMENT,
		NON_OPTION_ARGUMENT,
		DONE
	}

	/** Error messages. */
	private interface ErrorMsg
	{
		String	FILE_DOES_NOT_EXIST					= "The file does not exist.";
		String	ERROR_READING_FILE					= "An error occurred when reading the file.";
		String	UNCLOSED_QUOTATION					= "Line %d contains an unclosed quotation.";
		String	INVALID_OPTION						= "'" + OPTION_PREFIX + "%s' is not a valid option.";
		String	MISSING_OPTION_ARGUMENT				= "The '" + OPTION_PREFIX + "%s' option expects an argument.";
		String	UNEXPECTED_OPTION_ARGUMENT			= "The '" + OPTION_PREFIX + "%s' option does not take an argument.";
		String	OPTION_AFTER_NON_OPTION_ARGUMENT	= "All options must precede non-option arguments.";
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** A set of the options that may appear on this command line. */
	private	Set<E>	options;

	/** Flag: if {@code true}, the arguments of this command line are expected to appear in POSIX order; that is, all
		options must precede all non-option arguments. */
	private	boolean	posixOrder;

	/** The usage string. */
	private	String	usageStr;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a command line.
	 *
	 * @param optionClass
	 *          the class of options.
	 * @param posixOrder
	 *          if {@code true}, the arguments of this command line are expected to appear in POSIX order; that is, all
	 *          options must precede all non-option arguments.
	 * @param usageStr
	 *          the usage string, which may be {@code null}.
	 */

	public CommandLine(
		Class<E>	optionClass,
		boolean		posixOrder,
		String		usageStr)
	{
		// Initialise instance variables
		options = EnumSet.allOf(optionClass);
		this.posixOrder = posixOrder;
		this.usageStr = usageStr;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a string representation of the specified command-line arguments in which adjacent arguments are separated
	 * by a single space character (U+0020).  If an argument is empty or contains a space character, it is enclosed in
	 * quotation marks (U+0022), and each quotation mark within the argument is escaped by prefixing another quotation
	 * mark to it.
	 *
	 * @param  arguments
	 *           the arguments for which a string representation is required.
	 * @return a string representation of {@code arguments}.
	 */

	public static String argumentsToString(
		Iterable<String>	arguments)
	{
		// Allocate buffer
		StringBuilder buffer = new StringBuilder(256);

		// Concatenate arguments
		for (String argument : arguments)
		{
			if (!buffer.isEmpty())
				buffer.append(' ');
			if (argument.isEmpty() || argument.contains(" "))
			{
				buffer.append('"');
				buffer.append(argument.replace("\"", "\"\""));
				buffer.append('"');
			}
			else
				buffer.append(argument);
		}

		// Return string
		return buffer.toString();
	}

	//------------------------------------------------------------------

	/**
	 * Reads the file that is denoted by the specified pathname, which is assumed to be a text file that contains
	 * command-line arguments separated by whitespace, parses the content of the file and returns a list of the
	 * resulting arguments.  The file may contain comments that start with the specified prefix; all characters from
	 * the comment prefix to the end of the line are ignored.
	 *
	 * @param  pathname
	 *           the pathname of the file.
	 * @param  commentPrefix
	 *           the prefix of comments, which may be {@code null}.
	 * @return the list of arguments that result from parsing the file denoted by {@code pathname}.
	 * @throws CommandLineException
	 *           if an error occurred when reading or parsing the file.
	 */

	private static List<String> readArgumentFile(
		String	pathname,
		String	commentPrefix)
		throws CommandLineException
	{
		// Test whether file exists
		Path file = Path.of(PathnameUtils.parsePathname(pathname));
		if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
			throw new CommandLineException(ErrorMsg.FILE_DOES_NOT_EXIST, file);

		// Read file add extract arguments from it
		try
		{
			// Read file
			List<String> lines = Files.readAllLines(file);

			// Initialise argument extractor
			Tokeniser extractor = new Tokeniser(Character::isWhitespace);

			// Initialise list of arguments
			List<String> arguments = new ArrayList<>();

			// Extract arguments from file
			for (int i = 0; i < lines.size(); i++)
			{
				// Get line
				String str = lines.get(i);

				// Strip any comment
				if (commentPrefix != null)
				{
					int index = str.indexOf(commentPrefix);
					if (index >= 0)
						str = str.substring(0, index);
				}

				// Remove leading and trailing whitespace
				str = str.trim();

				// Extract arguments from line
				if (!str.isEmpty())
				{
					try
					{
						extractor.setSequence(str);
						arguments.addAll(Tokeniser.removeEmptyText(extractor.getTokenText(true)));
					}
					catch (Tokeniser.UnclosedQuotationException e)
					{
						throw new CommandLineException(String.format(ErrorMsg.UNCLOSED_QUOTATION, i + 1), file);
					}
				}
			}

			// Return list of arguments
			return arguments;
		}
		catch (IOException e)
		{
			throw new CommandLineException(ErrorMsg.ERROR_READING_FILE, e, file);
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Parses the specified command-line arguments and returns a list of the resulting command-line elements.
	 *
	 * @param  arguments
	 *           the command-line arguments that will be parsed.
	 * @return a list of the command-line elements that resulted from parsing {@code arguments}.
	 * @throws CommandLineException
	 *           if an error occurred when parsing {@code arguments} or when reading or parsing an argument file that
	 *           was specified as a command-line argument.
	 */

	public List<Element<E>> parse(
		String[]	arguments)
		throws CommandLineException
	{
		return parse(Arrays.asList(arguments), false, null);
	}

	//------------------------------------------------------------------

	/**
	 * Parses the specified command-line arguments and returns a list of the resulting command-line elements.
	 *
	 * @param  arguments
	 *           the command-line arguments that will be parsed.
	 * @param  expand
	 *           if {@code true} and an argument starts with "@", the remainder of the argument will be interpreted as
	 *           the pathname of a file that contains arguments that will replace the original argument.
	 * @param  commentPrefix
	 *           the prefix of line comments in an argument file, which may be {@code null}.  This is ignored if
	 *           {@code expand} is {@code false}.
	 * @return a list of the command-line elements that result from parsing {@code arguments}.
	 * @throws CommandLineException
	 *           if an error occurred when parsing {@code arguments} or when reading or parsing an argument file that
	 *           was specified as a command-line argument.
	 */

	public List<Element<E>> parse(
		String[]	arguments,
		boolean		expand,
		String		commentPrefix)
		throws CommandLineException
	{
		return parse(Arrays.asList(arguments), expand, commentPrefix);
	}

	//------------------------------------------------------------------

	/**
	 * Parses the specified command-line arguments and returns a list of the resulting command-line elements.
	 *
	 * @param  arguments
	 *           the command-line arguments that will be parsed.
	 * @return a list of the command-line elements that resulted from parsing {@code arguments}.
	 * @throws CommandLineException
	 *           if an error occurred when parsing {@code arguments} or when reading or parsing an argument file that
	 *           was specified as a command-line argument.
	 */

	public List<Element<E>> parse(
		Iterable<String>	arguments)
		throws CommandLineException
	{
		return parse(arguments, false, null);
	}

	//------------------------------------------------------------------

	/**
	 * Parses the specified command-line arguments and returns a list of the resulting command-line elements.
	 *
	 * @param  arguments
	 *           the command-line arguments that will be parsed.
	 * @param  expand
	 *           if {@code true} and an argument starts with "@", the remainder of the argument will be interpreted as
	 *           the pathname of a file that contains arguments that will replace the original argument.
	 * @param  commentPrefix
	 *           the prefix of line comments in an argument file, which may be {@code null}.  This is ignored if
	 *           {@code expand} is {@code false}.
	 * @return a list of the command-line elements that result from parsing {@code arguments}.
	 * @throws CommandLineException
	 *           if an error occurred when parsing {@code arguments} or when reading or parsing an argument file that
	 *           was specified as a command-line argument.
	 */

	public List<Element<E>> parse(
		Iterable<String>	arguments,
		boolean				expand,
		String				commentPrefix)
		throws CommandLineException
	{
		// Expand arguments
		List<String> args = new ArrayList<>();
		for (String argument : arguments)
		{
			if (expand && argument.startsWith(ARGUMENT_FILE_PREFIX))
			{
				String pathname = argument.substring(ARGUMENT_FILE_PREFIX.length());
				args.addAll(readArgumentFile(pathname, commentPrefix));
			}
			else
				args.add(argument);
		}

		// Initialise list of command-line elements
		List<Element<E>> elements = new ArrayList<>();

		// Initialise variables
		String argument = null;
		int argIndex = 0;
		IOption<E> option = null;
		boolean optionsEnded = false;
		State state = State.ARGUMENT;

		// Parse arguments
		while (state != State.DONE)
		{
			switch (state)
			{
			case ARGUMENT:
				if (argIndex < args.size())
				{
					argument = args.get(argIndex);
					if (argument.startsWith(OPTION_PREFIX))
					{
						++argIndex;
						state = State.OPTION;
					}
					else
						state = State.NON_OPTION_ARGUMENT;
				}
				else
					state = State.DONE;
				break;

			case OPTION:
				argument = argument.substring(OPTION_PREFIX.length());
				if (argument.isEmpty())
				{
					optionsEnded = true;
					state = State.NON_OPTION_ARGUMENT;
				}
				else
				{
					option = null;
					int index = argument.indexOf(OPTION_ARGUMENT_SEPARATOR_CHAR);
					if (index < 0)
					{
						option = getOption(argument);
						if (option == null)
							throw new CommandLineException(String.format(ErrorMsg.INVALID_OPTION, argument), usageStr);
						if (option.hasArgument())
						{
							argument = null;
							state = State.OPTION_ARGUMENT;
						}
						else
						{
							elements.add(new Element<>(option, null));
							state = State.ARGUMENT;
						}
					}
					else
					{
						String name = argument.substring(0, index);
						option = getOption(name);
						if (option == null)
							throw new CommandLineException(String.format(ErrorMsg.INVALID_OPTION, name), usageStr);
						if (!option.hasArgument())
							throw new CommandLineException(String.format(ErrorMsg.UNEXPECTED_OPTION_ARGUMENT, name), usageStr);
						argument = argument.substring(index + 1);
						state = State.OPTION_ARGUMENT;
					}
				}
				break;

			case OPTION_ARGUMENT:
				if (argument == null)
				{
					if (argIndex >= args.size())
						throw new CommandLineException(String.format(ErrorMsg.MISSING_OPTION_ARGUMENT, option.getName()),
													   usageStr);
					argument = args.get(argIndex++);
				}
				elements.add(new Element<>(option, argument));
				state = State.ARGUMENT;
				break;

			case NON_OPTION_ARGUMENT:
				if (argIndex < args.size())
				{
					argument = args.get(argIndex);
					if (argument.startsWith(OPTION_PREFIX) && !optionsEnded)
					{
						if (posixOrder)
							throw new CommandLineException(ErrorMsg.OPTION_AFTER_NON_OPTION_ARGUMENT, usageStr);
						state = State.ARGUMENT;
					}
					else
					{
						elements.add(new Element<>(null, argument));
						++argIndex;
					}
				}
				else
					state = State.DONE;
				break;

			case DONE:
				// do nothing
				break;
			}
		}

		// Return list of command-line elements
		return elements;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the option that has the specified name.
	 *
	 * @param  name
	 *           the name of the required option.
	 * @return the option whose name is {@code name}, or {@code null} if there is no such option.
	 */

	private IOption<E> getOption(
		String	name)
	{
		for (IOption<E> option : options)
		{
			if (option.getName().equals(name))
				return option;
		}
		return null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: COMMAND-LINE OPTION


	/**
	 * This interface defines the methods that must be implemented by a command-line option.
	 */

	public interface IOption<E extends Enum<E>>
	{

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the enumeration constant that is the key of this option.
		 *
		 * @return the enumeration constant that is the key of this option.
		 */

		E getKey();

		//--------------------------------------------------------------

		/**
		 * Returns the name of this option.
		 *
		 * @return the name of this option.
		 */

		String getName();

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if this option expects an argument.
		 *
		 * @return {@code true} if this option expects an argument.
		 */

		boolean hasArgument();

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: COMMAND-LINE ELEMENT


	/**
	 * This class implements an element of a command line.  An element is one of the following:
	 * <ul>
	 *   <li>an option, which starts with "--";</li>
	 *   <li>an argument of an option;</li>
	 *   <li>a non-option argument (eg, the pathname of an input file).</li>
	 * </ul>
	 */

	public static class Element<E extends Enum<E>>
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The option that is associated with this element. */
		private	IOption<E>	option;

		/** The value of this element. */
		private	String		value;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an element of a command line.
		 *
		 * @param option
		 *          the option that will be associated with the element.
		 * @param value
		 *          the value of the element.
		 */

		public Element(
			IOption<E>	option,
			String		value)
		{
			// Initialise instance variables
			this.option = option;
			this.value = value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * {@inheritDoc}
		 */

		@Override
		public String toString()
		{
			String optionStr = getOptionString();
			return (optionStr == null) ? (value == null) ? "" : value
									   : (value == null) ? optionStr : optionStr + " " + value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the option that is associated with this element.
		 *
		 * @return the option that is associated with this element.
		 */

		public IOption<E> getOption()
		{
			return option;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the value of this element.
		 *
		 * @return the value of this element.
		 */

		public String getValue()
		{
			return value;
		}

		//--------------------------------------------------------------

		/**
		 * Returns a string representation of the option that is associated with this element.
		 *
		 * @return a string representation of the option that is associated with this element.
		 */

		public String getOptionString()
		{
			return (option == null) ? null : OPTION_PREFIX + option.getName();
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: COMMAND-LINE EXCEPTION


	/**
	 * This class implements an exception that may be thrown when parsing a command line.
	 */

	public static class CommandLineException
		extends Exception
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** Miscellaneous strings. */
		private static final	String	ARGUMENT_FILE_STR	= "Command-line argument file: ";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of a command-line exception with the specified detail message.
		 *
		 * @param message
		 *          the detail message of the exception.
		 */

		private CommandLineException(
			String	message)
		{
			// Call superclass method
			super(message);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a new instance of a command-line exception with the specified detail message and usage string, which
		 * is appended to the detail message.
		 *
		 * @param message
		 *          the detail message of the exception.
		 * @param usageStr
		 *          the usage staring, which may be {@code null}.
		 */

		private CommandLineException(
			String	message,
			String	usageStr)
		{
			// Call superclass method
			super((usageStr == null) ? message : message + "\n" + usageStr);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a new instance of a command-line exception with the specified detail message and associated file.
		 *
		 * @param message
		 *          the detail message of the exception.
		 * @param file
		 *          the file that will be associated with the exception.
		 */

		private CommandLineException(
			String	message,
			Path	file)
		{
			// Call superclass method
			super(ARGUMENT_FILE_STR + file.toAbsolutePath() + "\n" + message);
		}

		//--------------------------------------------------------------

		/**
		 * Creates a new instance of a command-line exception with the specified detail message, underlying cause and
		 * associated file.
		 *
		 * @param message
		 *          the detail message of the exception.
		 * @param cause
		 *          the underlying cause of the exception.
		 * @param file
		 *          the file that will be associated with the exception.
		 */

		private CommandLineException(
			String		message,
			Throwable	cause,
			Path		file)
		{
			// Call superclass method
			super(ARGUMENT_FILE_STR + file.toAbsolutePath() + "\n" + message, cause);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
