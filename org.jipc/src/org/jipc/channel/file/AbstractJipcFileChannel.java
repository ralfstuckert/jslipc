package org.jipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

import org.jipc.JipcBinman;
import org.jipc.channel.JipcChannel;

/**
 * Common base class for file based {@link JipcChannel}s.
 */
public abstract class AbstractJipcFileChannel implements JipcChannel, JipcBinman  {

	protected FileChannel fileChannel;
	private RandomAccessFile randomAccessFile;
	private File closeMarker;
	private boolean closed;
	private boolean deleteFilesOnClose;
	private File file;

	public AbstractJipcFileChannel(File file, String mode) throws IOException {
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
	public JipcChannelState getState() {

		if (!isOpen()) {
			return JipcChannelState.Closed;
		}
		if (hasCloseMarker()) {
			return JipcChannelState.ClosedByPeer;
		}
		return JipcChannelState.Open;
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
		
		boolean closedByPeer = !getCloseMarker().createNewFile();
		if (closedByPeer) {
			fileChannel.close();
			randomAccessFile.close();
			if (deleteFilesOnClose) {
				file.delete();
				getCloseMarker().delete();
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
