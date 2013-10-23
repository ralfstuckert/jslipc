package org.jipc.ipc;

import java.io.IOException;

import org.jipc.JipcPipe;

public abstract class AbstractTestProducer extends AbstractTestEndpoint {
	
	public final static String HELLO = "Hello\n";

	protected static void init(String[] args) throws Exception {
		AbstractTestProducer producer = createEndpoint();
		JipcPipe pipe = producer.createPipe(args);
		producer.produce(pipe);
	}

	protected void produce(JipcPipe pipe)
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
