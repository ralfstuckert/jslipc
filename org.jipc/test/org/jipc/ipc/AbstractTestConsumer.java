package org.jipc.ipc;

import java.io.Closeable;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;

public abstract class AbstractTestConsumer extends AbstractTestEndpoint {

	protected static void init(String[] args) throws Exception {
		AbstractTestConsumer consumer = createEndpoint();
		JipcPipe pipe = consumer.createPipe(args);
		if (pipe instanceof JipcBinman) {
			((JipcBinman) pipe).cleanUpOnClose();
		}
		
		consumer.consume(pipe);
		
		if (pipe instanceof Closeable) {
			((Closeable) pipe).close();
		}
	}

	public String consume(JipcPipe pipe) throws Exception {
		TestClient client = new TestClient(pipe);
		// client.sleep();
		String received = client.read();
		// client.sleep();
		client.write(createReply(received));
		// client.sleep();
		String reply = client.read();
		return reply;
	}

	public String createReply(final String text) {
		return "I got: " + text;
	}

}
