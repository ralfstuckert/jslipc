package org.jslipc.ipc.pipe.file;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.channel.file.chunk.ReadableChunkFileChannel;
import org.jslipc.channel.file.chunk.WritableChunkFileChannel;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jslipc.util.FileUtil;
import org.jslipc.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In contrast to the {@link FilePipe} this implementation is based on chunk
 * files which are written to a directory by a {@link WritableChunkFileChannel}
 * and consumed by a {@link ReadableChunkFileChannel}. Once consumed, a chunk is
 * immediately deleted from disk, so only yet unread data needs to be persisted.
 * This way, the ChunkFilePipe does not block like the {@link SharedMemoryPipe}
 * and does not waste disk space like the {@link FilePipe}. The price is speed,
 * since creating vast of files costs time.<br/>
 * <br/>
 * You may either provide {@link #ChunkFilePipe(File, File) both directories} or
 * a {@link #ChunkFilePipe(File, JslipcRole) directory and the role} of
 * <em>this</em> end of the pipe. This implementation provides a
 * {@link JslipcBinman#cleanUpOnClose()} method, which will delete the files on
 * {@link #close()}.<br/>
 * <br/>
 */
public class ChunkFilePipe implements JslipcPipe, JslipcBinman {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ChunkFilePipe.class);

	public final static String YANG_TO_YIN_NAME = "yangToYin";
	public final static String YIN_TO_YANG_NAME = "yinToYang";
	private File pipeDir;
	private File sourceDir;
	private File sinkDir;
	private ReadableChunkFileChannel source;
	private WritableChunkFileChannel sink;
	private boolean cleanUpOnClose;

	/**
	 * This is an alternative to {@link #ChunkFilePipe(File, File)} where you do
	 * not pass the two files, but a directory hosting two files
	 * <code>yangToYin</code> and <code>yinToYang</code>. The files are created
	 * if they do not yet exist. Which one is used for source or sink depends on
	 * the role:
	 * <table border="1">
	 * <tr>
	 * <th>role</th>
	 * <th>source</th>
	 * <th>sink</th>
	 * </tr>
	 * <tr>
	 * <td>yang</td>
	 * <td>yinToYang</td>
	 * <td>yangToYin</td>
	 * </tr>
	 * <tr>
	 * <td>yin</td>
	 * <td>yangToYin</td>
	 * <td>yinToYang</td>
	 * </tr>
	 * </table>
	 * The role itself does not have any special semantics, means: it makes no
	 * difference whether you are {@link JslipcRole#Yin yin} or
	 * {@link JslipcRole#Yang yang}. It is just needed to distinguish the
	 * endpoints of the pipe, so one end should have the role yin, the other
	 * yang.
	 * 
	 * @param directory
	 * @param role
	 * @throws IOException
	 */
	public ChunkFilePipe(final File directory, final JslipcRole role)
			throws IOException {
		this(getSourceDir(directory, role), getSinkDir(directory, role));
		pipeDir = directory;

		LOGGER.info("created ChunkFilePipe with directory {} and role {}",
				directory, role);
	}

	/**
	 * Creates a pipe with a {@link ReadableChunkFileChannel} and
	 * {@link WritableChunkFileChannel} based on the given directories.
	 * 
	 * @param sourceDir
	 *            the directory to create the {@link ReadableChunkFileChannel}
	 *            from.
	 * @param sinkDir
	 *            the directory to create the {@link WritableChunkFileChannel}
	 *            from.
	 */
	public ChunkFilePipe(final File sourceDir, final File sinkDir) {
		checkDirectory(sourceDir, "sourceDir");
		checkDirectory(sinkDir, "sinkDir");

		this.sourceDir = sourceDir;
		this.sinkDir = sinkDir;

		LOGGER.info("created ChunkFilePipe with source {} and sink {}",
				sourceDir, sinkDir);
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
		if (cleanUpOnClose) {
			// issue 15: force source() and sink() if cleanUpOnClose
			source();
			sink();
		}
		checkCleanUpOnClose();
		if (source != null) {
			source.close();
		}
		if (sink != null) {
			sink.close();
		}
		if (cleanUpOnClose && pipeDir != null && !sourceDir.exists()
				&& !sinkDir.exists()) {
			LOGGER.debug("deleting pipe directory {}", pipeDir);
			FileUtil.delete(pipeDir, true);
		}

		LOGGER.info("closed ChunkFilePipe with source {} and sink {}",
				sourceDir, sinkDir);
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

	public static File getSourceDir(final File directory, final JslipcRole role)
			throws IOException {
		return getChannelDir(directory, role, JslipcRole.Yang);
	}

	public static File getSinkDir(final File directory, final JslipcRole role)
			throws IOException {
		return getChannelDir(directory, role, JslipcRole.Yin);
	}

	private static File getChannelDir(final File parent, final JslipcRole role,
			JslipcRole yinToYangRole) throws IOException {
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

	@Override
	public String toString() {
		return StringUtil.build(this).add("source", sourceDir).add("sink", sinkDir).toString();
	}
}
