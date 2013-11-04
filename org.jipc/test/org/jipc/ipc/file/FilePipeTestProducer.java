package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.ipc.AbstractTestProducer;

public class FilePipeTestProducer extends AbstractTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	@Override
	protected JipcPipe createPipe(String[] args) throws IOException {
		if (args.length == 2) {
			if (args[1].startsWith("-")) {
				File directory = new File(args[0]);
				JipcRole role = JipcRole.valueOf(args[1].substring(1));
				return createPipe(directory, role);
			} else {
				File source = new File(args[0]);
				File sink = new File(args[1]);
				return createPipe(source, sink);
			}
		}
		return null;
	}
	
	protected JipcPipe createPipe(final File source, final File sink) throws IOException {
		return new FilePipe(source, sink);
	}

	protected JipcPipe createPipe(final File directory, final JipcRole role) throws IOException {
		return new FilePipe(directory, role);
	}
}
