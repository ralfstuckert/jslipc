package org.jipc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.jipc.buffer.ByteBufferQueue;
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

	public static void writeToFile(final File file, final String text)
			throws IOException {
		FileWriter writer = new FileWriter(file, true);
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
		return createDirectory(getTempDir());
	}

	public static File createDirectory(final File parent) {
		File file = null;
		do {
			String name = UUID.randomUUID().toString();
			file = new File(parent, name);
		} while (!file.mkdir());
		return file;
	}
}
