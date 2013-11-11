package org.jipc.ipc.pipe.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.ipc.pipe.file.ChunkFilePipe;

public class ChunkFilePipeTestProducer extends FilePipeTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	protected JipcPipe createPipe(final File source, final File sink) throws IOException {
		return new ChunkFilePipe(source, sink);
	}

	protected JipcPipe createPipe(final File directory, final JipcRole role) throws IOException {
		return new ChunkFilePipe(directory, role);
	}

}
