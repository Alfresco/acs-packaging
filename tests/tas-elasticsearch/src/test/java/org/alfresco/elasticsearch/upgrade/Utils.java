package org.alfresco.elasticsearch.upgrade;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import com.google.common.util.concurrent.RateLimiter;

class Utils
{
    public static void waitFor(String description, final Duration timeout, final BooleanSupplier condition)
    {
        final RateLimiter rateLimiter = RateLimiter.create(5);
        final long numberOfIterations = Math.max(1L, timeout.getSeconds() * (long) rateLimiter.getRate());
        for (long i = 0; i < numberOfIterations; i++)
        {
            rateLimiter.acquire();
            if (condition.getAsBoolean())
            {
                return;
            }
        }
        throw new RuntimeException("Failed to wait for " + description + ".");
    }
}
