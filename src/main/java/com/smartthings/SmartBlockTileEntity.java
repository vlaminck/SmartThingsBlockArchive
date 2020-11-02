package com.smartthings;

import com.smartthings.network.STRest;
import com.smartthings.utility.*;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SmartBlockTileEntity extends TileEntity {

  private static String defaultChannel = "SmartThings";
  private static String authChannel = "SmartThingsA";

  private String token, endpoint;

  public String worldSeed, dimensionName, placedByUsername;
  public int lastNeighborChanged, signalStrength; // signalStrength is the signal strength that SmartThings set it to. It will be cleared when receiving power from any other block sends a signal in.

  private Boolean updatedFromST = false;
  private Boolean shouldUpdateST = false;
  private Boolean signalStrengthUpdated = false;
  private int numberOfTicks = 0;

  //================================================================================
  // Auth Setters
  //================================================================================

  public void updateAuth(String token, String endpoint) {

    Boolean tokenIsChanging = !attributeEquality(this.token, token);
    if (tokenIsChanging) {
      setToken(token);
    }

    Boolean endpointIsChanging = !attributeEquality(this.endpoint, endpoint);
    if (endpointIsChanging) {
      setEndpoint(endpoint);
    }

    sendAuthChangesToServer();
  }

  private void setToken(String token) {
    log("setting token to " + token);
    this.token = token;

    if (this.token == null || this.token.length() == 0) {
      // TODO: mark the block red or something. set metadata?
      STFileAccess.deleteToken();
    }
  }

  private void setEndpoint(String endpoint) {
    log("setting endpoint to " + endpoint);
    this.endpoint = endpoint;

    if (this.endpoint == null || this.endpoint.length() == 0) {
      // TODO: mark the block red or something. set metadata?
      STFileAccess.deleteEndpoint();
    }
  }

  //================================================================================
  // Setters
  //================================================================================

  public void setWorldSeed(String worldSeed) {
    log("setting worldSeed to " + worldSeed);
    this.worldSeed = worldSeed;
  }

  public void setDimensionName(String dimensionName) {
    log("setting dimensionName to " + dimensionName);
    this.dimensionName = dimensionName;
  }

  public void setPlacedByUsername(String placedByUsername) {
    log("setting placedByUsername to " + placedByUsername);
    this.placedByUsername = placedByUsername;
  }

  public void setSignalStrength(int signalStrength) {
    signalStrengthUpdated = true;
    log("setting signalStrength to " + signalStrength);
    this.signalStrength = signalStrength;
  }

  public void setLastNeighborChanged(int lastNeighborChanged) {
    log("setting lastNeighborChanged to " + lastNeighborChanged);
    this.lastNeighborChanged = lastNeighborChanged;
  }

  //================================================================================
  // Public
  //================================================================================

  public void authenticate() {
    if (this.token == null || this.token.length() == 0) {
      STRest.authenticate();
    }
  }

  public void collectEndpoint() {
    if (this.endpoint == null || this.endpoint.length() == 0) {
      try {
        STRest.getEndpoint(this.token);
      }
      catch (STException e) {
        log("collectEndpoint failed");

        updateAuth(null, null);

        e.printStackTrace();
        log(e.getLocalizedMessage());
      }
    }
  }

  public void blockPlaced() {
    STRest.asyncPOST(relativePath(), defaultParams(), token);
  }

  public void blockDestroyed() {
    STRest.asyncDELETE(relativePath(), defaultParams(), token);
  }


  public void neighborBlockChanged(int neighborBlockId) {
    // this is only happening on the server side

    if (getPowerLevel() > signalStrength) { // TODO: figure out how to turn off when receiving a smaller signal
      // received a higher signal than SmartThings provided. go bak to the default behavior.
      setSignalStrength(0);
    }

    setLastNeighborChanged(neighborBlockId);

    shouldUpdateST = true; // the next custom tick will call SmartThings
  }


  private void updateSmartThings() {

//    log("attempting to collect Block with id " + lastNeighborChanged);
    Block neighborBlock = Block.blocksList[lastNeighborChanged];

//    log("attempting to get name for Block with id " + lastNeighborChanged);
    String neighborBlockName = "UNKNOWN";
    try {
      neighborBlockName = neighborBlock.getLocalizedName();
    }
    catch (Exception e) {
      e.printStackTrace();
      log(e.getLocalizedMessage());
    }
//    log("BlockName: " + neighborBlockName);

    HashMap<String, String> params = defaultParams();
    params.put("blockId", "" + lastNeighborChanged);
    params.put("blockName", neighborBlockName);

    int blockPower = signalStrength; // signalStrength is 0 when not specified by SmartThings
    if (blockPower == 0) {
      blockPower = getPowerLevel();
    }
    params.put("signalStrength", "" + blockPower);

    STRest.asyncPUT(relativePath(), params, token);
  }

  public void receivedSmartThingsEvent(HashMap<String, String> params) {
    try {
//      log("parse params here " + params);

      String eventName = params.get("name");
      String eventValue = params.get("value");

//      log(eventName + " : " + eventValue);

      if (eventName == null || eventValue == null) {
        return;
      }

      if (eventName.equals("switch")) {
//        log("it's a switch event");
        if (eventValue.equals("on")) {
//          log("switch is on");

          setSignalStrength(15);
          syncServerAndClient();

        } else if (eventValue.equals("off")) {
//          log("switch is off");

          setSignalStrength(0);
          syncServerAndClient();

        } else {
//          log("switch is unknown");
        }
      } else if (eventName.equals("level")) {
//        log("it's a level event");
        int level = Integer.parseInt(eventValue);
        if (level > 15) level = 15;
        if (level < 0) level = 0;

        setSignalStrength(level);
        syncServerAndClient();
      }

      shouldUpdateST = true; // make sure SmartThings knows we received the command

    }
    catch (Exception e) {
      e.printStackTrace();
      log("failed to parse params from server");
    }

  }

  public void updateTileWithPacket(String packetString) {
    String worldSeed = SmartBlockPacketHandler.smartBlockWorldSeed(packetString);
    String dimensionName = SmartBlockPacketHandler.smartBlockDimensionName(packetString);
    String username = SmartBlockPacketHandler.smartBlockPlacedByUsername(packetString);
    int newSignalStrength = SmartBlockPacketHandler.smartBlockSignalStrength(packetString);
    int changedNeighbor = SmartBlockPacketHandler.smartBlockNeighborChanged(packetString);

    if (shouldUpdateSignalStrength(newSignalStrength)) {
      setSignalStrength(newSignalStrength);
    }

    if (shouldUpdateLastNeighborChanged(changedNeighbor)) {
      setLastNeighborChanged(changedNeighbor);
    }

    if (worldSeed != null) {
      setWorldSeed(worldSeed);
    }

    if (dimensionName != null) {
      setDimensionName(dimensionName);
    }

    if (username != null) {
      setPlacedByUsername(username);
    }

    worldObj.notifyBlockChange(xCoord, yCoord, zCoord, worldObj.getBlockId(xCoord, yCoord, zCoord));
  }

  public static void updateTilesWithPacket(String packetString) {
    int x = SmartBlockPacketHandler.smartBlockCoordinate(packetString, "x");
    int y = SmartBlockPacketHandler.smartBlockCoordinate(packetString, "y");
    int z = SmartBlockPacketHandler.smartBlockCoordinate(packetString, "z");

//    log("(" + x + "," + y + "," + z + ")");

    try {

      ArrayList<SmartBlockTileEntity> tiles = findSmartBlockTileEntities(x, y, z);

//      log("Updating " + tiles.size() + " tile" + (tiles.size() == 1 ? "" : "s"));

      for (SmartBlockTileEntity tile : tiles) {
        tile.updateTileWithPacket(packetString);
      }

    }
    catch (Exception e) {
      log("unable to parse tileEntity");
    }
  }

  private static ArrayList<SmartBlockTileEntity> findSmartBlockTileEntities(int x, int y, int z) {

    ArrayList<SmartBlockTileEntity> tiles = new ArrayList<SmartBlockTileEntity>();

    WorldServer[] worlds = DimensionManager.getWorlds(); // returns an empty array on the client so this is safe everywhere
    for (WorldServer world : worlds) {
      TileEntity blockTileEntity = world.getBlockTileEntity(x, y, z);
      if (blockTileEntity != null && blockTileEntity.getClass() == SmartBlockTileEntity.class) {
        tiles.add((SmartBlockTileEntity) blockTileEntity);
      }
    }

    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side == Side.CLIENT) {
      // this does not exist on the SERVER side and will crash the server if called

      TileEntity blockTileEntity = Minecraft.getMinecraft().theWorld.getBlockTileEntity(x, y, z);
      if (blockTileEntity != null && blockTileEntity.getClass() == SmartBlockTileEntity.class) {
        tiles.add((SmartBlockTileEntity) blockTileEntity);
      }

    }

    return tiles;
  }

  //================================================================================
  // Private
  //================================================================================

  private String authPacketMessage() {
    String message = xCoord + "|" + yCoord + "|" + zCoord;
    message += "|" + token;
    message += "|" + endpoint;
    return message;
  }

  private String packetMessage() {
    String message = xCoord + "|" + yCoord + "|" + zCoord;
    message += "|" + signalStrength;
    message += "|" + lastNeighborChanged;
    message += "|" + worldSeed;
    message += "|" + dimensionName;
    message += "|" + placedByUsername;
    return message;
  }

  private Packet250CustomPayload buildPackets(String message, String channel) {
    Packet250CustomPayload packet = new Packet250CustomPayload();
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
      DataOutputStream outputStream = new DataOutputStream(bos);

      // Write data
      byte[] data = message.getBytes("UTF-8");
      outputStream.writeInt(data.length);
      outputStream.write(data);

      // setup packet
      packet.channel = channel;
      packet.data = bos.toByteArray();
      packet.length = bos.size();

    }
    catch (Exception ex) {
      ex.printStackTrace();
      log(ex.getLocalizedMessage());

      packet = null;
    }

    return packet;
  }

  private void sendAuthChangesToServer() {
    log("Attempting to send credentials to the server");
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.CLIENT) {

      // update SmartThings
      blockPlaced();

      log("Not a client. not sending credentials");
      return;
    }

    Packet250CustomPayload authPacket = buildPackets(authPacketMessage(), authChannel);
    if (authPacket == null) {
      log("packet is null. not sending credentials to the server");
      return;
    }

//    log("sending packet to server");
    PacketDispatcher.sendPacketToServer(authPacket);

  }

  private void syncServerAndClient() {
    log("attempting to sync server and client");

    Packet250CustomPayload packet = buildPackets(packetMessage(), defaultChannel);
    if (packet == null) {
      log("packet is null");
      return;
    }

    Side side = FMLCommonHandler.instance().getEffectiveSide();

/*
    try {
      log("sending packet to server");
      PacketDispatcher.sendPacketToServer(packet);
    }
    catch (Exception e) {
      e.printStackTrace();
      log(e.getLocalizedMessage());
    }

    try {
      log("sending packet to all players");
      PacketDispatcher.sendPacketToAllPlayers(packet);
    }
    catch (Exception e) {
      e.printStackTrace();
      log(e.getLocalizedMessage());
    }
*/

    try { // doesn't work on local servers because the server thinks it's a client for some reason
      if (side == Side.CLIENT || side.isClient()) {
//        log("sending packet to server");
        PacketDispatcher.sendPacketToServer(packet);
      } else if (side == Side.SERVER || side.isServer()) {
//        log("sending packet to all players");
        PacketDispatcher.sendPacketToAllPlayers(packet);
      } else {
        log("unknown side. not sending packet");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      log("failed to send packet to all players");
    }

  }

  private HashMap<String, String> defaultParams() {
    HashMap<String, String> params = new HashMap<String, String>();

    params.put("x", "" + xCoord);
    params.put("y", "" + yCoord);
    params.put("z", "" + zCoord);


    String dimension = dimensionString();
    if (dimension != null) {
      params.put("dimensionName", worldObj.provider.getDimensionName());
    }

    if (placedByUsername != null && !placedByUsername.equals("")) {
      params.put("placedBy", placedByUsername);
    }

    String seed = seedString();
    if (seed != null) {
      params.put("worldSeed", seed);
    }

    String closestPlayerUsername = getClosestPlayerUsername();
    if (closestPlayerUsername != null) {
      params.put("closestPlayer", closestPlayerUsername);
    }

    return params;
  }

  private String seedString() {
    if (worldSeed != null && !worldSeed.equals("")) {
      return worldSeed;
    }

    String seed = "" + worldObj.getSeed();
    if (seed.startsWith("-")) {
      seed = seed.substring(1);
    }

    if (!seed.equals("0")) {
      setWorldSeed(seed);
      return seed;
    }

    return null;
  }

  private String dimensionString() {
    if (dimensionName != null && !dimensionName.equals("")) {
      return dimensionName;
    }

    String dimension = worldObj.provider.getDimensionName();
    if (dimension != null && !dimension.equals("")) {
      setDimensionName(dimension);
      return dimension;
    }

    return null;
  }

  private String relativePath() {
    return endpoint + "/block";
  }

  private Boolean attributeEquality(String a, String b) {
    if (a == null && b == null) {
      // both are null so they are equal
      return true;
    }

    if (a == null || b == null) {
      // they cannot both be null. If either is null, they are not equal
      return false;
    }

    // neither are null so just check equality
    return a.equals(b);
  }

  private Boolean shouldUpdateSignalStrength(int stSignalStrength) {
    if (stSignalStrength < 0) {
      return false;
    }

    return this.signalStrength != stSignalStrength;

  }

  private Boolean shouldUpdateLastNeighborChanged(int lastNeighborChanged) {
    if (lastNeighborChanged < 0) {
      return false;
    }

    return this.lastNeighborChanged != lastNeighborChanged;

  }

  private int getPowerLevel() {
    int blockPowerInput = worldObj.getBlockPowerInput(xCoord, yCoord, zCoord);
//    log("blockPowerInput " + blockPowerInput);

    int indirectPower = worldObj.getStrongestIndirectPower(xCoord, yCoord, zCoord);
//    log("indirectPower " + indirectPower);

    int blockPower = indirectPower > blockPowerInput ? indirectPower : blockPowerInput;
//    log("blockPower: " + blockPower);

    return blockPower;
  }

  private String getClosestPlayerUsername() {
    EntityPlayer closestPlayer = worldObj.getClosestPlayer(xCoord, yCoord, zCoord, 10);
    if (closestPlayer != null && closestPlayer.username.length() > 0) {
      return closestPlayer.username;
    }

    return null;
  }

  //================================================================================
  // TileEntity Overrides
  //================================================================================

  @Override
  public void writeToNBT(NBTTagCompound par1) {
    super.writeToNBT(par1);
    par1.setString("token", token == null ? "" : token);
    par1.setString("endpoint", endpoint == null ? "" : endpoint);
    par1.setInteger("signalStrength", signalStrength);
    par1.setInteger("lastNeighborChanged", lastNeighborChanged);
  }

  @Override
  public void readFromNBT(NBTTagCompound par1) {
    super.readFromNBT(par1);
    this.token = par1.getString("token");
    this.endpoint = par1.getString("endpoint");
    this.signalStrength = par1.getInteger("signalStrength");
    this.lastNeighborChanged = par1.getInteger("lastNeighborChanged");
  }

  @Override
  public Packet getDescriptionPacket() {
    NBTTagCompound tileTag = new NBTTagCompound();
    this.writeToNBT(tileTag);
    return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 0, tileTag);
  }

  @Override
  public void onDataPacket(INetworkManager net, Packet132TileEntityData pkt) {
    this.readFromNBT(pkt.data);
  }

  @Override
  public boolean canUpdate() {
//    Determines if this TileEntity requires update calls.
//    return true if you want updateEntity() to be called, false if not
    return true;
  }

  @Override
  public void updateEntity() {
//    Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count ticks and creates a new spawn inside its implementation.
    super.updateEntity();

    if (shouldUpdateEntity()) {
//      log("updatingEntity");
      worldObj.notifyBlockChange(xCoord, yCoord, zCoord, worldObj.getBlockId(xCoord, yCoord, zCoord));
    }

    if (shouldUpdateSmartThings()) {
//      log("updatingSmartThings");
      updateSmartThings();
    }

  }

  private Boolean shouldUpdateEntity() {
    if (updatedFromST || signalStrengthUpdated) {
      updatedFromST = false;
      signalStrengthUpdated = false;
      return true;
    }

    return false;
  }

  private Boolean shouldUpdateSmartThings() {

/*
    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.SERVER) {
      return false;
    }
*/

    if (++numberOfTicks > 20) {
      numberOfTicks = 0;
    }

    if (numberOfTicks > 0) {
      return false;
    }

    if (shouldUpdateST) {
      shouldUpdateST = false;
      return true;
    }

    return false;
  }

  //================================================================================
  // logging
  //================================================================================

  private static void log(Object message) {
    STLogger.fLog(message, SmartBlockTileEntity.class);
  }

   /* block sides
    * 0: -y, bottom
    * 1: +y, top
    * 2: -z
    * 3: +z
    * 4: -x
    * 5: +x
    * */


  //================================================================================
  // debug
  //================================================================================

  public void log() {
    log("tileEntity.endpoint: " + endpoint);
    log("tileEntity.token: " + token);
  }

  private void testing() {

    int blockPowerInput = getPowerLevel();
    log("BEFORE blockPowerInput " + blockPowerInput);

    signalStrength = 0;

    blockPowerInput = getPowerLevel();
    log("AFTER blockPowerInput " + blockPowerInput);

  /*
      if (stSignal > 0) {
        if (blockPowerInput > stSignal) {
          setSignalStrength(blockPowerInput);
          stSignal = 0;
        } else {
          signalStrength = 0;
          int check = redstoneCheck();
          if (check == 0) {
            signalStrength = stSignal;
          } else {
            stSignal = 0;
            setSignalStrength(check);
          }
        }
      } else {
        setSignalStrength(blockPowerInput);
      }
  */

  }

  private int redstoneCheck() {
    //    int old = signalStrength;
    //    int sig = 0;

    //    worldObj.notifyBlockChange(xCoord, yCoord, zCoord, worldObj.getBlockId(xCoord, yCoord, zCoord));

    int checking = 0;
    checking = greatestPower(checking, xCoord - 1, yCoord, zCoord);
    checking = greatestPower(checking, xCoord + 1, yCoord, zCoord);
    checking = greatestPower(checking, xCoord, yCoord - 1, zCoord);
    checking = greatestPower(checking, xCoord, yCoord + 1, zCoord);
    checking = greatestPower(checking, xCoord, yCoord, zCoord - 1);
    checking = greatestPower(checking, xCoord, yCoord, zCoord + 1);

    log("redstoneCheck: " + checking);

    return checking;
  }

  private int greatestPower(int greatest, int x, int y, int z) {
    int checking = getPowerLevel();
    if (checking < greatest) {
      checking = greatest;
    }

    return checking;
  }

  private void checkNeighbors() {
            /* block sides
      * 0: -y, bottom
      * 1: +y, top
      * 2: -z
      * 3: +z
      * 4: -x
      * 5: +x
      * */

    log("worldObj.isBlockProvidingPowerTo(xCoord - 1, yCoord, zCoord, 4); " + worldObj.isBlockProvidingPowerTo(xCoord - 1, yCoord, zCoord, 4));
    log("worldObj.isBlockProvidingPowerTo(xCoord + 1, yCoord, zCoord, 5); " + worldObj.isBlockProvidingPowerTo(xCoord + 1, yCoord, zCoord, 5));
    log("worldObj.isBlockProvidingPowerTo(xCoord, yCoord - 1, zCoord, 0); " + worldObj.isBlockProvidingPowerTo(xCoord, yCoord - 1, zCoord, 0));
    log("worldObj.isBlockProvidingPowerTo(xCoord, yCoord + 1, zCoord, 1); " + worldObj.isBlockProvidingPowerTo(xCoord, yCoord + 1, zCoord, 1));
    log("worldObj.isBlockProvidingPowerTo(xCoord, yCoord, zCoord - 1, 2); " + worldObj.isBlockProvidingPowerTo(xCoord, yCoord, zCoord - 1, 2));
    log("worldObj.isBlockProvidingPowerTo(xCoord, yCoord, zCoord + 1, 3); " + worldObj.isBlockProvidingPowerTo(xCoord, yCoord, zCoord + 1, 3));


  }
}
