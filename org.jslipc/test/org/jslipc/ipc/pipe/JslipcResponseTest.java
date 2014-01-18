package org.jslipc.ipc.pipe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.JslipcResponse.JslipcCode;
import org.jslipc.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JslipcResponse}.
 */
public class JslipcResponseTest extends AbstractJslipcMessageTest {

	private String header;
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

		header = "JSLIPC/1.0 " + JslipcCode.PipeCreated.value() + " "
				+ successMessage;
		response = header + "\n";

		responseWithParameter = response + parameter;
		responseBadHeader = "HTTP/1.0 127 dsfk\n";
		responseBadRequest = "JSLIPC/1.0 " + JslipcCode.BadRequest.value() + " "
				+ failureMessage;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Override
	protected JslipcResponse createMessage() throws IOException {
		return new JslipcResponse(response);
	}

	@Override
	protected JslipcResponse createMessageWithParameter() throws IOException {
		return new JslipcResponse(responseWithParameter);
	}

	@Test
	public void testGetHeader() throws Exception {
		JslipcResponse res = new JslipcResponse(response);
		assertEquals(header, res.getHeader());
	}

	@Test
	public void testJsiplcResponseByteArray() throws Exception {
		JslipcResponse res = new JslipcResponse(
				responseWithParameter.getBytes(StringUtil.CHARSET_UTF_8));
		assertEquals(JslipcCode.PipeCreated, res.getCode());
		assertEquals(successMessage, res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
		assertEquals("value1", res.getParameter("param1"));
	}

	@Test
	public void testJsiplcResponseString() throws Exception {
		JslipcResponse res = new JslipcResponse(responseWithParameter);
		assertEquals(JslipcCode.PipeCreated, res.getCode());
		assertEquals(successMessage, res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
		assertEquals("value1", res.getParameter("param1"));
	}

	@Test
	public void testJsiplcResponseCodeMessage() throws Exception {
		JslipcResponse res = new JslipcResponse(JslipcCode.BadRequest, "Kall, mei Droppe");
		assertEquals(JslipcCode.BadRequest, res.getCode());
		assertEquals("Kall, mei Droppe", res.getMessage());
		assertEquals("1.0", res.getProtocolVersion());
	}

	@Test(expected = IOException.class)
	public void testJsiplcResponseBadProtocol() throws Exception {
		new JslipcResponse(responseBadHeader);
	}

	@Test
	public void testGetCode() throws Exception {
		JslipcResponse res = new JslipcResponse(response);
		assertEquals(JslipcCode.PipeCreated, res.getCode());
		
		res = new JslipcResponse(responseBadRequest);
		assertEquals(JslipcCode.BadRequest, res.getCode());
	}

	@Test
	public void testGetMessage() throws Exception {
		JslipcResponse res = new JslipcResponse(response);
		assertEquals(successMessage, res.getMessage());
		
		res = new JslipcResponse(responseBadRequest);
		assertEquals(failureMessage, res.getMessage());
	}

	@Test
	public void testGetRoleParameter() throws Exception {
		JslipcResponse res = new JslipcResponse(response);
		res.setParameter(AbstractJslipcMessage.PARAM_ROLE,
				JslipcRole.Yang.toString());
		assertEquals(JslipcRole.Yang, res.getRoleParameter());

		res.setParameter(AbstractJslipcMessage.PARAM_ROLE,
				JslipcRole.Yin.toString());
		assertEquals(JslipcRole.Yin, res.getRoleParameter());
	}

	@Test
	public void testSetRoleParameter() throws Exception {
		JslipcResponse res = new JslipcResponse(response);
		res.setRoleParameter(JslipcRole.Yang);
		assertEquals(JslipcRole.Yang.toString(),
				res.getParameter(AbstractJslipcMessage.PARAM_ROLE));

		res.setRoleParameter(JslipcRole.Yin);
		assertEquals(JslipcRole.Yin.toString(),
				res.getParameter(AbstractJslipcMessage.PARAM_ROLE));
	}


}
