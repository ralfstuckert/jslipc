package org.jipc.ipc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.file.FileUtil;
import org.jipc.ipc.JipcResponse.JipcCode;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;

/**
 * This is an analogy to a ServerSocket. The method {@link #accept()} wait for
 * an incoming connection request sent by a {@link JipcPipeClient}.
 */
public class JipcPipeServer {

	private File connectDirectory;
	private File pipeDirectory;
	private Class<? extends JipcPipe>[] supportedTypes;

	/**
	 * Creates a JipcPipeServer supporting all pipe types.
	 * 
	 * @param connectDirectory
	 *            the directory the server searches for incoming connection
	 *            request.
	 * @param pipeDirectory
	 *            the directory used to set up pipes.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public JipcPipeServer(final File connectDirectory, final File pipeDirectory)
			throws IOException {
		this(connectDirectory, pipeDirectory, ChunkFilePipe.class,
				FilePipe.class, SharedMemoryPipe.class);
	}

	/**
	 * Creates a JipcPipeServer working on the given directory.
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
	public JipcPipeServer(final File connectDirectory,
			final File pipeDirectory,
			Class<? extends JipcPipe>... supportedTypes) throws IOException {
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
	public JipcConnection accept() throws IOException {
		File dir = waitForDirectory();
		FilePipe connectPipe = new FilePipe(dir, JipcRole.Yin);
		try {
			connectPipe.cleanUpOnClose();

			JipcResponse response;
			JipcRequest request = null;
			JipcPipe pipe = null;
			OutputStream out = new JipcChannelOutputStream(connectPipe.sink());

			try {
				request = readRequest(new JipcChannelInputStream(
						connectPipe.source()));
			} catch (IOException e) {
				sendResponse(
						new JipcResponse(JipcCode.BadRequest, e.getMessage()),
						out);
				return null;
			}
			try {
				response = new JipcResponse(JipcCode.PipeCreated, "ok");
				pipe = createPipe(request, response);
			} catch (IOException e) {
				sendResponse(
						new JipcResponse(JipcCode.InternalError, e.getMessage()),
						out);
				return null;
			}

			sendResponse(response, out);

			return new JipcConnection(pipe, request.getParameters());
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
	protected JipcPipe createPipe(final JipcRequest request,
			final JipcResponse response) throws IOException {
		Class<? extends JipcPipe> type = getSuitableType(request);

		if (FilePipe.class.equals(type)) {
			File dir = FileUtil.createDirectory(pipeDirectory);
			response.setTypeParameter(type);
			response.setFileParameter(JipcResponse.PARAM_DIRECTORY, dir);
			response.setParameter(JipcResponse.PARAM_ROLE,
					JipcRole.Yang.toString());
			return new FilePipe(dir, JipcRole.Yin);
		}
		if (ChunkFilePipe.class.equals(type)) {
			File dir = FileUtil.createDirectory(pipeDirectory);
			response.setTypeParameter(type);
			response.setFileParameter(JipcResponse.PARAM_DIRECTORY, dir);
			response.setParameter(JipcResponse.PARAM_ROLE,
					JipcRole.Yang.toString());
			return new ChunkFilePipe(dir, JipcRole.Yin);
		}
		if (SharedMemoryPipe.class.equals(type)) {
			File file = FileUtil.createFile(pipeDirectory);
			Integer size = request.getIntParameter(JipcResponse.PARAM_SIZE);
			response.setTypeParameter(type);
			response.setFileParameter(JipcResponse.PARAM_FILE, file);
			response.setParameter(JipcResponse.PARAM_ROLE,
					JipcRole.Yang.toString());

			if (size != null) {
				response.setIntParameter(JipcResponse.PARAM_SIZE, size);
				return new SharedMemoryPipe(file, size, JipcRole.Yin);
			}
			return new SharedMemoryPipe(file, JipcRole.Yin);
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
	protected Class<? extends JipcPipe> getSuitableType(JipcRequest request)
			throws IOException {
		List<Class<? extends JipcPipe>> acceptTypes = request.getAcceptTypes();
		for (Class<? extends JipcPipe> current : supportedTypes) {
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
	 * @return the read JipcRequest.
	 * @throws IOException
	 */
	protected JipcRequest readRequest(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		return new JipcRequest(baos.toByteArray());
	}

	/**
	 * Sends the response to the given stream.
	 * 
	 * @param response
	 * @param out
	 * @throws IOException
	 */
	protected void sendResponse(final JipcResponse response,
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
		while (dir == null) {
			File[] files = connectDirectory.listFiles();
			for (File file : files) {
				if (file.isDirectory() && !isMarkedServed(file)) {
					dir = file;
				}
			}
			sleep();
		}
		markServed(dir);
		return dir;
	}

	private void sleep() throws InterruptedIOException {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new InterruptedIOException();
		}

	}

	private boolean isMarkedServed(final File dir) {
		return new File(dir, ".served").exists();
	}

	private void markServed(final File dir) throws IOException {
		new File(dir, ".served").createNewFile();
	}

}
