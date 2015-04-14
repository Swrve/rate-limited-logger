

This is a set of JMH benchmarks for RateLimitedLogger.


## Building and Running

run:

    mvn clean install && java -jar target/benchmarks.jar


## Last Results

```
Result: 105.151 ?(99.9%) 1.513 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 105.151, 787456.000), stdev = 747.952
  Confidence interval (99.9%): [103.638, 106.664]
  Samples, N = 2645434
        mean =    105.151 ?(99.9%) 1.513 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =   1000.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   8992.000 ns/op
  p(99.9990) =  16992.000 ns/op
  p(99.9999) = 315740.017 ns/op
         max = 787456.000 ns/op


# Run complete. Total time: 00:20:28

Benchmark                                    Mode      Cnt    Score   Error  Units
BenchLogWithPatternAndLevel.testMethod     sample  3066802   76.829 ? 0.571  ns/op
BenchRateLimitedLogWithPattern.testMethod  sample  2458261   84.587 ? 0.627  ns/op
BenchWithStringKey.testMethod              sample  2645434  105.151 ? 1.513  ns/op
```
