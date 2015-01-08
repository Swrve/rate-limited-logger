package com.swrve.ratelimitedlogger;

/**
 * An interface used to implement the target for RateLimitedLogWithPattern#withMetrics(), allowing
 * callers to provide their own metric-recording implementation.
 */
public interface CounterMetric {

    /**
     * Increment the value of the named metric @param metricName by 1.
     */
    void increment(String metricName);
}
