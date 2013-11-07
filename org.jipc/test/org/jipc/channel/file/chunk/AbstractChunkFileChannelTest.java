package org.jipc.channel.file.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jipc.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Common base class for chunk file channels.
 */
public abstract class AbstractChunkFileChannelTest {

	protected final static int SIZE = 100;

	protected File directory;
	protected ByteBuffer buffer;
	protected File closedMarker;

	@Before
	public void setUp() throws Exception {
		directory = TestUtil.createDirectory();
		directory.deleteOnExit();
		buffer = TestUtil.createByteBuffer(SIZE);
		closedMarker = new File(directory, ".closed");
		closedMarker.deleteOnExit();
	}

	@After
	public void tearDown() throws Exception {
		if (closedMarker.exists()) {
			assertTrue(closedMarker.delete());
		}
	}

	protected abstract AbstractChunkFileChannel createChannel(
			final File directory);

	@Test
	public void testClose() throws Exception {
		assertFalse(closedMarker.exists());
		AbstractChunkFileChannel channel = createChannel(directory);
		channel.close();
		assertTrue(closedMarker.exists());
	}

	@Test
	public void testIsOpen() throws Exception {
		AbstractChunkFileChannel channel = createChannel(directory);
		assertEquals(true, channel.isOpen());

		channel.close();
		assertEquals(false, channel.isOpen());
	}

	@Test
	public void testCleanUpOnCloseStillUsedByPeer() throws Exception {
		AbstractChunkFileChannel channel = createChannel(directory);
		channel.cleanUpOnClose();
		channel.close();
		String[] files = directory.list();
		assertTrue("expected close marker " + Arrays.asList(files),
				files != null && files.length == 1);
	}

	@Test
	public void testCleanUpOnClose() throws Exception {
		AbstractChunkFileChannel channel = createChannel(directory);
		channel.cleanUpOnClose();
		assertTrue(closedMarker.createNewFile());
		
		createChunkFile(0, "herbert");		
		createChunkFile(1, "karl");		
		createChunkFile(2, "ilse");		
		
		channel.close();
		assertFalse("expected " + directory + " to be deleted", directory.exists());
		String[] files = directory.list();
		assertTrue("not deleted: " + toString(files),
				files == null || files.length == 0);
	}
	
	protected String toString(Object[] array) {
		if (array == null) {
			return "null";
		}
		return Arrays.asList(array).toString();
	}

	protected File createChunkFile(final int index) throws Exception {
		return createChunkFile(index, null);
	}

	protected File createChunkFile(final int index, final String content)
			throws Exception {
		File chunk = new File(directory,
				AbstractChunkFileChannel.CHUNK_FILE_NAME + "_" + index);
		assertTrue(chunk.createNewFile());
		chunk.deleteOnExit();
		if (content != null) {
			TestUtil.appendToFile(chunk, content);
		}
		return chunk;
	}

}
