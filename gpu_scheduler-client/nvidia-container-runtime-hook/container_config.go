package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path"
	"strconv"
	"strings"
)

const (
	//	envSwarmGPU      = "DOCKER_RESOURCE_GPU"
	envNVRequirePrefix      = "NVIDIA_REQUIRE_"
	envLegacyCUDAVersion    = "CUDA_VERSION"
	envNVRequireCUDA        = envNVRequirePrefix + "CUDA"
	envNVGPU                = "NVIDIA_VISIBLE_DEVICES"
	envNVDriverCapabilities = "NVIDIA_DRIVER_CAPABILITIES"
	allCapabilities         = "compute,compat32,graphics,utility,video"
	envNVDisableRequire     = "NVIDIA_DISABLE_REQUIRE"
        envGPUServer            = "GPU_SERVICE_SERVER"
)

type nvidiaConfig struct {
	Devices        string
	Capabilities   string
	Requirements   []string
	DisableRequire bool
}

type containerConfig struct {
	Pid    int
	Rootfs string
	Env    map[string]string
	Nvidia *nvidiaConfig
        Hostname string
}

// github.com/opencontainers/runtime-spec/blob/v1.0.0/specs-go/config.go#L94-L100
type Root struct {
	Path string `json:"path"`
}

// github.com/opencontainers/runtime-spec/blob/v1.0.0/specs-go/config.go#L30-L57
type Process struct {
	Env []string `json:"env,omitempty"`
}

// We use pointers to structs, similarly to the latest version of runtime-spec:
// https://github.com/opencontainers/runtime-spec/blob/v1.0.0/specs-go/config.go#L5-L28
type Spec struct {
	Process *Process `json:"process,omitempty"`
	Root    *Root    `json:"root,omitempty"`
        Hostname string  `json:"hostname,omitempty"`
}

type HookState struct {
	Pid int `json:"pid,omitempty"`
	// After 17.06, runc is using the runtime spec:
	// github.com/docker/runc/blob/17.06/libcontainer/configs/config.go#L262-L263
	// github.com/opencontainers/runtime-spec/blob/v1.0.0/specs-go/state.go#L3-L17
	Bundle string `json:"bundle"`
	// Before 17.06, runc used a custom struct that didn't conform to the spec:
	// github.com/docker/runc/blob/17.03.x/libcontainer/configs/config.go#L245-L252
	BundlePath string `json:"bundlePath"`
}

func parseCudaVersion(cudaVersion string) (vmaj, vmin, vpatch uint32) {
	if _, err := fmt.Sscanf(cudaVersion, "%d.%d.%d\n", &vmaj, &vmin, &vpatch); err != nil {
		vpatch = 0
		if _, err := fmt.Sscanf(cudaVersion, "%d.%d\n", &vmaj, &vmin); err != nil {
			vmin = 0
			if _, err := fmt.Sscanf(cudaVersion, "%d\n", &vmaj); err != nil {
				log.Panicln("invalid CUDA version:", cudaVersion)
			}
		}
	}

	return
}

func getEnvMap(e []string) (m map[string]string) {
	m = make(map[string]string)
	for _, s := range e {
		p := strings.SplitN(s, "=", 2)
		if len(p) != 2 {
			log.Panicln("environment error")
		}
		m[p[0]] = p[1]
	}
	return
}

func loadSpec(path string) (spec *Spec) {
	f, err := os.Open(path)
	if err != nil {
		log.Panicln("could not open OCI spec:", err)
	}
	defer f.Close()

	if err = json.NewDecoder(f).Decode(&spec); err != nil {
		log.Panicln("could not decode OCI spec:", err)
	}
	if spec.Process == nil {
		log.Panicln("Process is empty in OCI spec")
	}
	if spec.Root == nil {
		log.Panicln("Root is empty in OCI spec")
	}
	if spec.Hostname == "" {
		log.Panicln("Container hostname is empty in OCI spec")
	}
        return
}

func getRequirements(env map[string]string) []string {
	// All variables with the "NVIDIA_REQUIRE_" prefix are passed to nvidia-container-cli
	var requirements []string
	for name, value := range env {
		if strings.HasPrefix(name, envNVRequirePrefix) {
			requirements = append(requirements, value)
		}
	}
	return requirements
}

// Mimic the new CUDA images if no capabilities or devices are specified.
func getNvidiaConfigLegacy(env map[string]string) *nvidiaConfig {
	devices := env[envNVGPU]
	if len(devices) == 0 {
		devices = "all"
	}
	if devices == "none" {
		devices = ""
	}

	capabilities := env[envNVDriverCapabilities]
	if len(capabilities) == 0 || capabilities == "all" {
		capabilities = allCapabilities
	}

	requirements := getRequirements(env)

	vmaj, vmin, _ := parseCudaVersion(env[envLegacyCUDAVersion])
	cudaRequire := fmt.Sprintf("cuda>=%d.%d", vmaj, vmin)
	requirements = append(requirements, cudaRequire)

	// Don't fail on invalid values.
	disableRequire, _ := strconv.ParseBool(env[envNVDisableRequire])

	return &nvidiaConfig{
		Devices:        devices,
		Capabilities:   capabilities,
		Requirements:   requirements,
		DisableRequire: disableRequire,
	}
}

func getNvidiaConfig(env map[string]string) *nvidiaConfig {
	legacyCudaVersion := env[envLegacyCUDAVersion]
	cudaRequire := env[envNVRequireCUDA]
	if len(legacyCudaVersion) > 0 && len(cudaRequire) == 0 {
		// Legacy CUDA image detected.
		return getNvidiaConfigLegacy(env)
	}

	devices, ok := env[envNVGPU]
	if !ok {
		// envNVGPU is unset: not a GPU container.
		return nil
	}
	if devices == "none" {
		devices = ""
	}

	capabilities := env[envNVDriverCapabilities]
	if capabilities == "all" {
		capabilities = allCapabilities
	}

	requirements := getRequirements(env)

	// Don't fail on invalid values.
	disableRequire, _ := strconv.ParseBool(env[envNVDisableRequire])

	return &nvidiaConfig{
		Devices:        devices,
		Capabilities:   capabilities,
		Requirements:   requirements,
		DisableRequire: disableRequire,
	}
}

func getContainerConfig() (config *containerConfig) {
	var h HookState
	d := json.NewDecoder(os.Stdin)
	if err := d.Decode(&h); err != nil {
		log.Panicln("could not decode container state:", err)
	}

	b := h.Bundle
	if len(b) == 0 {
		b = h.BundlePath
	}

	s := loadSpec(path.Join(b, "config.json"))

	env := getEnvMap(s.Process.Env)
	return &containerConfig{
		Pid:    h.Pid,
		Rootfs: s.Root.Path,
		Env:    env,
		Nvidia: getNvidiaConfig(env),
                Hostname: s.Hostname
	}
}
