# opensky-json

## tools needed

- Java 11 
- sbt 1.7.*
- docker
- bash to run the build

## Building project
To build the project, you only have to run the build.sh file.

Add executable permission on the file, then do

```bash
./build.sh  
```
It will also create the docker image.

## Running project

### For PROD
To run the project, you only have to run the run.sh file after running doing the build step.

Add executable permission on the file, then do

```bash
./run.sh
```
This will run the docker image on port 9000, of course you can change that by replacing XXXX:9000 with port of your,
for example, 8200:9000 if you want to run on port 8200


### For DEV
just run the following 

```bash
sbt run
```

## Cleaning project
To clean the artifacts, you only have to run the clean.sh file.

Add executable permission on the file, then do

```bash
./clean.sh
```
It will also remove the docker image.