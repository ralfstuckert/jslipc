package org.jipc.ipc.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.jipc.JipcPipe;
import org.jipc.ipc.AbstractTestProducer;
import org.junit.Before;
import org.junit.Test;

public class FilePipeTest {

	private File source;
	private File sink;

	@Before
	public void setUp() throws Exception {
		source = File.createTempFile("inn", ".pipe");
		source.deleteOnExit();
		sink = File.createTempFile("out", ".pipe");
		sink.deleteOnExit();
	}

	@Test
	public void testFilePipeFileJipcRole() {
		fail("Not yet implemented");
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
	}

	@Test
	public void testSink() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());
	}

	@Test(timeout = 20000)
	public void testIpc() throws Exception {
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
		JipcPipe pipe = consumer.createPipe(producerToConsumer,
				consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

}
