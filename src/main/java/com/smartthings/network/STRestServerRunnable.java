package com.smartthings.network;

import com.smartthings.SmartBlockTileEntity;
import com.smartthings.utility.STFileAccess;
import com.smartthings.utility.STLogger;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;

public class STRestServerRunnable implements Runnable {
  private Socket server;
  private String line, input;

  STRestServerRunnable(Socket server) {
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

        System.out.println("STRestServerRunnable received line: |" + line + "|");

        try {
          if (processLine(line)) {
            // found it. Go ahead and exit
            line = null;
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("STRestServerRunnable failed to process line");
        }


        if (line != null && line.equals("")) {
          // end of headers; exit
          line = null;
        }

      } while (line != null);

      // Now write to the client
      out.println("done. \n\n");

      System.out.println("STRestServerRunnable received input: " + input);

      in.close();
      out.close();

    }
    catch (Exception e) {
      if (server != null && !server.isClosed()) {
        try {
          server.close();
        }
        catch (IOException ioe) {
          e.printStackTrace(System.err);
          System.out.println("STRestServerRunnable FAIL");
        }
      }
      e.printStackTrace();
    }
  }

  private Boolean processLine(String lineString) {
    String[] lineParts = lineString.split("\\s+");

    for (String linePart : lineParts) {
      if (linePart.startsWith("/block?")) {
        HashMap<String, String> parsedParams = new HashMap<String, String>();

        linePart = linePart.substring("/block?".length());
        String[] params = linePart.split("\u0026");

        for (String param : params) {
          String[] paramParts = param.split("=");
          String paramKey = paramParts[0];
          String paramValue = paramParts[1];

          try {
            paramKey = URLDecoder.decode(paramKey, "UTF-8");
            paramValue = URLDecoder.decode(paramValue, "UTF-8");
          }
          catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }

          parsedParams.put(paramKey, paramValue);
        }

        updateTile(parsedParams);
        return true;

      } else if (linePart.startsWith("/chat?")) {
        log("startsWith chat");
        HashMap<String, String> parsedParams = new HashMap<String, String>();

        linePart = linePart.substring("/chat?".length());
        String[] params = linePart.split("\u0026");

        for (String param : params) {
          String[] paramParts = param.split("=");
          String paramKey = paramParts[0];
          String paramValue = paramParts[1];

          try {
            paramKey = URLDecoder.decode(paramKey, "UTF-8");
            paramValue = URLDecoder.decode(paramValue, "UTF-8");
          }
          catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }

          parsedParams.put(paramKey, paramValue);
        }

        try {
          sendChat(parsedParams);
        }
        catch (Exception e) {
          log(e.getLocalizedMessage());
          e.printStackTrace();
        }

        return true;
      }
    }

    return false;
  }

  private void updateTile(HashMap<String, String> params) {
    // find tile by params.x, y, and z
    int x = Integer.parseInt(params.get("x"));
    int y = Integer.parseInt(params.get("y"));
    int z = Integer.parseInt(params.get("z"));

    WorldServer[] worlds = DimensionManager.getWorlds();
    SmartBlockTileEntity smartBlockTileEntity = null;

    for (WorldServer world : worlds) {
      try {
        TileEntity blockTileEntity = world.getBlockTileEntity(x, y, z);
        if (blockTileEntity != null && blockTileEntity.getClass() == SmartBlockTileEntity.class) {
          smartBlockTileEntity = (SmartBlockTileEntity) blockTileEntity;
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (smartBlockTileEntity != null) {
      // smartBlock is in memory; update it
      smartBlockTileEntity.receivedSmartThingsEvent(params);
    } else {
      // smartBlock is not in memory; save event for when it loads
      STFileAccess.receivedSmartThingsEvent(params);
    }

  }

  private void sendChat(HashMap<String, String> params) {

    String message = params.get("message");

    try {
      message = URLDecoder.decode(message, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    String username = params.get("username");
    if (username != null && username.length() == 0) {
      username = null;
    }

    List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    for (EntityPlayerMP receiver : players) {
      if (username == null || username.equals(receiver.username)) {
        log("sending: \"" + message + "\" to user: \"" + receiver.username + "\"");
        receiver.sendChatToPlayer(ChatMessageComponent.createFromText(message));
      }
    }

  }

  private static void log(Object message) {
    STLogger.fLog(message, STRestServerRunnable.class);
  }

}
