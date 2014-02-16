#!/bin/bash

LIBGDX_CLASSPATH="/home/manuel/dev/libgdx"
URACER_LIBS="../libs/java-getopt-1.0.14.jar"
URACER_CLASSPATH="./bin"
CONTRIBS_LIBS="/home/manuel/dev/libgdx-contribs"
BOX2DLIGHTS="../libs/box2dlights/box2dLight/bin"
JFUZZYLOGIC="../libs/jFuzzyLogic_v3.0.jar"

JARS="
${URACER_LIBS}:\
${CONTRIBS_LIBS}/postprocessing/bin:\
${CONTRIBS_LIBS}/utils/bin:\
${BOX2DLIGHTS}:\
${JFUZZYLOGIC}:\
${LIBGDX_CLASSPATH}/dist/gdx.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-natives.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-backend-lwjgl.jar:\
${LIBGDX_CLASSPATH}/dist/gdx-backend-lwjgl-natives.jar"

URACER="java -classpath ${JARS}:${URACER_CLASSPATH} com.bitfire.uracer.URacerDesktop"

echo "Environment set to: " ${JARS}
cd `dirname "$0"`
${URACER} "$@"
cd -