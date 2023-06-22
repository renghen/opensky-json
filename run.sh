#!/bin/bash

echo "********* running project *********"
docker run -it -p 9000:9000 open-sky:1.0

# deamon mode
# docker run -d -p 9000:9000 open-sky:1.0