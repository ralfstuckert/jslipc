package org.jslipc.ipc.pipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;

public class PipeConsumer {

	public void talkToProducer(JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JslipcChannelOutputStream(pipe.sink());
		InputStream in = new JslipcChannelInputStream(pipe.source());

		talkToProducer(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
		}
	}

	public void talkToProducer(OutputStream out, InputStream in)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		// receive message from producer
		String received = reader.readLine();
		System.out.println("producer asked: '" + received + "'" );
		// send message to producer
		System.out.println("answering: 'I'm fine'" );
		writer.write("I'm fine\n");

		// close all resources
		reader.close();
		writer.close();
	}
}
