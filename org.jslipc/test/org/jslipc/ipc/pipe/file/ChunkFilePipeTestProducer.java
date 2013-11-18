package org.jslipc.ipc.pipe.file;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;

public class ChunkFilePipeTestProducer extends FilePipeTestProducer {

	public static void main(String[] args) throws Exception {
		init(args);
	}
	
	protected JslipcPipe createPipe(final File source, final File sink) throws IOException {
		return new ChunkFilePipe(source, sink);
	}

	protected JslipcPipe createPipe(final File directory, final JslipcRole role) throws IOException {
		return new ChunkFilePipe(directory, role);
	}

}
