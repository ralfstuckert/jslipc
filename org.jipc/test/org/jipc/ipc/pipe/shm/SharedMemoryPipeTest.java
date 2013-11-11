package org.jipc.ipc.pipe.shm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.ipc.pipe.AbstractTestProducer;
import org.jipc.ipc.pipe.shm.SharedMemoryPipe;
import org.junit.Before;
import org.junit.Test;

public class SharedMemoryPipeTest {

	@Before
	public void setUp() throws Exception {
	}

	protected File createFile() throws Exception {
		File file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		return file;
	}

	@Test
	public void testSharedMemoryPipeFile() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				JipcRole.Yang);
		checkQueueBounds(pipe, 4096, JipcRole.Yang);

		pipe = new SharedMemoryPipe(createFile(), JipcRole.Yin);
		checkQueueBounds(pipe, 4096, JipcRole.Yin);
	}

	@Test
	public void testSharedMemoryPipeFileInt() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Yang);
		checkQueueBounds(pipe, 1789, JipcRole.Yang);

		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Yin);
		checkQueueBounds(pipe, 1789, JipcRole.Yin);
	}

	@Test
	public void testClose() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Yang);
		assertNotNull(pipe.source());
		assertNotNull(pipe.sink());

		pipe.close();
		assertFalse(pipe.source().isOpen());
		assertFalse(pipe.sink().isOpen());
	}
	
	@Test
	public void testCleanUpOnCloseStillUsedByPeer() throws Exception {
		File file = createFile();
		SharedMemoryPipe pipe = new SharedMemoryPipe(file,
				1789, JipcRole.Yang);
		assertNotNull(pipe.source());
		assertNotNull(pipe.sink());

		pipe.cleanUpOnClose();
		pipe.close();
		assertTrue(file.exists());
	}


	@Test
	public void testCleanUpOnClose() throws Exception {
		File file = createFile();
		SharedMemoryPipe pipe = new SharedMemoryPipe(file,
				1789, JipcRole.Yang);
		SharedMemoryPipe peer = new SharedMemoryPipe(file,
				1789, JipcRole.Yin);
		assertNotNull(peer.source());
		assertNotNull(peer.sink());
		assertNotNull(pipe.source());
		assertNotNull(pipe.sink());

		pipe.cleanUpOnClose();

		peer.close();
		assertFalse(peer.source().isOpen());
		assertFalse(peer.sink().isOpen());

		pipe.close();
		assertFalse(file.exists());
	}



	private void checkQueueBounds(SharedMemoryPipe pipe, int size, JipcRole role) {
		int bufferSize = size / 2;
		if (role == JipcRole.Yang) {
			assertNotNull(pipe.getInQueue());
			assertNotNull(pipe.getOutQueue());
			assertEquals(bufferSize, pipe.getInQueue().getStartIndex());
			assertEquals(2 * bufferSize - 1, pipe.getInQueue().getEndIndex());
			assertEquals(0, pipe.getOutQueue().getStartIndex());
			assertEquals(bufferSize - 1, pipe.getOutQueue().getEndIndex());
		} else {
			assertNotNull(pipe.getInQueue());
			assertNotNull(pipe.getOutQueue());
			assertEquals(0, pipe.getInQueue().getStartIndex());
			assertEquals(bufferSize - 1, pipe.getInQueue().getEndIndex());
			assertEquals(bufferSize, pipe.getOutQueue().getStartIndex());
			assertEquals(2 * bufferSize - 1, pipe.getOutQueue().getEndIndex());
		}
	}

	@Test
	public void testWhoLocksInitializes() throws Exception {
		checkWhoLocksInitializes(JipcRole.Yang);
		checkWhoLocksInitializes(JipcRole.Yin);
	}

	@SuppressWarnings("resource")
	protected void checkWhoLocksInitializes(JipcRole role) throws IOException,
			FileNotFoundException {
		File file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		SharedMemoryPipe pipe = new SharedMemoryPipe(file, 1789, role);
		assertTrue(pipe.getInQueue().isInitialized());
		assertTrue(pipe.getOutQueue().isInitialized());

		file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
		channel.lock();
		pipe = new SharedMemoryPipe(file, 1789, role);
		assertFalse(pipe.getInQueue().isInitialized());
		assertFalse(pipe.getOutQueue().isInitialized());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSource() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Yang);
		assertNotNull(pipe.source());
		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Yin);
		assertNotNull(pipe.source());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSink() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Yang);
		assertNotNull(pipe.sink());
		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Yin);
		assertNotNull(pipe.sink());
	}

	@Test(timeout=20000)
	public void testIpc() throws Exception {
		File file = createFile();
		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						SMPipeTestProducer.class.getName(), file.getAbsolutePath() });
		Thread.sleep(1000);
		SMPipeTestConsumer consumer = new SMPipeTestConsumer();
		JipcPipe pipe = consumer.createPipe(file);
		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);
		producer.waitFor();
	}

}
