package org.jipc.ipc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jipc.ipc.JipcResponse.JipcCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JipcResponse}.
 */
public class JipcResponseTest extends AbstractJipcMessageTest {

	private String response;
	private String responseWithParameter;
	private String responseBadHeader;
	private String responseBadRequest;
	private String successMessage;
	private String failureMessage;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		successMessage = "pipe successfully created";
		failureMessage = "just too bad";

		response = "JIPC/1.0 " + JipcCode.PipeCreated.value() + " "
				+ successMessage + "\n";

		responseWithParameter = response + parameter;
		responseBadHeader = "HTTP/1.0 127 dsfk\n";
		responseBadRequest = "JIPC/1.0 " + JipcCode.BadRequest.value() + " "
				+ failureMessage;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Override
	protected JipcResponse createMessage() throws IOException {
		return new JipcResponse(response);
	}

	@Override
	protected JipcResponse createMessageWithParameter() throws IOException {
		return new JipcResponse(responseWithParameter);
	}

	@Test
	public void testJipcResponseByteArray() throws Exception {
		JipcResponse res = new JipcResponse(
				responseWithParameter.getBytes(StandardCharsets.UTF_8));
		assertEquals(JipcCode.PipeCreated, res.getCode());
		assertEquals(successMessage, res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
		assertEquals("value1", res.getParameter("param1"));
	}

	@Test
	public void testJipcResponseString() throws Exception {
		JipcResponse res = new JipcResponse(responseWithParameter);
		assertEquals(JipcCode.PipeCreated, res.getCode());
		assertEquals(successMessage, res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
		assertEquals("value1", res.getParameter("param1"));
	}

	@Test
	public void testJipcResponseCodeMessage() throws Exception {
		JipcResponse res = new JipcResponse(JipcCode.BadRequest, "Kall, mei Droppe");
		assertEquals(JipcCode.BadRequest, res.getCode());
		assertEquals("Kall, mei Droppe", res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
	}

	@Test(expected = IOException.class)
	public void testJipcResponseBadProtocol() throws Exception {
		new JipcResponse(responseBadHeader);
	}

	@Test
	public void testGetCode() throws Exception {
		JipcResponse res = new JipcResponse(response);
		assertEquals(JipcCode.PipeCreated, res.getCode());
		
		res = new JipcResponse(responseBadRequest);
		assertEquals(JipcCode.BadRequest, res.getCode());
	}

	@Test
	public void testGetMessage() throws Exception {
		JipcResponse res = new JipcResponse(response);
		assertEquals(successMessage, res.getMessage());
		
		res = new JipcResponse(responseBadRequest);
		assertEquals(failureMessage, res.getMessage());
	}

}
