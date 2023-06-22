# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## tools needed

- NodeJS
- NPM
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
This will run the docker image on port 8080, of course you can change that by replacing XXXX:8080 with port of your,
for example, 8200:8080 if you want to run on port 8200


### For DEV
just run the following 

```bash
npm run dev
```

## Cleaning project
To clean the artifacts, you only have to run the clean.sh file.

Add executable permission on the file, then do

```bash
./clean.sh  
```
It will also destroy the docker image.