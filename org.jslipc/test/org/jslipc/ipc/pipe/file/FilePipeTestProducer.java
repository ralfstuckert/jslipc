package org.jslipc.ipc.pipe.file;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.AbstractTestProducer;
import org.jslipc.ipc.pipe.file.FilePipe;

public class FilePipeTestProducer extends AbstractTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	@Override
	protected JslipcPipe createPipe(String[] args) throws IOException {
		if (args.length == 2) {
			if (args[1].startsWith("-")) {
				File directory = new File(args[0]);
				JslipcRole role = JslipcRole.valueOf(args[1].substring(1));
				return createPipe(directory, role);
			} else {
				File source = new File(args[0]);
				File sink = new File(args[1]);
				return createPipe(source, sink);
			}
		}
		return null;
	}
	
	protected JslipcPipe createPipe(final File source, final File sink) throws IOException {
		return new FilePipe(source, sink);
	}

	protected JslipcPipe createPipe(final File directory, final JslipcRole role) throws IOException {
		return new FilePipe(directory, role);
	}
}
