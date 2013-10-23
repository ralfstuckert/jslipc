package org.jipc.ipc.file;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.file.ReadableJipcFileChannel;
import org.jipc.file.WritableJipcFileChannel;

public class FilePipe implements JipcPipe {
	
	private File sourceFile;
	private File sinkFile;
	private ReadableJipcFileChannel source;
	private WritableJipcFileChannel sink;

	public FilePipe(final File dir, final JipcRole role) {
		
	}

	public FilePipe(final File source, final File sink) {
		if (source == null) {
			throw new IllegalArgumentException("parameter 'source' must not be null");
		}
		if (sink == null) {
			throw new IllegalArgumentException("parameter 'sink' must not be null");
		}
		this.sourceFile = source;
		this.sinkFile = sink;
	}

	@Override
	public ReadableJipcFileChannel source() throws IOException {
		if (source == null) {
			source = new ReadableJipcFileChannel(sourceFile);
		}
		return source;
	}

	@Override
	public WritableJipcFileChannel sink() throws IOException {
		if (sink == null) {
			sink = new WritableJipcFileChannel(sinkFile);
		}
		return sink;
	}

}
