package org.ironrhino.core.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.ThrowableCallable;
import org.ironrhino.core.util.ThrowableRunnable;
import org.springframework.util.ClassUtils;

public class Metrics {

	private static boolean micrometerPresent = ClassUtils.isPresent("io.micrometer.core.instrument.Metrics",
			Metrics.class.getClassLoader());

	public static void recordTimer(String name, long amount, TimeUnit unit, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(amount, unit);
	}

	public static <T> T recordTimer(String name, Callable<T> callable, String... tags) throws Exception {
		if (!micrometerPresent)
			return callable.call();
		return io.micrometer.core.instrument.Metrics.timer(name, tags).recordCallable(callable);
	}

	public static void recordTimer(String name, Runnable runnable, String... tags) {
		if (!micrometerPresent) {
			runnable.run();
			return;
		}
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(runnable);
	}

	public static <T, E extends Throwable> T recordThrowableCallable(String name, ThrowableCallable<T, E> callable,
			String... tags) throws E {
		if (!micrometerPresent)
			return callable.call();
		io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = System.nanoTime();
		try {
			return callable.call();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static <E extends Throwable> void recordThrowableRunnable(String name, ThrowableRunnable<E> runnable,
			String... tags) throws E {
		if (!micrometerPresent) {
			runnable.run();
			return;
		}
		io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = System.nanoTime();
		try {
			runnable.run();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static void recordSummary(String name, long amount, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.summary(name, tags).record(amount);
	}

	public static void increment(String name, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment();
	}

	public static void increment(String name, double amount, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment(amount);
	}

}
