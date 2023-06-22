#!/bin/bash

echo "********* cleaning project *********" ;
sbt clean
rm -rf open-sky/

echo "********* deleting docker image *********" ;
docker rmi -f open-sky:1.0