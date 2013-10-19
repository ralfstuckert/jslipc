package org.jipc.buffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

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

	
	public static void assertEquals(final CharSequence expected, final ByteBuffer buffer) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int index=0; index<buffer.position();++index) {
			baos.write(buffer.get(index));
		}
		Assert.assertEquals( expected, new String(baos.toByteArray()));
	}
}
