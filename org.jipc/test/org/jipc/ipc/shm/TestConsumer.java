package org.jipc.ipc.shm;

import java.io.File;

import org.jipc.ipc.shm.MemoryMappedFilePipe;
import org.jipc.ipc.shm.MemoryMappedFilePipe.Role;

public class TestConsumer {
	public static void main(String[] args) throws Exception {
		File file = new File("buffer.mapped");
		if (args.length > 0) {
			file = new File(args[0]);
		}
		consume(file);
	}
	
	public static String consume(File file) throws Exception {
		MemoryMappedFilePipe pipe = new MemoryMappedFilePipe(file, 30,
				Role.Client);
		TestClient client = new TestClient(pipe);
//		client.sleep();
		String received = client.read();
//		client.sleep();
		client.write(createReply(received));
//		client.sleep();
		String reply = client.read();
		return reply;
	}
	
	public static String createReply(final String text) {
		return "I got: " + text;
	}

}
