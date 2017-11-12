package com.swrve.ratelimitedlogger.benchmarks;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import com.swrve.ratelimitedlogger.RateLimitedLogWithPattern;
import org.openjdk.jmh.annotations.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS )
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS )
@State(Scope.Benchmark)
public class BenchRateLimitedLogWithPattern {
    private static final Logger logger = LoggerFactory.getLogger(BenchRateLimitedLogWithPattern.class);
    private static final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                   .maxRate(1).every(Duration.ofSeconds(1000))
                   .build();

    private RateLimitedLogWithPattern testMessage;

    @Setup
    public void prepare() {
        // simulate a bunch of unimportant log lines (to fill out the registry)
        for (int i = 0; i < 100; i++) {
            rateLimitedLog.info("unused_" + i);
        }
        testMessage = rateLimitedLog.get("test");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testMethod() {
        testMessage.info();
    }
}
