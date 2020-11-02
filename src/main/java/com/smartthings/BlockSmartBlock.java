package com.smartthings;

import com.smartthings.utility.STFileAccess;
import com.smartthings.utility.STLogger;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockSmartBlock extends Block implements ITileEntityProvider {

  //================================================================================
  // Constructors
  //================================================================================

  public BlockSmartBlock(int id, Material material) {
    super(id, material);
  }

  //================================================================================
  // Block Overrides
  //================================================================================

  @Override
  public void registerIcons(IconRegister iconRegister) {
    blockIcon = iconRegister.registerIcon("smartblock:STLogo");
  }

  /* When a user sets a block, these methods get called in this order
   * onBlockPlaced
   * onBlockAdded
   * onBlockPlacedBy
   */

/*
  @Override
  public int onBlockPlaced(World par1World, int par2, int par3, int par4, int par5, float par6, float par7, float par8, int par9) {
    log("onBlockPlaced");
    return super.onBlockPlaced(par1World, par2, par3, par4, par5, par6, par7, par8, par9);
  }

  @Override
  public void onBlockAdded(World par1World, int par2, int par3, int par4) {
    log("onBlockAdded");
    super.onBlockAdded(par1World, par2, par3, par4);
  }
*/

  @Override
  public void onBlockPlacedBy(World world, int myX, int myY, int myZ, EntityLivingBase entityLivingBase, ItemStack itemStack) {
    super.onBlockPlacedBy(world, myX, myY, myZ, entityLivingBase, itemStack);

    // Update TileEntity
    TileEntity tileEntity = world.getBlockTileEntity(myX, myY, myZ);
    logTileEntity(tileEntity);
    if (tileEntity.getClass() == SmartBlockTileEntity.class) {

      SmartBlockTileEntity smartBlockTileEntity = (SmartBlockTileEntity) tileEntity;

      if (entityLivingBase instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) entityLivingBase;
        smartBlockTileEntity.setPlacedByUsername(player.username);
      }

      String token = STFileAccess.getToken();
      String endpoint = STFileAccess.getEndpoint();

      // this ultimately calls the SmartThings API once the server-side gets all of the details
      smartBlockTileEntity.updateAuth(token, endpoint);

      if (token == null || token.length() == 0) {
        // user is not authenticated
        smartBlockTileEntity.authenticate();
      } else if (endpoint == null || endpoint.length() == 0) {
        // authenticate will collect the endpoint so no need to do them both
        smartBlockTileEntity.collectEndpoint();
      }

    }
  }

  /*
  * Redstone power source
  *
  * onNeighborBlockChange
  *   isBlockIndirectlyGettingPowered: true
  *   getStrongestIndirectPower: 13
  *   getIndirectPowerOutput: false
  * */

  @Override
  public void onNeighborBlockChange(World world, int myX, int myY, int myZ, int neighborBlockId) {
    super.onNeighborBlockChange(world, myX, myY, myZ, neighborBlockId);
    log("onNeighborBlockChange");

    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.SERVER) {
      return;
    }

    if (neighborBlockId == this.blockID) {
      // SmartBlocks updating SmartBlocks gets messy with infinite loops
      return;
    }

    try {
      TileEntity tileEntity = world.getBlockTileEntity(myX, myY, myZ);
//      logTileEntity(tileEntity);
      SmartBlockTileEntity smartTile = (SmartBlockTileEntity) tileEntity;
      smartTile.neighborBlockChanged(neighborBlockId);

    }
    catch (Exception e) {
      e.printStackTrace();
      log(e.getLocalizedMessage());
    }
  }

  //================================================================================
  // Redstone Overrides
  //================================================================================

/*
  @Override
  public boolean canProvidePower() {
    // Can this block provide power. Only wire currently seems to have this change based on its state.
    // nothing can be placed on top of blocks that return true
//    return super.canProvidePower();
    return true;
  }
*/

  @Override
  public boolean shouldCheckWeakPower(World world, int x, int y, int z, int side) {
    boolean shouldCheckWeakPower = super.shouldCheckWeakPower(world, x, y, z, side);
    log("shouldCheckWeakPower: " + shouldCheckWeakPower);
//    return shouldCheckWeakPower;
//    return true;

    TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
    if (tileEntity == null || tileEntity.getClass() != SmartBlockTileEntity.class) {
      return shouldCheckWeakPower;
    }

    SmartBlockTileEntity smartTile = (SmartBlockTileEntity) tileEntity;
    return smartTile.signalStrength > 0;
  }

  @Override
  public int isProvidingStrongPower(IBlockAccess par1IBlockAccess, int myX, int myY, int myZ, int par5) {
    // Returns true if the block is emitting direct/strong redstone power on the specified side. Args: World, X, Y, Z, side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.

    TileEntity tileEntity = par1IBlockAccess.getBlockTileEntity(myX, myY, myZ);
//    logTileEntity(tileEntity);

    int providingStrongPower = super.isProvidingStrongPower(par1IBlockAccess, myX, myY, myZ, par5);
//    log("isProvidingStrongPower: " + providingStrongPower);
    if (tileEntity == null || tileEntity.getClass() != SmartBlockTileEntity.class) {
      log("not a SmartTile");
      return providingStrongPower;
    }

    SmartBlockTileEntity smartTile = (SmartBlockTileEntity) tileEntity;
    return smartTile.signalStrength;
  }

  @Override
  public int isProvidingWeakPower(IBlockAccess par1IBlockAccess, int myX, int myY, int myZ, int par5) {
    // Returns true if the block is emitting indirect/weak redstone power on the specified side. If isBlockNormalCube returns true, standard redstone propagation rules will apply instead and this will not be called. Args: World, X, Y, Z, side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.

    TileEntity tileEntity = par1IBlockAccess.getBlockTileEntity(myX, myY, myZ);
//    logTileEntity(tileEntity);

    int providingWeakPower = super.isProvidingWeakPower(par1IBlockAccess, myX, myY, myZ, par5);
    log("isProvidingWeakPower: " + providingWeakPower);
    if (tileEntity == null || tileEntity.getClass() != SmartBlockTileEntity.class) {
      log("not a SmartTile");
      return providingWeakPower;
    }

    SmartBlockTileEntity smartTile = (SmartBlockTileEntity) tileEntity;
    return smartTile.signalStrength;
  }

  @Override
  public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {
    log("onBlockActivated");
    return super.onBlockActivated(par1World, par2, par3, par4, par5EntityPlayer, par6, par7, par8, par9);
  }

  @Override
  public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
    log("canConnectRedstone");
    return super.canConnectRedstone(world, x, y, z, side);
  }

  @Override
  public void onBlockClicked(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer) {
    log("onBlockClicked");
    super.onBlockClicked(par1World, par2, par3, par4, par5EntityPlayer);
  }

  @Override
  public boolean onBlockEventReceived(World par1World, int par2, int par3, int par4, int par5, int par6) {
    log("onBlockEventReceived");
    return super.onBlockEventReceived(par1World, par2, par3, par4, par5, par6);
  }

  @Override
  public void onBlockPreDestroy(World world, int myX, int myY, int myZ, int par5) {
    super.onBlockPreDestroy(world, myX, myY, myZ, par5);
    log("onBlockPreDestroy");

    Side side = FMLCommonHandler.instance().getEffectiveSide();
    if (side != Side.SERVER) {
      return;
    }

    try {

      log("hasTileEntity: " + world.blockHasTileEntity(myX, myY, myZ));
      TileEntity tileEntity = world.getBlockTileEntity(myX, myY, myZ);
      logTileEntity(tileEntity);
      SmartBlockTileEntity smartTile = (SmartBlockTileEntity) tileEntity;
      smartTile.blockDestroyed();

    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  //================================================================================
  // TileEntity
  //================================================================================

  @Override
  public TileEntity createTileEntity(World world, int metadata) {
    log("createTileEntity");
    return new SmartBlockTileEntity();
  }

  @Override
  public TileEntity createNewTileEntity(World world) {
    log("createNewTileEntity");
    return new SmartBlockTileEntity();
  }

  //================================================================================
  // Logging
  //================================================================================

  private String coordinateString(int x, int y, int z) {
    return "(" + x + "," + y + "," + z + ")";

  }

  private void log(Object message) {
    STLogger.fLog(message, this.getClass());
  }

  private void logTileEntity(TileEntity tileEntity) {

    if (tileEntity == null) {
      log("tileEntity is null");
      return;
    }

    log("tileEntity.class = " + tileEntity.getClass());
    log("tileEntity.xCoord: " + tileEntity.xCoord);
    log("tileEntity.yCoord: " + tileEntity.yCoord);
    log("tileEntity.zCoord: " + tileEntity.zCoord);
    log("tileEntity.blockType: " + tileEntity.blockType);
    log("tileEntity.worldObj: " + tileEntity.worldObj);

    if (tileEntity.getClass() == SmartBlockTileEntity.class) {
      SmartBlockTileEntity smartBlockTileEntity = (SmartBlockTileEntity) tileEntity;
      smartBlockTileEntity.log();
    }
  }

}
