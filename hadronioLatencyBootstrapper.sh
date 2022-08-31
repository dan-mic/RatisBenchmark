java -jar build/libs/RatisBenchmark-1.0-SNAPSHOT.jar --hadroNIO --role bootstrapper --addresses $1";"$2";"$3 --benchmark latency --clientCount 2 --threadCount 10 --duration 5 --messagesPerSecond 5000
