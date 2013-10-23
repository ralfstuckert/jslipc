package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.ipc.AbstractTestProducer;

public class FilePipeTestProducer extends AbstractTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	@Override
	protected JipcPipe createPipe(String[] args) throws IOException {
		if (args.length == 2) {
			File source = new File(args[0]);
			File sink = new File(args[1]);
			return new FilePipe(source, sink);
		}
		return null;
	}

}
