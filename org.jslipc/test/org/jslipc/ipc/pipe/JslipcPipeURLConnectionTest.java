package org.jslipc.ipc.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jslipc.JslipcPipe;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.channel.ReadableJslipcByteChannel;
import org.jslipc.channel.WritableJslipcByteChannel;
import org.jslipc.ipc.pipe.JslipcPipeClient;
import org.jslipc.ipc.pipe.JslipcPipeURLConnection;
import org.jslipc.ipc.pipe.JslipcRequest;
import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JslipcPipeURLConnectionTest {

	@Mock
	private ReadableJslipcByteChannel readableChannelMock;
	@Mock
	private WritableJslipcByteChannel writableChannelMock;
	@Mock
	private JslipcPipe pipeMock;
	@Mock
	private JslipcPipeClient clientMock;

	private URL url;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(pipeMock.sink()).thenReturn(writableChannelMock);
		when(pipeMock.source()).thenReturn(readableChannelMock);
		when(clientMock.connect(any(JslipcRequest.class))).thenReturn(pipeMock);
		
		url = new URL("jslipc:///c:temp");
	}
	

	@After
	public void tearDown() {
		readableChannelMock = null;
		writableChannelMock = null;
	}


	@Test
	public void testJslipcPipeURLConnection() throws Exception {
		new JslipcPipeURLConnection(url, clientMock);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testJslipcPipeURLConnectionWithNullURL() throws Exception {
		new JslipcPipeURLConnection(null, clientMock);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testJslipcPipeURLConnectionWithNullPipe() throws Exception {
		new JslipcPipeURLConnection(url, null);
	}

	@Test
	public void testConnect() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		assertNull(connection.in);
		assertNull(connection.out);
		
		connection.connect();
		assertNotNull(connection.in);
		assertNotNull(connection.out);

		JslipcRequest expectedRequest = new JslipcRequest(JslipcCommand.CONNECT);
		verify(clientMock).connect(expectedRequest);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testConnectWithParameter() throws Exception {
		URL url = new URL(
				"jslipc:///c:/server/connect?accept-types=ChunkFilePipe,FilePipe&special-guest=spongebob&happy=&x");
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		assertNull(connection.in);
		assertNull(connection.out);
		
		connection.connect();
		assertNotNull(connection.in);
		assertNotNull(connection.out);

		JslipcRequest expectedRequest = new JslipcRequest(JslipcCommand.CONNECT);
		expectedRequest.setAcceptTypes(ChunkFilePipe.class,FilePipe.class);
		expectedRequest.setParameter("special-guest", "spongebob");
		verify(clientMock).connect(expectedRequest);
	}


	@Test
	public void testCreateRequest() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		JslipcRequest request = connection.createRequest();
		assertNotNull(request);
		assertEquals(JslipcCommand.CONNECT, request.getCommand());
		assertEquals(0, request.getParameters().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateRequestWithParameter() throws Exception {
		URL url = new URL(
				"jslipc:///c:/server/connect?accept-types=ChunkFilePipe,FilePipe&special-guest=spongebob&happy=&x");
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		JslipcRequest request = connection.createRequest();
		assertNotNull(request);
		assertEquals(JslipcCommand.CONNECT, request.getCommand());
		assertEquals(2, request.getParameters().size());

		assertEquals(Arrays.asList(ChunkFilePipe.class, FilePipe.class),
				request.getAcceptTypes());
		assertEquals("spongebob", request.getParameter("special-guest"));
		assertEquals(null, request.getParameter("happy"));
		assertEquals(null, request.getParameter("x"));
		assertEquals(null, request.getParameter("y"));
	}


	@Test
	public void testSetConnectTimeout() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		connection.setConnectTimeout(101);
		connection.connect();

		verify(clientMock).setTimeout(101);
	}

	@Test
	public void testSetReadTimeout() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		connection.setReadTimeout(765);
		connection.connect();

		assertEquals(765, retrieveJslipcInputStream(connection).getTimeout());
		assertEquals(765, retrieveJslipcOutputStream(connection).getTimeout());
	}

	@Test
	public void testGetInputStream() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		InputStream in = connection.getInputStream();
		assertNotNull(in);
		
		byte[] buf = new byte[10];
		mockReadBytes(17, 12, 56);
		assertEquals(3, in.read(buf, 1, 5));
		assertEquals((byte) 17, buf[1]);
		assertEquals((byte) 12, buf[2]);
		assertEquals((byte) 56, buf[3]);
	}

	@Test
	public void testGetOutputStream() throws Exception {
		JslipcPipeURLConnection connection = new JslipcPipeURLConnection(url, clientMock);
		OutputStream out = connection.getOutputStream();
		assertNotNull(out);

		mockWriteBytes(17, 12, 56);
		byte[] buf = createByteArray(10, 1, 17, 12, 56);
		out.write(buf, 1, 3);
		out.flush();
		verifyWriteCalled();
	}

	protected void mockReadBytes(final int... dates) throws IOException {
		doAnswer(new Answer<Integer>() {
					public Integer answer(InvocationOnMock invocation) {
						ByteBuffer buffer = (ByteBuffer) invocation
								.getArguments()[0];
						for (int date : dates) {
							buffer.put((byte) date);
						}
						return dates.length;
					}
				}).when(readableChannelMock).read(any(ByteBuffer.class));
	}

	protected void mockWriteBytes(final int... dates) throws IOException {
		doAnswer(new Answer<Integer>() {
			public Integer answer(InvocationOnMock invocation) {
				ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
				assertEquals(dates.length, buffer.remaining());
				for (int date : dates) {
					assertEquals((byte) date, buffer.get());
				}
				return dates.length;
			}
		}).when(writableChannelMock).write(any(ByteBuffer.class));
	}

	protected void verifyWriteCalled() throws IOException {
		verify(writableChannelMock).write(any(ByteBuffer.class));
		reset(writableChannelMock);
	}

	protected byte[] createByteArray(int size, int offset, int... data) {
		byte[] result = new byte[size];
		for (int i = 0; i < data.length; i++) {
			result[i + offset] = (byte) data[i];
		}
		return result;
	}

	protected JslipcChannelInputStream retrieveJslipcInputStream(JslipcPipeURLConnection connection) throws Exception {
		FilterInputStream inputStream = (FilterInputStream) connection.getInputStream();
		Field in = FilterInputStream.class.getDeclaredField("in");
		in.setAccessible(true);
		return (JslipcChannelInputStream) in.get(inputStream);
	}

	protected JslipcChannelOutputStream retrieveJslipcOutputStream(JslipcPipeURLConnection connection) throws Exception {
		FilterOutputStream outputStream = (FilterOutputStream) connection.getOutputStream();
		Field out = FilterOutputStream.class.getDeclaredField("out");
		out.setAccessible(true);
		return (JslipcChannelOutputStream) out.get(outputStream);
	}
}
