package com.vip.mlp.docker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Schedule cluster GPUs for containers on different nodes.
 *
 */
public class Scheduler {
  public static final Logger log = LogManager.getLogger(Scheduler.class);

  // Map from NodeId to Reservation
  private Map<String, Reservation> reservations = null;
  private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private volatile long lastModifyTime = 0L;
  
  public static Scheduler fromJson(String json) {
    Type type = new TypeToken<HashMap<String, Reservation>>(){}.getType();
    Map<String, Reservation> containers = gson.fromJson(json, type);
    return new Scheduler(containers);
  }
  
  public String toJson() {
    return gson.toJson(reservations);
  }
  
  public Scheduler(Map<String, Reservation> reservations) {
    this.reservations = reservations;
  }
  
  public Allocation addContainer(String nodeId, String containerId) {
    Reservation reservation = reservations.get(nodeId);
    if (reservation != null) {
      synchronized (reservation) {
        lastModifyTime = System.currentTimeMillis();
        return reservation.addContainer(containerId);
      }
    }
    return null;
  }
  
  public void removeContainer(String nodeId, String containerId) {
    Reservation reservation = reservations.get(nodeId);
    if (reservation != null) {
      synchronized (reservation) {
        lastModifyTime = System.currentTimeMillis();
        reservation.removeContainer(containerId);
      }
    }
  }
  
  // Return a deep-copy of current reservation. It represents spot-in-time status of gpu reservation.
  public List<Reservation> getReservations() {
    List<Reservation> result = new ArrayList<Reservation>();
    for (Reservation item: reservations.values()) {
      result.add(item.copy());
    }
    return result;
  }
  
  public long getLastModifyTime() { return this.lastModifyTime; }
}

// Single allocation. Allocate container containerId on node with GPU index.
class Allocation {
  public String node = null;
  public String containerId = null;
  public int index = -1;

  public Allocation(String node, String containerId, int slotIndex) {
    this.node = node;
    this.containerId = containerId;
    this.index = slotIndex;
  }
  
  public String toString() {
    return "Allocation(container:" + containerId + ", node:" + node + ", index:" + Integer.toString(index) + ")";
  }
}

// GPU reservation on single Node
class Reservation {
  
  public static final Logger log = LogManager.getLogger(Scheduler.class);
  private String nodeId;
  // The index of this array means slot index of GPU.
  // For example [null, id1, null, id2] means GPU 0 and GPU 2 are not used.
  private String[] containers;
  
  public Reservation(String nodeId, String[] containers) {
    this.nodeId = nodeId;
    this.containers = containers;
  }
  
  // Return null if there is no free GPU slot
  public Allocation addContainer(String containerId) {
    
    if (null == containerId || Arrays.asList(containers).contains(containerId)) {
      log.error("Find duplication on " + nodeId + " for container: " + containerId);
      return null;
    }
    
    for (int i = 0; i < containers.length; i++) {
      if (containers[i] == null){
        containers[i] = containerId;
        return new Allocation(nodeId, containerId, i);
      }
    }
    return null;
  }
  
  public void removeContainer(String containerId) {
    for (int i = 0; i < containers.length; i++) {
      if (null != containerId && containerId.equals(containers[i])) {
        containers[i] = null;
      }
    }
  }
  
  public String getNodeId() { return nodeId; }
  
  public List<String> getContainers() {
    return Arrays.asList(containers);
  }
  
  public Reservation copy() {
    return new Reservation(this.nodeId, Arrays.copyOf(containers, containers.length));
  }
  
  public String toString() {
    return "node: " + nodeId + Arrays.toString(containers);
  }
}
