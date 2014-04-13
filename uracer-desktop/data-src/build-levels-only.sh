#!/bin/bash
DEST="/home/manuel/dev/uracer-libgdx/uracer-desktop/data"

CLASS_PATH="/home/manuel/dev/libgdx/dist"
JARS="${CLASS_PATH}/gdx.jar:${CLASS_PATH}/gdx-natives.jar:${CLASS_PATH}/gdx-backend-lwjgl.jar:${CLASS_PATH}/gdx-backend-lwjgl-natives.jar"
GDX_TOOLS_PATH="/home/manuel/dev/libgdx/dist/extensions/gdx-tools/"
TILED_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar com.badlogic.gdx.tiledmappacker.TiledMapPacker"


# tileset graphics and tmx levels
echo -n "Cooking levels..."
rm -rf "${DEST}/levels/"

# packer
mkdir -p ${DEST}
${TILED_PACKER} levels/ ${DEST}/levels --strip-unused

# no packer
#mkdir -p ${DEST}/levels/
#mkdir -p ${DEST}/levels/tilesets/desert/
#cp -r levels/*.tmx ${DEST}/levels/
#cp -r levels/tilesets/desert/224.png ${DEST}/levels/tilesets/desert/

echo "done!"

# tileset friction maps
echo -n "Cooking friction maps..."
	cp levels/tilesets/desert/desert-friction-easy.png ${DEST}/levels/tileset/
	cp levels/tilesets/desert/desert-friction-hard.png ${DEST}/levels/tileset/
echo "done!"
