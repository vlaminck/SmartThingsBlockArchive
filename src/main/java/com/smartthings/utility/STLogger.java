package com.smartthings.utility;

import cpw.mods.fml.common.FMLCommonHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class STLogger {

  //================================================================================
  // Logging
  //================================================================================

  public static void log(Object outputString) {
    System.out.println(sideLogMessage() + outputString);
  }

  public static void fLog(Object outputString) {
    logToFile(outputString, "");
  }

  public static void fLog(Object outputString, Class clazz) {
    logToFile(outputString, clazz.getSimpleName());
    logToFile(outputString, "");
  }

  public static void fLog(Object outputString, String fileName) {

    logToFile(outputString, fileName);

    if (fileName != null && fileName.length() != 0 && !fileName.equals("SmartThings_log.txt")) {
      // log to the SmartThings_log.txt file as well as the specified file
      logToFile(outputString, "");
    }

  }

  public static void logToFile(Object outputString, String fileName) {
    try {

      fileName = processFileName(fileName);
      File file = new File(processFileName(fileName));
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
      BufferedWriter bw = new BufferedWriter(fw);

      outputString = sideLogMessage() + outputString;
      System.out.println("[" + fileName + "]" + outputString);
      bw.write(outputString + "\n");
      bw.close();

    }
    catch (IOException e) {

      System.err.println("Unable to write to file");
      e.printStackTrace();

    }

  }

  private static String processFileName(String fileName) {

    if (fileName == null || fileName.length() == 0) {
      return "SmartThings_log.txt";
    }

    if (!fileName.startsWith("SmartThings_")) {
      fileName = "SmartThings_" + fileName;
    }

    if (fileName.endsWith("_log.txt")) {
      return fileName;
    }

    if (fileName.endsWith(".txt")) {
      // _log.txt suffix would have been returned by now. remove .txt and add _log.txt
      fileName = fileName.substring(0, fileName.indexOf(".txt"));
      return fileName + "_log.txt";
    }

    // has SmartThings_ prefix, but does not have the proper suffix. add it.
    return fileName + "_log.txt";
  }

  public static String sideLogMessage() {

    cpw.mods.fml.relauncher.Side side = FMLCommonHandler.instance().getEffectiveSide();

    if (side == cpw.mods.fml.relauncher.Side.SERVER) {
      return "[SERVER_SIDE] ";
    } else if (side == cpw.mods.fml.relauncher.Side.CLIENT) {
      return "[CLIENT_SIDE] ";
    }

    return "[UNKNOWN_SIDE] ";
  }


}
