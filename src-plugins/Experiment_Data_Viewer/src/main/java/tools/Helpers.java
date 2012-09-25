package tools;

import ij.IJ;

import java.io.File;

import java.util.Random;

public class Helpers {

	// random number generator
	private static final Random random = new Random();
	// set of alpha numeric symbols
	private static String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	// Available log levels
	public static enum LogLevel { Quiet, Standard, Verbose }
	// The current log level
	public static LogLevel logLevel = LogLevel.Verbose;

	public static void log(String msg) {
		log(msg, LogLevel.Standard);
	}

	public static void logVerbose(String msg) {
		log(msg, LogLevel.Verbose);
	}

	public static void log(String msg, LogLevel msgLevel) {
		if (logLevel == LogLevel.Quiet) {
			return;
		} else if (logLevel == LogLevel.Standard) {
			if (msgLevel == LogLevel.Standard) {
				IJ.log(msg);
			}
		} else if (logLevel == LogLevel.Verbose) {
			if (msgLevel == LogLevel.Verbose || msgLevel == LogLevel.Standard) {
				IJ.log(msg);
			}
		}
	}

	/**
	 * Combine two path elements and mimic Python's os.path.join method.
	 */
	public static String joinPath(String path1, String path2) {
		if (path1.length() == 0) {
			return path2;
		} else if (path2.length() == 2 ) {
			return path1;
		} else {
			return new File( path1, path2 ).getPath();
		}
	}

	/**
	 * Generates a random alpha numeric string of the given length.
	 */
	public static String randomAlphaNumericString(int length) {
		char[] text = new char[length];
		for (int i = 0; i < length; i++)
		{
			text[i] = alphaNumeric.charAt(random.nextInt(alphaNumeric.length()));
		}
		return new String(text);
	}
}
