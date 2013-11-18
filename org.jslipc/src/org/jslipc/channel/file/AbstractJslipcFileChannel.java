package org.jslipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

import org.jslipc.JslipcBinman;
import org.jslipc.channel.JslipcChannel;
import org.jslipc.util.FileUtil;

/**
 * Common base class for file based {@link JslipcChannel}s.
 */
public abstract class AbstractJslipcFileChannel implements JslipcChannel, JslipcBinman  {

	protected FileChannel fileChannel;
	private RandomAccessFile randomAccessFile;
	private File closeMarker;
	private boolean closed;
	private boolean deleteFilesOnClose;
	private File file;

	public AbstractJslipcFileChannel(File file, String mode) throws IOException {
		this.file = file;
		this.randomAccessFile = new RandomAccessFile(file, mode);
		this.closeMarker = new File(file.getAbsolutePath() + ".closed");
		fileChannel = this.randomAccessFile.getChannel();
	}

	@Override
	public void cleanUpOnClose() {
		deleteFilesOnClose = true;
	}

	protected FileChannel getFileChannel() {
		return fileChannel;
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

	protected void checkClosed() throws ClosedChannelException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
	}

	@Override
	public void close() throws IOException {
		if (!isOpen()) {
			return;
		}
		this.closed = true;
		fileChannel.close();
		randomAccessFile.close();
		
		boolean closedByPeer = !getCloseMarker().createNewFile();
		if (closedByPeer) {
			if (deleteFilesOnClose) {
				FileUtil.delete(file);
				FileUtil.delete(getCloseMarker());
			}
		}
	}

	private File getCloseMarker() {
		return closeMarker;
	}

	private boolean hasCloseMarker() {
		return getCloseMarker().exists();
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

}
