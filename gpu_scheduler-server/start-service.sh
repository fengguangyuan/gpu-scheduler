#!/bin/bash

jar=gpu-service-1.0-SNAPSHOT-jar-with-dependencies.jar
classname=com.vip.mlp.docker.Main

echo "============================================================"
echo "Help usages to start service:"
echo "-h --help      Print this usage information"
echo "-v --verbose   Print out VERBOSE information"
echo "-host --host   Host ip listening on"
echo "-port --port   Host port listening on"
echo "-file          File to save the GPU info of a cluster (absolute path)"
echo "-period        Period (Seconds) to executing docker service"
echo "-config         Configurations of the program (under development now)"
echo "============================================================"

## A to start the service
java -cp $jar $classname -host 10.212.51.53 -port 21497 -interval 10 --file `pwd`/resource.json
