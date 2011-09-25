#!/bin/bash

CLASS_PATH="/home/manuel/dev/libgdx/target/dist"
TILED_PACKER_PATH="/home/manuel/dev/libgdx/extensions/tiled-preprocessor/bin"
JARS="${TILED_PACKER_PATH}:${CLASS_PATH}/gdx.jar:${CLASS_PATH}/gdx-natives.jar:${CLASS_PATH}/gdx-backend-jogl.jar:${CLASS_PATH}/gdx-backend-jogl-natives.jar:${CLASS_PATH}/gdx-tools.jar"

TEX_PACKER="java -classpath ${JARS} com.badlogic.gdx.tools.imagepacker.TexturePacker $1 $2"
TILED_PACKER="java -classpath ${JARS} com.badlogic.gdx.tiledmappacker.TiledMapPacker $1 $2"


DEST="/home/manuel/dev/uracer-libgdx/uracer-desktop/data"


# tileset graphics and tmx levels
echo -n "Cooking levels..."
rm -rf "${DEST}/levels/"
mkdir -p ${DEST}
${TILED_PACKER} levels/ ${DEST}/levels >/dev/null
echo "done!"

# fonts
echo -n "Cooking fonts..."
cd base
${TEX_PACKER} font ${DEST}/base/font >/dev/null
cp -f font/*.fnt ${DEST}/base/font
echo "done!"