#!/bin/bash

cd ~/Code/Java/fileSynchronizer/
mvn package && cp -r ./target/classes/* ./dev/bin/ && cp -r ./src/main/java/fileSynchronizer/* ./dev/src