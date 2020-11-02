package com.smartthings.utility;

import com.smartthings.SmartBlockTileEntity;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import java.io.*;
import java.util.HashMap;
import java.util.List;

public class STFileAccess {

  //================================================================================
  // Read
  //================================================================================


  public static String getSmartThingsBaseURL() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    return getFromFile(".baseURL");
  }

  public static String getToken() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    return getFromFile(".token");
  }

  public static String getEndpoint() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    return getFromFile(".endpoint");
  }

  public static String getClientId() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }


    String clientId = getFromFile(".clientId");

    if (clientId == null || clientId.length() == 0) {
      clientId = "5633bb99-2fc3-401d-8235-c7bceb5795fa";
//      writeToFile(clientId, ".clientId");
    }

    return clientId;
  }

  public static String getClientSecret() {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    String clientSecret = getFromFile(".clientSecret");

    if (clientSecret == null || clientSecret.length() == 0) {
      clientSecret = "28ea96b3-a767-423e-a38d-1ecf1c9a51d3";
//      writeToFile(clientSecret, ".clientSecret");
    }

    return clientSecret;
  }

  private static String getFromFile(String extension) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    String content = null;
    try {
      EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
      String username = thePlayer.username;
      log("getting content for user: " + username);

      String fileName = "SmartThings-" + username + extension;
      File file = new File(fileName);

      if (!file.exists()) {
        throw new Exception("Unable to get content for user " + username);
      }

      FileReader fr = new FileReader(file.getAbsoluteFile());
      BufferedReader bw = new BufferedReader(fr);
      content = bw.readLine();
      bw.close();

    }
    catch (Exception e) {
      e.printStackTrace();
      content = null;
    }

    return content;
  }

  //================================================================================
  // Write
  //================================================================================


  public static void writeEndpointToFile(String endpoint) {
    String fileExtension = ".endpoint";
    writeToFile(endpoint, fileExtension);
  }

  public static String writeTokenToFile(String token) {
    String fileExtension = ".token";
    return writeToFile(token, fileExtension);
  }

  private static String writeToFile(String fileContents, String fileExtension) {

    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return null;
    }

    String success = null;

    try {

      EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
      String username = thePlayer.username;
      log("writing to file for user: " + username);

      String fileName = "SmartThings-" + username + fileExtension;
      File file = new File(fileName);

      if (file.exists() && !file.delete()) { // delete it if it exists
        throw new IOException("Unable to delete " + fileName);
      }

      if (!file.createNewFile()) { // create a new file
        throw new IOException("Unable to create " + fileName);
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
      BufferedWriter bw = new BufferedWriter(fw);

      bw.write(fileContents);
      bw.close();

      success = fileContents;
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return success;
  }

  public static void deleteToken() {
    log("DELETING TOKEN");
    String fileExtension = ".token";
    deleteFile(fileExtension);
  }

  public static void deleteEndpoint() {
    log("DELETING ENDPOINT");
    String fileExtension = ".endpoint";
    deleteFile(fileExtension);
  }

  private static void deleteFile(String fileExtension) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {
      return;
    }

    try {
      EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
      String username = thePlayer.username;
      log("deleting file for user: " + username);

      String fileName = "SmartThings-" + username + fileExtension;
      File file = new File(fileName);
      if (file.exists()) {
        file.delete();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      log(e.getLocalizedMessage());
    }
    finally {
      cleanUpSmartBlockTileEntities();
    }
  }

  //================================================================================
  // TileEntity
  //================================================================================

  private static void cleanUpSmartBlockTileEntities() {
    try {
      List loadedTileEntityList = Minecraft.getMinecraft().theWorld.loadedTileEntityList;
      for (Object te : loadedTileEntityList) {
        if (te != null && te.getClass() == SmartBlockTileEntity.class) {

          SmartBlockTileEntity ste = (SmartBlockTileEntity) te;
          ste.updateAuth(null, null);

        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      log("unable to update tile loaded entities");
    }
  }

  public static void receivedSmartThingsEvent(HashMap<String, String> params) {
    log("parse params here, write them to disk, and figure out how to collect them in SmartBlockTileEntity" + params);
  }

  //================================================================================
  // Logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, STFileAccess.class);
  }


}
