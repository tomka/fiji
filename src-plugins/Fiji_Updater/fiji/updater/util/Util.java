package fiji.updater.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.DecimalFormat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/*
 * Class functionality:
 * Extend from it if you need to
 * - Calculate timestamps of files
 * - Calculate the checksums of files
 * - Get the absolute path (prefix()) of Fiji main directory
 * - Copy a file over to a particular location
 * - Get details of the Operating System Fiji application is on
 */
public class Util {
	public static String MAIN_URL = "http://fiji.sc/update/";
	public static String UPDATE_DIRECTORY = "/var/www/update/";
	public static String SSH_HOST = "fiji.sc";

	public static final String XML_COMPRESSED = "db.xml.gz";

	// Key names for ij.Prefs for saved values
	// Note: ij.Prefs is only saved during shutdown of Fiji
	public static final String PREFS_USER = "fiji.updater.login";

	public final static boolean useMacPrefix;
	public final static String macPrefix = "Contents/MacOS/";

	public final static String ijRoot, platform;
	public final static boolean isDeveloper;
	public final static String[] platforms, launchers;
	protected final static Set<String> updateablePlatforms;

	static {
		String property = System.getProperty("ij.dir");
		ijRoot = property != null ? property + File.separator :
			new Util().getClass().getResource("Util.class")
			.toString().replace("jar:file:", "")
			.replace("plugins/Fiji_Updater.jar!/"
				+ "fiji/updater/util/Util.class", "");
		isDeveloper = new File(ijRoot + "/ImageJ.c").exists();
		platform = getPlatform();

		String macLauncher = macPrefix + "ImageJ-macosx";
		useMacPrefix = platform.equals("macosx") &&
			new File(ijRoot + "/" + macLauncher).exists();

		String[] list = {
			"linux32", "linux64", "macosx", "tiger", "win32", "win64"
		};

		Arrays.sort(list);
		platforms = list.clone();
		for (int i = 0; i < list.length; i++)
			list[i] = "ImageJ-" + list[i] +
				(list[i].startsWith("win") ? ".exe" : "");
		launchers = list.clone();

		updateablePlatforms = new HashSet<String>();
		updateablePlatforms.add(platform);
		if (new File(ijRoot, macLauncher).exists())
			updateablePlatforms.add("macosx");
		String[] files = new File(ijRoot).list();
		for (String name : files == null ? new String[0] : files)
			if (name.startsWith("ImageJ-")) {
				name = name.substring(5);
				if (name.endsWith(".exe"))
					name = name.substring(0, name.length() - 4);
				updateablePlatforms.add(name);
			}
	}

	private Util() {} // make sure this class is not instantiated

	public static String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public static String stripPrefix(String string, String prefix) {
		if (!string.startsWith(prefix))
			return string;
		if (useMacPrefix && string.startsWith(prefix + macPrefix))
			return string.substring((prefix + macPrefix).length());
		return string.substring(prefix.length());
	}

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "32");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		//System.err.println("Unknown platform: " + osName);
		return osName.toLowerCase();
	}

	//get digest of the file as according to fullPath
	public static String getDigest(String path, String fullPath)
			throws NoSuchAlgorithmException, FileNotFoundException,
			IOException, UnsupportedEncodingException {
		if (path.endsWith(".jar"))
			return getJarDigest(fullPath);
		MessageDigest digest = getDigest();
		digest.update(path.getBytes("ASCII"));
		if (fullPath != null)
			updateDigest(new FileInputStream(fullPath), digest);
		return toHex(digest.digest());
	}

	public static MessageDigest getDigest()
			throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-1");
	}

	public static void updateDigest(InputStream input, MessageDigest digest)
			throws IOException {
		byte[] buffer = new byte[65536];
		DigestInputStream digestStream =
			new DigestInputStream(input, digest);
		while (digestStream.read(buffer) >= 0)
			; /* do nothing */
		digestStream.close();
	}

	public final static char[] hex = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public static String toHex(byte[] bytes) {
		char[] buffer = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			buffer[i * 2] = hex[(bytes[i] & 0xf0) >> 4];
			buffer[i * 2 + 1] = hex[bytes[i] & 0xf];
		}
		return new String(buffer);
	}

	public static String getJarDigest(String path)
			throws FileNotFoundException, IOException {
		MessageDigest digest = null;
		try {
			digest = getDigest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		if (path != null) {
			JarFile jar = new JarFile(path);
			List<JarEntry> list = Collections.list(jar.entries());
			Collections.sort(list, new JarEntryComparator());

			for (JarEntry entry : list) {
				digest.update(entry.getName().getBytes("ASCII"));
				updateDigest(jar.getInputStream(entry), digest);
			}
		}
		return toHex(digest.digest());
	}

	private static class JarEntryComparator implements Comparator<JarEntry> {
		public int compare(JarEntry entry1, JarEntry entry2) {
			String name1 = entry1.getName();
			String name2 = entry2.getName();
			return name1.compareTo(name2);
		}

		public boolean equals(JarEntry entry1, JarEntry entry2) {
			String name1 = entry1.getName();
			String name2 = entry2.getName();
			return name1.equals(name2);
		}
	}

	//Gets the location of specified file when inside of saveDirectory
	public static String prefix(String saveDirectory, String filename) {
		return prefix(saveDirectory + File.separator + filename);
	}

	public static long getTimestamp(String filename) {
		String fullPath = prefix(filename);
		long modified = new File(fullPath).lastModified();
		return Long.parseLong(timestamp(modified));
	}

	public static String timestamp(long millis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(millis);
		return timestamp(date);
	}

	public static String timestamp(Calendar date) {
		DecimalFormat format = new DecimalFormat("00");
		int month = date.get(Calendar.MONTH) + 1;
		int day = date.get(Calendar.DAY_OF_MONTH);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int minute = date.get(Calendar.MINUTE);
		int second = date.get(Calendar.SECOND);
		return "" + date.get(Calendar.YEAR) +
			format.format(month) + format.format(day) +
			format.format(hour) + format.format(minute) +
			format.format(second);
	}

	public static long timestamp2millis(long timestamp) {
		return timestamp2millis("" + timestamp);
	}

	public static long timestamp2millis(String timestamp) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Integer.parseInt(timestamp.substring(0, 4)),
			Integer.parseInt(timestamp.substring(4, 6)) - 1,
			Integer.parseInt(timestamp.substring(6, 8)),
			Integer.parseInt(timestamp.substring(8, 10)),
			Integer.parseInt(timestamp.substring(10, 12)),
			Integer.parseInt(timestamp.substring(12, 14)));
		return calendar.getTimeInMillis();
	}

	public static long getFilesize(String filename) {
		return new File(prefix(filename)).length();
	}

	public static String getDigest(String filename) {
		try {
			return getDigest(filename, prefix(filename));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String prefix(String path) {
		if (new File(path).isAbsolute())
			return path;
		if (useMacPrefix && path.startsWith(macPrefix))
			path = path.substring(macPrefix.length());
		if (File.separator.equals("\\"))
			path = path.replace("\\", "/");
		return ijRoot + (isLauncher(path) ?
				(isDeveloper ? "precompiled/" :
				 (useMacPrefix ? macPrefix : "")) : "") + path;
	}

	public static String prefixUpdate(String path) {
		return prefix("update/" + path);
	}

	public static boolean fileExists(String filename) {
		return new File(prefix(filename)).exists();
	}

	public static boolean isLauncher(String filename) {
		return Arrays.binarySearch(launchers,
				stripPrefix(stripPrefix(filename, ijRoot), "precompiled/")) >= 0;
	}

	public static String[] getLaunchers() {
		if (platform.equals("macosx"))
			return new String[] { "ImageJ-macosx", "ImageJ-tiger" };

		int index = Arrays.binarySearch(launchers, "ImageJ-" + platform);
		if (index < 0)
			index = -1 - index;
		return new String[] { launchers[index] };
	}

	public static boolean isUpdateablePlatform(String platform) {
		return updateablePlatforms.contains(platform);
	}

	public static boolean isMacOSX() {
		return platform.equals("macosx");
	}

	public static<T> String join(String delimiter, Iterable<T> list) {
		StringBuilder builder = new StringBuilder();
		for (T object : list)
			builder.append((builder.length() > 0 ? ", " : "")
				+ object.toString());
		return builder.toString();
	}

	public static void useSystemProxies() {
		System.setProperty("java.net.useSystemProxies", "true");
	}
}
