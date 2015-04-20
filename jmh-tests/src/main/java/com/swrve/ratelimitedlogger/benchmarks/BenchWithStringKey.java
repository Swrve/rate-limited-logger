package com.swrve.ratelimitedlogger.benchmarks;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.joda.time.Duration;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS )
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS )
@State(Scope.Benchmark)
public class BenchWithStringKey {
    private static final Logger logger = LoggerFactory.getLogger(BenchWithStringKey.class);
    private static final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                   .maxRate(1).every(Duration.standardSeconds(1000))
                   .build();

    @Setup
    public void prepare() {
        // simulate a bunch of unimportant log lines (to fill out the registry)
        for (int i = 0; i < 100; i++) {
            rateLimitedLog.info("unused_" + i);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testMethod() {
        rateLimitedLog.info("test");
    }
}
