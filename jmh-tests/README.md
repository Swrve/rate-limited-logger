

This is a set of JMH benchmarks for RateLimitedLogger.


## Building and Running

run:

    mvn clean install && java -jar target/benchmarks.jar


## Last Results

```
Benchmark                                   Mode  Cnt         Score        Error  Units
BenchRateLimitedLogWithPattern.testMethod  thrpt  200  65634369.658 ? 606516.028  ops/s
BenchWithStringKey.testMethod              thrpt  200  46708458.941 ? 296683.688  ops/s
```
