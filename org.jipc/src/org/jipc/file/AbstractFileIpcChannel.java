package org.jipc.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class AbstractFileIpcChannel implements Channel {

	protected FileChannel fileChannel;
	protected RandomAccessFile file;
	private File closeMarker;
	private boolean closed;

	public AbstractFileIpcChannel(File file, String mode) throws IOException {
		this.file = new RandomAccessFile(file, mode);
		this.closeMarker = new File(file.getAbsolutePath() + ".closed");
		fileChannel = this.file.getChannel();
	}

	protected FileChannel getFileChannel() {
		return fileChannel;
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
		getCloseMarker().createNewFile();
	}

	private File getCloseMarker() {
		return closeMarker;
	}
	
	protected boolean hasCloseMarker() {
		return getCloseMarker().exists();
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

}
