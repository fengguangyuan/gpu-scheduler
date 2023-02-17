package com.vip.mlp.docker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TestHouseKeeper {

  @Test
  public void testHouseKeeping() throws Exception {
    DockerService docker = mock(DockerService.class);
    Scheduler scheduler = mock(Scheduler.class);
    HouseKeeper houseKeeper = new HouseKeeper(docker, scheduler);
    
    when(docker.getActiveContainers()).thenReturn(
      new ArrayList(Arrays.asList("container1", "container2", "container3")));
    
    String[] node1Containers = {"container1"};
    String[] node2Containers = {"container3", "container5"};
  
    when(scheduler.getReservations()).thenReturn(
      new ArrayList(Arrays.asList(
        new Reservation("node1", node1Containers),
        new Reservation("node2", node2Containers))));
  
    Map<String, String> cleaned = houseKeeper.houseKeeping();
    assert(cleaned.size() == 1 && cleaned.get("container5").equals("node2"));
    verify(scheduler).removeContainer("node2", "container5");
  }
}
