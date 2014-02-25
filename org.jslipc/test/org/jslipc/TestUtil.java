package org.jslipc;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jslipc.channel.buffer.ByteBufferQueue;
import org.jslipc.util.FileUtil;
import org.junit.Assert;

/**
 * Some helper methods for testing.
 */
public class TestUtil {

	public static ByteBufferQueue createByteBufferQueue(final int size) {
		return new ByteBufferQueue(createByteBuffer(size), 0, size);
	}

	/**
	 * Creates a ByteBuffer with the given size, and intitalizes it with
	 * <code>0xff</code>.
	 * 
	 * @param size
	 * @return the allocated buffer.
	 */
	public static ByteBuffer createByteBuffer(final int size) {
		return createByteBuffer(size, (byte) 0xff);
	}

	/**
	 * Creates a ByteBuffer with the given size, and intitalizes it with the
	 * given byte.
	 * 
	 * @param size
	 * @param init
	 * @return the allocated buffer.
	 */
	public static ByteBuffer createByteBuffer(final int size, final byte init) {
		ByteBuffer buffer = ByteBuffer.allocate(size);
		for (int i = 0; i < size; i++) {
			buffer.put(i, init);
		}
		return buffer;
	}

	/**
	 * Wraps the given String into a ByteBuffer using String.getBytes().
	 * @param text
	 * @return the wrapping buffer.
	 */
	public static ByteBuffer toBuffer(final String text) {
		return ByteBuffer.wrap(text.getBytes());
	}
	


	public static void appendToFile(final File file, final String text)
			throws IOException {
		FileWriter writer = new FileWriter(file, true);
		writer.append(text);
		writer.close();
	}

	public static void writeNewFile(final File file, final String text)
			throws IOException {
		if (file.exists()) {
			assertTrue(file.delete());
		}
		assertTrue(file.createNewFile());
		FileWriter writer = new FileWriter(file);
		writer.append(text);
		writer.close();
	}

	public static String readFile(final File file) throws IOException {
		StringBuilder bob = new StringBuilder();
		FileReader reader = new FileReader(file);
		int data = 0;
		while ((data = reader.read()) != -1) {
			bob.append((char) data);
		}
		reader.close();
		return bob.toString();
	}

	public static void assertEquals(final CharSequence expected,
			final ByteBuffer buffer) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int index = 0; index < buffer.position(); ++index) {
			baos.write(buffer.get(index));
		}
		Assert.assertEquals(expected, new String(baos.toByteArray()));
	}

	public static void assertEquals(final CharSequence expected, final File file)
			throws IOException {
		Assert.assertEquals(expected, readFile(file));
	}

	public static File getTempDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	public static File createDirectory() {
		return FileUtil.createDirectory(getTempDir());
	}

	public static String getTestClassPath() {
		String cp = System.getProperty("java.class.path");
		String pathSepatator = File.pathSeparator;
		// filter logging implementation since it log output may block on system console
		int slfIndex = cp.indexOf("slf4j-simple");
		if (slfIndex >= 0) {
			int startIndex = cp.substring(0, slfIndex).lastIndexOf(pathSepatator);
			if (startIndex < 0) {
				startIndex = 0;
			}
			int endIndex = cp.substring(slfIndex).indexOf(pathSepatator);
			if (endIndex < 0) {
				endIndex = cp.length()-1;
			} else {
				endIndex += slfIndex;
			}
			cp = cp.substring(0, startIndex) + pathSepatator + cp.substring(endIndex);
		}
		return cp;
	}
	
	public static String getJvm() {
		return System.getProperty("java.home") + "/bin/java";
	}
}
