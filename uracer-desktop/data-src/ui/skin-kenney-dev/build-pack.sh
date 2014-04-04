#!/bin/bash

CLASS_PATH="/home/manuel/dev/libgdx/dist"
JARS="${CLASS_PATH}/gdx.jar:${CLASS_PATH}/gdx-natives.jar:${CLASS_PATH}/gdx-backend-lwjgl.jar:${CLASS_PATH}/gdx-backend-lwjgl-natives.jar"

GDX_TOOLS_PATH="/home/manuel/dev/libgdx/dist/extensions/gdx-tools/"
TEX_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar com.badlogic.gdx.tools.texturepacker.TexturePacker"

DEST="./out"

rm -rf "${DEST}"
mkdir -p "${DEST}"

# add fonts
cp font/*.png sprites

${TEX_PACKER} sprites ${DEST}
