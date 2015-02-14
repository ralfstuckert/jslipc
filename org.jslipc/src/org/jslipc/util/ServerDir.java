package org.jslipc.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerDir implements Closeable {

	public final static String SERVER_LOCK_FILE = "server.lock";

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ServerDir.class);

	private final File directory;
	private final RandomAccessFile lockFile;
	private final FileLock lock;

	protected ServerDir(File directory, RandomAccessFile lockFile, FileLock lock) {
		if (directory == null) {
			throw new IllegalArgumentException(
					"parameter directory must not be null");
		}
		if (lockFile == null) {
			throw new IllegalArgumentException(
					"parameter lockFile must not be null");
		}
		if (lock == null) {
			throw new IllegalArgumentException(
					"parameter lock must not be null");
		}
		this.directory = directory;
		this.lockFile = lockFile;
		this.lock = lock;
	}

	public File getDirectory() {
		return directory;
	}

	public boolean isActive() {
		return lock.isValid();
	}

	@Override
	public void close() throws IOException {
		try {
			if (lock.isValid()) {
				lock.release();
			}
		} finally {
			lockFile.close();
		}
	}

	@Override
	public String toString() {
		return "ServerDir [directory=" + directory + "]";
	}

	public static ServerDir create(final File parentDirectory)
			throws IOException {
		deleteOldDirectories(parentDirectory);

		File lockFile = new File(parentDirectory, SERVER_LOCK_FILE);
		if (!lockFile.exists()) {
			lockFile.createNewFile();
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(lockFile, "rwd");
			FileLock lock = lock(raf);
			if (lock == null) {
				FileUtil.closeSilent(raf);
				throw new ServerActiveException(
						getActive(parentDirectory));
			}
			File serverDirectory = FileUtil.createDirectory(parentDirectory);
			raf.writeUTF(serverDirectory.getName());

			ServerDir ServerDir = new ServerDir(serverDirectory, raf, lock);
			return ServerDir;
		} catch (IOException e) {
			FileUtil.closeSilent(raf);
			throw new ServerActiveException(
					getActive(parentDirectory));
		} catch (OverlappingFileLockException e) {
			FileUtil.closeSilent(raf);
			throw new ServerActiveException(
					getActive(parentDirectory));
		}
	}

	public static File getActive(final File parentDirectory) {
		File lockFile = new File(parentDirectory, SERVER_LOCK_FILE);
		if (!lockFile.exists()) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(lockFile, "rw");
			if (!isLocked(raf)) {
				return null;
			}
			String dirName = raf.readUTF();
			if (dirName == null || dirName.isEmpty()) {
				return null;
			}
			File serverDirectory = new File(parentDirectory, dirName);
			return serverDirectory;
		} catch (IOException e) {
			LOGGER.warn("IOException occurred", e);
			return null;
		} finally {
			FileUtil.closeSilent(raf);
		}
	}

	private static boolean isLocked(final RandomAccessFile raf) {
		try {
			FileLock lock = lock(raf);
			if (lock != null) {
				lock.release();
				return false;
			}
			return true;
		} catch (OverlappingFileLockException e) {
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	protected static FileLock lock(final RandomAccessFile raf)
			throws IOException {
		return raf.getChannel().tryLock(Long.MAX_VALUE - 1, 1, false);
	}

	protected static void deleteOldDirectories(final File parentDirectory) {
		if (parentDirectory == null || !parentDirectory.isDirectory()) {
			return;
		}
		File activeServerDirectory = getActive(parentDirectory);
		File[] oldDirectories = parentDirectory.listFiles(DIRECTORY_FILTER);
		for (File oldDirectory : oldDirectories) {
			if (!oldDirectory.equals(activeServerDirectory)) {
				FileUtil.delete(oldDirectory);
			}
		}
	}

	private static final FileFilter DIRECTORY_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}

	};
}
