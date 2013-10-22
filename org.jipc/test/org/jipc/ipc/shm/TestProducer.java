package org.jipc.ipc.shm;

import java.io.File;

import org.jipc.JipcRole;

public class TestProducer {
	
	public final static String HELLO = "Hello\n";

	public static void main(String[] args) throws Exception {
		File file = new File("buffer.mapped");
		if (args.length > 0) {
			file = new File(args[0]);
		}
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(file, 30,
				JipcRole.Server);
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
