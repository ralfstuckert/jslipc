package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;

public class ChunkFilePipeTestConsumer extends FilePipeTestConsumer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	protected JipcPipe createPipe(final File source, final File sink)
			throws IOException {
		return new ChunkFilePipe(source, sink);
	}

	protected JipcPipe createPipe(final File directory, final JipcRole role)
			throws IOException {
		return new ChunkFilePipe(directory, role);
	}
}