package org.jipc.channel.file.chunk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jipc.channel.ReadableJipcByteChannel;
import org.jipc.util.FileUtil;

/**
 * A {@link ReadableJipcByteChannel} implementation that reads chunk files written into
 * the given directory by a {@link WritableChunkFileChannel}. Read chunks will be deleted
 * immediately to save disk space.
 */
public class ReadableChunkFileChannel extends AbstractChunkFileChannel
		implements ReadableJipcByteChannel {

	private RandomAccessFile currentChunkRAF;
	private File currentChunkFile;
	private int nextChunkIndex = 0;
	private FileChannel currentChunkFileChannel;

	public ReadableChunkFileChannel(final File directory) {
		super(directory);
	}

	protected FileChannel getChunkFileChannel() throws IOException {
		if (currentChunkFileChannel != null) {
			return currentChunkFileChannel;
		}
		if (currentChunkRAF == null) {
			currentChunkFile = getNextChunk();
			if (currentChunkFile != null) {
				currentChunkRAF = new RandomAccessFile(currentChunkFile, "r");
			}
		}
		if (currentChunkRAF != null) {
			currentChunkFileChannel = currentChunkRAF.getChannel();
		}
		return currentChunkFileChannel;
	}

	protected void markChunkRead() throws IOException {
		if (currentChunkFileChannel != null) {
			currentChunkFileChannel.close();
			currentChunkFileChannel = null;
		}
		if (currentChunkRAF != null) {
			currentChunkRAF.close();
			currentChunkRAF = null;
		}
		if (currentChunkFile != null) {
			FileUtil.delete(currentChunkFile);
		}
	}

	protected File getNextChunk() {
		return getNextChunk(CHUNK_FILE_NAME);
	}

	private File getNextChunk(final String filePrefix) {
		File nextChunk = new File(getDirectory(), getFileName(filePrefix,
				nextChunkIndex));
		if (nextChunk.exists()) {
			++nextChunkIndex;
			return nextChunk;
		}

		nextChunk = null;
		File[] list = getDirectory().listFiles(getFilenameFilter());
		if (list == null) {
			return null;
		}
		// find minimum index that is >= nextChunkIndex
		int currentMin = Integer.MAX_VALUE;
		for (File file : list) {
			int current = getIndex(file);
			if (current >= nextChunkIndex && current < currentMin) {
				currentMin = current;
				nextChunk = file;
			}
		}
		if (nextChunk != null) {
			nextChunkIndex = currentMin + 1;
		}

		return nextChunk;
	}

	private String getFileName(String filePrefix, int index) {
		StringBuilder bob = new StringBuilder(filePrefix.length() + 10);
		bob.append(filePrefix);
		bob.append("_");
		bob.append(Integer.toString(index));
		return bob.toString();
	}

	private int getIndex(final File file) {
		int begin = file.getName().lastIndexOf('_');
		return Integer.parseInt(file.getName().substring(begin + 1));
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int sum = 0;
		int count = 0;
		while ((count = readInternal(dst)) > 0) {
			sum += count;
		}
		if (sum == 0) {
			return count;
		}
		return sum;
	}

	private int readInternal(ByteBuffer dst) throws IOException {
		checkClosed();
		FileChannel channel = getChunkFileChannel();
		if (channel == null) {
			if (getState() != JipcChannelState.Open) {
				return -1;
			}
			return 0;
		}
		int count = channel.read(dst);
		if (count != -1) {
			return count;
		}
		markChunkRead();
		return read(dst);
	}

}
