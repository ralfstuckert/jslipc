package org.jslipc.ipc.pipe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JslipcRequest}.
 */
public class JslipcRequestTest extends AbstractJslipcMessageTest {

	private String header;
	private String request;
	private String requestWithParameter;
	private String requestBadHeader;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		header = "CONNECT JSLIPC/1.0";
		request = header + "\n";
		requestWithParameter = request + parameter;
		requestBadHeader = "CONNECT HTTP/1.0\n";
	}

	@After
	public void tearDown() throws Exception {
	}

	@Override
	protected JslipcRequest createMessage() throws IOException {
		return new JslipcRequest(request);
	}

	@Override
	protected JslipcRequest createMessageWithParameter() throws IOException {
		return new JslipcRequest(requestWithParameter);
	}

	@Test
	public void testGetHeader() throws Exception {
		JslipcRequest req = new JslipcRequest(request);
		assertEquals(header, req.getHeader());
	}

	@Test
	public void testJsiplcRequestByteArray() throws Exception {
		JslipcRequest req = new JslipcRequest(
				requestWithParameter.getBytes(StringUtil.CHARSET_UTF_8));
		assertEquals(JslipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
		assertEquals("value1", req.getParameter("param1"));
	}

	@Test
	public void testJsiplcRequestString() throws Exception {
		JslipcRequest req = new JslipcRequest(requestWithParameter);
		assertEquals(JslipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
		assertEquals("value1", req.getParameter("param1"));
	}

	@Test
	public void testJsiplcRequestCommand() throws Exception {
		JslipcRequest req = new JslipcRequest(JslipcCommand.CONNECT);
		assertEquals(JslipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
	}

	@Test(expected = IOException.class)
	public void testJsiplcRequestBadProtocol() throws Exception {
		new JslipcRequest(requestBadHeader);
	}

	@Test
	public void testGetCommand() throws Exception {
		JslipcRequest req = new JslipcRequest(request);
		assertEquals(JslipcCommand.CONNECT, req.getCommand());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSetAcceptTypes() throws Exception {
		JslipcRequest req = new JslipcRequest(JslipcCommand.CONNECT);
		req.setAcceptTypes(FilePipe.class);
		assertEquals("FilePipe", req.getParameter(JslipcRequest.PARAM_ACCEPT_TYPES));
		
		req.setAcceptTypes(ChunkFilePipe.class, FilePipe.class);
		assertEquals("ChunkFilePipe,FilePipe", req.getParameter(JslipcRequest.PARAM_ACCEPT_TYPES));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAcceptTypes() throws Exception {
		JslipcRequest req = new JslipcRequest(JslipcCommand.CONNECT);
		req.setParameter(JslipcRequest.PARAM_ACCEPT_TYPES, "FilePipe");
		assertEquals(Arrays.asList(FilePipe.class), req.getAcceptTypes() );
		
		req.setParameter(JslipcRequest.PARAM_ACCEPT_TYPES, "ChunkFilePipe,FilePipe");
		assertEquals(Arrays.asList(ChunkFilePipe.class, FilePipe.class), req.getAcceptTypes() );
	}

}
