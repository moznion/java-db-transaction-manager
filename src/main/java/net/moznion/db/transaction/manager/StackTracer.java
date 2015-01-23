package net.moznion.db.transaction.manager;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * The tracer for {@code StackTraceElement}.
 * 
 * <p>
 * This class replace to {@code Thread.currentThread.getStackTrace}.
 * {@code Thread.currentThread.getStackTrace} is really expensive to call,
 * So change to invoke private method of Throwable to realize the same function.
 * It is a bit hackish, but fast and cheap.
 *
 * ref: {@linkplain https://github.com/tokuhirom/caller}
 * </p>
 * 
 * @author moznion
 *
 */
@Slf4j
class StackTracer {
	private static boolean initialized = false;
	private static Method getStackTraceElement;

	static Optional<StackTraceElement> getStackTraceElement(int n) {
		if (!initialized) {
			initialize();
		}

		if (getStackTraceElement != null) {
			try {
				return Optional.of((StackTraceElement)getStackTraceElement.invoke(new Throwable(), n));
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				log.warn(new StringBuilder("Failed to invoke getStachTraceElement() method via reflection: ")
					.append(e.toString())
					.toString());
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private static synchronized void initialize() {
		try {
			Method method = Throwable.class.getDeclaredMethod("getStackTraceElement", int.class);
			method.setAccessible(true);
			getStackTraceElement = method;
		} catch (NoSuchMethodException e) {
			log.warn(new StringBuilder("Throwable.getStackTraceElement is not available: ")
				.append(e.toString())
				.toString());
		} finally {
			initialized = true;
		}
	}
}
