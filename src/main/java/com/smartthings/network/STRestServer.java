package com.smartthings.network;

import com.smartthings.utility.STException;
import com.smartthings.utility.STExceptionType;
import com.smartthings.utility.STLogger;

import java.net.ServerSocket;
import java.net.Socket;

public class STRestServer {
  private static int port = 3333, maxConnections = 0;
  private static Socket server;
  private static ServerSocket listener;

  // Listen for incoming connections and handle them
  public static void run() throws STException {

    log("Starting STRestServer");

    int i = 0;

    try {
      listener = new ServerSocket(port);
//      Socket server;

      while ((i++ < maxConnections) || (maxConnections == 0)) {
        log("STRestServer is listening");

        STRestServerRunnable connection;

        server = listener.accept();
        STRestServerRunnable conn_c = new STRestServerRunnable(server);
        Thread t = new Thread(conn_c);
        t.start();
      }
    }
    catch (Exception e) {
      log("Exception on socket listen: " + e);
      e.printStackTrace();
      throw new STException(e, STExceptionType.ST_REST_SERVER_FAIL);
    }
  }

  //================================================================================
  // Logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, STRestServer.class);
  }

}
