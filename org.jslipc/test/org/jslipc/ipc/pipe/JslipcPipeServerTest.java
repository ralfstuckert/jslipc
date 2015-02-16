package org.jslipc.ipc.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TestUtil;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.ipc.pipe.JslipcResponse.JslipcCode;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jslipc.util.FileUtil;
import org.jslipc.util.HostDir;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JslipcPipeServer}.
 */
public class JslipcPipeServerTest {

	private File serverConnectDir;
	private File serverPipeDir;
	private File hostDirParent;
	private HostDir hostDir;

	@Before
	public void setUp() throws Exception {
		serverConnectDir = TestUtil.createDirectory();
		serverPipeDir = TestUtil.createDirectory();
		hostDirParent = TestUtil.createDirectory();
		hostDir = HostDir.create(hostDirParent);
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(serverConnectDir, true);
		FileUtil.delete(serverPipeDir, true);
		hostDir.close();
		FileUtil.delete(hostDirParent, true);
	}

	@Test
	public void testJslipcPipeServer() throws Exception {
		new JslipcPipeServer(serverConnectDir, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJslipcPipeServerConnectDirNull() throws Exception {
		new JslipcPipeServer(null, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJslipcPipeServerPipeDirNull() throws Exception {
		new JslipcPipeServer(serverConnectDir, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsipcPipeServerWithNonExistingConnectDir() throws Exception {
		new JslipcPipeServer(new File("herbert"), serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsipcPipeServerWithNonExistingPipeDir() throws Exception {
		new JslipcPipeServer(serverConnectDir, new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsipcPipeServerWithSameDir() throws Exception {
		new JslipcPipeServer(serverConnectDir, serverConnectDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsipcPipeServerWithConnectFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JslipcPipeServer(file, serverPipeDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsipcPipeServerWithPipeFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JslipcPipeServer(serverConnectDir, file);
	}

	@Test
	public void testJslipcPipeServerWithHostDir() throws Exception {
		JslipcPipeServer server = new JslipcPipeServer(hostDir);
		assertEquals(new File(hostDir.getDirectory(), "connect"), server.getConnectDir());
		assertEquals(new File(hostDir.getDirectory(), "pipes"), server.getPipesDir());
	}

	@Test(expected=IOException.class)
	public void testJslipcPipeServerWithClosedHostDir() throws Exception {
		hostDir.close();
		new JslipcPipeServer(hostDir);
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

	public void checkAccept(Class<? extends JslipcPipe> expectedPipeClass,
			Class<? extends JslipcPipe>... acceptTypes) throws IOException,
			InterruptedException {
		final AtomicReference<JslipcConnection> pipeRef = new AtomicReference<JslipcConnection>();
		Thread thread = new Thread() {
			public void run() {
				JslipcPipeServer server;
				try {
					server = new JslipcPipeServer(serverConnectDir, serverPipeDir);
					pipeRef.set(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = FileUtil.createDirectory(serverConnectDir);
		FilePipe connectPipe = new FilePipe(connectDir, JslipcRole.Yang);
		connectPipe.cleanUpOnClose();

		JslipcRequest request = new JslipcRequest(JslipcCommand.CONNECT);
		request.setAcceptTypes(acceptTypes);
		writeRequest(connectPipe, request);

		JslipcResponse response = readResponse(connectPipe);
		assertEquals(response.getMessage(), JslipcCode.PipeCreated, response.getCode());
		assertEquals(expectedPipeClass, response.getTypeParameter());
		File pipeDir = response.getFileParameter(JslipcResponse.PARAM_DIRECTORY);
		if (SharedMemoryPipe.class.equals(expectedPipeClass)) {
			pipeDir = response.getFileParameter(JslipcResponse.PARAM_FILE);
		}
		assertNotNull(pipeDir);
		assertTrue(pipeDir.exists());
		assertEquals(serverPipeDir, pipeDir.getParentFile());

		connectPipe.close();

		thread.join();
		JslipcConnection connection = pipeRef.get();
		assertNotNull(connection);
		assertEquals(request.getParameters(), connection.getRequestParameters());
		assertNotNull(connection.getPipe());
		assertEquals(expectedPipeClass, connection.getPipe().getClass());
	}

	@Test(timeout = 10000)
	public void testWaitForDirectory() throws Exception {
		File dir = FileUtil.createDirectory(serverConnectDir);
		final JslipcPipeServer server = new JslipcPipeServer(serverConnectDir,
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

	@Test(timeout = 10000)
	public void testAccessTimeout() throws Exception {
		final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
			Thread thread = new Thread() {
				public void run() {
					JslipcPipeServer server;
					try {
						server = new JslipcPipeServer(serverConnectDir, serverPipeDir);
						server.setAcceptTimeout(3000);
						server.accept();
					} catch (IOException e) {
						exceptionRef.set(e);
					}
				}
			};
			thread.start();

			thread.join(2000);
			assertTrue(thread.isAlive());

			thread.join(2000);
			assertFalse(thread.isAlive());
			assertNotNull(exceptionRef.get());
			assertEquals(InterruptedIOException.class, exceptionRef.get().getClass());
	}


	@Test//(timeout = 10000)
	public void testConnectTimeout() throws Exception {
		final AtomicReference<JslipcConnection> pipeRef = new AtomicReference<JslipcConnection>();
		final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
		Thread thread = new Thread() {
			public void run() {
				JslipcPipeServer server;
				try {
					server = new JslipcPipeServer(serverConnectDir, serverPipeDir);
					server.setTimeout(2000);
					pipeRef.set(server.accept());
				} catch (IOException e) {
					exceptionRef.set(e);
				}
			}
		};
		thread.start();

		// set up connection, but do not write request
		File connectDir = FileUtil.createDirectory(serverConnectDir);
		FilePipe connectPipe = new FilePipe(connectDir, JslipcRole.Yang);
		connectPipe.cleanUpOnClose();

		thread.join(2000);
		assertTrue(thread.isAlive());

		thread.join(2000);
		assertFalse(thread.isAlive());
		assertNull(exceptionRef.get());
		// connect timeout leads to response: bad request 
		JslipcResponse response = readResponse(connectPipe);
		assertEquals(response.getMessage(), JslipcCode.BadRequest, response.getCode());
		assertTrue(response.getMessage().contains("timeout"));
	}

	@SuppressWarnings("unchecked")
	public void testCreatePipe() throws Exception {
		final JslipcPipeServer server = new JslipcPipeServer(serverConnectDir,
				serverPipeDir);

		JslipcRequest request = new JslipcRequest(JslipcCommand.CONNECT);
		request.setAcceptTypes(SharedMemoryPipe.class);
		JslipcResponse response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		JslipcPipe pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
		assertEquals(SharedMemoryPipe.class, response.getTypeParameter());
		assertEquals(JslipcRole.Yang, response.getRoleParameter());
		File file = response.getFileParameter(JslipcResponse.PARAM_FILE);
		assertNotNull(file);
		assertTrue(file.exists());
		assertEquals(serverPipeDir, file.getParentFile());

		request.setAcceptTypes(FilePipe.class, ChunkFilePipe.class,
				SharedMemoryPipe.class);
		response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		assertEquals(ChunkFilePipe.class, response.getTypeParameter());
		assertEquals(JslipcRole.Yang, response.getRoleParameter());
		File dir = response.getFileParameter(JslipcResponse.PARAM_DIRECTORY);
		assertNotNull(dir);
		assertTrue(dir.exists());
		assertEquals(serverPipeDir, dir.getParentFile());
	}

	private JslipcResponse readResponse(FilePipe connectPipe) throws IOException {
		InputStream in = new JslipcChannelInputStream(connectPipe.source());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		JslipcResponse response = new JslipcResponse(baos.toByteArray());
		return response;
	}

	private void writeRequest(FilePipe connectPipe, JslipcRequest request)
			throws IOException {
		OutputStream out = new JslipcChannelOutputStream(connectPipe.sink());
		out.write(request.toBytes());
		out.close();
	}

}
