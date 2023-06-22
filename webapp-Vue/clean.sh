#!/bin/bash

echo "********* cleaning project *********" ;
rm -rf dist/

echo "********* deleting docker image *********" ;
docker rmi -f opensky-webapp:1.0