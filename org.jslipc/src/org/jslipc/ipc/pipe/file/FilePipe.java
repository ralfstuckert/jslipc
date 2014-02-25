package org.jslipc.ipc.pipe.file;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.channel.file.ReadableJslipcFileChannel;
import org.jslipc.channel.file.WritableJslipcFileChannel;
import org.jslipc.util.FileUtil;
import org.jslipc.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The FilePipe uses a {@link ReadableJslipcFileChannel} and
 * {@link WritableJslipcFileChannel} to set up a pipe based on two files. You may
 * either provide {@link #FilePipe(File, File) both files} or a
 * {@link #FilePipe(File, JslipcRole) directory and the role} of <em>this</em> end
 * of the pipe. This implementation provides a
 * {@link JslipcBinman#cleanUpOnClose()} method, which will delete the files on
 * {@link #close()}.<br/>
 * <br/>
 * A FilePipe is bound only by the underlying OS' maximum file size and
 * therefore a write will not block until any limit is reached. But be aware
 * that the underlying files always grow (by writes) and do not shrink (by
 * reads).
 */
public class FilePipe implements JslipcPipe, JslipcBinman {
	
	private final static Logger LOGGER = LoggerFactory
			.getLogger(FilePipe.class);

	public final static String YANG_TO_YIN_NAME = "yangToYin.channel";
	public final static String YIN_TO_YANG_NAME = "yinToYang.channel";

	private File pipeDir;
	private File sourceFile;
	private File sinkFile;
	private ReadableJslipcFileChannel source;
	private WritableJslipcFileChannel sink;
	private boolean cleanUpOnClose;

	/**
	 * This is an alternative to {@link #FilePipe(File, File)} where you do not
	 * pass the two files, but a directory hosting two files
	 * <code>yangToYin.channel</code> and <code>yinToYang.channel</code>. The
	 * files are created if they do not yet exist. Which one is used for source
	 * or sink depends on the role:
	 * <table border="1">
	 * <tr>
	 * <th>role</th>
	 * <th>source</th>
	 * <th>sink</th>
	 * </tr>
	 * <tr>
	 * <td>yang</td>
	 * <td>yinToYang.channel</td>
	 * <td>yangToYin.channel</td>
	 * </tr>
	 * <tr>
	 * <td>yin</td>
	 * <td>yangToYin.channel</td>
	 * <td>yinToYang.channel</td>
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
	public FilePipe(final File directory, final JslipcRole role)
			throws IOException {
		this(getSourceFile(directory, role), getSinkFile(directory, role));
		pipeDir = directory;

		LOGGER.info("created FilePipe with directory {} and role {}",
				directory, role);
	}

	/**
	 * Creates a pipe with a {@link ReadableJslipcFileChannel} and
	 * {@link WritableJslipcFileChannel} based on the given files.
	 * 
	 * @param source
	 *            the file to create the {@link ReadableJslipcFileChannel} from.
	 * @param sink
	 *            the file to create the {@link WritableJslipcFileChannel} from.
	 */
	public FilePipe(final File source, final File sink) {
		if (source == null) {
			throw new IllegalArgumentException(
					"parameter 'source' must not be null");
		}
		if (sink == null) {
			throw new IllegalArgumentException(
					"parameter 'sink' must not be null");
		}
		this.sourceFile = source;
		this.sinkFile = sink;

		LOGGER.info("created FilePipe with source {} and sink {}", source, sink);
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
		if (cleanUpOnClose && pipeDir != null && !sourceFile.exists()
				&& !sinkFile.exists()) {
			LOGGER.debug("deleting pipe directory {}", pipeDir);
			FileUtil.delete(pipeDir, true);
		}

		LOGGER.info("closed FilePipe with source {} and sink {}",
				sourceFile, sinkFile);
	}

	@Override
	public ReadableJslipcFileChannel source() throws IOException {
		if (source == null) {
			source = new ReadableJslipcFileChannel(sourceFile);
			checkCleanUpOnClose();
		}
		return source;
	}

	@Override
	public WritableJslipcFileChannel sink() throws IOException {
		if (sink == null) {
			sink = new WritableJslipcFileChannel(sinkFile);
			checkCleanUpOnClose();
		}
		return sink;
	}

	public static File getSourceFile(final File directory, final JslipcRole role)
			throws IOException {
		return getChannelFile(directory, role, JslipcRole.Yang);
	}

	public static File getSinkFile(final File directory, final JslipcRole role)
			throws IOException {
		return getChannelFile(directory, role, JslipcRole.Yin);
	}

	private static File getChannelFile(final File directory,
			final JslipcRole role, JslipcRole yinToYangRole) throws IOException {
		if (directory == null) {
			throw new IllegalArgumentException(
					"parameter 'directory' must not be null");
		}
		if (!directory.exists()) {
			throw new IllegalArgumentException("directory '"
					+ directory.getAbsolutePath() + "' does not exist");
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("file '"
					+ directory.getAbsolutePath() + "' is not a directory");
		}
		if (role == null) {
			throw new IllegalArgumentException(
					"parameter 'role' must not be null");
		}

		String name = YANG_TO_YIN_NAME;
		if (role == yinToYangRole) {
			name = YIN_TO_YANG_NAME;
		}
		File file = new File(directory, name);
		file.createNewFile();
		return file;
	}
	
	@Override
	public String toString() {
		return StringUtil.build(this).add("source", sourceFile).add("sink", sinkFile).toString();
	}
}
