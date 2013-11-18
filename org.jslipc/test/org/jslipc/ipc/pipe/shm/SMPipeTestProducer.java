package org.jslipc.ipc.pipe.shm;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.AbstractTestProducer;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;

public class SMPipeTestProducer extends AbstractTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	@Override
	protected JslipcPipe createPipe(String[] args) throws IOException {
		File file = new File("buffer.mapped");
		if (args.length > 0) {
			file = new File(args[0]);
		}
		return new SharedMemoryPipe(file, 30,
				JslipcRole.Yin);
	}

}
