package com.smartthings.network;

import com.smartthings.utility.STException;
import com.smartthings.utility.STExceptionType;
import com.smartthings.utility.STLogger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class STServer {
  private static int port = 4444, maxConnections = 0;
  private static Socket server;
  private static ServerSocket listener;

  // Listen for incoming connections and handle them
  public static void run() throws STException {

    log("Starting STServer");

    int i = 0;

    new Thread(new Runnable() {
      public void run() {
        scheduleKill();
      }
    }).start();

    try {
      listener = new ServerSocket(port);
//      Socket server;

      while ((i++ < maxConnections) || (maxConnections == 0)) {
        log("STServer is listening");

        STServerRunnable connection;

        server = listener.accept();
        STServerRunnable conn_c = new STServerRunnable(server);
        Thread t = new Thread(conn_c);
        t.start();
      }
    }
    catch (Exception e) {
      log("Exception on socket listen: " + e);
      e.printStackTrace();
      throw new STException(e, STExceptionType.ST_SERVER_FAIL);
    }
  }

  public static void kill() {

    try {
      log("Closing server");
      server.close();
      log("Finished Closing server");
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    try {
      log("Closing listener");
      listener.close();
      log("Finished Closing listener");
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }


  private static void scheduleKill() {

    Timer timer = new Timer();

    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        log("executing scheduled server kill");
        System.out.println("executing scheduled server kill");
        STServer.kill();
      }
    };

    Date now = new Date();
    long t = now.getTime();
    Date future = new Date(t + 150000); // about 2.5 minutes

    log("scheduling task for " + future.toString());
    timer.schedule(task, future);

  }

  //================================================================================
  // Logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, STServer.class);
  }

}
