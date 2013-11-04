package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.file.chunk.ReadableChunkFileChannel;
import org.jipc.channel.file.chunk.WritableChunkFileChannel;

public class ChunkFilePipe implements JipcPipe, JipcBinman {

	public final static String YANG_TO_YIN_NAME = "yangToYin";
	public final static String YIN_TO_YANG_NAME = "yinToYang";
	private File sourceDir;
	private File sinkDir;
	private ReadableChunkFileChannel source;
	private WritableChunkFileChannel sink;
	private boolean cleanUpOnClose;

	public ChunkFilePipe(final File directory, final JipcRole role)
			throws IOException {
		this(getSourceDir(directory, role), getSinkDir(directory, role));
	}

	public ChunkFilePipe(final File sourceDir, final File sinkDir) {
		checkDirectory(sourceDir, "sourceDir");
		checkDirectory(sinkDir, "sinkDir");

		this.sourceDir = sourceDir;
		this.sinkDir = sinkDir;

	}

	private void checkDirectory(File directory, String parameterName) {
		if (directory == null) {
			throw new IllegalArgumentException("parameter '" + parameterName
					+ "' must not be null");
		}
		if (!directory.exists()) {
			throw new IllegalArgumentException("directory '" + directory
					+ "' does not exist");
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("file '" + directory
					+ "' is not a directory");
		}
	}

	@Override
	public void cleanUpOnClose() {
		cleanUpOnClose = true;
		checkCleanUpOnClose();
	}

	private void checkCleanUpOnClose() {
		if (cleanUpOnClose) {
			if (source != null) {
				source.cleanUpOnClose();
			}
			if (sink != null) {
				sink.cleanUpOnClose();
			}
		}
	}

	@Override
	public void close() throws IOException {
		checkCleanUpOnClose();
		if (source != null) {
			source.close();
		}
		if (sink != null) {
			sink.close();
		}
	}


	@Override
	public ReadableChunkFileChannel source() throws IOException {
		if (source == null) {
			source = new ReadableChunkFileChannel(sourceDir);
			checkCleanUpOnClose();
		}
		return source;
	}

	@Override
	public WritableChunkFileChannel sink() throws IOException {
		if (sink == null) {
			sink = new WritableChunkFileChannel(sinkDir);
			checkCleanUpOnClose();
		}
		return sink;
	}

	protected static File getSourceDir(final File directory, final JipcRole role)
			throws IOException {
		return getChannelDir(directory, role, JipcRole.Yang);
	}

	protected static File getSinkDir(final File directory, final JipcRole role)
			throws IOException {
		return getChannelDir(directory, role, JipcRole.Yin);
	}

	private static File getChannelDir(final File parent, final JipcRole role,
			JipcRole yinToYangRole) throws IOException {
		if (parent == null) {
			throw new IllegalArgumentException(
					"parameter 'directory' must not be null");
		}
		if (!parent.exists()) {
			throw new IllegalArgumentException("directory '"
					+ parent.getAbsolutePath() + "' does not exist");
		}
		if (!parent.isDirectory()) {
			throw new IllegalArgumentException("file '"
					+ parent.getAbsolutePath() + "' is not a directory");
		}
		if (role == null) {
			throw new IllegalArgumentException(
					"parameter 'role' must not be null");
		}

		String name = YANG_TO_YIN_NAME;
		if (role == yinToYangRole) {
			name = YIN_TO_YANG_NAME;
		}
		File directory = new File(parent, name);
		directory.mkdir();
		if (!directory.exists()) {
			throw new IOException("directory '" + directory
					+ "' could not be created");
		}
		if (!directory.isDirectory()) {
			throw new IOException(
					"directory '"
							+ directory
							+ "' could not be created, a file with that name already exists");
		}
		return directory;
	}

}
