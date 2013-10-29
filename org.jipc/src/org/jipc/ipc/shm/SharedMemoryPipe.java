package org.jipc.ipc.shm;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannel.JipcChannelState;
import org.jipc.channel.buffer.BufferUtil;
import org.jipc.channel.buffer.ByteBufferQueue;
import org.jipc.channel.buffer.ReadableBbqChannel;
import org.jipc.channel.buffer.WritableBbqChannel;

/**
 * This pipe uses shared memory to create in-memory buffers which are used to
 * set up two {@link ByteBufferQueue}s as a backend for two (uni-directional)
 * channels. Which buffer/channel is used for reading or writing depends on the
 * given {@link JipcRole role}, so the role is needed to distinguish the two
 * endpoints, but do not have any further semantics. <br/>
 * <br/>
 * Be aware that the buffers are limited to the given size and thus are
 * implemented as ring-buffers. So, you can not write if the buffer is full, or
 * read if the buffer is empty. If you are using streams on top of the channel,
 * they will block in this situation. The content of the shared memory is never
 * forced to write back to disk, so the buffer is volatile.
 */
public class SharedMemoryPipe implements JipcPipe, JipcBinman {

	private static final int DEFAULT_SIZE = 4096;
	private File file;
	private RandomAccessFile mappedFile;
	private ByteBufferQueue inQueue;
	private ByteBufferQueue outQueue;
	private ReadableBbqChannel source;
	private WritableBbqChannel sink;
	private FileLock lock;
	private boolean cleanUpOnClose;
	private MappedByteBuffer buffer;

	/**
	 * Creates a pipe with the given parameter in shared memory. The given file
	 * is mapped into memory (to create shared memory), where a default size of
	 * 4k of shared memory is allocated. The role itself does not have any
	 * special semantics, means: it makes no difference whether you are
	 * {@link JipcRole#Yin server} or {@link JipcRole#Yang yang}. It is
	 * just needed to distinguish the endpoints of the pipe, so one end should
	 * have the role yin, the other yang.
	 */
	public SharedMemoryPipe(final File file, JipcRole role) throws IOException {
		this(file, DEFAULT_SIZE, role);
	}

	/**
	 * Creates a pipe with the given parameter in shared memory. The given file
	 * is mapped into memory (to create shared memory), where the size indicates
	 * the amount of shared memory to allocate. The role itself does not have
	 * any special semantics, means: it makes no difference whether you are
	 * {@link JipcRole#Yin server} or {@link JipcRole#Yang yang}. It is
	 * just needed to distinguish the endpoints of the pipe, so one end should
	 * have the role yin, the other yang.
	 * 
	 * @param file
	 * @param size
	 * @param role
	 * @throws IOException
	 */
	public SharedMemoryPipe(final File file, final int size, JipcRole role)
			throws IOException {
		this.file = file;
		mappedFile = new RandomAccessFile(file, "rw");
		FileChannel fileChannel = mappedFile.getChannel();
		try {
			lock = fileChannel.tryLock();
		} catch (OverlappingFileLockException e) {
			// already locked
		}
		buffer = fileChannel.map(MapMode.READ_WRITE, 0, size);
		inQueue = createQueue(buffer, size, role, true);
		outQueue = createQueue(buffer, size, role, false);
	}

	@Override
	public void cleanUpOnClose() {
		cleanUpOnClose = true;
	}

	@Override
	public void close() throws IOException {
		boolean sourceClosedByPeer = false;
		boolean sinkClosedByPeer = false;
		if (source != null) {
			sourceClosedByPeer = source.getState() == JipcChannelState.ClosedByPeer;
			source.close();
		}
		if (sink != null) {
			sinkClosedByPeer = sink.getState() == JipcChannelState.ClosedByPeer;
			sink.close();
		}
		if (lock != null) {
			lock.release();
			lock = null;
		}
		if (mappedFile != null) {
			mappedFile.close();
		}
		BufferUtil.releaseBufferSilently(buffer);

		if (cleanUpOnClose && sourceClosedByPeer && sinkClosedByPeer) {
			file.delete();
		}
	}

	protected ByteBufferQueue createQueue(final MappedByteBuffer buffer,
			final int fileSize, final JipcRole role, final boolean in)
			throws IOException {
		int queueSize = fileSize / 2;
		boolean out = !in;
		ByteBufferQueue queue = null;
		if (role == JipcRole.Yin && in || role == JipcRole.Yang && out) {
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
