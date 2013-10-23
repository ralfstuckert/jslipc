package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.ipc.AbstractTestConsumer;

public class FilePipeTestConsumer extends AbstractTestConsumer {

	public static void main(String[] args) throws Exception {
		init(args);
	}

	@Override
	protected JipcPipe createPipe(String[] args) throws IOException {
		if (args.length == 2) {
			if (args[1].startsWith("-")) {
				File directory = new File(args[0]);
				JipcRole role = JipcRole.valueOf(args[1].substring(1));
				return new FilePipe(directory, role);
			} else {
				File source = new File(args[0]);
				File sink = new File(args[1]);
				return new FilePipe(source, sink);
			}
		}
		return null;
	}

}
