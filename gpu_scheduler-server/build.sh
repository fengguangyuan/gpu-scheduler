#!/bin/bash

mvn clean & mvn package

# Copy the release package to publish directory
mkdir -p  ~/dist/gpu_scheduler/
mkdir -p  ~/dist/mlp_releases/gpu_scheduler/
scp target/gpu-service*dependencies*.jar ~/dist/gpu_scheduler/
scp target/gpu-service*dependencies*.jar ~/dist/mlp_releases/gpu_scheduler/
