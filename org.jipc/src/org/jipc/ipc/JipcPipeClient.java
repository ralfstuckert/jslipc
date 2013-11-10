package org.jipc.ipc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.file.FileUtil;
import org.jipc.ipc.JipcRequest.JipcCommand;
import org.jipc.ipc.JipcResponse.JipcCode;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;

/**
 * This is an analogy to socket which requests a connection pipe from a
 * {@link JipcPipeServer}. Method {@link #connect(Class...)} requests a new
 * connection and waits for the server response.
 */
public class JipcPipeClient {

	@SuppressWarnings("unchecked")
	public final static Class<? extends JipcPipe>[] ALL_PIPES = new Class[] {
			FilePipe.class, ChunkFilePipe.class, SharedMemoryPipe.class };

	private File serverDirectory;

	/**
	 * Creates a client talking to the {@link JipcPipeServer} on the given
	 * directory.
	 * 
	 * @param serverDirectory
	 *            the JipcPipeServer directory.
	 * @throws IOException
	 */
	public JipcPipeClient(final File serverDirectory) throws IOException {
		if (serverDirectory == null) {
			throw new IllegalArgumentException(
					"parameter 'serverDirectory' must not be null");
		}
		if (!serverDirectory.exists()) {
			throw new IllegalArgumentException(
					serverDirectory.getAbsolutePath() + " does not exist");
		}
		if (!serverDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					serverDirectory.getAbsolutePath() + " is not a directory");
		}
		this.serverDirectory = serverDirectory;
	}

	/**
	 * Requests and waits for a pipe created by the corresponding
	 * {@link JipcPipeServer}
	 * 
	 * @param acceptedTypes
	 *            the pipe types accepted by the client.
	 * @return the created pipe.
	 * @throws IOException
	 */
	public JipcPipe connect(Class<? extends JipcPipe>... acceptedTypes)
			throws IOException {
		File directory = FileUtil.createDirectory(serverDirectory);
		FilePipe pipe = new FilePipe(directory, JipcRole.Yang);
		pipe.cleanUpOnClose();

		sendRequest(new JipcChannelOutputStream(pipe.sink()), acceptedTypes);
		JipcPipe response = readResponse(new JipcChannelInputStream(
				pipe.source()));

		pipe.close();
		return response;
	}

	/**
	 * Writes a request for a pipe to the given OutputStream.
	 * 
	 * @param out
	 *            the stream to write to.
	 * @param acceptedTypes
	 *            the pipe types accepted by the client.
	 * @throws IOException
	 */
	protected void sendRequest(final OutputStream out,
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(acceptedTypes);
		byte[] bytes = request.toBytes();
		out.write(bytes);
		out.flush();
		out.close();
	}

	/**
	 * Reads the response from the server and creates the pipe.
	 * 
	 * @param in
	 *            the stream to read from.
	 * @return the created pipe.
	 * @throws IOException
	 */
	protected JipcPipe readResponse(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();

		JipcResponse response = new JipcResponse(baos.toByteArray());
		if (response.getCode() != JipcCode.PipeCreated) {
			throw new IOException("connect failed,  " + response.getCode()
					+ ", " + response.getMessage());
		}
		Class<? extends JipcPipe> type = response.getTypeParameter();
		if (FilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new FilePipe(dir, role);
		}
		if (ChunkFilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new ChunkFilePipe(dir, role);
		}
		if (SharedMemoryPipe.class.equals(type)) {
			File file = response.getFileParameter(JipcResponse.PARAM_FILE);
			JipcRole role = getRole(response);
			Integer size = response.getIntParameter(JipcResponse.PARAM_SIZE);
			if (size != null) {
				return new SharedMemoryPipe(file, size, role);
			}
			return new SharedMemoryPipe(file, role);
		}
		throw new IOException("unknown type '" + type + "'");
	}

	private JipcRole getRole(JipcResponse response) throws IOException {
		try {
			return JipcRole.valueOf(response
					.getParameter(JipcResponse.PARAM_ROLE));
		} catch (Exception e) {
			throw new IOException("unkown role '"
					+ response.getParameter(JipcResponse.PARAM_ROLE) + "'");
		}
	}

}
