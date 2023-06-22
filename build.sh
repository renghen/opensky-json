#!/bin/bash

echo "********* building project *********"
sbt dist
unzip -d open-sky/ ./target/universal/opensky-json-1.0.zip #unzip
mv open-sky/*/* open-sky/ # move one level up
rm open-sky/bin/*.bat # remove useless files
mv open-sky/bin/* open-sky/bin/start # rename startup file to start
rm -rf open-sky/opensky-json-1.0/ # remove empty folder

echo "********* building docker *********"
docker build -t open-sky:1.0 .