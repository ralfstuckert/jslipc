package org.jipc.channel.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jipc.TestUtil;
import org.jipc.channel.buffer.ByteBufferQueue;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class ByteBufferQueueTest {

	private final static int QUEUE_SIZE = 32;

	private static final List<String> UNINITIALIZED_PUBLIC_METHODS = Arrays
			.asList("init", "isInitialized", "getStartIndex", "getEndIndex");

	@DataPoints
	public static ByteBufferQueue[] createQueues() {
		ByteBuffer buffer = TestUtil.createByteBuffer(QUEUE_SIZE);
		ByteBufferQueue queue = new ByteBufferQueue(buffer, 0, QUEUE_SIZE);
		ByteBuffer bigBuffer = TestUtil.createByteBuffer(100);
		ByteBufferQueue boundedQueue = new ByteBufferQueue(bigBuffer, 12,
				QUEUE_SIZE);
		return new ByteBufferQueue[] { queue, boundedQueue };
	}

	@Theory
	public void testSize(final ByteBufferQueue queue) {
		queue.init();
		assertEquals(0, queue.size());

		queue.add((byte) 0x17);
		assertEquals(1, queue.size());

		queue.add((byte) 0x17);
		queue.add((byte) 0x17);
		queue.add((byte) 0x17);
		queue.add((byte) 0x17);
		assertEquals(5, queue.size());

		queue.remove();
		queue.remove();
		assertEquals(3, queue.size());

		queue.remove();
		queue.remove();
		queue.remove();
		assertEquals(0, queue.size());

		// already 5 added/remove, now
		// overflow buffer (tail < head)
		int filledUp = queue.getCapacity() - 3;
		for (int i = 0; i < filledUp; i++) {
			assertTrue(queue.offer((byte) 17));
		}
		assertEquals(filledUp, queue.size());

		assertTrue(queue.offer((byte) 17));
		assertTrue(queue.offer((byte) 17));
		assertTrue(queue.offer((byte) 17));
		assertEquals(queue.getCapacity(), queue.size());

		for (int i = 0; i < queue.getCapacity(); i++) {
			assertNotNull(queue.remove());
		}
		assertEquals(0, queue.size());
	}

	@Theory
	public void testIsEmpty(final ByteBufferQueue queue) {
		queue.init();
		assertTrue(queue.isEmpty());

		queue.add((byte) 0x17);
		assertFalse(queue.isEmpty());

		queue.add((byte) 0x17);
		assertFalse(queue.isEmpty());

		queue.remove();
		assertFalse(queue.isEmpty());

		queue.remove();
		assertTrue(queue.isEmpty());
	}

	@Test
	public void testByteBufferQueue() {
		ByteBuffer buffer = TestUtil.createByteBuffer(QUEUE_SIZE);
		ByteBufferQueue queue = new ByteBufferQueue(buffer, 0, 16);
		queue.init();
		assertEquals(16 - ByteBufferQueue.QUEUE_OFFSET - 1, queue.getCapacity());
		assertEquals(0, queue.getStartIndex());
		assertEquals(15, queue.getEndIndex());
	}

	@Theory
	public void testInit(final ByteBufferQueue queue) {
		int start = queue.getStartIndex();
		int end = start + 10;
		for (int index = start; index < end; ++index) {
			assertEquals((byte) 0xff, queue.getBuffer().get(index));
		}

		queue.init();
		assertTrue(queue.isInitialized());
		assertFalse(queue.isClosed());
		assertEquals(queue.getQueueStartIndex(), queue.getHead());
		assertEquals(queue.getQueueStartIndex(), queue.getTail());
	}

	@Theory
	public void testIsInitialized(final ByteBufferQueue queue) {
		assertFalse(queue.isInitialized());
		queue.init();
		assertTrue(queue.isInitialized());
	}

	@Theory
	public void testPeek(final ByteBufferQueue queue) {
		queue.init();
		assertEquals(null, queue.peek());
		assertEquals(null, queue.peek());

		queue.offer((byte) 17);
		queue.offer((byte) 44);
		queue.offer((byte) 88);

		assertEquals(new Byte((byte) 17), queue.peek());
		assertEquals(new Byte((byte) 17), queue.peek());
		assertEquals(new Byte((byte) 17), queue.peek());
	}

	@Theory
	public void testIsFull(final ByteBufferQueue queue) {
		queue.init();

		assertFalse(queue.isFull());

		for (int i = 0; i < queue.getCapacity() - 1; i++) {
			queue.offer((byte) 17);
		}
		assertFalse(queue.isFull());

		queue.offer((byte) 17);
		assertTrue(queue.isFull());
	}

	@Theory
	public void testGetCapacity(final ByteBufferQueue queue) {
		queue.init();
		assertEquals(QUEUE_SIZE - ByteBufferQueue.QUEUE_OFFSET - 1,
				queue.getCapacity());
	}

	@Theory
	public void testClose(final ByteBufferQueue queue) throws Exception {
		queue.init();
		assertFalse(queue.isClosed());

		queue.close();
		assertTrue(queue.isClosed());
	}

	@Theory
	public void testIsClosed(final ByteBufferQueue queue) {
		queue.init();
		assertFalse(queue.isClosed());

		queue.getBuffer().put(
				queue.getStartIndex() + ByteBufferQueue.CLOSED_OFFSET,
				ByteBufferQueue.TRUE);
		assertTrue(queue.isClosed());
	}

	@Theory
	public void testOffer(final ByteBufferQueue queue) {
		queue.init();

		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) 17));
		}

		assertFalse(queue.offer((byte) 17));

		queue.remove();
		queue.remove();
		assertTrue(queue.offer((byte) 17));
		assertTrue(queue.offer((byte) 17));
		assertFalse(queue.offer((byte) 17));
	}

	@Theory
	public void testPoll(final ByteBufferQueue queue) {
		queue.init();

		assertNull(queue.poll());

		assertTrue(queue.offer((byte) 17));
		assertTrue(queue.offer((byte) 17));
		assertTrue(queue.offer((byte) 17));
		assertEquals(new Byte((byte) 17), queue.poll());
		assertEquals(new Byte((byte) 17), queue.poll());
		assertEquals(new Byte((byte) 17), queue.poll());
		assertNull(queue.poll());

		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) i));
		}
		for (int i = 0; i < queue.getCapacity(); i++) {
			assertEquals(new Byte((byte) i), queue.poll());
		}

	}

	@Theory
	public void testIterator(final ByteBufferQueue queue) {
		queue.init();
		assertNotNull(queue.iterator());
		assertFalse(queue.iterator().hasNext());

		Iterator<Byte> iterator = queue.iterator();
		assertFalse(iterator.hasNext());

		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) 17));
			assertTrue(iterator.hasNext());
			assertEquals(new Byte((byte) 17), iterator.next());
		}

		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) i));
			assertTrue(iterator.hasNext());
			assertEquals(new Byte((byte) i), iterator.next());
		}

	}

	@Test
	public void testAccessUnitializedByteBufferQueue() throws Throwable {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(QUEUE_SIZE);
		callAllPublicMethods(queue, true);

		queue.init();
		callAllPublicMethods(queue, false);
	}


	private void callAllPublicMethods(ByteBufferQueue queue,
			boolean expectException) throws Throwable {
		Method[] declaredMethods = ByteBufferQueue.class.getDeclaredMethods();
		Method closeMethod = null;
		for (Method method : declaredMethods) {
			if (UNINITIALIZED_PUBLIC_METHODS.contains(method.getName()))  {
				continue;
			}
			if ("close".equals(method.getName())) {
				closeMethod = method;
				continue;
			}
			if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
				continue;
			}
			checkCallMethod(queue, expectException, method);
		}
		// call close at last since it invalidates the queue
		checkCallMethod(queue, expectException, closeMethod);
	}

	private void checkCallMethod(ByteBufferQueue queue,
			boolean expectException, Method method)
			throws IllegalAccessException, Throwable, InvocationTargetException {
		try {
			Object[] params = new Object[method.getParameterTypes().length];
			if ("offer".equals(method.getName())) {
				params[0] = new Byte((byte) 12);
			}
			method.invoke(queue, params); // offer(byte)
			if (expectException) {
				fail("expected IllegalStateException on call to "
						+ method.getName());
			}
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof IllegalStateException)) {
				throw e.getCause();
			}
			if (!expectException) {
				throw e;
			}
		}
	}

}
