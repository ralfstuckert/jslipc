package org.jipc.ipc.shm;

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
import org.jipc.ipc.AbstractTestProducer;
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
				JipcRole.Client);
		checkQueueBounds(pipe, 4096, JipcRole.Client);

		pipe = new SharedMemoryPipe(createFile(), JipcRole.Server);
		checkQueueBounds(pipe, 4096, JipcRole.Server);
	}

	@Test
	public void testSharedMemoryPipeFileInt() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Client);
		checkQueueBounds(pipe, 1789, JipcRole.Client);

		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Server);
		checkQueueBounds(pipe, 1789, JipcRole.Server);
	}

	private void checkQueueBounds(SharedMemoryPipe pipe, int size, JipcRole role) {
		int bufferSize = size / 2;
		if (role == JipcRole.Client) {
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
		checkWhoLocksInitializes(JipcRole.Client);
		checkWhoLocksInitializes(JipcRole.Server);
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

	@Test
	public void testSource() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Client);
		assertNotNull(pipe.source());
		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Server);
		assertNotNull(pipe.source());
	}

	@Test
	public void testSink() throws Exception {
		SharedMemoryPipe pipe = new SharedMemoryPipe(createFile(),
				1789, JipcRole.Client);
		assertNotNull(pipe.sink());
		pipe = new SharedMemoryPipe(createFile(), 1789, JipcRole.Server);
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