package org.jslipc.util;

import java.io.File;

public class TestServer {

	public static void main(final String[] args) throws Exception {
		Thread thread = new Thread() {
			public void run() {
				createServerDirectory(args);

			}
		};
		thread.start();
		thread.join(60000);
		System.exit(0);
	}

	private static void createServerDirectory(String[] args) {
		ServerDir serverDir = null;
		try {
			File parentDirectory = new File(args[0]);
			serverDir = ServerDir.create(parentDirectory);
			Thread.sleep(20000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtil.closeSilent(serverDir);
		}
	}
}
