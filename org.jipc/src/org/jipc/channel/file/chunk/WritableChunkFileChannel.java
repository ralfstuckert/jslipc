package org.jipc.channel.file.chunk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jipc.channel.WritableJipcByteChannel;

/**
 * A {@link WritableJipcByteChannel} implementation that writes the data as
 * chunk files into a given directory. During {@link #write(ByteBuffer) writing}, 
 * the file is named <code>.chunk_xx.tmp</code>, where <code>xx</code> is the index of the chunk.
 * Once the complete buffer is written, the file is renamed to <code>.chunk_xx</code> in 
 * order to signal a consuming {@link ReadableChunkFileChannel} that is chunk is completely
 * written and may be read.
 */
public class WritableChunkFileChannel extends AbstractChunkFileChannel
		implements WritableJipcByteChannel {

	private static final String TMP_SUFFIX = ".tmp";
	private int nextChunkIndex = 0;

	public WritableChunkFileChannel(File directory) {
		super(directory);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkClosed();
		if (getState() == JipcChannelState.ClosedByPeer) {
			return 0;
		}

		File chunk = getNextChunk();
		RandomAccessFile raf = new RandomAccessFile(chunk, "rw");
		FileChannel channel = raf.getChannel();
		int count = channel.write(src);
		channel.close();
		raf.close();
		String name = chunk.getName();
		chunk.renameTo(new File(getDirectory(), name.substring(0, name.length()
				- TMP_SUFFIX.length())));
		return count;
	}

	protected File getNextChunk() {
		return getNextChunk(CHUNK_FILE_NAME);
	}

	private File getNextChunk(final String filePrefix) {
		File nextChunk = new File(getDirectory(), getFileName(filePrefix,
				nextChunkIndex));
		if (!nextChunk.exists()) {
			++nextChunkIndex;
			return nextChunk;
		}

		nextChunk = null;
		File[] list = getDirectory().listFiles(getFilenameFilter());
		if (list == null) {
			return null;
		}
		// find maximum index that is > nextChunkIndex
		int currentMax = nextChunkIndex;
		for (File file : list) {
			int current = getIndex(file);
			if (current > currentMax) {
				currentMax = current;
			}
		}
		nextChunkIndex = currentMax + 1;
		nextChunk = new File(getFileName(CHUNK_FILE_NAME, nextChunkIndex));
		nextChunkIndex++;

		return nextChunk;
	}

	private String getFileName(String filePrefix, int index) {
		StringBuilder bob = new StringBuilder(filePrefix.length() + 10);
		bob.append(filePrefix);
		bob.append("_");
		bob.append(Integer.toString(index));
		bob.append(TMP_SUFFIX);
		return bob.toString();
	}

	private int getIndex(final File file) {
		String name = file.getName();
		int begin = name.lastIndexOf('_') + 1;
		int end = begin + 1;
		while (end < name.length() && Character.isDigit(name.charAt(end))) {
			++end;
		}
		return Integer.parseInt(name.substring(begin, end));
	}

}
