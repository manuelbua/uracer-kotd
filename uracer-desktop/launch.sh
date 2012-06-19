#!/bin/bash

LIBGDX_CLASSPATH="/home/manuel/dev/libgdx"
URACER_LIBS="../libs/java-getopt-1.0.14.jar"
URACER_CLASSPATH="./bin"

JARS="
${URACER_LIBS}:\
${LIBGDX_CLASSPATH}/dist/gdx.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-natives.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-backend-lwjgl.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-backend-lwjgl-natives.jar:\
${LIBGDX_CLASSPATH}/extensions/model-loaders/model-loaders/bin"

URACER="java -classpath ${JARS}:${URACER_CLASSPATH} com.bitfire.uracer.URacerDesktop"

${URACER} "$@"