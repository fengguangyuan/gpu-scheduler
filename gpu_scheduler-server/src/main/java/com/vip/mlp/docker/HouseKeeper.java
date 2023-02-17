package com.vip.mlp.docker;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * This class is scheduled periodically to free GPU resource that are no longer used.
 */
class HouseKeeper {
  private Logger log = LogManager.getLogger(this.getClass());
  private DockerService docker;
  private Scheduler scheduler;
  private long lastModifiedTime = 0L;
  
  public HouseKeeper(DockerService docker,  Scheduler scheduler) {
    this.scheduler = scheduler;
    this.docker = docker;
  }

  // Return containers that are cleaned.
  public Map<String, String> houseKeeping() throws Exception {
    // Get a immutable map of current reservations
    List<Reservation> reservations = this.scheduler.getReservations();
    Set<String> activeContainers = new HashSet(this.docker.getActiveContainers());
    
    // Key is container Id, value is node Id
    Map<String, String> containerToBeCleaned = new HashMap<String, String>();
    
    // Find containers no longer alive
    for (Reservation reservation: reservations) {
      for (String id: reservation.getContainers()) {
        if (id != null && !activeContainers.contains(id)) {
          containerToBeCleaned.put(id, reservation.getNodeId());
        }
      }
    }
    
    // Clean inactive containers from the scheduler reservation map
    for (Map.Entry<String, String> containerAndNodeId : containerToBeCleaned.entrySet()) {
      String containerId = containerAndNodeId.getKey();
      String nodeId = containerAndNodeId.getValue();
      this.scheduler.removeContainer(nodeId, containerId);
    }
  
    if (containerToBeCleaned.size() > 0) {
      log.info("HouseKeeper finished. Reclaimed " + containerToBeCleaned.size() + " slots. containers: " +
        containerToBeCleaned.toString());
    }
    return containerToBeCleaned;
  }
  
  public void flushResourceFile(String resource) throws IOException {
    long newChanged = scheduler.getLastModifyTime();
    String json = scheduler.toJson();
    if (newChanged > lastModifiedTime) {
      lastModifiedTime = newChanged;
      log.info("Flush resource file with content: \n" + json);
      FileUtils.write(new File(resource), json, Charset.forName("UTF-8"));
    }
  }
}
