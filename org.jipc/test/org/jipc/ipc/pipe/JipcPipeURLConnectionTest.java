package org.jipc.ipc.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import org.jipc.JipcPipe;
import org.jipc.channel.ReadableJipcByteChannel;
import org.jipc.channel.WritableJipcByteChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JipcPipeURLConnectionTest {

	@Mock
	private ReadableJipcByteChannel readableChannelMock;
	@Mock
	private WritableJipcByteChannel writableChannelMock;
	@Mock
	private JipcPipe pipeMock;
	private URL url;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(pipeMock.sink()).thenReturn(writableChannelMock);
		when(pipeMock.source()).thenReturn(readableChannelMock);
		
		url = new URL("jipc:///c:temp");
	}
	

	@After
	public void tearDown() {
		readableChannelMock = null;
		writableChannelMock = null;
	}


	@Test
	public void testJipcPipeURLConnection() {
		new JipcPipeURLConnection(url, pipeMock);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testJipcPipeURLConnectionWithNullURL() {
		new JipcPipeURLConnection(null, pipeMock);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testJipcPipeURLConnectionWithNullPipe() {
		new JipcPipeURLConnection(url, null);
	}

	@Test
	public void testConnect() throws Exception {
		JipcPipeURLConnection connection = new JipcPipeURLConnection(url, pipeMock);
		assertNull(connection.in);
		assertNull(connection.out);
		
		connection.connect();
		assertNotNull(connection.in);
		assertNotNull(connection.out);
	}

	@Test
	public void testGetInputStream() throws Exception {
		JipcPipeURLConnection connection = new JipcPipeURLConnection(url, pipeMock);
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
		JipcPipeURLConnection connection = new JipcPipeURLConnection(url, pipeMock);
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

}
