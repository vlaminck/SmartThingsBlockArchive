package com.smartthings;

import com.smartthings.utility.STLogger;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashMap;

public class SmartBlockPacketHandler implements IPacketHandler {

  //================================================================================
  // IPacketHandler Overrides
  //================================================================================

  @Override
  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity) {
    log("onPacketData");
    log("packet.channel: " + packet.channel);

    if (packet.channel.equals("SmartThings")) {
      handleSmartThingsPacket(packet);
    }

    if (packet.channel.equals("SmartThingsA")) {
      handleSmartThingsAuthPacket(packet);
    }

  }

  //================================================================================
  // channel processing
  //================================================================================

  private void handleSmartThingsPacket(Packet250CustomPayload packet) {
    String packetString = processSmartThingsPacket(packet);
    log("packetString: " + packetString);

    SmartBlockTileEntity.updateTilesWithPacket(packetString);
  }

  private void handleSmartThingsAuthPacket(Packet250CustomPayload packet) {
    String packetString = processSmartThingsPacket(packet);
    log("packetString: " + packetString);
    WorldServer[] worlds = DimensionManager.getWorlds();
    for (WorldServer world : worlds) {

      int x = smartBlockCoordinate(packetString, "x");
      int y = smartBlockCoordinate(packetString, "y");
      int z = smartBlockCoordinate(packetString, "z");

      log("(" + x + "," + y + "," + z + ")");

      try {
        TileEntity blockTileEntity = world.getBlockTileEntity(x, y, z);
        if (blockTileEntity != null && blockTileEntity.getClass() == SmartBlockTileEntity.class) {
          SmartBlockTileEntity tile = (SmartBlockTileEntity) blockTileEntity;
          tile.log();

          tile.updateAuth(smartBlockToken(packetString), smartBlockEndpoint(packetString));

        } else {
          log("unable to find tileEntity");
        }
      }
      catch (Exception e) {
        log("unable to parse tileEntity");
      }

    }
  }

  private String processSmartThingsPacket(Packet250CustomPayload packet) {
    log("handling packet");

    String str = "";

    try {
      byte[] data1 = packet.data;
      log("data1 created");
      ByteArrayInputStream in = new ByteArrayInputStream(data1);
      log("in created");
      DataInputStream inputStream = new DataInputStream(in);

      log("created inputStream");

      // Read data
      int length = inputStream.readInt();
      byte[] data = new byte[length];
      inputStream.readFully(data);
      str = new String(data, "UTF-8");
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    log("Read string: " + str);

    return str;
  }

  public static int smartBlockCoordinate(String packetString, String axis) {

    try {
      String[] packetParts = packetString.split("\\|");

      if (axis.equals("x")) {
        return Integer.parseInt(packetParts[0]);
      } else if (axis.equals("y")) {
        return Integer.parseInt(packetParts[1]);
      } else if (axis.equals("z")) {
        return Integer.parseInt(packetParts[2]);
      }
    }
    catch (Exception e) {
      log("Failed to get coordinate from: " + packetString);

      e.printStackTrace();
    }

    return 0;
  }

  private static int parsePacketIntAtIndex(String packetString, int i) {

    int indexedPacketInt = -1;
    try {
      String[] packetParts = packetString.split("\\|");
      indexedPacketInt = Integer.parseInt(packetParts[i]);
    }
    catch (Exception e) {
      log("Failed to get indexedPacketInt at index: " + i + " from: " + packetString);
      e.printStackTrace();
      indexedPacketInt = -1;
    }

    return indexedPacketInt;
  }

  private static String parsePacketStringAtIndex(String packetString, int i) {
    String indexedPacketString = null;
    try {
      String[] packetParts = packetString.split("\\|");
      indexedPacketString = packetParts[i];
      if (indexedPacketString.equals("null") || indexedPacketString.equals("")) {
        indexedPacketString = null;
      }
    }
    catch (Exception e) {
      log("Failed to get indexedPacketString at index: " + i + " from: " + packetString);
      e.printStackTrace();
      indexedPacketString = null;
    }

    return indexedPacketString;
  }

  public static String smartBlockToken(String packetString) {
    return parsePacketStringAtIndex(packetString, 3);
  }

  public static String smartBlockEndpoint(String packetString) {
    return parsePacketStringAtIndex(packetString, 4);
  }

  public static int smartBlockSignalStrength(String packetString) {
    return parsePacketIntAtIndex(packetString, 3);
  }

  public static int smartBlockNeighborChanged(String packetString) {
    return parsePacketIntAtIndex(packetString, 4);
  }

  public static String smartBlockWorldSeed(String packetString) {
    return parsePacketStringAtIndex(packetString, 5);
  }

  public static String smartBlockDimensionName(String packetString) {
    return parsePacketStringAtIndex(packetString, 6);
  }

  public static String smartBlockPlacedByUsername(String packetString) {
    return parsePacketStringAtIndex(packetString, 7);
  }

  //================================================================================
  // Logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, SmartBlockPacketHandler.class);
  }

}
