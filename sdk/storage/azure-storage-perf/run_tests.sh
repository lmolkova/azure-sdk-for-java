#!/bin/bash

java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        downloadblob --size 10240 --parallel 64 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        downloadblob --size 10485760 --parallel 32 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        downloadblobtofile --size 10485760 --parallel 32 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        uploadblob --size 10240 --parallel 64 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        uploadblob --size 10485760 --parallel 32 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        uploadfromfile --size 10485760 --parallel 32 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        listblobs --count 5 --parallel 64 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        listblobs --count 500 --parallel 32 --warmup 15 --duration 30
java -jar ./target/azure-storage-perf-1.0.0-beta.1-jar-with-dependencies.jar        listblobs --count 50000 --parallel 32 --warmup 60 --duration 60













