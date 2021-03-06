package org.ironrhino.core.throttle;

import static io.github.resilience4j.ratelimiter.utils.MetricNames.AVAILABLE_PERMISSIONS;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.WAITING_THREADS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.CheckedCallable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@ClassPresentConditional("io.github.resilience4j.ratelimiter.RateLimiter")
@Slf4j
public class RateLimiterRegistry {

	private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

	public <T, E extends Throwable> T executeCheckedCallable(String name, Supplier<RateLimiterConfig> configSupplier,
			CheckedCallable<T, E> callable) throws E {
		RateLimiter limiter = of(name, configSupplier);
		RateLimiterConfig rateLimiterConfig = limiter.getRateLimiterConfig();
		boolean permission = limiter.getPermission(rateLimiterConfig.getTimeoutDuration());
		if (Thread.interrupted()) {
			throw new IllegalStateException("Thread was interrupted during permission wait");
		}
		if (!permission) {
			throw new RequestNotPermitted("Request not permitted for limiter: " + limiter.getName());
		}
		return callable.call();
	}

	public RateLimiter of(String name, Supplier<RateLimiterConfig> configSupplier) {
		return rateLimiters.computeIfAbsent(name, key -> {
			RateLimiter rateLimiter = RateLimiter.of(key, configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + WAITING_THREADS, rateLimiter, rl -> rl.getMetrics().getNumberOfWaitingThreads());
				Metrics.gauge(prefix + AVAILABLE_PERMISSIONS, rateLimiter,
						rl -> rl.getMetrics().getAvailablePermissions());
			}
			return rateLimiter;
		});
	}

	public void changeLimitForPeriod(String name, int oldLimitForPeriod, int newLimitForPeriod) {
		RateLimiter rateLimiter = rateLimiters.get(name);
		if (rateLimiter != null) {
			synchronized (rateLimiter) {
				if (rateLimiter.getRateLimiterConfig().getLimitForPeriod() == oldLimitForPeriod) {
					rateLimiter.changeLimitForPeriod(newLimitForPeriod);
					log.info("Change limitForPeriod of RateLimiter('{}') from {} to {}", name, oldLimitForPeriod,
							newLimitForPeriod);
				} else {
					throw new OptimisticLockingFailureException("State changed, please refresh and retry.");
				}
			}
		}
	}

}
