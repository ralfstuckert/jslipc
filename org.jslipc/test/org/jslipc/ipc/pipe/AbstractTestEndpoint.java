package org.jslipc.ipc.pipe;

import java.io.IOException;

import org.jslipc.JslipcPipe;

public abstract class AbstractTestEndpoint {

	protected abstract JslipcPipe createPipe(String[] args) throws IOException;
	
	@SuppressWarnings("unchecked")
	protected static <T> T createEndpoint() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElement = stackTrace[stackTrace.length-1]; // main is the first step
		System.err.println("trying to instantiate "+ stackTraceElement.getClassName());
		Class<?> clazz = Class.forName(stackTraceElement.getClassName());
		T instance = (T) clazz.newInstance();
		return instance;
	}

}
