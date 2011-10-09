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
rm -rf "${DEST}/font/"
mkdir -p "${DEST}/font/"
${TEX_PACKER} font ${DEST}/font >/dev/null
cp -f font/*.fnt ${DEST}/font
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
cp track/wall.png track/*.g3dt ${DEST}/track >/dev/null
echo "done!"

# cars
echo -n "Cooking car graphics..."
rm -rf "${DEST}/cars/"
mkdir -p "${DEST}/cars/"
${TEX_PACKER} cars ${DEST}/cars >/dev/null
echo "done!"

# particle effects
echo -n "Cooking particle effects..."
rm -rf "${DEST}/partfx/"
mkdir -p "${DEST}/partfx/"
cp partfx/* ${DEST}/partfx > /dev/null
echo "done!"