package org.jipc.ipc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jipc.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for both {@link JipcRequestTest} and {@link JipcResponseTest}.
 */
public abstract class AbstractJipcMessageTest {

	protected String parameter;

	@Before
	public void setUp() throws Exception {
		parameter = "param1: value1\n" + //
				"param2: 17\n" + //
				"param3: " + URLEncoder.encode("����", "UTF-8") + "\n";
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Must be overridden by the subclasses to instantiate a concrete message.
	 */
	protected abstract AbstractJipcMessage createMessage() throws IOException;

	/**
	 * Must be overridden by the subclasses to instantiate a concrete message with the {@link #parameter}.
	 */
	protected abstract AbstractJipcMessage createMessageWithParameter() throws IOException;

	@Test
	public void testGetProtocolVersion() throws Exception {
		AbstractJipcMessage req = createMessage();
		assertEquals("1.0", req.getProtocolVersion());
	}

	@Test
	public void testGetParameter() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		assertEquals("value1", req.getParameter("param1"));
		assertEquals("17", req.getParameter("param2"));
		assertEquals("����", req.getParameter("param3"));
		assertEquals(null, req.getParameter("aloha"));
	}

	@Test
	public void testGetFileParameter() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		assertEquals(new File("value1"), req.getFileParameter("param1"));
		assertEquals(null, req.getFileParameter("aloha"));
	}

	@Test
	public void testGetIntParameter() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		assertEquals(new Integer(17), req.getIntParameter("param2"));
		assertEquals(null, req.getIntParameter("aloha"));
	}

	@Test(expected = IOException.class)
	public void testGetIntParameterFails() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		req.getIntParameter("param1");
	}

	@Test
	public void testHasParameter() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		assertTrue(req.hasParameter("param1"));
		assertTrue(req.hasParameter("param2"));
		assertTrue(req.hasParameter("param3"));

		assertFalse(req.hasParameter("aloha"));
		assertFalse(req.hasParameter(""));
	}

	@Test
	public void testToString() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		String expected = req.getHeader() + "\n" + parameter;
		String string = req.toString();
		assertNotNull(string);
		assertEquals(expected, string);
	}

	@Test
	public void testToBytes() throws Exception {
		AbstractJipcMessage req = createMessageWithParameter();
		byte[] expected = new String( req.getHeader() + "\n" + parameter).getBytes(StandardCharsets.UTF_8);
		byte[] actual = req.toBytes();

		assertArrayEquals(expected, actual);
	}
	
	@Test
	public void testAddParameter() throws Exception {
		AbstractJipcMessage req = createMessage();
		req.setParameter("param1", "value1");
		req.setParameter("param2", "17");
		req.setParameter("param3", "����");
		
		String expected = createMessageWithParameter().toString();
		assertEquals(expected, req.toString());
	}
	
}
