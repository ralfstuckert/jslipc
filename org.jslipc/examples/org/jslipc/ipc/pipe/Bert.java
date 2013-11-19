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

public class Bert {

	public void talkToErnie(final JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JslipcChannelOutputStream(pipe.sink());
		InputStream in = new JslipcChannelInputStream(pipe.source());

		talkToErnie(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
		}
	}

	public void talkToErnie(OutputStream out, InputStream in)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

		System.out.println("This is Bert sendinng a message to Ernie.");
		// send message to ernie
		System.out.println("I'm sending: 'Hi Ernie, how are you?'");
		writer.write("Hi Ernie, how are you?\n");
		writer.flush();
		// receive message from ernie
		String received = reader.readLine();
		System.out.println("Ernie replied: '" + received + "'\n");

		// close all resources
		reader.close();
		writer.close();
	}

}
