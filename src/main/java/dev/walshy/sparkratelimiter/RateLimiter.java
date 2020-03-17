package dev.walshy.sparkratelimiter;

import spark.Request;
import spark.Spark;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class RateLimiter {

    private final Map<String, Semaphore> keyCache = new HashMap<>();

    private final int maxRequests;
    private final long resetTime;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService service;
    private final Function<Request, String> keyFunction;

    private long lastReset;

    @ParametersAreNonnullByDefault
    public RateLimiter(int maxRequests, long resetTime, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "You need to specify a TimeUnit!");

        this.maxRequests = maxRequests;
        this.resetTime = resetTime;
        this.timeUnit = timeUnit;
        this.lastReset = System.currentTimeMillis();
        this.keyFunction = Request::ip;

        service = Executors.newSingleThreadScheduledExecutor();
        scheduleExpiry();
    }

    @ParametersAreNonnullByDefault
    public RateLimiter(int maxRequests, long resetTime, TimeUnit timeUnit, Function<Request, String> keyFunction) {
        Objects.requireNonNull(timeUnit, "You need to specify a TimeUnit!");
        Objects.requireNonNull(keyFunction, "You need to specify a Function!!");

        this.maxRequests = maxRequests;
        this.resetTime = resetTime;
        this.timeUnit = timeUnit;
        this.lastReset = System.currentTimeMillis();
        this.keyFunction = keyFunction;

        service = Executors.newSingleThreadScheduledExecutor();
        scheduleExpiry();
    }

    public void map(@Nonnull String path) {
        Spark.before(path, (req, res) -> {
            final Semaphore semaphore = getSemaphore(req);

            boolean rateLimited = !semaphore.tryAcquire();

            res.header("X-RateLimit-Limit", String.valueOf(maxRequests));
            res.header("X-RateLimit-Remaining", String.valueOf(semaphore.availablePermits()));
            res.header("X-RateLimit-Reset", String.valueOf(Math.max(0, getTimeLeft())));

            if (rateLimited)
                Spark.halt(429);
        });
    }

    public boolean tryAcquire(@Nonnull Request request) {
        return this.getSemaphore(request).tryAcquire();
    }

    public long getTimeLeft() {
        return (lastReset + timeUnit.toMillis(resetTime)) - System.currentTimeMillis();
    }

    public int getRequestsLeft(@Nonnull Request request) {
        return this.getSemaphore(request).availablePermits();
    }

    public void stop() {
        this.service.shutdownNow();
    }

    private void scheduleExpiry() {
        service.scheduleAtFixedRate(() -> {
            this.lastReset = System.currentTimeMillis();
            for (Map.Entry<String, Semaphore> entry : keyCache.entrySet()) {
                final Semaphore semaphore = entry.getValue();
                if (semaphore.availablePermits() == maxRequests)
                    keyCache.remove(entry.getKey());
                else
                    semaphore.release(maxRequests - semaphore.availablePermits());
            }
        }, resetTime, resetTime, timeUnit);
    }

    private Semaphore getSemaphore(@Nonnull Request request) {
        return this.keyCache.computeIfAbsent(keyFunction.apply(request), s -> new Semaphore(this.maxRequests));
    }
}