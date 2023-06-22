#!/bin/bash

echo "********* running Web APP *********"
docker run -it -p 8080:8080 opensky-webapp:1.0

# deamon mode
# docker run -d -p 8080:8080 opensky-webapp:1.0 