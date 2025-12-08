# Object Tracking Using (Distributed) Particle Filters)


## Experiment 1: Centralized Particle Filter

In this scenario, 
 a node moves through a 2D space following a trajectory defined by a function `f(x)` 
(e.g., `f(x) = x + sin(x)`). 
A second node, acting as an observer, 
 attempts to estimate the moving node’s position over time using a centralized particle filter. 
To run the simulation, simply execute:

```bash
./gradlew runTrackMovementCentralizedGraphic
```

## Generate charts 
```bash
python3 plotter/plot_movement.py
```