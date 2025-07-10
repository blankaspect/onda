/*====================================================================*\

Tokeniser.java

Class: tokeniser.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// IMPORTS


import java.util.ArrayList;
import java.util.List;

import java.util.function.Predicate;

import uk.blankaspect.common.function.IProcedure0;

//----------------------------------------------------------------------


// CLASS: TOKENISER


/**
 * This class provides a means of extracting tokens from an input sequence of characters.  Adjacent tokens are separated
 * by a specified character, and the end of the input sequence is optionally denoted by another specified character.
 * <p>
 * A tokeniser can be used in a mode in which subsequences of a token may be <i>quoted</i> (enclosed in quotation marks
 * (U+0022)).  Within a quoted subsequence, end-of-token and end-of-input characters are ignored.  The enclosing
 * quotation marks are removed when the token is extracted.  Within a quoted subsequence, each adjacent pair of
 * quotation marks is replaced by a single quotation mark.
 * </p>
 */

public class Tokeniser
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/**
	 * The states of the finite-state machine that extracts tokens from an input sequence.
	 */
	private enum TokenState
	{
		TOKEN,
		QUOTATION,
		QUOTATION_PENDING,
		END_OF_TOKEN
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	/** The test that is performed on a character to determine whether it separates adjacent tokens. */
	private	Predicate<Character>	tokenSeparatorTest;

	/** The test that is performed on a character to determine whether it denotes the end of the input sequence. */
	private	Predicate<Character>	endOfInputTest;

	/** The input sequence. */
	private	CharSequence			sequence;

	/** The index of the current character in the input sequence. */
	private	int						sequenceIndex;

	/** The index of the end of the input sequence. */
	private	int						sequenceEndIndex;

	/** Flag: if {@code true}, the end of the input sequence has been reached. */
	private	boolean					endOfInput;

	/** A list of the tokens that have been extracted from input sequence. */
	private	List<Token>				tokens;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a tokeniser that extracts tokens from an input sequence of characters.
	 *
	 * @param tokenSeparatorChar
	 *          the character that separates adjacent tokens in the input sequence.
	 */

	public Tokeniser(
		char	tokenSeparatorChar)
	{
		// Call alternative constructor
		this(tokenSeparatorChar, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a tokeniser that extracts tokens from an input sequence of characters.
	 *
	 * @param tokenSeparatorChar
	 *          the character that separates adjacent tokens in the input sequence.
	 * @param endOfInputChar
	 *          the character that denotes the end of the input sequence, which may be {@code null}.
	 */

	public Tokeniser(
		char		tokenSeparatorChar,
		Character	endOfInputChar)
	{
		// Call alternative constructor
		this(ch -> ch == tokenSeparatorChar, (endOfInputChar == null) ? null : ch -> ch == endOfInputChar.charValue());
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a tokeniser that extracts tokens from an input sequence of characters.
	 *
	 * @param  tokenSeparatorTest
	 *           the test that will be performed on a character to determine whether it separates adjacent tokens.
	 * @throws IllegalArgumentException
	 *           if {@code tokenSeparatorTest} is {@code null}.
	 */

	public Tokeniser(
		Predicate<Character>	tokenSeparatorTest)
	{
		// Call alternative constructor
		this(tokenSeparatorTest, null);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a tokeniser that extracts tokens from an input sequence of characters.
	 *
	 * @param  tokenSeparatorTest
	 *           the test that will be performed on a character to determine whether it separates adjacent tokens.
	 * @param  endOfInputTest
	 *           the test that will be performed on a character to determine whether it denotes the end of the input
	 *           sequence.  It may be {@code null}.
	 * @throws IllegalArgumentException
	 *           if {@code tokenSeparatorTest} is {@code null}.
	 */

	public Tokeniser(
		Predicate<Character>	tokenSeparatorTest,
		Predicate<Character>	endOfInputTest)
	{
		// Validate arguments
		if (tokenSeparatorTest == null)
			throw new IllegalArgumentException("Null token-separator test");

		// Initialise instance variables
		sequence = "";
		this.tokenSeparatorTest = tokenSeparatorTest;
		this.endOfInputTest = (endOfInputTest == null) ? ch -> false : endOfInputTest;
		sequenceEndIndex = -1;
		tokens = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a list whose elements are the specified tokens from which all empty tokens have been removed.  An empty
	 * token is one whose text is empty or contains only whitespace.
	 *
	 * @param  tokens
	 *           the tokens that will be filtered.
	 * @return a list whose elements are those of {@code tokens} from which all empty tokens have been removed.
	 */

	public static List<Token> removeEmptyTokens(
		Iterable<? extends Token>	tokens)
	{
		List<Token> outTokens = new ArrayList<>();
		for (Token token : tokens)
		{
			if (!token.text.isBlank())
				outTokens.add(token);
		}
		return outTokens;
	}

	//------------------------------------------------------------------

	/**
	 * Returns a list whose elements are the specified items of text from which all empty items have been removed.  An
	 * empty item is text that is empty or contains only whitespace.
	 *
	 * @param  texts
	 *           the items of text that will be filtered.
	 * @return a list whose elements are those of {@code texts} from which all empty items of text have been removed.
	 */

	public static List<String> removeEmptyText(
		Iterable<String>	texts)
	{
		List<String> outTexts = new ArrayList<>();
		for (String text : texts)
		{
			if (!text.isBlank())
				outTexts.add(text);
		}
		return outTexts;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Sets the input sequence to the specified value.
	 *
	 * @param  sequence
	 *           the value to which the input sequence will be set.
	 * @return this tokeniser.
	 */

	public Tokeniser setSequence(
		CharSequence	sequence)
	{
		// Update instance variable
		this.sequence = sequence;

		// Reset to start of input sequence
		reset();

		// Return this tokeniser
		return this;
	}

	//------------------------------------------------------------------

	/**
	 * Extracts the next token from the input sequence and returns the result.  Quotation marks (U+0022) have no
	 * special significance.
	 *
	 * @return the next token that was extracted from the input sequence, or {@code null} if the end of the input
	 *         sequence has been reached.
	 * @throws UnclosedQuotationException
	 *           if {@code quotingEnabled} is {@code true} and the token text contains an opening quotation mark
	 *           (U+0022) that does not have a corresponding closing quotation mark.
	 */

	public Token nextToken()
	{
		return nextToken(false);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts the next token from the input sequence and returns the result.
	 *
	 * @param  quotingEnabled
	 *           if {@code true} and a subsequence of the token is enclosed in quotation marks (U+0022), the quotation
	 *           marks will be removed, and adjacent pairs of quotation marks within the subsequence will be replaced by
	 *           a single quotation mark.
	 * @return the next token that was extracted from the input sequence, or {@code null} if the end of the input
	 *         sequence has been reached.
	 * @throws UnclosedQuotationException
	 *           if {@code quotingEnabled} is {@code true} and the token text contains an opening quotation mark
	 *           (U+0022) that does not have a corresponding closing quotation mark.
	 */

	public Token nextToken(
		boolean	quotingEnabled)
	{
		// Test for end of input sequence
		if (endOfInput)
			return null;

		// Initialise variables
		int startIndex = sequenceIndex;
		StringBuilder buffer = new StringBuilder(128);

		// Create container for local variables
		class Vars
		{
			int        endIndex = sequenceIndex;
			TokenState state    = TokenState.TOKEN;
		}
		Vars vars = new Vars();

		// Create procedure that is invoked at end of input sequence
		IProcedure0 onEndOfInput = () ->
		{
			vars.endIndex = sequenceIndex;
			sequenceEndIndex = sequenceIndex;
			endOfInput = true;
			vars.state = TokenState.END_OF_TOKEN;
		};

		// Process input sequence with finite-state machine
		while (vars.state != TokenState.END_OF_TOKEN)
		{
			switch (vars.state)
			{
				case TOKEN:
					if (sequenceIndex < sequence.length())
					{
						char ch = sequence.charAt(sequenceIndex);
						if (endOfInputTest.test(ch))
							onEndOfInput.invoke();
						else
						{
							++sequenceIndex;
							if (tokenSeparatorTest.test(ch))
							{
								vars.endIndex = sequenceIndex - 1;
								vars.state = TokenState.END_OF_TOKEN;
							}
							else if ((ch == '"') && quotingEnabled)
								vars.state = TokenState.QUOTATION;
							else
								buffer.append(ch);
						}
					}
					else
						onEndOfInput.invoke();
					break;

				case QUOTATION:
					if (sequenceIndex < sequence.length())
					{
						char ch = sequence.charAt(sequenceIndex++);
						if (ch == '"')
							vars.state = TokenState.QUOTATION_PENDING;
						else
							buffer.append(ch);
					}
					else
						throw new UnclosedQuotationException(sequenceIndex);
					break;

				case QUOTATION_PENDING:
					if (sequenceIndex < sequence.length())
					{
						char ch = sequence.charAt(sequenceIndex);
						if (endOfInputTest.test(ch))
							onEndOfInput.invoke();
						else
						{
							++sequenceIndex;
							if (tokenSeparatorTest.test(ch))
							{
								vars.endIndex = sequenceIndex - 1;
								vars.state = TokenState.END_OF_TOKEN;
							}
							else
							{
								buffer.append(ch);
								if (ch == '"')
									vars.state = TokenState.QUOTATION;
								else
									vars.state = TokenState.TOKEN;
							}
						}
					}
					else
						onEndOfInput.invoke();
					break;

				case END_OF_TOKEN:
					// do nothing
					break;
			}
		}

		// Create token
		Token token =
				new Token(startIndex, buffer.toString(), sequence.subSequence(startIndex, vars.endIndex).toString());

		// Add token to list
		tokens.add(token);

		// Return token
		return token;
	}

	//------------------------------------------------------------------

	/**
	 * Resets this tokeniser to the start of its input sequence.
	 */

	public void reset()
	{
		sequenceIndex = 0;
		sequenceEndIndex = -1;
		endOfInput = false;
		tokens.clear();
	}

	//------------------------------------------------------------------

	/**
	 * Removes the last element from the list of tokens that have been extracted from the input sequence and sets the
	 * extraction position to the start of that token.  In effect, the operation puts the token back into the input
	 * sequence.
	 *
	 * @return {@code true} if a token was back into the input sequence.
	 */

	public boolean putBack()
	{
		boolean hasTokens = !tokens.isEmpty();
		if (hasTokens)
		{
			sequenceIndex = tokens.remove(tokens.size() - 1).index;
			endOfInput = false;
		}
		return hasTokens;
	}

	//------------------------------------------------------------------

	/**
	 * Extracts all the tokens from the start of the input sequence and returns them as a list.  Quotation marks
	 * (U+0022) have no special significance.
	 *
	 * @return a list of the tokens that were extracted from the start of the input sequence.
	 */

	public List<Token> getTokens()
	{
		return getTokens(false);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts all the tokens from the start of the input sequence and returns them as a list.
	 *
	 * @param  quotingEnabled
	 *           if {@code true} and a subsequence of the token is enclosed in quotation marks (U+0022), the quotation
	 *           marks will be removed, and adjacent pairs of quotation marks within the subsequence will be replaced by
	 *           a single quotation mark.
	 * @return a list of the tokens that were extracted from the start of the input sequence.
	 */

	public List<Token> getTokens(
		boolean	quotingEnabled)
	{
		// Reset extraction position to start of input sequence
		reset();

		// Extract all tokens from input sequence and return them
		return getRemainingTokens(quotingEnabled);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence, starting from the current extraction position (ie, from the token that
	 * would be returned by a call to {@link #nextToken()} or {@link #nextToken(boolean)}), and returns a list of the
	 * tokens.  Quotation marks (U+0022) have no special significance.
	 *
	 * @return a list of the tokens that were extracted from the input sequence, starting from the current extraction
	 *         position.
	 */

	public List<Token> getRemainingTokens()
	{
		return getRemainingTokens(false);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence, starting from the current extraction position (ie, from the token that
	 * would be returned by a call to {@link #nextToken()} or {@link #nextToken(boolean)}), and returns a list of the
	 * tokens.
	 *
	 * @param  quotingEnabled
	 *           if {@code true} and a subsequence of the token is enclosed in quotation marks (U+0022), the quotation
	 *           marks will be removed, and adjacent pairs of quotation marks within the subsequence will be replaced by
	 *           a single quotation mark.
	 * @return a list of the tokens that were extracted from the input sequence, starting from the current extraction
	 *         position.
	 */

	public List<Token> getRemainingTokens(
		boolean	quotingEnabled)
	{
		// Get index of start of remaining tokens
		int index = tokens.size();

		// Extract tokens from input sequence
		while (!endOfInput)
			nextToken(quotingEnabled);

		// Return list of tokens
		return new ArrayList<>(tokens.subList(index, tokens.size()));
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence and returns a list of the text of the tokens.  Quotation marks (U+0022)
	 * have no special significance.
	 *
	 * @return a list of the text of the tokens that were extracted from the input sequence.
	 */

	public List<String> getTokenText()
	{
		return getTokenText(false);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence and returns a list of the text of the tokens.
	 *
	 * @param  quotingEnabled
	 *           if {@code true} and a subsequence of the token is enclosed in quotation marks (U+0022), the quotation
	 *           marks will be removed, and adjacent pairs of quotation marks within the subsequence will be replaced by
	 *           a single quotation mark.
	 * @return a list of the text of the tokens that were extracted from the input sequence.
	 */

	public List<String> getTokenText(
		boolean	quotingEnabled)
	{
		// Reset extraction position to start of input sequence
		reset();

		// Extract all tokens from input sequence and return their text
		return getRemainingTokenText(quotingEnabled);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence, starting from the current extraction position (ie, from the token that
	 * would be returned by a call to {@link #nextToken()} or {@link #nextToken(boolean)}), and returns a list of the
	 * text of the tokens.  Quotation marks (U+0022) have no special significance.
	 *
	 * @return a list of the text of the tokens that were extracted from the input sequence, starting from the current
	 *         extraction position.
	 */

	public List<String> getRemainingTokenText()
	{
		return getRemainingTokenText(false);
	}

	//------------------------------------------------------------------

	/**
	 * Extracts tokens from the input sequence, starting from the current extraction position (ie, from the token that
	 * would be returned by a call to {@link #nextToken()} or {@link #nextToken(boolean)}), and returns a list of the
	 * text of the tokens.
	 *
	 * @param  quotingEnabled
	 *           if {@code true} and a subsequence of the token is enclosed in quotation marks (U+0022), the quotation
	 *           marks will be removed, and adjacent pairs of quotation marks within the subsequence will be replaced by
	 *           a single quotation mark.
	 * @return a list of the text of the tokens that were extracted from the input sequence, starting from the current
	 *         extraction position.
	 */

	public List<String> getRemainingTokenText(
		boolean	quotingEnabled)
	{
		// Get index of start of remaining tokens
		int index = tokens.size();

		// Extract tokens from input sequence
		while (!endOfInput)
			nextToken(quotingEnabled);

		// Return list of token text
		return tokens.subList(index, tokens.size()).stream().map(token -> token.text).toList();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the residual text that follows the end of the input sequence.
	 *
	 * @return the residual text that follows the end of the input sequence, or {@code null} if the end of the input
	 *         sequence has not been reached.
	 */

	public String getResidue()
	{
		return endOfInput ? sequence.subSequence(sequenceEndIndex, sequence.length()).toString() : null;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: TOKEN


	/**
	 * This class encapsulates a token that was extracted from an input sequence by a {@link Tokeniser}.
	 */

	public static class Token
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The index of the start of this token in the input sequence. */
		private	int		index;

		/** The text of this token. */
		private	String	text;

		/** The text of this token as it was found in the input sequence. */
		private	String	rawText;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of a token that was was extracted from an input sequence by a {@link Tokeniser}.
		 *
		 * @param index
		 *          the index of the start of the token in the input sequence.
		 * @param text
		 *          the text of the token.
		 * @param rawText
		 *          the text of this token as it was found in the input sequence.
		 */

		public Token(
			int		index,
			String	text,
			String	rawText)
		{
			// Initialise instance variables
			this.index = index;
			this.text = text;
			this.rawText = rawText;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the text of this token.
		 */

		@Override
		public String toString()
		{
			return text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the index of the start of this token in the input sequence.
		 *
		 * @return the index of the start of this token in the input sequence.
		 */

		public int getIndex()
		{
			return index;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the text of this token.
		 *
		 * @return the text of this token.
		 */

		public String getText()
		{
			return text;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the text of this token as it was found in the input sequence.
		 *
		 * @return the text of this token as it was found in the input sequence.
		 */

		public String getRawText()
		{
			return rawText;
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if the text of this token differs from the raw text of the token in the input sequence.
		 * Any difference is attributable to the removal of quotation marks.
		 *
		 * @return {@code true} if the text of this token differs from the raw text of the token in the input sequence.
		 */

		public boolean isModified()
		{
			return (text.length() != rawText.length());
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if this token was enclosed in quotation marks (U+0022) in the input sequence.
		 *
		 * @return {@code true} if this token was enclosed in quotation marks (U+0022) in the input sequence.
		 */

		public boolean isQuoted()
		{
			int length = text.length();
			int rawLength = rawText.length();
			return (length > 1) && (rawLength > length) && (rawText.charAt(0) == '"')
					&& (rawText.charAt(rawLength - 1) == '"');
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if the text of this token matches the specified string.
		 *
		 * @param  str
		 *           the string against which the text of this token will be matched.
		 * @return {@code true} if the text of this token matches the specified string.
		 */

		public boolean matches(
			String	str)
		{
			return text.equals(str);
		}

		//--------------------------------------------------------------

		/**
		 * Returns {@code true} if the raw text of this token matches the specified string.
		 *
		 * @param  str
		 *           the string against which the raw text of this token will be matched.
		 * @return {@code true} if the raw text of this token matches the specified string.
		 * @see    #getRawText()
		 */

		public boolean matchesRaw(
			String	str)
		{
			return rawText.equals(str);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: UNCLOSED QUOTATION EXCEPTION


	/**
	 * This class implements an exception that is thrown when a token contains an opening quotation mark (U+0022) that
	 * does not have a corresponding closing quotation mark.
	 */

	public static class UnclosedQuotationException
		extends RuntimeException
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** Miscellaneous strings. */
		private static final	String	INDEX_STR	= "Index: ";

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		/** The index of the input sequence at which the exception occurred. */
		private	int	index;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an <i>unclosed quotation</i> exception.
		 *
		 * @param index
		 *          the index of the input sequence at which the exception occurred.
		 */

		private UnclosedQuotationException(
			int	index)
		{
			// Call superclass constructor
			super(INDEX_STR + Integer.toString(index));

			// Initialise instance variables
			this.index = index;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the index of the input sequence at which the exception occurred.
		 *
		 * @return the index of the input sequence at which the exception occurred.
		 */

		public int getIndex()
		{
			return index;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
