#!/bin/bash

CLASS_PATH="/home/manuel/dev/libgdx/dist"
JARS="${CLASS_PATH}/gdx.jar:${CLASS_PATH}/gdx-natives.jar:${CLASS_PATH}/gdx-backend-lwjgl.jar:${CLASS_PATH}/gdx-backend-lwjgl-natives.jar"

GDX_TOOLS_PATH="/home/manuel/dev/libgdx/dist/extensions/gdx-tools/"
TEX_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar com.badlogic.gdx.tools.texturepacker.TexturePacker"

# in uracer/libs
#GDX_TILED_PREP_PATH="/home/manuel/dev/uracer-libgdx/libs/gdx-tiled-preprocessor/bin"
#TILED_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar:${GDX_TILED_PREP_PATH} com.badlogic.gdx.tiledmappacker.TiledMapPacker"

# in libgdx/extensions
GDX_TILED_PREP_PATH="/home/manuel/dev/libgdx/dist/extensions/gdx-tiled-preprocessor/"
TILED_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}/gdx-tools.jar:${GDX_TILED_PREP_PATH}/gdx-tiled-preprocessor.jar com.badlogic.gdx.tiledmappacker.TiledMapPacker"

#SKIN_PACKER="java -classpath ${JARS}:${GDX_TOOLS_PATH}:/home/manuel/dev/uracer-skin-packer/bin com.bitfire.uracer.skinpacker.Packer"

DEST="/home/manuel/dev/uracer-libgdx/uracer-desktop/data"

# utilities
if [ "$1" = "clean-levels" ]; then
	rm -rf "$DEST/levels"
	mkdir "$DEST/levels"
	echo "Levels cleaned"
	exit
fi

# base
echo -n "Cooking base..."
	rm -rf "${DEST}/base/"
	mkdir -p "${DEST}/base"
	cp base/*.png ${DEST}/base
	cp -r base/progress ${DEST}/base
echo "done!"

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
	cp track/wall*.png ${DEST}/track >/dev/null
echo "done!"

# cars graphics
echo -n "Cooking cars..."
	rm -rf "${DEST}/cars/"
	mkdir -p "${DEST}/cars/"
	${TEX_PACKER} cars/out ${DEST}/cars
	cp cars/out/car-shapes ${DEST}/cars/ >/dev/null
	cp 3d/car/out/* ${DEST}/3d/textures >/dev/null
echo "done!"


# particle effects defs
echo "Cooking particle effects definitions..."
rm -rf "${DEST}/partfx/"
mkdir -p "${DEST}/partfx/textures"
	# remove abs path for release
	PFX_FILES="partfx/*.p"
	for f in $PFX_FILES
	do
		echo -en "\tProcessing ${f}..."
		cat ${f} | sed -r 's/\/home\/manuel\/dev\/uracer-libgdx\/uracer-desktop\/data-src\/partfx\/textures\/(.*)\.png/\1/g' > ${DEST}/${f}
		echo "done!"
	done

# particle effects defs
echo -n "Cooking particle effects graphics..."
	${TEX_PACKER} partfx/textures ${DEST}/partfx/textures >/dev/null
echo "done!"

# prepare shaders/
echo -n "NOT Merging GLSL shaders from libgdx-contribs/postprocessing to data-src/shaders..."
	#cp -r /home/manuel/dev/libgdx-contribs/postprocessing/src/main/resources/shaders/ ./
#echo "done!"

# copy shaders/ to a new dest/shaders
echo -n "Cooking GLSL shaders..."
	rm -rf "${DEST}/shaders/"
	mkdir -p "${DEST}/shaders/"
	cp -r shaders/* ${DEST}/shaders > /dev/null
echo "done!"

# ui skin
echo -n "Cooking UI skins..."
rm -rf "${DEST}/ui/"
mkdir -p "${DEST}/ui/"
echo -n "Baking default skin..."
	mkdir -p "${DEST}/ui/skin"
	#${SKIN_PACKER} ui/skin
	#cp ui/skin/skin.png ${DEST}/ui >/dev/null
	${TEX_PACKER} ui/skin ${DEST}/ui/skin skin >/dev/null

	# default skin
	cp ui/skin/skin-small.json ${DEST}/ui/skin >/dev/null
	cp ui/skin/skin-mid.json ${DEST}/ui/skin >/dev/null
	cp ui/skin/skin-big.json ${DEST}/ui/skin >/dev/null
	cp ui/skin/sans12.fnt ${DEST}/ui/skin >/dev/null
	cp ui/skin/sans13.fnt ${DEST}/ui/skin >/dev/null
	cp ui/skin/sans15.fnt ${DEST}/ui/skin >/dev/null
echo "done!"

echo -n "Copying holo skin..."
	mkdir -p "${DEST}/ui/holo"

	# holo skin
	cp ui/holo/* ${DEST}/ui/holo >/dev/null
echo "done!"

echo -n "Copying kenney skin..."
	mkdir -p "${DEST}/ui/kenney"

	# holo skin
	cp ui/kenney/* ${DEST}/ui/kenney >/dev/null
echo "done!"

echo -n "Copying menu sfx..."
	mkdir -p "${DEST}/audio/menu-sfx"

	# holo skin
	cp audio/menu-sfx/* ${DEST}/audio/menu-sfx >/dev/null
echo "done!"
