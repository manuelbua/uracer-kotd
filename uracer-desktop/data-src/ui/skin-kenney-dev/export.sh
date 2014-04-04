#!/bin/bash

rm -rf ../kenney
mkdir ../kenney

# package at the upper level
cp out/* ../kenney/

# export fonts
cp font/*.fnt ../kenney

# export skin def
cp kenney.json ../kenney

# also copy to the data folder (debug purposes)
rm -rf ../../../data/ui/kenney
mkdir -p ../../../data/ui/kenney
cp -r ../kenney ../../../data/ui/
