package org.jipc.buffer;

import java.nio.ByteBuffer;

import org.jipc.buffer.ByteBufferQueue;

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
	 * Creates a ByteBuffer with the given size, and intitalizes it with
	 * the given byte.
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

}
