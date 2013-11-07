package org.jipc.ipc;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.UrlUtil;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.file.FileUtil;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JipcPipeClient}.
 */
public class JipcPipeClientTest {

	private File directory;

	@Before
	public void setUp() throws Exception {
		directory = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(directory, true);
	}

	@Test
	public void testJipcPipeClient() throws Exception {
		new JipcPipeClient(directory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithNull() throws Exception {
		new JipcPipeClient(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithNonExistingDir() throws Exception {
		new JipcPipeClient(new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JipcPipeClient(file);
	}

	@Test(timeout = 600000)
	public void testConnect() throws Exception {

		final AtomicReference<JipcPipe> pipeRef = new AtomicReference<JipcPipe>();
		Thread thread = new Thread() {
			public void run() {
				JipcPipeClient client;
				try {
					client = new JipcPipeClient(directory);
					pipeRef.set(client.connect(ChunkFilePipe.class));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = waitForDirectory();
		FilePipe connectPipe = new FilePipe(connectDir, JipcRole.Yin);
		connectPipe.cleanUpOnClose();
		List<String> requestLines = readRequest(connectPipe);
		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JIPC/1.0", requestLines.get(0));
		assertThat(requestLines, hasItem("ACCEPT-TYPES: ChunkFilePipe"));

		File pipeDir = FileUtil.createDirectory(directory);
		StringBuilder bob = new StringBuilder();
		bob.append("200 JIPC/1.0\n");
		bob.append("TYPE: ChunkFilePipe\n");
		bob.append("DIRECTORY: ");
		bob.append(URLEncoder.encode(pipeDir.getAbsolutePath(),
				StandardCharsets.UTF_8.toString()));
		bob.append("\n");
		bob.append("ROLE: Yang\n");
		writeResponse(connectPipe, bob.toString());
		connectPipe.close();

		thread.join();
		assertNotNull(pipeRef.get());
		assertEquals(ChunkFilePipe.class, pipeRef.get().getClass());
	}

	private void writeResponse(FilePipe connectPipe, String response)
			throws IOException {
		Writer writer = new OutputStreamWriter(new JipcChannelOutputStream(
				connectPipe.sink()), StandardCharsets.UTF_8);
		writer.write(response);
		writer.close();
	}

	private List<String> readRequest(FilePipe connectPipe) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new JipcChannelInputStream(connectPipe.source()),
				StandardCharsets.UTF_8));
		List<String> request = new ArrayList<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println("read: " + line);
			request.add(line);
		}
		reader.close();
		return request;
	}

	private File waitForDirectory() throws InterruptedException {
		File dir = null;
		while (dir == null) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					dir = file;
				}
			}
			Thread.sleep(200);
		}
		return dir;
	}

	@Test
	public void testRequestPipe() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JIPC/1.0", requestLines.get(0));
	}

	@Test
	public void testRequestPipeWithAcceptedTypes() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client, FilePipe.class,
				ChunkFilePipe.class);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JIPC/1.0", requestLines.get(0));
		assertThat(
				requestLines,
				hasItem("ACCEPT-TYPES: "
						+ UrlUtil.urlEncode("FilePipe,ChunkFilePipe")));
	}

	protected List<String> sendRequest(final JipcPipeClient client,
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		client.requestPipe(baos, acceptedTypes);
		String request = new String(baos.toByteArray());
		System.out.println(request);
		return Arrays.asList(request.split("\\n"));
	}

	@Test
	public void testWaitForResponse() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);

		checkWaitForFilePipe(client);
		checkWaitForChunkFilePipe(client);
		checkWaitForSharedMemoryPipe(client);
	}

	private void checkWaitForFilePipe(JipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JipcRole role = JipcRole.Yang;
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put("TYPE", FilePipe.class.getSimpleName());
		parameter.put("ROLE", role.toString());
		parameter.put(
				"DIRECTORY",
				URLEncoder.encode(dir.getAbsolutePath(),
						StandardCharsets.UTF_8.toString()));

		JipcPipe pipe = waitForResponse(client, parameter);
		assertNotNull(pipe);
		assertEquals(FilePipe.class, pipe.getClass());
		File sink = FilePipe.getSinkFile(dir, role);
		assertTrue(sink.exists());
		File source = FilePipe.getSourceFile(dir, role);
		assertTrue(source.exists());
	}

	private void checkWaitForChunkFilePipe(JipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JipcRole role = JipcRole.Yang;
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put("TYPE", ChunkFilePipe.class.getSimpleName());
		parameter.put("ROLE", role.toString());
		parameter.put(
				"DIRECTORY",
				URLEncoder.encode(dir.getAbsolutePath(),
						StandardCharsets.UTF_8.toString()));

		JipcPipe pipe = waitForResponse(client, parameter);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		File sink = ChunkFilePipe.getSinkDir(dir, role);
		assertTrue(sink.exists());
		assertTrue(sink.isDirectory());
		File source = ChunkFilePipe.getSourceDir(dir, role);
		assertTrue(source.exists());
		assertTrue(source.isDirectory());
	}

	private void checkWaitForSharedMemoryPipe(JipcPipeClient client)
			throws Exception {

		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		JipcRole role = JipcRole.Yang;
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put("TYPE", SharedMemoryPipe.class.getSimpleName());
		parameter.put("ROLE", role.toString());
		parameter.put("FILE", URLEncoder.encode(file.getAbsolutePath(),
				StandardCharsets.UTF_8.toString()));
		parameter.put("SIZE", Integer.toString(8292));

		JipcPipe pipe = waitForResponse(client, parameter);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
	}

	private JipcPipe waitForResponse(JipcPipeClient client,
			Map<String, String> parameter) throws IOException {
		StringBuilder bob = new StringBuilder();
		bob.append("200 JIPC/1.0\n");
		for (Entry<String, String> entry : parameter.entrySet()) {
			bob.append(entry.getKey());
			bob.append(": ");
			bob.append(entry.getValue());
			bob.append("\n");
		}
		InputStream in = new ByteArrayInputStream(bob.toString().getBytes(
				StandardCharsets.UTF_8));

		JipcPipe pipe = client.waitForResponse(in);
		return pipe;
	}

}
