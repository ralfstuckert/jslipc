package org.jipc.ipc.shm;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.buffer.ByteBufferQueue;
import org.jipc.channel.buffer.ReadableBbqChannel;
import org.jipc.channel.buffer.WritableBbqChannel;

public class SharedMemoryPipe implements JipcPipe {


	private static final int DEFAULT_SIZE = 4096;
	private RandomAccessFile mappedFile;
	private ByteBufferQueue inQueue;
	private ByteBufferQueue outQueue;
	private ReadableBbqChannel source;
	private WritableBbqChannel sink;
	private FileLock lock;

	public SharedMemoryPipe(final File file, JipcRole role) throws IOException {
		this(file, DEFAULT_SIZE, role);
	}

	public SharedMemoryPipe(final File file, final int fileSize, JipcRole role)
			throws IOException {
		mappedFile = new RandomAccessFile(file, "rw");
		FileChannel fileChannel = mappedFile.getChannel();
		try {
			lock = fileChannel.tryLock();
		} catch (OverlappingFileLockException e) {
			// already locked
		}
		MappedByteBuffer buffer = fileChannel.map(MapMode.READ_WRITE, 0,
				fileSize);
		inQueue = createQueue(buffer, fileSize, role, true);
		outQueue = createQueue(buffer, fileSize, role, false);
	}

	protected ByteBufferQueue createQueue(final MappedByteBuffer buffer,
			final int fileSize, final JipcRole role, final boolean in)
			throws IOException {
		int queueSize = fileSize / 2;
		boolean out = !in;
		ByteBufferQueue queue = null;
		if (role == JipcRole.Server && in || role == JipcRole.Client && out) {
			queue = new ByteBufferQueue(buffer, 0, queueSize);
		} else {
			queue = new ByteBufferQueue(buffer, queueSize, queueSize);
		}

		if (lock != null) {
			queue.init();
		}
		return queue;
	}

	protected ByteBufferQueue getInQueue() {
		return inQueue;
	}

	protected ByteBufferQueue getOutQueue() {
		return outQueue;
	}

	protected RandomAccessFile getFile() {
		return mappedFile;
	}

	@Override
	public ReadableBbqChannel source() {
		if (source == null) {
			source = new ReadableBbqChannel(inQueue);
		}
		return source;
	}

	@Override
	public WritableBbqChannel sink() {
		if (sink == null) {
			sink = new WritableBbqChannel(outQueue);
		}
		return sink;
	}
}
