package org.jipc.ipc.shm;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.ipc.AbstractTestConsumer;

public class SMPipeTestConsumer extends AbstractTestConsumer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	
	@Override
	protected JipcPipe createPipe(String[] args) throws IOException {
		File file = new File("buffer.mapped");
		if (args.length > 0) {
			file = new File(args[0]);
		}
		return createPipe(file);
	}

	public JipcPipe createPipe(File file) throws IOException {
		return new SharedMemoryPipe(file, 30, JipcRole.Yang);
	}

}
