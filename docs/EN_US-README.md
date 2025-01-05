# WayAlgorithm

WayAlgorithm is a plugin that uses the A* algorithm to find and display the shortest path from a starting point to an endpoint.

## Language
[Main](https://github.com/LacyCat/WayAlgorithm/tree/master/README.md)
___

## Install
Installing the plugin is simple:
1. Download the plugin compatible with your Minecraft version from [here](https://github.com/LacyCat/WayAlgorithm/releases).
2. Place the downloaded plugin into the "plugins" folder in your server directory.
3. Start the server and confirm that the plugin has loaded successfully.

## Usage
WayAlgorithm provides four commands:

### Set1
`/set1 <x> <y> <z>`  
Defines the starting point for the A* algorithm to calculate the shortest path to the endpoint.

### Set2
`/set2 <x> <y> <z>`  
Defines the endpoint for the A* algorithm to calculate the shortest path from the starting point.

### Calc
`/calc`  
Calculates the shortest path from the starting point to the endpoint using the A* algorithm.

### Special Feature
This algorithm is optimized for Minecraft and supports interaction with certain blocks.

#### Ladder
**Ladders**: While functionality for ladders was attempted, it is currently not implemented. Future updates will address this issue.

#### Door
**Doors**: The door must be an **oak door**, and it is considered "passable" if it is open.  
