package org.jipc.ipc.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.ipc.pipe.JipcResponse.JipcCode;
import org.jipc.ipc.pipe.file.ChunkFilePipe;
import org.jipc.ipc.pipe.file.FilePipe;
import org.jipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jipc.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JipcPipeServer}.
 */
public class JipcPipeServerTest {

	private File serverConnectDir;
	private File serverPipeDir;

	@Before
	public void setUp() throws Exception {
		serverConnectDir = TestUtil.createDirectory();
		serverPipeDir = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(serverConnectDir, true);
		FileUtil.delete(serverPipeDir, true);
	}

	@Test
	public void testJipcPipeServer() throws Exception {
		new JipcPipeServer(serverConnectDir, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerConnectDirNull() throws Exception {
		new JipcPipeServer(null, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerPipeDirNull() throws Exception {
		new JipcPipeServer(serverConnectDir, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithNonExistingConnectDir() throws Exception {
		new JipcPipeServer(new File("herbert"), serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithNonExistingPipeDir() throws Exception {
		new JipcPipeServer(serverConnectDir, new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithSameDir() throws Exception {
		new JipcPipeServer(serverConnectDir, serverConnectDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithConnectFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JipcPipeServer(file, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithPipeFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JipcPipeServer(serverConnectDir, file);
	}

	@SuppressWarnings("unchecked")
	@Test(timeout = 600000)
	public void testAcceptWithNoAcceptType() throws Exception {
		// ChunkFilePipe is the default
		checkAccept(ChunkFilePipe.class);
	}

	@SuppressWarnings("unchecked")
	@Test(timeout = 600000)
	public void testAcceptWithFilePipe() throws Exception {
		checkAccept(FilePipe.class, FilePipe.class);
	}

	@SuppressWarnings("unchecked")
	@Test(timeout = 600000)
	public void testAcceptWithChunkFilePipe() throws Exception {
		checkAccept(ChunkFilePipe.class, ChunkFilePipe.class);
	}

	@SuppressWarnings("unchecked")
	@Test(timeout = 600000)
	public void testAcceptWithSharedMemoryPipe() throws Exception {
		checkAccept(SharedMemoryPipe.class, SharedMemoryPipe.class);
	}

	public void checkAccept(Class<? extends JipcPipe> expectedPipeClass,
			Class<? extends JipcPipe>... acceptTypes) throws IOException,
			InterruptedException {
		final AtomicReference<JipcConnection> pipeRef = new AtomicReference<JipcConnection>();
		Thread thread = new Thread() {
			public void run() {
				JipcPipeServer server;
				try {
					server = new JipcPipeServer(serverConnectDir, serverPipeDir);
					pipeRef.set(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = FileUtil.createDirectory(serverConnectDir);
		FilePipe connectPipe = new FilePipe(connectDir, JipcRole.Yang);
		connectPipe.cleanUpOnClose();

		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(acceptTypes);
		writeRequest(connectPipe, request);

		JipcResponse response = readResponse(connectPipe);
		assertEquals(response.getMessage(), JipcCode.PipeCreated, response.getCode());
		assertEquals(expectedPipeClass, response.getTypeParameter());
		File pipeDir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
		if (SharedMemoryPipe.class.equals(expectedPipeClass)) {
			pipeDir = response.getFileParameter(JipcResponse.PARAM_FILE);
		}
		assertNotNull(pipeDir);
		assertTrue(pipeDir.exists());
		assertEquals(serverPipeDir, pipeDir.getParentFile());

		connectPipe.close();

		thread.join();
		JipcConnection connection = pipeRef.get();
		assertNotNull(connection);
		assertEquals(request.getParameters(), connection.getRequestParameters());
		assertNotNull(connection.getPipe());
		assertEquals(expectedPipeClass, connection.getPipe().getClass());
	}

	@Test(timeout = 10000)
	public void testWaitForDirectory() throws Exception {
		File dir = FileUtil.createDirectory(serverConnectDir);
		final JipcPipeServer server = new JipcPipeServer(serverConnectDir,
				serverPipeDir);
		assertEquals(dir, server.waitForDirectory());

		final AtomicReference<File> fileRef = new AtomicReference<File>();
		Thread thread = new Thread() {
			public void run() {
				try {
					fileRef.set(server.waitForDirectory());
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
		};
		thread.start();

		thread.join(3000);
		assertTrue(thread.isAlive());

		thread.interrupt();
		thread.join(3000);
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("unchecked")
	public void testCreatePipe() throws Exception {
		final JipcPipeServer server = new JipcPipeServer(serverConnectDir,
				serverPipeDir);

		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(SharedMemoryPipe.class);
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		JipcPipe pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
		assertEquals(SharedMemoryPipe.class, response.getTypeParameter());
		assertEquals(JipcRole.Yang, response.getRoleParameter());
		File file = response.getFileParameter(JipcResponse.PARAM_FILE);
		assertNotNull(file);
		assertTrue(file.exists());
		assertEquals(serverPipeDir, file.getParentFile());

		request.setAcceptTypes(FilePipe.class, ChunkFilePipe.class,
				SharedMemoryPipe.class);
		response = new JipcResponse(JipcCode.PipeCreated, "ok");
		pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		assertEquals(ChunkFilePipe.class, response.getTypeParameter());
		assertEquals(JipcRole.Yang, response.getRoleParameter());
		File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
		assertNotNull(dir);
		assertTrue(dir.exists());
		assertEquals(serverPipeDir, dir.getParentFile());
	}

	private JipcResponse readResponse(FilePipe connectPipe) throws IOException {
		InputStream in = new JipcChannelInputStream(connectPipe.source());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		JipcResponse response = new JipcResponse(baos.toByteArray());
		return response;
	}

	private void writeRequest(FilePipe connectPipe, JipcRequest request)
			throws IOException {
		OutputStream out = new JipcChannelOutputStream(connectPipe.sink());
		out.write(request.toBytes());
		out.close();
	}

}
