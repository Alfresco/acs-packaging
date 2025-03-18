package org.alfresco.elasticsearch.upgrade;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Uninterruptibles;

class AvailabilityProbe
{
    private final Thread thread;
    private final RateLimiter rateLimiter;
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final Supplier<ProbeResult> probingFunction;
    private final AtomicLongMap<ProbeResult> stats = AtomicLongMap.create();

    public static AvailabilityProbe create(int requestsPerSecond, Supplier<ProbeResult> probingFunction)
    {
        return new AvailabilityProbe(requestsPerSecond, probingFunction);
    }

    private AvailabilityProbe(int requestsPerSecond, Supplier<ProbeResult> probingFunction)
    {
        rateLimiter = RateLimiter.create(requestsPerSecond);
        this.probingFunction = probingFunction;
        thread = new Thread(this::probing);
        thread.setDaemon(true);
    }

    public Stats getStats()
    {
        return new Stats(stats.asMap());
    }

    public Stats stop()
    {
        stopRequested.set(true);
        Uninterruptibles.joinUninterruptibly(thread, 10, TimeUnit.SECONDS);
        return getStats();
    }

    public void start()
    {
        if (thread.isAlive())
            return;
        thread.start();
    }

    private void probing()
    {
        while (!stopRequested.get())
        {
            rateLimiter.acquire();
            stats.incrementAndGet(probingFunction.get());
        }
    }

    public static class Stats
    {
        private final Map<ProbeResult, Long> results;

        private Stats(Map<ProbeResult, Long> results)
        {
            this.results = Map.copyOf(results);
        }

        @Override
        public String toString()
        {
            return results.toString();
        }

        public int getSuccessRatioInPercents()
        {
            final long ok = Optional.ofNullable(results.get(ProbeResult.ok())).orElse(0L);
            if (ok == 0)
                return 0;

            final long total = results.values().stream().mapToLong(Number::longValue).sum();
            return (int) (((ok * 1000) / total) / 10);
        }
    }

    public static class ProbeResult
    {
        private final Object result;
        private static final ProbeResult OK = new ProbeResult("OK");
        private static final ProbeResult FAIL = new ProbeResult("FAIL");
        private static final ConcurrentHashMap<Class<? extends Throwable>, ProbeResult> FAILURES_CACHE = new ConcurrentHashMap<>();

        public static ProbeResult ok()
        {
            return OK;
        }

        public static ProbeResult fail()
        {
            return FAIL;
        }

        public static ProbeResult fail(Throwable reason)
        {
            if (reason == null)
                return fail();
            return FAILURES_CACHE.computeIfAbsent(reason.getClass(), ProbeResult::new);
        }

        private ProbeResult(Object result)
        {
            this.result = Objects.requireNonNull(result);
        }

        @Override
        public String toString()
        {
            return result.toString();
        }

        @Override
        public boolean equals(Object o)
        {
            return (this == o) || (getClass() == o.getClass() && this.result.equals(((ProbeResult) o).result));
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(result);
        }
    }
}
