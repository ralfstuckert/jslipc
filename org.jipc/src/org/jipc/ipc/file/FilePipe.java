package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.file.ReadableJipcFileChannel;
import org.jipc.channel.file.WritableJipcFileChannel;

/**
 * The FilePipe uses a {@link ReadableJipcFileChannel} and
 * {@link WritableJipcFileChannel} to set up a pipe based on two files. You may
 * either provide {@link #FilePipe(File, File) both files} or a
 * {@link #FilePipe(File, JipcRole) directory and the role} of <em>this</em> end
 * of the pipe. This implementation provides a
 * {@link JipcBinman#cleanUpOnClose()} method, which will delete the files on
 * {@link #close()}.<br/>
 * <br/>
 * A FilePipe is bound only by the underlying OS' maximum file size and therefore
 * a write will not block until any limit is reached. But be aware that the underlying 
 * files always grow (by writes) and do not shrink (by reads).
 */
public class FilePipe implements JipcPipe, JipcBinman {

	public final static String YANG_TO_YIN_NAME = "yangToYin.channel";
	public final static String YIN_TO_YANG_NAME = "yinToYang.channel";

	private File sourceFile;
	private File sinkFile;
	private ReadableJipcFileChannel source;
	private WritableJipcFileChannel sink;
	private boolean cleanUpOnClose;

	/**
	 * This is an alternative to {@link #FilePipe(File, File)} where you do not
	 * pass the two files, but a directory hosting two files
	 * <code>yangToYin.channel</code> and
	 * <code>yinToYang.channel</code>. The files are created if they do not
	 * yet exist. Which one is used for source or sink depends on the role:
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
	 * The role itself does not have any special semantics, means: it makes no difference whether
	 * you are {@link JipcRole#Yin yin} or {@link JipcRole#Yang yang}. It is just needed
	 * to distinguish the endpoints of the pipe, so one end should have the role yin, the other
	 * yang.
	 * 
	 * @param directory
	 * @param role
	 * @throws IOException
	 */
	public FilePipe(final File directory, final JipcRole role)
			throws IOException {
		this(getSourceFile(directory, role), getSinkFile(directory, role));
	}

	/**
	 * Creates a pipe with a {@link ReadableJipcFileChannel} and
	 * {@link WritableJipcFileChannel} based on the given files.
	 * 
	 * @param source
	 *            the file to create the {@link ReadableJipcFileChannel} from.
	 * @param sink
	 *            the file to create the {@link WritableJipcFileChannel} from.
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

	protected static File getSourceFile(final File directory,
			final JipcRole role) throws IOException {
		return getChannelFile(directory, role, JipcRole.Yang);
	}

	protected static File getSinkFile(final File directory, final JipcRole role)
			throws IOException {
		return getChannelFile(directory, role, JipcRole.Yin);
	}

	private static File getChannelFile(final File directory,
			final JipcRole role, JipcRole yinToYangRole)
			throws IOException {
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
}
