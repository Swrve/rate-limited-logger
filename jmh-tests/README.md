

This is a set of JMH microbenchmarks for RateLimitedLogger.


## Building and Running

run:

```
    mvn install:install-file -DgroupId=com.swrve -Dpackaging=jar \
        -DartifactId=rate-limited-logger -DgeneratePom=true \
        -Dversion=1.1 -Dfile=../build/libs/rate-limited-logger-1.1.jar

    mvn clean install && java -jar target/benchmarks.jar
```


## Last Results

```
Result: 56.023 ?(99.9%) 0.751 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 56.023, 10992.000), stdev = 235.553
  Confidence interval (99.9%): [55.272, 56.775]
  Samples, N = 1063912
        mean =     56.023 ?(99.9%) 0.751 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =      0.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   1000.000 ns/op
  p(99.9990) =   8992.000 ns/op
  p(99.9999) =  10928.598 ns/op
         max =  10992.000 ns/op

Result: 58.433 ?(99.9%) 1.788 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 58.433, 802816.000), stdev = 684.113
  Confidence interval (99.9%): [56.644, 60.221]
  Samples, N = 1584857
        mean =     58.433 ?(99.9%) 1.788 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =      0.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   1000.000 ns/op
  p(99.9990) =   8992.000 ns/op
  p(99.9999) = 374157.196 ns/op
         max = 802816.000 ns/op

Result: 73.518 ?(99.9%) 0.728 ns/op [Average]
  Statistics: (min, avg, max) = (0.000, 73.518, 32960.000), stdev = 277.994
  Confidence interval (99.9%): [72.789, 74.246]
  Samples, N = 1577061
        mean =     73.518 ?(99.9%) 0.728 ns/op
         min =      0.000 ns/op
  p( 0.0000) =      0.000 ns/op
  p(50.0000) =      0.000 ns/op
  p(90.0000) =      0.000 ns/op
  p(95.0000) =   1000.000 ns/op
  p(99.0000) =   1000.000 ns/op
  p(99.9000) =   1000.000 ns/op
  p(99.9900) =   2000.000 ns/op
  p(99.9990) =  12000.000 ns/op
  p(99.9999) =  31242.663 ns/op
         max =  32960.000 ns/op


# Run complete. Total time: 00:07:47

Benchmark                                    Mode      Cnt   Score   Error  Units
BenchLogWithPatternAndLevel.testMethod     sample  1063912  56.023 ? 0.751  ns/op
BenchRateLimitedLogWithPattern.testMethod  sample  1584857  58.433 ? 1.788  ns/op
BenchWithStringKey.testMethod              sample  1577061  73.518 ? 0.728  ns/op
```
