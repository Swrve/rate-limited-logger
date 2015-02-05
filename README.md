RateLimitedLogger [![CircleCI badge](https://circleci.com/gh/Swrve/rate-limited-logger.svg?style=svg&circle-token=a2d7a24d30021fc04658b58c24c1758e891e66fc)](https://circleci.com/gh/Swrve/rate-limited-logger)
========

An SLF4J-compatible, simple, fluent API for rate-limited logging in Java.

Logging is vital for production-ready, operable code; however, in certain
situations, it can be dangerous.  It is easy to wipe out throughput of a
performance-critical backend component by several orders of magnitude with a 
misplaced log call, or if the input data suddenly changes to something
slightly unexpected, triggering disk I/O in a performance hotspot.

With RateLimitedLogger, however, this risk is avoided.  A RateLimitedLog object
imposes an efficient rate limit internally, and will efficiently suppress
logging if this is exceeded.  When a log is suppressed, at the end of the limit
period, another log message is output indicating how many logs were hidden.

This style of rate limiting is the same as the one used by UNIX syslog, so
should be comprehensible, easy to predict, and familiar to many users, unlike
more complex adaptive rate limits.

The RateLimitedLogger library is thread-safe.

RateLimitedLogger wraps your existing SLF4J loggers, so should be easy to plug
into existing Java code.


## Usage

```
  private static final Logger logger = LoggerFactory.getLogger(getClass());

  private static final RateLimitedLog rateLimitedLog = RateLimitedLog
            .withRateLimit(logger)
            .maxRate(10).every(10, TimeUnit.SECONDS)
            .build();
```

This will wrap an existing SLF4J Logger object, allowing a max of 10 messages
to be output every 10 seconds, suppressing any more than that.  


## Sample output

```
  22:16:03.584 [main] INFO Demo - message 1
  22:16:03.604 [main] INFO Demo - message 2
  22:16:03.634 [main] INFO Demo - message 3
  22:16:04.986 [RateLimitedLogRegistry-0] INFO Demo - (suppressed 39 logs similar to 'message {}' in PT1.0S)
```

## Interpolation

Each log message has its own internal rate-limiting AtomicLong counter.  In
other words, if you have 2 log messages, you can safely reuse the same
RateLimitedLog object to log both, and a high rate of one will not cause the
other to be suppressed as a side effect.

However, this means that if you wish to include dynamic, variable data in the
log output, you will need to use SLF4J-style templates, instead of ("foo " +
bar + " baz") string interpolation. For example:

```
  rateLimitedLog.info("Just saw an event of type {}: {}", event.getType(), event);
```

"{}" will implicitly invoke an object's toString() method, so toString() does
not need to be called explicitly when logging.  (This has obvious performance
benefits, in that those toString() methods will not be called at all once the
rate limits have been exceeded or if the log-level threshold isn't reached.)

A RateLimitedLog object has a limited capacity for the number of log messages
it'll hold; if over 1000 different strings are used as the message template for
a single RateLimitedLog object, it is assumed that the caller is accidentally
using an already-interpolated string containing variable data in place of the
template, and the current set of logs will be flushed in order to avoid an
OutOfMemory condition.  This has a performance impact, but at least it won't
lose data!

Where performance is critical, note that you can obtain a reference to the
RateLimitedLogWithPattern object for an individual log template, which will
then avoid a ConcurrentHashMap lookup.


## Dependencies

All versions are minimum versions -- later versions should also work fine.

- Java 1.6
- SLF4J API 1.7.7
- Guava 15.0
- Joda-Time 2.3
- Findbugs Annotations 1.0.0
- Findbugs JSR-305 Annotations 2.0.2


## License

(c) Copyright 2014-2015 Swrve Mobile Inc or its licensors.
Distributed under version 2.0 of the Apache License, see "LICENSE".


## Building

```
  ./gradlew all
```


## Credits

- The Swrve dev team: http://www.swrve.com/
- Our blog: http://swrveengineering.wordpress.com/

