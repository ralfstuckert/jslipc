package org.jslipc.ipc.pipe;

import java.io.Closeable;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;

public abstract class AbstractTestConsumer extends AbstractTestEndpoint {

	protected static void init(String[] args) throws Exception {
		AbstractTestConsumer consumer = createEndpoint();
		JslipcPipe pipe = consumer.createPipe(args);
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman) pipe).cleanUpOnClose();
		}
		
		consumer.consume(pipe);
		
		if (pipe instanceof Closeable) {
			((Closeable) pipe).close();
		}
	}

	public String consume(JslipcPipe pipe) throws Exception {
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
