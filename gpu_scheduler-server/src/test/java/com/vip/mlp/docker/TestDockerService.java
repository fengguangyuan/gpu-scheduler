package com.vip.mlp.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.swarm.ContainerStatus;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;
import org.junit.Test;

import java.util.*;

import static org.mockito.Mockito.*;

public class TestDockerService {

  @Test
  public void testDockerService() throws Exception {
    DockerClient docker = mock(DockerClient.class);
    DockerService service = new DockerService(docker);
    
    Task task = mock(Task.class);
    TaskStatus status = mock(TaskStatus.class);
    when(task.status()).thenReturn(status);
    ContainerStatus containerStatus = mock(ContainerStatus.class);
    when(status.containerStatus()).thenReturn(containerStatus);
    when(containerStatus.containerId()).thenReturn("container1", "container2", "container3");
    
    Task[] tasks = {task, task};
    when(docker.listTasks()).thenReturn(
      new ArrayList(Arrays.asList(tasks))
    );
    when(task.desiredState()).thenReturn("RUNNING");
    List<String> active = service.getActiveContainers();
    assert(active.size() == 2 && active.get(0).equals("container1") && active.get(1).equals("container2"));
    when(task.desiredState()).thenReturn("FAILED");
    List<String> active2 = service.getActiveContainers();
    assert(active2.size() == 0);
  
    // Test Long Ids
    when(containerStatus.containerId()).thenReturn("abcdef0123456789", "bbcdef0123456789", "cbcdef0123456789");
    when(task.desiredState()).thenReturn("ACCEPTED");
    List<String> active3 = service.getActiveContainers();
    assert(active3.size() == 2 && active3.get(0).length() == DockerService.HOSTNAME_LENGTH);
  }
}
