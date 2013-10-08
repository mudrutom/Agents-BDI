# Agents-BDI

Simulation of simple Multi-Agent system where the agents are using Belief-Desire-Intention architecture.

It's the first assignment for the course Multiagent Systems (AE4M36MAS) at FEE CTU.

## Problem description
* three blue agents have to visit the four locations {[2,2], [27,2], [27,27], [2,27]} indicated in red in the picture
* the environment is a 30×30 grid with a blocked central area and a system of fences that can be opened using the corresponding switch
* the switch is activated if an agent stands next to it
* the agents have a limited visibility range
* the solution of the assignment will be tested on a map with different configuration of fences (so it is not possible to hard code fences positions)

![The environment](/environment.png "The environment")

## Environment description
* Grid 30×30 (top-left corner has coordinates [0, 0])
* Obstacles in the form of straight fences
 * A fence is opened if some agent stands at the switch, otherwise it is closed
 * A fence always connects the inner and outer square
 * A switch is always located at the outer border of the grid
 * There is at least 3 fields space between the fences
* An agent is able to move on 8 neighbouring fields (north, south, east, west, northeast, northwest,…)
