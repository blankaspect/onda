/*====================================================================*\

AppConfig.java

Application configuration class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Component;
import java.awt.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.lang.reflect.Field;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import uk.blankaspect.common.cls.ClassUtils;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.filesystem.PathnameUtils;

import uk.blankaspect.common.iff.ChunkFilter;

import uk.blankaspect.common.property.Property;
import uk.blankaspect.common.property.PropertySet;

import uk.blankaspect.common.range.IntegerRange;

import uk.blankaspect.common.ui.progress.IProgressView;

import uk.blankaspect.ui.swing.filechooser.FileChooserUtils;

import uk.blankaspect.ui.swing.font.FontEx;

import uk.blankaspect.ui.swing.text.TextRendering;

//----------------------------------------------------------------------


// APPLICATION CONFIGURATION CLASS


class AppConfig
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int		MIN_NUM_CHUNK_FILTER_IDS	= 1;
	public static final		int		MAX_NUM_CHUNK_FILTER_IDS	= 32;
	public static final		int		MAX_NUM_CHUNK_FILTERS		= 64;

	public static final		ChunkFilter[]	GENERIC_FILTERS	=
	{
		ChunkFilter.INCLUDE_ALL,
		ChunkFilter.EXCLUDE_ALL
	};
	public static final		int		MIN_CHUNK_FILTER_INDEX	= -GENERIC_FILTERS.length;

	private static final	String	CHUNK_FILTER_SEPARATORS	= "/\\,.;:|\u00A6";

	public static final		AppConfig	INSTANCE	= new AppConfig();

	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= 0;
	private static final	int		MAX_SUPPORTED_VERSION	= 0;

	private static final	String	CONFIG_DIR_KEY		= Property.APP_PREFIX + "configDir";
	private static final	String	PROPERTIES_FILENAME	= OndaApp.NAME_KEY + "-properties"
															+ AppConstants.XML_FILENAME_EXTENSION;
	private static final	String	FILENAME_STEM		= OndaApp.NAME_KEY + "-config";
	private static final	String	CONFIG_FILENAME		= FILENAME_STEM + AppConstants.XML_FILENAME_EXTENSION;
	private static final	String	CONFIG_OLD_FILENAME	= FILENAME_STEM + "-old" + AppConstants.XML_FILENAME_EXTENSION;

	private static final	String	CONFIGURATION_ERROR_STR		= "Configuration error";
	private static final	String	SAVE_CONFIGURATION_FILE_STR	= "Save configuration file";
	private static final	String	WRITING_STR					= "Writing";

	private interface Key
	{
		String	APPEARANCE					= "appearance";
		String	BLOCK_LENGTH				= "blockLength";
		String	CHARACTER_ENCODING			= "characterEncoding";
		String	CHUNK_FILTER				= "chunkFilter";
		String	COMPRESS					= "compress";
		String	COMPRESSION					= "compression";
		String	CONFIGURATION				= OndaApp.NAME_KEY + "Configuration";
		String	EXPAND						= "expand";
		String	FILTER						= "filter";
		String	FONT						= "font";
		String	GENERAL						= "general";
		String	IGNORE_FILENAME_CASE		= "ignoreFilenameCase";
		String	INDEX						= "index";
		String	LOOK_AND_FEEL				= "lookAndFeel";
		String	MAIN_WINDOW_LOCATION		= "mainWindowLocation";
		String	PATH						= "path";
		String	SELECT_TEXT_ON_FOCUS_GAINED	= "selectTextOnFocusGained";
		String	SHOW_OVERALL_PROGRESS		= "showOverallProgress";
		String	SHOW_UNIX_PATHNAMES			= "showUnixPathnames";
		String	TEXT_ANTIALIASING			= "textAntialiasing";
		String	VALIDATE					= "validate";
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	File			file;
	private	boolean			fileRead;
	private	File			selectedFile;
	private	JFileChooser	fileChooser;
	private	List<Property>	properties;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private AppConfig()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static void showWarningMessage(AppException exception)
	{
		OndaApp.INSTANCE.showWarningMessage(OndaApp.SHORT_NAME + " : " + CONFIGURATION_ERROR_STR, exception);
	}

	//------------------------------------------------------------------

	public static void showErrorMessage(AppException exception)
	{
		OndaApp.INSTANCE.showErrorMessage(OndaApp.SHORT_NAME + " : " + CONFIGURATION_ERROR_STR, exception);
	}

	//------------------------------------------------------------------

	private static File getFile()
		throws AppException
	{
		File file = null;

		// Get location of container of class file of application
		Path containerLocation = null;
		try
		{
			containerLocation = ClassUtils.getClassFileContainer(AppConfig.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Get pathname of configuration directory from properties file
		String pathname = null;
		Path propertiesFile = (containerLocation == null) ? Path.of(PROPERTIES_FILENAME)
														  : containerLocation.resolveSibling(PROPERTIES_FILENAME);
		if (Files.isRegularFile(propertiesFile, LinkOption.NOFOLLOW_LINKS))
		{
			try
			{
				Properties properties = new Properties();
				properties.loadFromXML(new FileInputStream(propertiesFile.toFile()));
				pathname = properties.getProperty(CONFIG_DIR_KEY);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_READING_PROPERTIES_FILE, propertiesFile.toFile());
			}
		}

		// Get pathname of configuration directory from system property or set system property to pathname
		try
		{
			if (pathname == null)
				pathname = System.getProperty(CONFIG_DIR_KEY);
			else
				System.setProperty(CONFIG_DIR_KEY, pathname);
		}
		catch (SecurityException e)
		{
			// ignore
		}

		// Look for configuration file in default locations
		if (pathname == null)
		{
			// Look for configuration file in local directory
			file = new File(CONFIG_FILENAME);

			// Look for configuration file in default configuration directory
			if (!file.isFile())
			{
				file = null;
				pathname = Utils.getPropertiesPathname();
				if (pathname != null)
				{
					file = new File(pathname, CONFIG_FILENAME);
					if (!file.isFile())
						file = null;
				}
			}
		}

		// Get location of configuration file from pathname of configuration directory
		else if (!pathname.isEmpty())
		{
			file = new File(PathnameUtils.parsePathname(pathname), CONFIG_FILENAME);
			if (!file.isFile())
				throw new FileException(ErrorId.NO_CONFIGURATION_FILE, file);
		}

		return file;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public File chooseFile(Component parent)
	{
		if (fileChooser == null)
		{
			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(SAVE_CONFIGURATION_FILE_STR);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			FileChooserUtils.setFilter(fileChooser, AppConstants.XML_FILE_FILTER);
			selectedFile = file;
		}

		fileChooser.setSelectedFile((selectedFile == null) ? new File(CONFIG_FILENAME).getAbsoluteFile()
														   : selectedFile.getAbsoluteFile());
		fileChooser.rescanCurrentDirectory();
		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			selectedFile = Utils.appendSuffix(fileChooser.getSelectedFile(), AppConstants.XML_FILENAME_EXTENSION);
			return selectedFile;
		}
		return null;
	}

	//------------------------------------------------------------------

	public void read()
	{
		// Read configuration file
		fileRead = false;
		ConfigFile configFile = null;
		try
		{
			file = getFile();
			if (file != null)
			{
				configFile = new ConfigFile();
				configFile.read(file);
				fileRead = true;
			}
		}
		catch (AppException e)
		{
			showErrorMessage(e);
		}

		// Get properties
		if (fileRead)
			getProperties(configFile, Property.getSystemSource());
		else
			getProperties(Property.getSystemSource());

		// Reset changed status of properties
		resetChanged();
	}

	//------------------------------------------------------------------

	public void write()
	{
		if (isChanged())
		{
			try
			{
				if (file == null)
				{
					if (System.getProperty(CONFIG_DIR_KEY) == null)
					{
						String pathname = Utils.getPropertiesPathname();
						if (pathname != null)
						{
							File directory = new File(pathname);
							if (!directory.exists() && !directory.mkdirs())
								throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
							file = new File(directory, CONFIG_FILENAME);
						}
					}
				}
				else
				{
					if (!fileRead)
						file.renameTo(new File(file.getParentFile(), CONFIG_OLD_FILENAME));
				}
				if (file != null)
				{
					write(file);
					resetChanged();
				}
			}
			catch (AppException e)
			{
				showErrorMessage(e);
			}
		}
	}

	//------------------------------------------------------------------

	public void write(File file)
		throws AppException
	{
		// Initialise progress view
		IProgressView progressView = Task.getProgressView();
		if (progressView != null)
		{
			progressView.setInfo(WRITING_STR, file);
			progressView.setProgress(0, -1.0);
		}

		// Create new DOM document
		ConfigFile configFile = new ConfigFile(Integer.toString(VERSION));

		// Set configuration properties in document
		putProperties(configFile);

		// Write file
		configFile.write(file);
	}

	//------------------------------------------------------------------

	private void getProperties(Property.ISource... propertySources)
	{
		for (Property property : getProperties())
		{
			try
			{
				property.get(propertySources);
			}
			catch (AppException e)
			{
				showWarningMessage(e);
			}
		}
	}

	//------------------------------------------------------------------

	private void putProperties(Property.ITarget propertyTarget)
	{
		for (Property property : getProperties())
			property.put(propertyTarget);
	}

	//------------------------------------------------------------------

	private boolean isChanged()
	{
		for (Property property : getProperties())
		{
			if (property.isChanged())
				return true;
		}
		return false;
	}

	//------------------------------------------------------------------

	private void resetChanged()
	{
		for (Property property : getProperties())
			property.setChanged(false);
	}

	//------------------------------------------------------------------

	private List<Property> getProperties()
	{
		if (properties == null)
		{
			properties = new ArrayList<>();
			for (Field field : getClass().getDeclaredFields())
			{
				try
				{
					if (field.getName().startsWith(Property.FIELD_PREFIX))
						properties.add((Property)field.get(this));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		return properties;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		ERROR_READING_PROPERTIES_FILE
		("An error occurred when reading the properties file."),

		NO_CONFIGURATION_FILE
		("No configuration file was found at the specified location."),

		NO_VERSION_NUMBER
		("The configuration file does not have a version number."),

		INVALID_VERSION_NUMBER
		("The version number of the configuration file is invalid."),

		UNSUPPORTED_CONFIGURATION_FILE
		("The version of the configuration file (%1) is not supported by this version of " + OndaApp.SHORT_NAME + "."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory for the configuration file.");

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

		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// FILTER LIST CLASS


	private static class FilterList
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private FilterList()
		{
			filters = new ArrayList<>();
			index = MIN_CHUNK_FILTER_INDEX;
		}

		//--------------------------------------------------------------

		private FilterList(List<ChunkFilter> filters,
						   int               index)
		{
			this.filters = filters;
			this.index = index;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof FilterList)
			{
				FilterList filterList = (FilterList)obj;
				return filters.equals(filterList.filters) && (index == filterList.index);
			}
			return false;
		}

		//--------------------------------------------------------------

		@Override
		public int hashCode()
		{
			int code = filters.hashCode();
			code = 31 * code + index;
			return code;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		List<ChunkFilter>	filters;
		int					index;

	}

	//==================================================================


	// CONFIGURATION FILE CLASS


	private static class ConfigFile
		extends PropertySet
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	String	CONFIG_FILE1_STR	= "configuration file";
		private static final	String	CONFIG_FILE2_STR	= "Configuration file";

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ConfigFile()
		{
		}

		//--------------------------------------------------------------

		private ConfigFile(String versionStr)
			throws AppException
		{
			super(Key.CONFIGURATION, null, versionStr);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String getSourceName()
		{
			return CONFIG_FILE2_STR;
		}

		//--------------------------------------------------------------

		@Override
		protected String getFileKindString()
		{
			return CONFIG_FILE1_STR;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void read(File file)
			throws AppException
		{
			// Read file
			read(file, Key.CONFIGURATION);

			// Validate version number
			String versionStr = getVersionString();
			if (versionStr == null)
				throw new FileException(ErrorId.NO_VERSION_NUMBER, file);
			try
			{
				int version = Integer.parseInt(versionStr);
				if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
					throw new FileException(ErrorId.UNSUPPORTED_CONFIGURATION_FILE, file, versionStr);
			}
			catch (NumberFormatException e)
			{
				throw new FileException(ErrorId.INVALID_VERSION_NUMBER, file);
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// PROPERTY CLASS: CHARACTER ENCODING


	private class CPCharacterEncoding
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPCharacterEncoding()
		{
			super(concatenateKeys(Key.GENERAL, Key.CHARACTER_ENCODING));
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public String getCharacterEncoding()
	{
		return cpCharacterEncoding.getValue();
	}

	//------------------------------------------------------------------

	public void setCharacterEncoding(String value)
	{
		cpCharacterEncoding.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPCharacterEncoding	cpCharacterEncoding	= new CPCharacterEncoding();

	//==================================================================


	// PROPERTY CLASS: IGNORE FILENAME CASE


	private class CPIgnoreFilenameCase
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPIgnoreFilenameCase()
		{
			super(concatenateKeys(Key.GENERAL, Key.IGNORE_FILENAME_CASE));
			value = false;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public boolean isIgnoreFilenameCase()
	{
		return cpIgnoreFilenameCase.getValue();
	}

	//------------------------------------------------------------------

	public void setIgnoreFilenameCase(boolean value)
	{
		cpIgnoreFilenameCase.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPIgnoreFilenameCase	cpIgnoreFilenameCase	= new CPIgnoreFilenameCase();

	//==================================================================


	// PROPERTY CLASS: SHOW UNIX PATHNAMES


	private class CPShowUnixPathnames
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPShowUnixPathnames()
		{
			super(concatenateKeys(Key.GENERAL, Key.SHOW_UNIX_PATHNAMES));
			value = false;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public boolean isShowUnixPathnames()
	{
		return cpShowUnixPathnames.getValue();
	}

	//------------------------------------------------------------------

	public void setShowUnixPathnames(boolean value)
	{
		cpShowUnixPathnames.setValue(value);
	}

	//------------------------------------------------------------------

	public void addShowUnixPathnamesObserver(Property.IObserver observer)
	{
		cpShowUnixPathnames.addObserver(observer);
	}

	//------------------------------------------------------------------

	public void removeShowUnixPathnamesObserver(Property.IObserver observer)
	{
		cpShowUnixPathnames.removeObserver(observer);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPShowUnixPathnames	cpShowUnixPathnames	= new CPShowUnixPathnames();

	//==================================================================


	// PROPERTY CLASS: SELECT TEXT ON FOCUS GAINED


	private class CPSelectTextOnFocusGained
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPSelectTextOnFocusGained()
		{
			super(concatenateKeys(Key.GENERAL, Key.SELECT_TEXT_ON_FOCUS_GAINED));
			value = true;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public boolean isSelectTextOnFocusGained()
	{
		return cpSelectTextOnFocusGained.getValue();
	}

	//------------------------------------------------------------------

	public void setSelectTextOnFocusGained(boolean value)
	{
		cpSelectTextOnFocusGained.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPSelectTextOnFocusGained	cpSelectTextOnFocusGained	= new CPSelectTextOnFocusGained();

	//==================================================================


	// PROPERTY CLASS: MAIN WINDOW LOCATION


	private class CPMainWindowLocation
		extends Property.SimpleProperty<Point>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPMainWindowLocation()
		{
			super(concatenateKeys(Key.GENERAL, Key.MAIN_WINDOW_LOCATION));
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input input)
			throws AppException
		{
			if (input.getValue().isEmpty())
				value = null;
			else
			{
				int[] outValues = input.parseIntegers(2, null);
				value = new Point(outValues[0], outValues[1]);
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString()
		{
			return (value == null) ? "" : value.x + ", " + value.y;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public boolean isMainWindowLocation()
	{
		return (getMainWindowLocation() != null);
	}

	//------------------------------------------------------------------

	public Point getMainWindowLocation()
	{
		return cpMainWindowLocation.getValue();
	}

	//------------------------------------------------------------------

	public void setMainWindowLocation(Point value)
	{
		cpMainWindowLocation.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPMainWindowLocation	cpMainWindowLocation	= new CPMainWindowLocation();

	//==================================================================


	// PROPERTY CLASS: LOOK-AND-FEEL


	private class CPLookAndFeel
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPLookAndFeel()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.LOOK_AND_FEEL));
			value = "";
			for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels())
			{
				if (lookAndFeelInfo.getClassName().
											equals(UIManager.getCrossPlatformLookAndFeelClassName()))
				{
					value = lookAndFeelInfo.getName();
					break;
				}
			}
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public String getLookAndFeel()
	{
		return cpLookAndFeel.getValue();
	}

	//------------------------------------------------------------------

	public void setLookAndFeel(String value)
	{
		cpLookAndFeel.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPLookAndFeel	cpLookAndFeel	= new CPLookAndFeel();

	//==================================================================


	// PROPERTY CLASS: TEXT ANTIALIASING


	private class CPTextAntialiasing
		extends Property.EnumProperty<TextRendering.Antialiasing>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPTextAntialiasing()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.TEXT_ANTIALIASING),
				  TextRendering.Antialiasing.class);
			value = TextRendering.Antialiasing.DEFAULT;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public TextRendering.Antialiasing getTextAntialiasing()
	{
		return cpTextAntialiasing.getValue();
	}

	//------------------------------------------------------------------

	public void setTextAntialiasing(TextRendering.Antialiasing value)
	{
		cpTextAntialiasing.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPTextAntialiasing	cpTextAntialiasing	= new CPTextAntialiasing();

	//==================================================================


	// PROPERTY CLASS: SHOW OVERALL PROGRESS


	private class CPShowOverallProgress
		extends Property.BooleanProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPShowOverallProgress()
		{
			super(concatenateKeys(Key.APPEARANCE, Key.SHOW_OVERALL_PROGRESS));
			value = true;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public boolean isShowOverallProgress()
	{
		return cpShowOverallProgress.getValue();
	}

	//------------------------------------------------------------------

	public void setShowOverallProgress(boolean value)
	{
		cpShowOverallProgress.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPShowOverallProgress	cpShowOverallProgress	= new CPShowOverallProgress();

	//==================================================================


	// PROPERTY CLASS: COMPRESSION BLOCK LENGTH


	private class CPBlockLength
		extends Property.IntegerProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPBlockLength()
		{
			super(concatenateKeys(Key.COMPRESSION, Key.BLOCK_LENGTH),
				  OndaFile.MIN_BLOCK_LENGTH, OndaFile.MAX_BLOCK_LENGTH);
			value = OndaFile.DEFAULT_BLOCK_LENGTH;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public int getBlockLength()
	{
		return cpBlockLength.getValue();
	}

	//------------------------------------------------------------------

	public void setBlockLength(int value)
	{
		cpBlockLength.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPBlockLength	cpBlockLength	= new CPBlockLength();

	//==================================================================


	// PROPERTY CLASS: PATHNAME OF COMPRESSION DIRECTORY


	private class CPCompressPathname
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPCompressPathname()
		{
			super(concatenateKeys(Key.PATH, Key.COMPRESS));
			value = PathnameUtils.USER_HOME_PREFIX;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public String getCompressPathname()
	{
		return cpCompressPathname.getValue();
	}

	//------------------------------------------------------------------

	public File getCompressDirectory()
	{
		return new File(PathnameUtils.parsePathname(getCompressPathname()));
	}

	//------------------------------------------------------------------

	public void setCompressPathname(String value)
	{
		cpCompressPathname.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPCompressPathname	cpCompressPathname	= new CPCompressPathname();

	//==================================================================


	// PROPERTY CLASS: PATHNAME OF EXPANSION DIRECTORY


	private class CPExpandPathname
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPExpandPathname()
		{
			super(concatenateKeys(Key.PATH, Key.EXPAND));
			value = PathnameUtils.USER_HOME_PREFIX;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public String getExpandPathname()
	{
		return cpExpandPathname.getValue();
	}

	//------------------------------------------------------------------

	public File getExpandDirectory()
	{
		return new File(PathnameUtils.parsePathname(getExpandPathname()));
	}

	//------------------------------------------------------------------

	public void setExpandPathname(String value)
	{
		cpExpandPathname.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPExpandPathname	cpExpandPathname	= new CPExpandPathname();

	//==================================================================


	// PROPERTY CLASS: PATHNAME OF VALIDATION DIRECTORY


	private class CPValidatePathname
		extends Property.StringProperty
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPValidatePathname()
		{
			super(concatenateKeys(Key.PATH, Key.VALIDATE));
			value = PathnameUtils.USER_HOME_PREFIX;
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public String getValidatePathname()
	{
		return cpValidatePathname.getValue();
	}

	//------------------------------------------------------------------

	public File getValidateDirectory()
	{
		return new File(PathnameUtils.parsePathname(getValidatePathname()));
	}

	//------------------------------------------------------------------

	public void setValidatePathname(String value)
	{
		cpValidatePathname.setValue(value);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPValidatePathname	cpValidatePathname	= new CPValidatePathname();

	//==================================================================


	// PROPERTY CLASS: CHUNK FILTERS


	private class CPChunkFilters
		extends Property.PropertyMap<AudioFileKind, FilterList>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPChunkFilters()
		{
			super(Key.CHUNK_FILTER, AudioFileKind.class);
			for (AudioFileKind fileKind : AudioFileKind.values())
				values.put(fileKind, new FilterList());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input         input,
						  AudioFileKind fileKind)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public String toString(AudioFileKind fileKind)
		{
			return null;
		}

		//--------------------------------------------------------------

		@Override
		protected void getEntry(Property.ISource[] sources,
								AudioFileKind      fileKind)
		{
			try
			{
				// Filters
				for (int i = 0; i < MAX_NUM_CHUNK_FILTERS; i++)
				{
					Input input = Input.create(sources, getFilterKey(fileKind, i));
					if (input != null)
					{
						try
						{
							ChunkFilter filter = new ChunkFilter(input.getValue());
							int numIds = filter.getNumIds();
							if ((numIds < MIN_NUM_CHUNK_FILTER_IDS) ||
								 (numIds > MAX_NUM_CHUNK_FILTER_IDS))
								throw new IllegalValueException(input);
							getValue(fileKind).filters.add(filter);
						}
						catch (IllegalArgumentException e)
						{
							throw new IllegalValueException(input);
						}
					}
				}

				// Index
				Input input = Input.create(sources, getKey(fileKind, Key.INDEX));
				if (input != null)
				{
					int maxIndex = getValue(fileKind).filters.size() - 1;
					getValue(fileKind).index =
								input.parseInteger(new IntegerRange(MIN_CHUNK_FILTER_INDEX, maxIndex));
				}
			}
			catch (AppException e)
			{
				showWarningMessage(e);
			}
		}

		//--------------------------------------------------------------

		@Override
		protected boolean putEntry(Property.ITarget target,
								   AudioFileKind    fileKind)
		{
			// Filters
			boolean result = true;
			for (int i = 0; i < getValue(fileKind).filters.size(); i++)
			{
				if (!target.putProperty(getFilterKey(fileKind, i),
										getValue(fileKind).filters.get(i).
																getKeyValue(CHUNK_FILTER_SEPARATORS)))
					result = false;
			}

			// Index
			if (!target.putProperty(getKey(fileKind, Key.INDEX),
									Integer.toString(getValue(fileKind).index)))
				result = false;

			return result;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private String getKey(AudioFileKind fileKind,
							  String        key)
		{
			return concatenateKeys(this.key, fileKind.getKey(), key);
		}

		//--------------------------------------------------------------

		private String getFilterKey(AudioFileKind fileKind,
									int           index)
		{
			return concatenateKeys(getKey(fileKind, Key.FILTER), indexToKey(index));
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public List<ChunkFilter> getChunkFilters(AudioFileKind fileKind)
	{
		return Collections.unmodifiableList(cpChunkFilters.getValue(fileKind).filters);
	}

	//------------------------------------------------------------------

	public ChunkFilter getChunkFilter(AudioFileKind fileKind)
	{
		List<ChunkFilter> filters = cpChunkFilters.getValue(fileKind).filters;
		int index = cpChunkFilters.getValue(fileKind).index;
		return (index < 0)
					? GENERIC_FILTERS[index - MIN_CHUNK_FILTER_INDEX]
					: (index < filters.size())
							? filters.get(index)
							: null;
	}

	//------------------------------------------------------------------

	public void setFilterList(AudioFileKind     fileKind,
							  List<ChunkFilter> filters,
							  int               index)
	{
		cpChunkFilters.setValue(fileKind, new FilterList(filters, index));
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPChunkFilters	cpChunkFilters	= new CPChunkFilters();

	//==================================================================


	// PROPERTY CLASS: FONTS


	private class CPFonts
		extends Property.PropertyMap<AppFont, FontEx>
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private CPFonts()
		{
			super(Key.FONT, AppFont.class);
			for (AppFont font : AppFont.values())
				values.put(font, font.getFontEx().clone());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public void parse(Input   input,
						  AppFont appFont)
		{
			try
			{
				FontEx font = new FontEx(input.getValue());
				appFont.setFontEx(font);
				values.put(appFont, font);
			}
			catch (IllegalArgumentException e)
			{
				showWarningMessage(new IllegalValueException(input));
			}
			catch (uk.blankaspect.common.exception2.ValueOutOfBoundsException e)
			{
				showWarningMessage(new ValueOutOfBoundsException(input));
			}
		}

		//--------------------------------------------------------------

		@Override
		public String toString(AppFont appFont)
		{
			return getValue(appFont).toString();
		}

		//--------------------------------------------------------------

	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance methods : associated methods in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	public FontEx getFont(int index)
	{
		return cpFonts.getValue(AppFont.values()[index]);
	}

	//------------------------------------------------------------------

	public void setFont(int    index,
						FontEx font)
	{
		cpFonts.setValue(AppFont.values()[index], font);
	}

	//------------------------------------------------------------------

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  Instance variables : associated variables in enclosing class
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

	private	CPFonts	cpFonts	= new CPFonts();

	//==================================================================

}

//----------------------------------------------------------------------
