package com.vip.mlp.docker;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestScheduler {
  
  @Test
  public void testJson() throws Exception {
    Map<String, Reservation> allocations = new HashMap<String, Reservation>();
  
    String[] node1Containers = {"container1"};
    String[] node2Containers = {"container3", "container5"};
    allocations.put("node1", new Reservation("node1", node1Containers));
    allocations.put("node2", new Reservation("node2", node2Containers));
    
    Scheduler scheduler = new Scheduler(allocations);
    String json = scheduler.toJson();
    System.out.println(json);
    
    Scheduler schedule2 = Scheduler.fromJson(json);
    assert (schedule2.toJson().equals(json));
    List<Reservation> reservations = schedule2.getReservations();
    for (Reservation reservation : reservations) {
      assert (allocations.get(reservation.getNodeId()).toString().equals(reservation.toString()));
    }
  }
  
  @Test
  public void testAddRemoveContainer() throws Exception {
    Map<String, Reservation> allocations = new HashMap<String, Reservation>();
  
    String[] node1Containers = {"c1", null, "c2", null};
    String[] node2Containers = {"c3", "c4", "c5", "c6"};
    allocations.put("node1", new Reservation("node1", node1Containers));
    allocations.put("node2", new Reservation("node2", node2Containers));
    Scheduler scheduler = new Scheduler(allocations);
  
    // Full, returns null
    assert (null == scheduler.addContainer("node2", "c7"));
    int indexOfC6 = 3;
    scheduler.removeContainer("node2", "c6");
    Allocation allocation = scheduler.addContainer("node2", "c7");
    assert (null != allocation);
    assert (allocation.node.equals("node2") && allocation.containerId.equals("c7") && allocation.index == indexOfC6);
  
    // Node id not exists, return null
    assert (null == scheduler.addContainer("node99", "c7"));
  }
}