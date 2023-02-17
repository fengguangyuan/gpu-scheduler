package com.vip.mlp.docker;

import com.spotify.docker.client.DefaultDockerClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.*;

public class Main {
  public static final int DEFAULT_HOUSEKEEPING_INTERVAL = 20;
  public static final String HOST = "host";
  public static final String PORT = "port";
  public static final String PERIOD = "period";
  public static final String FILE = "file";

  private static Logger log = LogManager.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    CommandLine options = parse(args);
    String host = options.getOptionValue(HOST);
    int port = Integer.parseInt(options.getOptionValue(PORT));
    int intervalSeconds = Integer.parseInt(options.getOptionValue(PERIOD));
    log.info("Start resource manager at " + host + ":" + port);

    final String resourceFile = options.getOptionValue(FILE);
    String allocationConfig = FileUtils.readFileToString(new File(resourceFile), Charset.forName("UTF-8"));
    Scheduler scheduler = Scheduler.fromJson(allocationConfig);
    ResourceManager resourceManager = new ResourceManager(host, port, scheduler);
    resourceManager.start();
    
    // start house keeper
    log.info("Start House Keeper with interval " + intervalSeconds + "...");
    final DockerService docker = new DockerService(DefaultDockerClient.fromEnv().build());
    final HouseKeeper housekeeper = new HouseKeeper(docker, scheduler);
    ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
    ses.scheduleAtFixedRate(() -> {
      try {
        housekeeper.houseKeeping();
        housekeeper.flushResourceFile(resourceFile);
      } catch (Exception e) {
        log.error("Failed to do house-keeping...", e);
      }
    }, 3, intervalSeconds, TimeUnit.SECONDS);
  
    // Add clean up
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          housekeeper.flushResourceFile(resourceFile);
        } catch (IOException e) {
          log.error("Failed to flush resource file when program exit...", e);
        }
      }
    });
  }

  private static CommandLine parse(String[] inputArgs) throws ParseException {
    CommandLineParser parser = new BasicParser();
    Option[] ops = {
      new Option(HOST, "host", true, "Host ip to listen on."),
      new Option(PORT, "port", true, "Host port to listen on."),
      new Option(FILE, "file", true, "GPU allocation file to use"),
      new Option(PERIOD, "interval", true, "interval (seconds) for housekeeping."),
    };
  
    Options options = new Options();
    for (Option option: ops) {
      option.setRequired(true);
      options.addOption(option);
    }
    CommandLine commandLine = parser.parse(options, inputArgs);
    return commandLine;
  }
}
