package org.jslipc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jslipc.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link PipeUtil}.
 */
public class PipeUtilTest {

	private HostDir hostDir;
	private File hostParentDir;

	@Before
	public void setUp() throws Exception {
		hostParentDir = TestUtil.createDirectory();
		hostDir = HostDir.create(hostParentDir);
	}

	@After
	public void tearDown() throws Exception {
		hostDir.close();
		FileUtil.delete(hostParentDir, true);
	}

	@Test
	public void testGetConnectDir() throws Exception {
		File connectDir = PipeUtil.createConnectDir(hostDir);
		assertNotNull(connectDir);
		assertTrue("file exists", connectDir.exists());
		assertTrue("is directory", connectDir.isDirectory());
		assertEquals("connect", connectDir.getName());
		assertEquals(hostDir.getDirectory(), connectDir.getParentFile());
	}

	@Test(expected = IOException.class)
	public void testGetConnectDirWithInactiveHostDir() throws Exception {
		hostDir.close();
		PipeUtil.createConnectDir(hostDir);
	}

	@Test
	public void testGetPipesDir() throws Exception {
		File pipesDir = PipeUtil.createPipesDir(hostDir);
		assertNotNull(pipesDir);
		assertTrue("file exists", pipesDir.exists());
		assertTrue("is directory", pipesDir.isDirectory());
		assertEquals("pipes", pipesDir.getName());
		assertEquals(hostDir.getDirectory(), pipesDir.getParentFile());
	}

	@Test(expected = IOException.class)
	public void testGetPipesDirWithInactiveHostDir() throws Exception {
		hostDir.close();
		PipeUtil.createPipesDir(hostDir);
	}

	@Test
	public void testGetActiveHostConnectDir() throws Exception {
		File connectDir = PipeUtil.createConnectDir(hostDir);
		assertEquals(connectDir, PipeUtil.getActiveHostConnectDir(hostParentDir));
	}
	
	@Test(expected=IOException.class)
	public void testGetActiveHostConnectDirHostClosed() throws Exception {
		hostDir.close();
		PipeUtil.getActiveHostConnectDir(hostParentDir);
	}
	
	@Test(expected=IOException.class)
	public void testGetActiveHostConnectDirNotExists() throws Exception {
		PipeUtil.getActiveHostConnectDir(hostParentDir);
	}
	
}
