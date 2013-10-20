wget --progress=dot moon:8000/build/uracer-kotd.zip
rm -rf game
mkdir game
unzip uracer-kotd.zip -d game
rm -f prev.zip
mv uracer-kotd.zip prev.zip