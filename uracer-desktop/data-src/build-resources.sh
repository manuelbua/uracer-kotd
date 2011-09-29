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
rm -rf "${DEST}/base/font/"
mkdir -p "${DEST}/base/font/"
${TEX_PACKER} base/font ${DEST}/base/font >/dev/null
cp -f base/font/*.fnt ${DEST}/base/font
echo "done!"

# 3d
#echo -n "Cooking models textures..."
#rm -rf "${DEST}/3d/textures/"
#mkdir -p "${DEST}/3d/textures/"
#${TEX_PACKER} 3d ${DEST}/3d/textures >/dev/null
#echo "done!"

# track
echo -n "Cooking track meshes..."
rm -rf "${DEST}/track/"
mkdir -p "${DEST}/track/"
cp track/wall.jpg track/*.g3dt ${DEST}/track >/dev/null
echo "done!"