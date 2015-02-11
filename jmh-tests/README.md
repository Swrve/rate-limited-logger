

This is a set of JMH benchmarks for RateLimitedLogger.


## Building and Running

run:

    mvn clean install && java -jar target/benchmarks.jar


## Last Results

```
Result: 56.741 ?(99.9%) 0.876 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 56.741, 417792.000), stdev = 384.357
  Confidence interval (99.9%): [55.865, 57.617]
  Samples, N = 2084806
        mean =     56.741 ?(99.9%) 0.876 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =      0.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   4519.300 ns/op
  p(99.9990) =  12992.000 ns/op
  p(99.9999) =  39489.801 ns/op
         max = 417792.000 ns/op

Result: 68.418 ?(99.9%) 0.770 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 68.418, 289792.000), stdev = 355.517
  Confidence interval (99.9%): [67.648, 69.188]
  Samples, N = 2310521
        mean =     68.418 ?(99.9%) 0.770 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =      0.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   8000.000 ns/op
  p(99.9990) =  14887.622 ns/op
  p(99.9999) = 135050.981 ns/op
         max = 289792.000 ns/op

Run complete. Total time: 00:13:36
Benchmark                                    Mode      Cnt   Score   Error  Units
BenchRateLimitedLogWithPattern.testMethod  sample  2084806  56.741 ? 0.876  ns/op
BenchWithStringKey.testMethod              sample  2310521  68.418 ? 0.770  ns/op
```
