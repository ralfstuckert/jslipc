package org.jipc.channel.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractQueue;
import java.util.Iterator;

/**
 * A queue implementation that uses (a part of) a buffer for storing the queue elements and pointers.
 * It is dedicated to be used with shared memory.
 */
public class ByteBufferQueue extends AbstractQueue<Byte> {

	protected static final byte TRUE = (byte) 0x01;
	protected static final byte FALSE = 0x00;
	protected static final int POINTER_SIZE = 4;
	protected static final int HEAD_OFFSET = 0;
	protected static final int TAIL_OFFSET = HEAD_OFFSET + POINTER_SIZE;
	protected static final int INIT_OFFSET = TAIL_OFFSET + POINTER_SIZE;
	protected static final int CLOSED_OFFSET = INIT_OFFSET + 1;
	protected static final int QUEUE_OFFSET = CLOSED_OFFSET + 1;

	private int startIndex;
	private int length;
	private ByteBuffer buffer;

	public ByteBufferQueue(final ByteBuffer buffer, final int startIndex,
			final int length) {

		this.buffer = buffer;
		this.startIndex = startIndex;
		this.length = length;
	}

	public void init() {
		if (isInitialized()) {
			return;
		}
		setHead(getQueueStartIndex());
		setTail(getQueueStartIndex());
		buffer.put(getStartIndex() + CLOSED_OFFSET, FALSE);
		buffer.put(getStartIndex() + INIT_OFFSET, TRUE);
	}

	public boolean isInitialized() {
		return buffer == null
				|| buffer.get(getStartIndex() + INIT_OFFSET) == TRUE;
	}

	private void checkInitialized() {
		if (!isInitialized()) {
			throw new IllegalStateException("queue must be initialized first");
		}
	}
	
	@Override
	public int size() {
		checkInitialized();
		int tail = getTail();
		int head = getHead();
		if (tail >= head) {
			return tail - head;
		}
		return tail - getQueueStartIndex() + getEndIndex() - head+1;
	}

	@Override
	public Byte peek() {
		checkInitialized();
		if (isEmpty()) {
			return null;
		}
		int head = getHead();
		byte date = buffer.get(head);
		return date;
	}
	
	protected ByteBuffer getBuffer() {
		return buffer;
	}

	protected byte read() {
		int head = getHead();
		byte date = buffer.get(head);
		setHead(increment(head));
		return date;
	}

	protected void write(byte date) {
		int tail = getTail();
		buffer.put(tail, date);
		tail = increment(tail);
		setTail(tail);
	}

	protected int increment(int index) {
		int result = index + 1;
		if (result > getEndIndex()) {
			result = result - getEndIndex() + getQueueStartIndex() -1;
		}
		return result;
	}

	public boolean isEmpty() {
		checkInitialized();
		return size() == 0;
//		int tail = getTail();
//		int head = getHead();
//		return head == tail;
	}

	public boolean isFull() {
		checkInitialized();
		return size() == getCapacity();
//		int tail = getTail();
//		int head = getHead();
//		return increment(tail) == head;
	}

	public int getCapacity() {
		checkInitialized();
		return length-QUEUE_OFFSET-1;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return getStartIndex() + length- 1;
	}

	protected int getQueueStartIndex() {
		return getStartIndex() + QUEUE_OFFSET;
	}

	protected synchronized int getHead() {
		return buffer.getInt(getStartIndex() + HEAD_OFFSET);
	}

	protected synchronized void setHead(final int newHead) {
		buffer.putInt(getStartIndex() + HEAD_OFFSET, newHead);
	}

	protected synchronized int getTail() {
		return buffer.getInt(getStartIndex() + TAIL_OFFSET);
	}

	protected synchronized void setTail(final int newTail) {
		buffer.putInt(getStartIndex() + TAIL_OFFSET, newTail);
	}

	public void close() throws IOException {
		checkInitialized();
		buffer.put(getStartIndex() + CLOSED_OFFSET, TRUE);
	}

	public boolean isClosed() {
		checkInitialized();
		return buffer == null
				|| buffer.get(getStartIndex() + CLOSED_OFFSET) == TRUE;
	}

	@Override
	public boolean offer(Byte e) {
		checkInitialized();
		if (isFull()) {
			return false;
		}
		write(e);
		return true;
	}

	@Override
	public Byte poll() {
		checkInitialized();
		if (isEmpty()) {
			return null;
		}
		return read();
	}

	@Override
	public Iterator<Byte> iterator() {
		checkInitialized();
		return new ByteQueueIterator();
	}

	
	public class ByteQueueIterator implements Iterator<Byte> {

		@Override
		public boolean hasNext() {
			return !isEmpty();
		}

		@Override
		public Byte next() {
			return poll();
		}

		@Override
		public void remove() {
			remove();
		}
		
	}
}
