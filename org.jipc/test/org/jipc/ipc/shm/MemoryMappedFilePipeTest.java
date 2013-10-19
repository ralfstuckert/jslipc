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

import org.jipc.ipc.shm.MemoryMappedFilePipe.Role;
import org.junit.Before;
import org.junit.Test;

public class MemoryMappedFilePipeTest {

	@Before
	public void setUp() throws Exception {
	}

	protected File createFile() throws Exception {
		File file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		return file;
	}

	@Test
	public void testMemoryMappedFilePipeFile() throws Exception {
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(createFile(),
				Role.Client);
		checkQueueBounds(pipe, 4096, Role.Client);

		pipe = new MemoryMappedFilePipe(createFile(), Role.Server);
		checkQueueBounds(pipe, 4096, Role.Server);
	}

	@Test
	public void testMemoryMappedFilePipeFileInt() throws Exception {
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(createFile(),
				1789, Role.Client);
		checkQueueBounds(pipe, 1789, Role.Client);

		pipe = new MemoryMappedFilePipe(createFile(), 1789, Role.Server);
		checkQueueBounds(pipe, 1789, Role.Server);
	}

	private void checkQueueBounds(MemoryMappedFilePipe pipe, int size, Role role) {
		int bufferSize = size / 2;
		if (role == Role.Client) {
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
		checkWhoLocksInitializes(Role.Client);
		checkWhoLocksInitializes(Role.Server);
	}

	@SuppressWarnings("resource")
	protected void checkWhoLocksInitializes(Role role) throws IOException,
			FileNotFoundException {
		File file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(file, 1789, role);
		assertTrue(pipe.getInQueue().isInitialized());
		assertTrue(pipe.getOutQueue().isInitialized());

		file = File.createTempFile("test", ".mapped");
		file.deleteOnExit();
		FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
		channel.lock();
		pipe = new MemoryMappedFilePipe(file, 1789, role);
		assertFalse(pipe.getInQueue().isInitialized());
		assertFalse(pipe.getOutQueue().isInitialized());
	}

	@Test
	public void testSource() throws Exception {
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(createFile(),
				1789, Role.Client);
		assertNotNull(pipe.source());
		pipe = new MemoryMappedFilePipe(createFile(), 1789, Role.Server);
		assertNotNull(pipe.source());
	}

	@Test
	public void testSink() throws Exception {
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(createFile(),
				1789, Role.Client);
		assertNotNull(pipe.sink());
		pipe = new MemoryMappedFilePipe(createFile(), 1789, Role.Server);
		assertNotNull(pipe.sink());
	}

	@Test(timeout=20000)
	public void testIpc() throws Exception {
		File file = createFile();
		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						TestProducer.class.getName(), file.getAbsolutePath() });
		Thread.sleep(1000);
		String reply = TestConsumer.consume(file);
		String expectedReply = TestConsumer.createReply(TestProducer.HELLO);
		assertEquals(expectedReply, reply);
		producer.waitFor();
	}

}
