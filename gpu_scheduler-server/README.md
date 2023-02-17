### This project provides the ability to schuduling GPU resources among Docker Services,
### based on nvidia-container-runtime project in Standard Container Level.

### nvidia container runtime hook
You could get a simple GPU request client based on `https://github.com/NVIDIA/nvidia-container-runtime` from here:
https://github.com/fengguangyuan/nvidia-container-runtime.git

## Two keywords

### NVIDIA_VISIBLE_DEVICES
If `none` was assigned to this environment, when starting a service or task, the container instance owned by that
service or task will not load GPU service.
If `1` or `1,2` liked values or no values was assigned to this environment, the container instance owned by that
service or task will send a request to GPU_SERVICE_SERVER to get an available GPU index on one node.

### GPU_SERVICE_SERVER
This word figures out a http server address, and a sample value would looks like `127.0.0.1:12345`.

## How to run the Server and Client
### run Server
```shell
git clone 

mvn clean & mvn package

cd target

cp ../start-service.sh ./

./start-service.sh
```
### run Client
```shell
git clone https://github.com/fengguangyuan/nvidia-container-runtime.git
cd nvidia-container-runtime
git checkout gpu-service
## then compile the project as the official said

./fetch-hook.sh

sudo mv /usr/bin/nvidia-container-runtime-hook /usr/bin/nvidia-container-runtime-hook-bak
sudo cp /usr/bin/nvidia-container-runtime-hook /usr/bin
```

#### Start containers with or without GPUs
*** Attention: The resource service won't record the containers started with
GPUs internally. This will lead to a service task will share a same GPU with
the containers.***

```shell
## In default, the `test` container will send a request to server, to load GPU
## with the GPU device sequence number of the current host.
## When running a GPU container, `-e GPU_SERVICE_SERVER=10.212.51.53:21497` must
## be set to let docker client know the GPU Service address.
docker run -itd --name test -e GPU_SERVICE_SERVER=10.212.51.53:21497 127.0.0.1:35000/mlp:gpu-P40 /bin/bash

## If an explicit argument of visible GPUs was specified, in the current version,
## still a random index will be returned from server, which means it works with
## the above command.
docker run -itd -e NVIDIA_VISIBLE_DEVICES=all -e GPU_SERVICE_SERVER=10.212.51.53:21497 --name test 127.0.0.1:35000/mlp:gpu-P40 /bin/bash
docker run -itd -e NVIDIA_VISIBLE_DEVICES=0,1 -e GPU_SERVICE_SERVER=10.212.51.53:21497 --name test 127.0.0.1:35000/mlp:gpu-P40 /bin/bash

## With no GPUs
docker run -itd -e NVIDIA_VISIBLE_DEVICES=none --name test 127.0.0.1:35000/mlp:gpu-P40 /bin/bash
```

#### Start services with or without GPUs
```shell
## With GPUs
docker service create --name test -e GPU_SERVICE_SERVER=10.212.51.53:21497 127.0.0.1:35000/mlp:gpu-P40 /bin/bash -c "sleep 20s"
## Without GPUs
docker service create --name test -e NVIDIA_VISIBLE_DEVICES=none 127.0.0.1:35000/mlp:gpu-P40 /bin/bash -c "sleep 20s"
```

## Log Dir
In default, when the server started, it's all logs will stored in /tmp/docker/logs/.
Users can modify log4j.xml to reset the log path.
