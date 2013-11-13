package org.jipc.channel.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jipc.TestUtil;
import org.jipc.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractJipcFileChannelTest {

	protected final static int SIZE = 100;

	protected File file;
	protected File closedMarker;
	protected ByteBuffer buffer;

	protected abstract AbstractJipcFileChannel createChannel(final File file) throws IOException;
	
	@Before
	public void setUp() throws Exception {
		file = File.createTempFile("test", ".txt");
		closedMarker = new File(file.getAbsolutePath() + ".closed");
		buffer = TestUtil.createByteBuffer(SIZE);
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(closedMarker);
	}


	@Test
	public void testClose() throws Exception {
		assertFalse(closedMarker.exists());
		AbstractJipcFileChannel channel = createChannel(file);
		channel.close();
		assertTrue(closedMarker.exists());
	}

	@Test
	public void testIsOpen() throws Exception {
		AbstractJipcFileChannel channel = createChannel(file);
		assertEquals(true, channel.isOpen());

		channel.close();
		assertEquals(false, channel.isOpen());
	}


	@Test
	public void testCleanUpOnCloseStillUsedByPeer() throws Exception {
		AbstractJipcFileChannel channel = createChannel(file);
		channel.cleanUpOnClose();
		channel.close();
		assertTrue(file.exists());
	}

	@Test
	public void testCleanUpOnClose() throws Exception {
		AbstractJipcFileChannel channel = createChannel(file);
		channel.cleanUpOnClose();
		assertTrue(closedMarker.createNewFile());
		channel.close();
		assertFalse(file.exists());
		assertFalse(closedMarker.exists());
	}

	@Test
	public void testCloseReleaseFiles() throws Exception {
		AbstractJipcFileChannel channel = createChannel(file);
		channel.close();
		assertTrue(file.exists());
		assertTrue(file.delete());
	}
}
