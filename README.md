RateLimitedLogger [![CircleCI badge](https://circleci.com/gh/Swrve/rate-limited-logger.svg?style=svg&circle-token=a2d7a24d30021fc04658b58c24c1758e891e66fc)](https://circleci.com/gh/Swrve/rate-limited-logger)
========

An SLF4J-compatible, simple, fluent API for rate-limited logging in Java.

Logging is vital for production-ready, operable code; however, in certain
situations, it can be dangerous.  It is easy to wipe out throughput of a
performance-critical backend component by several orders of magnitude with disk
I/O in a performance hotspot, caused by a misplaced log call, or input data
changing to something slightly unexpected.

With RateLimitedLogger, however, this risk is avoided.  A RateLimitedLog object
tracks the rate of log message emission, imposes an internal rate limit, and
will efficiently suppress logging if this is exceeded.  When a log is
suppressed, at the end of the limit period, another log message is output
indicating how many log lines were suppressed.

This style of rate limiting is the same as the one used by UNIX syslog; this
means it should be comprehensible, easy to predict, and familiar to many users,
unlike more complex adaptive rate limits.

RateLimitedLogger wraps your existing SLF4J loggers, so should be easy to plug
into existing Java code.


## Binaries:

This module is available in the Maven Central repository at
http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rate-limited-logger%22

WARNING: version 2.0.0 drops support for Java 6 and 7, and is Java 8-only. It
also requires (minor) code changes to use the java.time classes instead of
Joda-Time.  If you still need support for Java 6 or 7, use version 1.1.0 or
earlier.

Maven:

```
  <dependency>
      <groupId>com.swrve</groupId>
      <artifactId>rate-limited-logger</artifactId>
      <version>2.0.2</version>
  </dependency>
```

Gradle:

```
  compile group: 'com.swrve', name: 'rate-limited-logger', version: '2.0.2'
```

Sample code:

```
  private static final Logger logger = LoggerFactory.getLogger(getClass());

  private static final RateLimitedLog rateLimitedLog = RateLimitedLog
            .withRateLimit(logger)
            .maxRate(5).every(Duration.ofSeconds(10))
            .build();
```

This will wrap an existing SLF4J Logger object, allowing a max of 5 messages
to be output every 10 seconds, suppressing any more than that.


## More documentation

Javadoc can be found at http://swrve.github.io/rate-limited-logger/javadoc/2.0.2/


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


## Performance

In tests using JMH with Java 7 on a 2012 Macbook Pro, using RateLimitedLog with
logger.info("string") ran in, on average, 73 nanoseconds per op, with a P99.99
of 2000 ns/op and a P99.999 of 12000 ns/op, once the rate limit was exceeded.

Where performance is critical, note that you can obtain a reference to the
LogWithPatternAndLevel object for an individual log template and level, which will
then avoid a ConcurrentHashMap and AtomicReferenceArray lookup:

```
  ref = logger.get("string", Level.INFO)
  ref.log()
```

Using this approach, the average post-ratelimit time dropped to 56 nanoseconds
per op, with a P99.99 of 1000 ns/op and a P99.999 of 8992 ns/op.

More details: https://github.com/Swrve/rate-limited-logger/tree/master/jmh-tests


## Thread-Safety

The RateLimitedLogger library is thread-safe.  Under heavy load, though, it is
possible for more log messages to be exceeded than the limit specifies, for a
short period after the limit is exceeded (typically on the order of a duration
of a few milliseconds).


## Dependencies

All versions are minimum versions -- later versions should also work fine.

- Java 8
- SLF4J API 1.7.7
- Findbugs Annotations 1.0.0
- Findbugs JSR-305 Annotations 2.0.2

version 1.1.0 of RateLimitedLogger supports Java 7, but requires Guava 15.0 and
Joda-Time 2.3 in addition to the above.


## License

(c) Copyright 2014-2018 Swrve Mobile Inc or its licensors.
Distributed under version 2.0 of the Apache License, see "LICENSE".


## Building

Build all JARs, test, measure coverage:

```
  ./gradlew all
```


## Credits

- The Swrve team: http://www.swrve.com/
    - Our dev blog: http://swrveengineering.wordpress.com/

- Fabien Comte <comtef at gmail.com>, for Java 8 support

