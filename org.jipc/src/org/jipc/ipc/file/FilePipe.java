package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.file.ReadableJipcFileChannel;
import org.jipc.channel.file.WritableJipcFileChannel;

public class FilePipe implements JipcPipe {
	
	private File sourceFile;
	private File sinkFile;
	private ReadableJipcFileChannel source;
	private WritableJipcFileChannel sink;

	public FilePipe(final File directory, final JipcRole role) throws IOException {
		this(getSourceFile(directory, role), getSinkFile(directory, role));
	}

	public FilePipe(final File source, final File sink) {
		if (source == null) {
			throw new IllegalArgumentException("parameter 'source' must not be null");
		}
		if (sink == null) {
			throw new IllegalArgumentException("parameter 'sink' must not be null");
		}
		this.sourceFile = source;
		this.sinkFile = sink;
	}

	@Override
	public ReadableJipcFileChannel source() throws IOException {
		if (source == null) {
			source = new ReadableJipcFileChannel(sourceFile);
		}
		return source;
	}

	@Override
	public WritableJipcFileChannel sink() throws IOException {
		if (sink == null) {
			sink = new WritableJipcFileChannel(sinkFile);
		}
		return sink;
	}
	
	protected final static String CLIENT_TO_SERVER_NAME = "clientToServer.channel";
	protected final static String SERVER_TO_CLIENT_NAME = "serverToClient.channel";

	
	protected static File getSourceFile(final File directory, final JipcRole role) throws IOException {
		return getChannelFile(directory, role, JipcRole.Client);
	}
	
	protected static File getSinkFile(final File directory, final JipcRole role) throws IOException {
		return getChannelFile(directory, role, JipcRole.Server);
	}

	private static File getChannelFile(final File directory,
			final JipcRole role, JipcRole serverToClientRole)
			throws IOException {
		if (directory == null) {
			throw new IllegalArgumentException("parameter 'directory' must not be null");
		}
		if (!directory.exists()) {
			throw new IllegalArgumentException("directory '" + directory.getAbsolutePath()+ "' does not exist");
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("file '" + directory.getAbsolutePath()+ "' is not a directory");
		}
		if (role == null) {
			throw new IllegalArgumentException("parameter 'role' must not be null");
		}
		
		String name = CLIENT_TO_SERVER_NAME;
		if (role == serverToClientRole) {
			name = SERVER_TO_CLIENT_NAME;
		}
		File file = new File(directory, name);
		file.createNewFile();
		return file;
	}
}
