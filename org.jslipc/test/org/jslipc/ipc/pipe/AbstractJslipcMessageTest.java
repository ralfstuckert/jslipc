package org.jslipc.ipc.pipe;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for both {@link JslipcRequestTest} and
 * {@link JslipcResponseTest}.
 */
public abstract class AbstractJslipcMessageTest {

	protected String parameter;

	@Before
	public void setUp() throws Exception {
		parameter = "param1: value1\n" + //
				"param2: 17\n" + //
				"param3: " + URLEncoder.encode("äöüß", "UTF-8") + "\n";
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Must be overridden by the subclasses to instantiate a concrete message.
	 */
	protected abstract AbstractJslipcMessage createMessage() throws IOException;

	/**
	 * Must be overridden by the subclasses to instantiate a concrete message
	 * with the {@link #parameter}.
	 */
	protected abstract AbstractJslipcMessage createMessageWithParameter()
			throws IOException;

	@Test
	public void testGetProtocolVersion() throws Exception {
		AbstractJslipcMessage req = createMessage();
		assertEquals("1.0", req.getProtocolVersion());
	}

	@Test
	public void testGetParameter() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		assertEquals("value1", req.getParameter("param1"));
		assertEquals("17", req.getParameter("param2"));
		assertEquals("äöüß", req.getParameter("param3"));
		assertEquals(null, req.getParameter("aloha"));
	}

	@Test
	public void testGetFileParameter() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		assertEquals(new File("value1"), req.getFileParameter("param1"));
		assertEquals(null, req.getFileParameter("aloha"));
	}

	@Test
	public void testSetFileParameter() throws Exception {
		AbstractJslipcMessage req = createMessage();
		File file = new File("value1");
		req.setFileParameter("param1", file);
		assertEquals(file.getAbsolutePath(), req.getParameter("param1"));
		assertEquals(file.getAbsoluteFile(), req.getFileParameter("param1"));

		req.setFileParameter("param1", null);
		assertEquals(null, req.getParameter("param1"));
		assertEquals(null, req.getFileParameter("param1"));
	}

	@Test
	public void testGetIntParameter() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		assertEquals(new Integer(17), req.getIntParameter("param2"));
		assertEquals(null, req.getIntParameter("aloha"));
	}

	@Test(expected = IOException.class)
	public void testGetIntParameterFails() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		req.getIntParameter("param1");
	}

	@Test
	public void testSetIntParameter() throws Exception {
		AbstractJslipcMessage req = createMessage();
		req.setIntParameter("param2", 23);
		assertEquals("23", req.getParameter("param2"));
		assertEquals(new Integer(23), req.getIntParameter("param2"));

		req.setIntParameter("param2", null);
		assertEquals(null, req.getParameter("param2"));
		assertEquals(null, req.getIntParameter("param2"));
	}

	@Test
	public void testHasParameter() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		assertTrue(req.hasParameter("param1"));
		assertTrue(req.hasParameter("param2"));
		assertTrue(req.hasParameter("param3"));

		assertFalse(req.hasParameter("aloha"));
		assertFalse(req.hasParameter(""));
	}

	@Test
	public void testToString() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		String expected = req.getHeader() + "\n" + parameter;
		String string = req.toString();
		assertNotNull(string);
		assertEquals(expected, string);
	}

	@Test
	public void testToBytes() throws Exception {
		AbstractJslipcMessage req = createMessageWithParameter();
		byte[] expected = new String(req.getHeader() + "\n" + parameter)
				.getBytes(StringUtil.CHARSET_UTF_8);
		byte[] actual = req.toBytes();

		assertArrayEquals(expected, actual);
	}

	@Test
	public void testAddParameter() throws Exception {
		AbstractJslipcMessage req = createMessage();
		req.setParameter("param1", "value1");
		req.setParameter("param2", "17");
		req.setParameter("param3", "äöüß");

		String expected = createMessageWithParameter().toString();
		assertEquals(expected, req.toString());
	}

	@Test
	public void testGetTypeParameter() throws Exception {
		AbstractJslipcMessage req = createMessage();
		req.setParameter(AbstractJslipcMessage.PARAM_TYPE,
				ChunkFilePipe.class.getSimpleName());
		assertEquals(ChunkFilePipe.class, req.getTypeParameter());

		req.setParameter(AbstractJslipcMessage.PARAM_TYPE,
				FilePipe.class.getSimpleName());
		assertEquals(FilePipe.class, req.getTypeParameter());
	}

	@Test
	public void testSetTypeParameter() throws Exception {
		AbstractJslipcMessage req = createMessage();
		req.setTypeParameter(ChunkFilePipe.class);
		assertEquals(ChunkFilePipe.class.getSimpleName(),
				req.getParameter(AbstractJslipcMessage.PARAM_TYPE));

		req.setTypeParameter(FilePipe.class);
		assertEquals(FilePipe.class.getSimpleName(),
				req.getParameter(AbstractJslipcMessage.PARAM_TYPE));
	}

}
