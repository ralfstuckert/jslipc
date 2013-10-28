package org.jipc.channel.buffer;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

/**
 * Utility class for handling buffers.
 */
public final class BufferUtil {
	private BufferUtil() {
		// utility classes should have private constructor.
	}

	/**
	 * Due to a weakness in the implementation in mapped byte buffers on windows, the underlying
	 * file cannot be deleted even if all channels/files are closed. As a workaround, this method
	 * tries to call sun.misc.Cleaner.clean() in order to free mapped buffer.
	 * 
	 * @param buffer
	 * @throws Exception
	 */
	public static void releaseBuffer(MappedByteBuffer buffer) throws Exception {
		if (buffer == null) {
			return;
		}
		Method cleaner = buffer.getClass().getMethod("cleaner");
		cleaner.setAccessible(true);
		Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
		clean.setAccessible(true);
		clean.invoke(cleaner.invoke(buffer));
		buffer = null;
		System.gc();
	}

	/**
	 * Calls {@link #releaseBuffer(MappedByteBuffer)} and silently catches all exceptions.
	 * @param buffer
	 */
	public static void releaseBufferSilently(MappedByteBuffer buffer) {
		try {
			releaseBuffer(buffer);
		} catch (Exception e) {
			// TODO log
			e.printStackTrace();
		}
	}

}
