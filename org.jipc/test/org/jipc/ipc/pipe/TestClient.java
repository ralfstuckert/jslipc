package org.jipc.ipc.pipe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;

public class TestClient {

	private final static boolean LOG_TO_CONSOLE = false;
	private static final int TIMEOUT = 5000;
	private OutputStream out;
	private InputStream in;

	public TestClient(JipcPipe pipe) throws IOException {
		out = new JipcChannelOutputStream(pipe.sink());
		in = new JipcChannelInputStream(pipe.source());
	}

	public void sleep() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() throws IOException {
		in.close();
		out.close();
	}

	public void write(final String text) throws Exception {
		byte[] content = text.getBytes();
		for (int i = 0; i < content.length; i++) {
			writeByte(content[i]);
		}
	}

	public String read() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = in.read();
		while (date != -1 && date != (byte) 10) {
			baos.write(date);
			date = readByte();
		}
		if (date == 10) {
			baos.write(10);
		}
		return new String(baos.toByteArray());
	}

	private int readByte() throws IOException, TimeoutException,
			InterruptedException {
		final AtomicInteger integer = new AtomicInteger();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					log("reading");
					integer.set(in.read());
					log("read " + integer.get() + " -> "
							+ (char) integer.get());
				} catch (IOException e) {
					caught.set(e);
				}
			};
		};
		thread.start();
		thread.join(TIMEOUT);
		if (thread.isAlive()) {
			thread.interrupt();
			throw new TimeoutException();
		}
		return integer.get();
	}

	private void writeByte(final byte date) throws IOException,
			TimeoutException, InterruptedException {
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					log("writing " + date + " -> " + (char) date);
					out.write(date);
					log("wrote");
				} catch (IOException e) {
					caught.set(e);
				}
			};
		};
		thread.start();
		thread.join(TIMEOUT);
		if (thread.isAlive()) {
			thread.interrupt();
			throw new TimeoutException();
		}
	}

	private static void log(final String text) {
		if (LOG_TO_CONSOLE) {
			System.err.println(text);
		}
	}
}
