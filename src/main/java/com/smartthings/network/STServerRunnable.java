package com.smartthings.network;

import java.io.*;
import java.net.Socket;

public class STServerRunnable implements Runnable {
  private Socket server;
  private String line, input, code;

  STServerRunnable(Socket server) {
    this.server = server;
  }

  public void run() {

    input = "";

    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
      PrintStream out = new PrintStream(server.getOutputStream());

      do {
        line = in.readLine();
        input = input + line;

        if (hasCode(line) || input.endsWith("\n\n") || input.endsWith("\r\n")) {
          line = null;
        }
      } while (line != null);

      // Now write to the client
      out.println("Now go play SmartCraft :)"); // TODO: error handling messages

      if (code != null && code.length() > 0) {
        STRest.getToken(code);
      }

      in.close();
      out.close();
      server.close();
      STServer.kill();

    }
    catch (Exception e) {
      if (server != null && !server.isClosed()) {
        try {
          server.close();
        }
        catch (IOException ioe) {
          e.printStackTrace(System.err);
        }
      }
      e.printStackTrace();
    }
  }

  private Boolean hasCode(String lineString) {
    String[] lineParts = lineString.split("\\s+");

    for (String linePart : lineParts) {
      if (linePart.startsWith("/oauth/callback?")) {
        String[] callbackParts = linePart.split("\\?");
        for (String callbackPart : callbackParts) {
          if (callbackPart.startsWith("code=")) {
            code = callbackPart.substring("code=".length());
            return true;
          }
        }
      }
    }

    return false;
  }

}
