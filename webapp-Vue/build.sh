#!/bin/bash

echo "********* building web app *********"
npm run build

echo "********* building docker *********"
docker build -t opensky-webapp:1.0 .