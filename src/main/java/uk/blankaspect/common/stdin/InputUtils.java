/*====================================================================*\

InputUtils.java

Class: input utility methods.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.stdin;

//----------------------------------------------------------------------


// IMPORTS


import java.io.IOException;

//----------------------------------------------------------------------


// CLASS: INPUT UTILITY METHODS


/**
 * This class defines some utility methods that read from standard input.
 */

public class InputUtils
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Prevents this class from being instantiated externally.
	 */

	private InputUtils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Repeatedly writes the specified question to standard output and reads a single-character response from standard
	 * input until the response matches one of the specified valid responses, then returns the response.  Case is
	 * when matching a response.
	 * @param  question        the question that will be written to standard output to prompt for a response.
	 * @param  validResponses  the valid reponses that will cause this method to return.
	 * @return the response (one of {@code validResponses} that was read from standard input.
	 */

	public static char readResponse(String  question,
									char... validResponses)
	{
		while (true)
		{
			try
			{
				// Clear standard input stream
				while (System.in.available() > 0)
					System.in.read();

				// Write prompt
				System.out.print(question + " ? ");

				// Read response and compare it with valid responses
				char ch = Character.toUpperCase((char)System.in.read());
				for (int i = 0; i < validResponses.length; i++)
				{
					char response = validResponses[i];
					if (Character.toUpperCase(response) == ch)
						return response;
				}
			}
			catch (IOException e)
			{
				// ignore
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * Repeatedly writes the specified question to standard output and reads a single-character response from standard
	 * input until the response matches one of the specified valid responses, then returns the response.  Case is
	 * when matching a response.
	 * @param  question        the question that will be written to standard output to prompt for a response.
	 * @param  validResponses  a string whose characters are valid reponses that will cause this method to return.
	 * @return the response (one of {@code validResponses} that was read from standard input.
	 */

	public static char readResponse(String question,
									String validResponses)
	{
		return readResponse(question, validResponses.toCharArray());
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------

