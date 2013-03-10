#!/bin/bash


CLASS_PATH="/home/manuel/dev/libgdx/dist"
JARS="${CLASS_PATH}/gdx.jar:${CLASS_PATH}/gdx-natives.jar:${CLASS_PATH}/gdx-backend-lwjgl.jar:${CLASS_PATH}/gdx-backend-lwjgl-natives.jar"

GDX_TOOLS_PATH="/home/manuel/dev/libgdx/dist/extensions/gdx-tools/"
TEX_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar com.badlogic.gdx.tools.imagepacker.TexturePacker2"

GDX_TILED_PREP_PATH="/home/manuel/dev/uracer-libgdx/libs/gdx-tiled-preprocessor/bin"
TILED_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar:${GDX_TILED_PREP_PATH} com.badlogic.gdx.tiledmappacker.TiledMapPacker"

#SKIN_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}:/home/manuel/dev/uracer-skin-packer/bin com.bitfire.uracer.skinpacker.Packer"

DEST="/home/manuel/dev/uracer-libgdx/uracer-desktop/data"

# base
echo -n "Cooking base..."
rm -rf "${DEST}/base/"
mkdir -p "${DEST}/base"
cp base/*.png ${DEST}/base
cp base/*.shape ${DEST}/base
cp -r base/progress ${DEST}/base
echo "done!"

# tileset graphics and tmx levels
echo -n "Cooking levels..."
rm -rf "${DEST}/levels/"

# packer
mkdir -p ${DEST}
${TILED_PACKER} levels/ ${DEST}/levels #--strip-unused

# no packer
#mkdir -p ${DEST}/levels/
#mkdir -p ${DEST}/levels/tilesets/desert/
#cp -r levels/*.tmx ${DEST}/levels/
#cp -r levels/tilesets/desert/224.png ${DEST}/levels/tilesets/desert/

echo "done!"

# tileset friction maps
echo -n "Cooking friction maps..."
cp levels/tilesets/desert/224-friction-easy.png ${DEST}/levels/tilesets/desert/
cp levels/tilesets/desert/224-friction-hard.png ${DEST}/levels/tilesets/desert/
echo "done!"

# fonts
echo -n "Cooking fonts..."
rm -rf "${DEST}/font/"
mkdir -p "${DEST}/font/"
${TEX_PACKER} font ${DEST}/font >/dev/null
cp -f font/*.fnt ${DEST}/font
echo "done!"

# track
echo -n "Cooking track meshes..."
rm -rf "${DEST}/track/"
mkdir -p "${DEST}/track/"
#cp track/wall.png track/*.g3dt ${DEST}/track >/dev/null
cp track/wall.png ${DEST}/track >/dev/null
echo "done!"

# cars graphics
echo -n "Cooking car graphics..."
rm -rf "${DEST}/cars/"
mkdir -p "${DEST}/cars/"
${TEX_PACKER} cars ${DEST}/cars >/dev/null
echo "done!"

# cars physical shapes
echo -n "Cooking car physical shapes..."
cp cars/car-shapes ${DEST}/cars >/dev/null
echo "done!"

# particle effects
echo -n "Cooking particle effects..."
rm -rf "${DEST}/partfx/"
mkdir -p "${DEST}/partfx/"
cp partfx/* ${DEST}/partfx > /dev/null
echo "done!"

# shaders
echo -n "Merging GLSL shaders from libgdx-contribs/postprocessing to data-src..."
cp -r /home/manuel/dev/libgdx-contribs/postprocessing/shaders/ ./
echo "done!"

# shaders
echo -n "Cooking GLSL shaders..."
rm -rf "${DEST}/shaders/"
mkdir -p "${DEST}/shaders/"
cp -r shaders/* ${DEST}/shaders > /dev/null
echo "done!"

# ui skin
echo -n "Cooking UI skin..."
rm -rf "${DEST}/ui/"
mkdir -p "${DEST}/ui/"
${TEX_PACKER} ui/skin ${DEST}/ui skin >/dev/null
#${SKIN_PACKER} ui/skin
#cp ui/skin/skin.png ${DEST}/ui >/dev/null
cp ui/skin/skin.json ${DEST}/ui >/dev/null
cp ui/font/default.fnt ${DEST}/ui >/dev/null
cp ui/font/default.png ${DEST}/ui >/dev/null
echo "done!"