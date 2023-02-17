package com.vip.mlp.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.swarm.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Stub class to access docker swarm api
public class DockerService {
  private DockerClient client;
  private Logger logger = LogManager.getLogger(DockerService.class.getName());
  
  // TODO: Remove this in future
  // Currently, the client side in runc only sends the hostname which is 12 bytes prefix
  // of actual container Id.
  public static int HOSTNAME_LENGTH = 12;
  
  
  public DockerService(DockerClient client) {
    this.client = client;
  }
  
  public List<String> getActiveContainers() throws Exception {
      List<String> result = new ArrayList<String>();
      
      // TODO: Why using desiredState??
      List validStates = new ArrayList<>(Arrays.asList("running", "accepted"));
      List<Task> tasks = this.client.listTasks();
      for (Task task : tasks) {
        logger.debug(task);
        if (validStates.contains(task.desiredState().toLowerCase())) {
          String taskId = toShortId(task.status().containerStatus().containerId());
          result.add(taskId);
        }
      }
      return result;
  }
  
  // TODO: Remove this in future
  // Currently, the client side in runc only sends the hostname which is 12 bytes prefix
  // of actual container Id.
  private String toShortId(String longContainerId) {
    if (null != longContainerId && longContainerId.length() > HOSTNAME_LENGTH) {
      return longContainerId.substring(0, HOSTNAME_LENGTH);
    }
    return longContainerId;
  }
}
