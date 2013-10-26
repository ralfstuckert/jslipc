package org.jipc.ipc.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.ipc.AbstractTestProducer;
import org.junit.Before;
import org.junit.Test;

public class FilePipeTest {

	private File source;
	private File sink;
	private File directory;

	@Before
	public void setUp() throws Exception {
		source = File.createTempFile("inn", ".pipe");
		source.deleteOnExit();
		sink = File.createTempFile("out", ".pipe");
		sink.deleteOnExit();
		directory = TestUtil.createDirectory();
		directory.deleteOnExit();
	}

	@Test
	public void testGetSourceFile() throws Exception {
		File source = FilePipe.getSourceFile(directory, JipcRole.Client);
		checkChannelFile(source, FilePipe.SERVER_TO_CLIENT_NAME);

		source = FilePipe.getSourceFile(directory, JipcRole.Server);
		checkChannelFile(source, FilePipe.CLIENT_TO_SERVER_NAME);
	}

	@Test
	public void testGetSinkFile() throws Exception {
		File source = FilePipe.getSinkFile(directory, JipcRole.Server);
		checkChannelFile(source, FilePipe.SERVER_TO_CLIENT_NAME);

		source = FilePipe.getSinkFile(directory, JipcRole.Client);
		checkChannelFile(source, FilePipe.CLIENT_TO_SERVER_NAME);
	}

	protected void checkChannelFile(File file, String expectedName) {
		assertTrue(file.exists());
		assertEquals(directory, file.getParentFile());
		assertEquals(expectedName, file.getName());
	}

	@Test
	public void testFilePipeFileJipcRole() throws Exception {
		new FilePipe(directory, JipcRole.Client);
		new FilePipe(directory, JipcRole.Server);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNullJipcRole() throws Exception {
		new FilePipe(null, JipcRole.Client);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNotDirectoryJipcRole() throws Exception {
		new FilePipe(source, JipcRole.Client);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileJipcRoleNull() throws Exception {
		new FilePipe(directory, (JipcRole) null);
	}

	@Test
	public void testFilePipeFileFile() throws Exception {
		new FilePipe(source, sink);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNullFile() throws Exception {
		new FilePipe(source, (File) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeNullFileFile() throws Exception {
		new FilePipe((File) null, sink);
	}

	@Test
	public void testSource() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.source());

		pipe = new FilePipe(directory, JipcRole.Client);
		assertNotNull(pipe.source());
	}

	@Test
	public void testSink() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe = new FilePipe(directory, JipcRole.Client);
		assertNotNull(pipe.sink());
	}

	@Test(timeout = 20000)
	public void testIpcWithFileChannels() throws Exception {
		File consumerToProducer = File.createTempFile("consumerToProducer",
				".pipe");
		consumerToProducer.deleteOnExit();
		File producerToConsumer = File.createTempFile("producerToConsumer",
				".pipe");
		producerToConsumer.deleteOnExit();

		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						FilePipeTestProducer.class.getName(),
						consumerToProducer.getAbsolutePath(),
						producerToConsumer.getAbsolutePath() });

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JipcPipe pipe = new FilePipe(producerToConsumer, consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

	@Test(timeout = 20000)
	public void testIpcWithDirectory() throws Exception {
		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						FilePipeTestProducer.class.getName(),
						directory.getAbsolutePath(), "-" + JipcRole.Server });
		
//		int date = 0;
//		while ((date = producer.getErrorStream().read()) != -1) {
//			System.err.write(date);
//		}

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JipcPipe pipe = new FilePipe(directory, JipcRole.Client);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

}
