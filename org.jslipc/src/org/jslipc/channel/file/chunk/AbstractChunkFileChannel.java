package org.jslipc.channel.file.chunk;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.util.regex.Pattern;

import org.jslipc.JslipcBinman;
import org.jslipc.channel.JslipcChannel;
import org.jslipc.util.FileUtil;

/**
 * Common base class for chunk file based {@link JslipcChannel}s.
 */
public abstract class AbstractChunkFileChannel implements JslipcChannel,
		InterruptibleChannel, JslipcBinman {

	protected static final String CHUNK_FILE_NAME = ".chunk";
	
	private File directory;
	private boolean deleteFilesOnClose;
	private boolean closed;
	private File closeMarker;
	private ChunkFilenameFilter filenameFilter;

	public AbstractChunkFileChannel(final File directory) {
		this.directory = directory;
		closeMarker = new File(directory, ".closed");
		filenameFilter = new ChunkFilenameFilter();
	}

	protected File getDirectory() {
		return directory;
	}

	protected ChunkFilenameFilter getFilenameFilter() {
		return filenameFilter;
	}
	
	protected void checkClosed() throws ClosedChannelException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
	}

	@Override
	public JslipcChannelState getState() {
		if (!isOpen()) {
			return JslipcChannelState.Closed;
		}
		if (hasCloseMarker()) {
			return JslipcChannelState.ClosedByPeer;
		}
		return JslipcChannelState.Open;
	}

	@Override
	public void close() throws IOException {
		if (!isOpen()) {
			return;
		}
		this.closed = true;

		boolean closedByPeer = !getCloseMarker().createNewFile();
		if (closedByPeer) {
			if (deleteFilesOnClose) {
				FileUtil.delete(directory, true);
			}
		}
	}

	protected File getCloseMarker() {
		return closeMarker;
	}

	private boolean hasCloseMarker() {
		return getCloseMarker().exists();
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	@Override
	public void cleanUpOnClose() {
		deleteFilesOnClose = true;
	}

	private static class ChunkFilenameFilter implements FilenameFilter {

		private Pattern pattern = Pattern.compile(".*_\\d");

		@Override
		public boolean accept(File dir, String name) {
			return pattern.matcher(name).matches();
		}

	}


}
