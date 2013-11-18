package org.jslipc.ipc.pipe;

import java.io.Closeable;
import java.io.IOException;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;

public abstract class AbstractTestProducer extends AbstractTestEndpoint {
	
	public final static String HELLO = "Hello\n";

	protected static void init(String[] args) throws Exception {
		AbstractTestProducer producer = createEndpoint();
		JslipcPipe pipe = producer.createPipe(args);
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman) pipe).cleanUpOnClose();
		}
		
		producer.produce(pipe);
		
		if (pipe instanceof Closeable) {
			((Closeable) pipe).close();
		}
	}

	protected void produce(JslipcPipe pipe)
			throws IOException, Exception {
		TestClient client = new TestClient(pipe);
//		client.sleep();
		client.write(HELLO);
//		client.sleep();
		String received = client.read();
//		client.sleep();
		client.write(received);
//		client.sleep();
		client.close();
	}

}
