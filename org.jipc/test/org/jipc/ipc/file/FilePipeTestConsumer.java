package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.ipc.AbstractTestConsumer;

public class FilePipeTestConsumer extends AbstractTestConsumer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	
	@Override
	protected JipcPipe createPipe(String[] args) throws IOException {
		if (args.length == 2) {
			File source = new File(args[0]);
			File sink = new File(args[1]);
			return createPipe(source, sink);
		}
		return null;
	}

	public JipcPipe createPipe(File source, File sink) throws IOException {
		return new FilePipe(source, sink);
	}

}
