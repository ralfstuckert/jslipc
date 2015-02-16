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

/**
 * Utility class for dealing with host directories, see <a
 * href="https://code.google.com/p/jslipc/issues/detail?id=17">issue 17</a>.
 * This utility allows client and server to use ONE directory as the shared
 * information needed to set up a client server pipeline.
 * <p>Method {@link #create(File)} tries to create a new (sub-) directory. The name
 * of this directory is hold in a special file called <code>host.lock</code>, which is
 * locked by the host. The created directory will be the {@link #getActive(File) active}
 * host directory as long as the host process lives (holds the file lock), or the HostDir is 
 * {@link #close() closed}.</p>
 * <p>If you try to {@link #create(File) create} a new HostDir while there is already an active one,
 * you will receive a {@link ActiveHostException}. This exception provides you the 
 * {@link ActiveHostException#getActiveHostDirectory() directory} of the currently active host.
 * Using that mechanism, you can also set up multi-process scenarios where the every process tries
 * {@link #create(File) to be the server}. If it gets an exception, it will know that there is already 
 * an active server and act as a client.</p>
 * 
 * @author Ralf
 * 
 */
public class HostDir implements Closeable {

	public final static String HOST_LOCK_FILE = "host.lock";

	private final static Logger LOGGER = LoggerFactory
			.getLogger(HostDir.class);

	private final File directory;
	private final RandomAccessFile lockFile;
	private final FileLock lock;

	protected HostDir(File directory, RandomAccessFile lockFile, FileLock lock) {
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

	/**
	 * @return the active host directory.
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * @return <code>true</code> if the directory represented by this {@link HostDir} is currently active.
	 */
	public boolean isActive() {
		return lock.isValid();
	}

	/**
	 * Closes this HostDir and releases the file lock hold on <code>host.lock</code>.
	 */
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
		return "HostDir [directory=" + directory + "]";
	}

	/**
	 * Creates a new HostDir.
	 * @param parentDirectory the parent directory used as the shared information between the server and clients.
	 * @return the created HostDir.
	 * @throws ActiveHostException if there is already active server in the given parent directory.
	 * @throws IOException on any I/O problem.
	 */
	public static HostDir create(final File parentDirectory)
			throws IOException {
		deleteOldDirectories(parentDirectory);

		File lockFile = new File(parentDirectory, HOST_LOCK_FILE);
		if (!lockFile.exists()) {
			lockFile.createNewFile();
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(lockFile, "rwd");
			FileLock lock = lock(raf);
			if (lock == null) {
				FileUtil.closeSilent(raf);
				throw new ActiveHostException(getActive(parentDirectory));
			}
			File hostDirectory = FileUtil.createDirectory(parentDirectory);
			raf.writeUTF(hostDirectory.getName());

			HostDir hostDir = new HostDir(hostDirectory, raf, lock);
			return hostDir;
		} catch (IOException e) {
			FileUtil.closeSilent(raf);
			throw new ActiveHostException(getActive(parentDirectory));
		} catch (OverlappingFileLockException e) {
			FileUtil.closeSilent(raf);
			throw new ActiveHostException(getActive(parentDirectory));
		}
	}

	/**
	 * The active directory represented by this HostDir. If there is none,
	 * <code>null</code> is returned.
	 * @param parentDirectory
	 * @return the active directory represented by this HostDir.
	 */
	public static File getActive(final File parentDirectory) {
		File lockFile = new File(parentDirectory, HOST_LOCK_FILE);
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
			File hostDirectory = new File(parentDirectory, dirName);
			return hostDirectory;
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
		File activeHostDirectory = getActive(parentDirectory);
		File[] oldDirectories = parentDirectory.listFiles(DIRECTORY_FILTER);
		for (File oldDirectory : oldDirectories) {
			if (!oldDirectory.equals(activeHostDirectory)) {
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
