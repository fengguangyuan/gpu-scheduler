package com.vip.mlp.docker;

import fi.iki.elonen.NanoHTTPD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class ResourceManager extends NanoHTTPD {
  Logger log = LogManager.getLogger(this.getClass());
  public static String NODE = "node";
  
  // Actucally it is container Id
  public static String CONTAINER = "task";
  public boolean daemon = false;
  private Scheduler scheduler;

  public ResourceManager(String hostname, int port,  Scheduler scheduler) {
    super(hostname, port);
    this.scheduler = scheduler;
  }

  public void start() throws IOException {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, this.daemon);
  }

  @Override
  public Response serve(IHTTPSession session) {
    Response response = null;
    Method method = session.getMethod();
    if (Method.GET.equals(method)) {
      Map<String, String> params = session.getParms();
      if (params.containsKey(NODE) && params.containsKey(CONTAINER)) {
        String node = params.get(NODE);
        String containerId = params.get(CONTAINER);
        Allocation allocation = this.scheduler.addContainer(node, containerId);
        
        if (allocation != null) {
          String msg = "GPU-info = " + allocation.node + "," + allocation.containerId + "," + allocation.index + "\n";
          log.info("Schedule container: " + allocation);
          response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);
        } else {
          // return
          String noFreeResource = "GPU-info = " + node + "," + containerId + "," + -1 + "\n";
          log.info("No free resource on node: " + node + " for container: " + containerId);
          response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, noFreeResource);
        }
      }
    }
    
    if (response == null) {
      log.error("Unrecognized http request message with " + session.getQueryParameterString());
      response = newFixedLengthResponse("BAD REQUEST");
    }
    response.addHeader("Connection", "close");
    return response;
  }
}
