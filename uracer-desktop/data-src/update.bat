wget moon:8000/build/uracer-kotd.zip
rmdir /Q /S game
mkdir game
unzip uracer-kotd.zip -d game
del /Q prev.zip
ren uracer-kotd.zip prev.zip
pause