#LAYERS

- `track` contains the tiles building up its graphical representation

	- *PROPERTIES*
		- **start \<left|right|up|down\>** specifies the direction to follow in order to walk the map



- `static-meshes` contains objects placed on the map

	- *OBJECT PROPERTIES (!are optional)*
		- **type** specifies the object class to create an instance from
		- **x, y** the world position
		- **!scale** specifies the scaling factor

#TILES PROPS

- **type**
	- `start` defines this tile as the starting position if contained in a map
- **orient**
	- **h**orizontal, **v**ertical, **t**op**l**eft, **t**op**r**ight, **b**ottom**l**eft, **b**ottom**r**ight



###track construction

- read tile props for each tile in the map
- tilesize=224px, track white\<-\>white=144px