package org.jipc.ipc.pipe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.ipc.pipe.file.ChunkFilePipe;
import org.jipc.ipc.pipe.file.FilePipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JipcRequest}.
 */
public class JipcRequestTest extends AbstractJipcMessageTest {

	private String header;
	private String request;
	private String requestWithParameter;
	private String requestBadHeader;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		header = "CONNECT JIPC/1.0";
		request = header + "\n";
		requestWithParameter = request + parameter;
		requestBadHeader = "CONNECT HTTP/1.0\n";
	}

	@After
	public void tearDown() throws Exception {
	}

	@Override
	protected JipcRequest createMessage() throws IOException {
		return new JipcRequest(request);
	}

	@Override
	protected JipcRequest createMessageWithParameter() throws IOException {
		return new JipcRequest(requestWithParameter);
	}

	@Test
	public void testGetHeader() throws Exception {
		JipcRequest req = new JipcRequest(request);
		assertEquals(header, req.getHeader());
	}

	@Test
	public void testJipcRequestByteArray() throws Exception {
		JipcRequest req = new JipcRequest(
				requestWithParameter.getBytes(StandardCharsets.UTF_8));
		assertEquals(JipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
		assertEquals("value1", req.getParameter("param1"));
	}

	@Test
	public void testJipcRequestString() throws Exception {
		JipcRequest req = new JipcRequest(requestWithParameter);
		assertEquals(JipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
		assertEquals("value1", req.getParameter("param1"));
	}

	@Test
	public void testJipcRequestCommand() throws Exception {
		JipcRequest req = new JipcRequest(JipcCommand.CONNECT);
		assertEquals(JipcCommand.CONNECT, req.getCommand());
		assertEquals("1.0", req.getProtocolVersion());
	}

	@Test(expected = IOException.class)
	public void testJipcRequestBadProtocol() throws Exception {
		new JipcRequest(requestBadHeader);
	}

	@Test
	public void testGetCommand() throws Exception {
		JipcRequest req = new JipcRequest(request);
		assertEquals(JipcCommand.CONNECT, req.getCommand());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSetAcceptTypes() throws Exception {
		JipcRequest req = new JipcRequest(JipcCommand.CONNECT);
		req.setAcceptTypes(FilePipe.class);
		assertEquals("FilePipe", req.getParameter(JipcRequest.PARAM_ACCEPT_TYPES));
		
		req.setAcceptTypes(ChunkFilePipe.class, FilePipe.class);
		assertEquals("ChunkFilePipe,FilePipe", req.getParameter(JipcRequest.PARAM_ACCEPT_TYPES));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAcceptTypes() throws Exception {
		JipcRequest req = new JipcRequest(JipcCommand.CONNECT);
		req.setParameter(JipcRequest.PARAM_ACCEPT_TYPES, "FilePipe");
		assertEquals(Arrays.asList(FilePipe.class), req.getAcceptTypes() );
		
		req.setParameter(JipcRequest.PARAM_ACCEPT_TYPES, "ChunkFilePipe,FilePipe");
		assertEquals(Arrays.asList(ChunkFilePipe.class, FilePipe.class), req.getAcceptTypes() );
	}

}
