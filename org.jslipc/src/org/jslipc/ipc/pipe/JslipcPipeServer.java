package org.jslipc.ipc.pipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TimeoutAware;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.ipc.pipe.JslipcResponse.JslipcCode;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jslipc.util.FileUtil;
import org.jslipc.util.TimeUtil;

/**
 * This is an analogy to a ServerSocket. The method {@link #accept()} wait for
 * an incoming connection request sent by a {@link JslipcPipeClient}.
 */
public class JslipcPipeServer implements TimeoutAware {

	private File connectDirectory;
	private File pipeDirectory;
	private Class<? extends JslipcPipe>[] supportedTypes;
	private int timeout = 0;

	/**
	 * Creates a JslipcPipeServer supporting all pipe types.
	 * 
	 * @param connectDirectory
	 *            the directory the server searches for incoming connection
	 *            request.
	 * @param pipeDirectory
	 *            the directory used to set up pipes.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public JslipcPipeServer(final File connectDirectory, final File pipeDirectory)
			throws IOException {
		this(connectDirectory, pipeDirectory, ChunkFilePipe.class,
				FilePipe.class, SharedMemoryPipe.class);
	}

	/**
	 * Creates a JslipcPipeServer working on the given directory.
	 * 
	 * @param connectDirectory
	 *            the directory the server searches for incoming connection
	 *            request.
	 * @param pipeDirectory
	 *            the directory used to set up pipes.
	 * @param supportedTypes
	 *            the pipe types supported by the server, the first one is
	 *            preferred.
	 * @throws IOException
	 */
	public JslipcPipeServer(final File connectDirectory,
			final File pipeDirectory,
			Class<? extends JslipcPipe>... supportedTypes) throws IOException {
		checkDirectory(connectDirectory, "connectDirectory");
		checkDirectory(pipeDirectory, "pipeDirectory");
		if (connectDirectory.equals(pipeDirectory)) {
			throw new IllegalArgumentException(
					"connect- and pipe-directory must not be the same: "
							+ connectDirectory.getAbsolutePath());
		}
		this.connectDirectory = connectDirectory;
		this.pipeDirectory = pipeDirectory;
		this.supportedTypes = supportedTypes;
	}

	public void checkDirectory(File directory, String name) {
		if (directory == null) {
			throw new IllegalArgumentException("parameter '" + name
					+ "' must not be null");
		}
		if (!directory.exists()) {
			throw new IllegalArgumentException(directory.getAbsolutePath()
					+ " does not exist");
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException(directory.getAbsolutePath()
					+ " is not a directory");
		}
	}

	/**
	 * Waits for an incoming request, and creates an appropriate pipe.
	 * 
	 * @return an accepted connection.
	 * @throws IOException
	 */
	public JslipcConnection accept() throws IOException {
		File dir = waitForDirectory();
		FilePipe connectPipe = new FilePipe(dir, JslipcRole.Yin);
		try {
			connectPipe.cleanUpOnClose();

			JslipcResponse response;
			JslipcRequest request = null;
			JslipcPipe pipe = null;
			OutputStream out = new JslipcChannelOutputStream(connectPipe.sink());

			try {
				request = readRequest(new JslipcChannelInputStream(
						connectPipe.source()));
			} catch (IOException e) {
				sendResponse(
						new JslipcResponse(JslipcCode.BadRequest, e.getMessage()),
						out);
				return null;
			}
			try {
				response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
				pipe = createPipe(request, response);
			} catch (IOException e) {
				sendResponse(
						new JslipcResponse(JslipcCode.InternalError, e.getMessage()),
						out);
				return null;
			}

			sendResponse(response, out);

			if (pipe instanceof JslipcBinman) {
				((JslipcBinman)pipe).cleanUpOnClose();
			}
			return new JslipcConnection(pipe, request.getParameters());
		} finally {
			connectPipe.close();
		}
	}

	/**
	 * Creates a pipe from the given request and sets up the response.
	 * 
	 * @param request
	 * @param response
	 * @return the created pipe.
	 * @throws IOException
	 */
	protected JslipcPipe createPipe(final JslipcRequest request,
			final JslipcResponse response) throws IOException {
		Class<? extends JslipcPipe> type = getSuitableType(request);

		if (FilePipe.class.equals(type)) {
			File dir = FileUtil.createDirectory(pipeDirectory);
			response.setTypeParameter(type);
			response.setFileParameter(JslipcResponse.PARAM_DIRECTORY, dir);
			response.setParameter(JslipcResponse.PARAM_ROLE,
					JslipcRole.Yang.toString());
			return new FilePipe(dir, JslipcRole.Yin);
		}
		if (ChunkFilePipe.class.equals(type)) {
			File dir = FileUtil.createDirectory(pipeDirectory);
			response.setTypeParameter(type);
			response.setFileParameter(JslipcResponse.PARAM_DIRECTORY, dir);
			response.setParameter(JslipcResponse.PARAM_ROLE,
					JslipcRole.Yang.toString());
			return new ChunkFilePipe(dir, JslipcRole.Yin);
		}
		if (SharedMemoryPipe.class.equals(type)) {
			File file = FileUtil.createFile(pipeDirectory);
			Integer size = request.getIntParameter(JslipcResponse.PARAM_SIZE);
			response.setTypeParameter(type);
			response.setFileParameter(JslipcResponse.PARAM_FILE, file);
			response.setParameter(JslipcResponse.PARAM_ROLE,
					JslipcRole.Yang.toString());

			if (size != null) {
				response.setIntParameter(JslipcResponse.PARAM_SIZE, size);
				return new SharedMemoryPipe(file, size, JslipcRole.Yin);
			}
			return new SharedMemoryPipe(file, JslipcRole.Yin);
		}
		throw new IOException("unknown type '" + type + "'");
	}

	/**
	 * Returns an appropriate pipe class for the request.
	 * 
	 * @param request
	 * @return the pipe type.
	 * @throws IOException
	 *             if none of the accepted types is supported by this server.
	 */
	protected Class<? extends JslipcPipe> getSuitableType(JslipcRequest request)
			throws IOException {
		List<Class<? extends JslipcPipe>> acceptTypes = request.getAcceptTypes();
		if (acceptTypes == null || acceptTypes.size() == 0) {
			return supportedTypes[0];
		}
		for (Class<? extends JslipcPipe> current : supportedTypes) {
			if (acceptTypes.contains(current)) {
				return current;
			}
		}
		throw new IOException("requested types '" + acceptTypes
				+ "'are not supported: " + Arrays.asList(supportedTypes));
	}

	/**
	 * Reads an request from the given stream.
	 * 
	 * @param in
	 * @return the read JslipcRequest.
	 * @throws IOException
	 */
	protected JslipcRequest readRequest(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		return new JslipcRequest(baos.toByteArray());
	}

	/**
	 * Sends the response to the given stream.
	 * 
	 * @param response
	 * @param out
	 * @throws IOException
	 */
	protected void sendResponse(final JslipcResponse response,
			final OutputStream out) throws IOException {
		out.write(response.toBytes());
		out.close();
	}

	/**
	 * Wait for a not yet served FilePipe directory.
	 * 
	 * @return the directory.
	 * @throws IOException
	 */
	protected synchronized File waitForDirectory() throws IOException {
		File dir = null;
		long waitingSince = System.currentTimeMillis();
		while (dir == null) {
			File[] files = connectDirectory.listFiles();
			for (File file : files) {
				if (file.isDirectory() && !isMarkedServed(file)) {
					dir = file;
				}
			}
			sleep(waitingSince);
		}
		markServed(dir);
		return dir;
	}

	private boolean isMarkedServed(final File dir) {
		return new File(dir, ".served").exists();
	}

	private void markServed(final File dir) throws IOException {
		new File(dir, ".served").createNewFile();
	}

	/**
	 * Sleeps for the default time and watches for timeouts.
	 * @param waitingSince the timestamp when the operation started to block.
	 * @throws InterruptedIOException
	 * @throws InterruptedByTimeoutException
	 */
	protected void sleep(long waitingSince) throws InterruptedIOException {
		try {
			TimeUtil.sleep(getTimeout(), waitingSince);
		} catch (InterruptedException e) {
			throw new InterruptedIOException("interrupted by timeout");
		}
	}
	
	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("parameter timeout must be > 0: " + timeout);
		}
		this.timeout = timeout;
	}
}
