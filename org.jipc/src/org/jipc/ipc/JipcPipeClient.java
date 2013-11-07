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

public class JipcPipeClient {

	private File serverDirectory;

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

	public JipcPipe connect(Class<? extends JipcPipe>... acceptedTypes)
			throws IOException {
		File directory = FileUtil.createDirectory(serverDirectory);
		FilePipe pipe = new FilePipe(directory, JipcRole.Yang);
		pipe.cleanUpOnClose();

		requestPipe(new JipcChannelOutputStream(pipe.sink()), acceptedTypes);
		JipcPipe response = waitForResponse(new JipcChannelInputStream(
				pipe.source()));

		pipe.close();
		return response;
	}

	protected void requestPipe(final OutputStream out,
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(acceptedTypes);
		byte[] bytes = request.toBytes();
		out.write(bytes);
		out.flush();
		out.close();
	}

	protected JipcPipe waitForResponse(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		
		JipcResponse response = new JipcResponse(baos.toByteArray());
		if (response.getCode() != JipcCode.PipeCreated) {
			throw new IOException("connect failed,  " + response.getCode() + ", " + response.getMessage());
		}
		String type = response.getParameter(JipcResponse.PARAM_TYPE);
		if (getTypeName(FilePipe.class).equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new FilePipe(dir, role);
		}
		if (getTypeName(ChunkFilePipe.class).equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new ChunkFilePipe(dir, role);
		}
		if (getTypeName(SharedMemoryPipe.class).equals(type)) {
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
			return JipcRole.valueOf(response.getParameter(JipcResponse.PARAM_ROLE));
		} catch (Exception e) {
			throw new IOException("unkown role '" + response.getParameter(JipcResponse.PARAM_ROLE) + "'");
		}
	}


	protected String getTypeName(final Class<? extends JipcPipe> pipeClass) {
		return pipeClass.getSimpleName();
	}
}
