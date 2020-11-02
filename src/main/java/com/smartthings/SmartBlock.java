package com.smartthings;

import com.smartthings.network.STRestServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler; // used in 1.6.2
//import cpw.mods.fml.common.Mod.PreInit;    // used in 1.5.2
//import cpw.mods.fml.common.Mod.Init;       // used in 1.5.2
//import cpw.mods.fml.common.Mod.PostInit;   // used in 1.5.2
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "SmartBlockModID", name = "SmartBlock", version = "0.9")
@NetworkMod(clientSideRequired = true, serverSideRequired = false, channels = {"SmartThings", "SmartThingsA"}, packetHandler = SmartBlockPacketHandler.class)
public class SmartBlock {

  // The instance of your mod that Forge uses.
  @Instance(value = "SmartBlockModID")
  public static SmartBlock instance;

  public static Block smartBlock;

  // Says where the client and server 'proxy' code is loaded.
  @SidedProxy(clientSide = "com.smartthings.client.ClientProxy", serverSide = "com.smartthings.CommonProxy")
  public static CommonProxy proxy;

  @EventHandler // used in 1.6.2
  //@PreInit    // used in 1.5.2
  public void preInit(FMLPreInitializationEvent event) {

    // Setup BlockSmartBlock
    smartBlock = new BlockSmartBlock(500, Material.ground)
        .setHardness(0.5F).setStepSound(Block.soundGravelFootstep)
        .setUnlocalizedName("smartBlock").setCreativeTab(CreativeTabs.tabBlock)
        .setTextureName("smartblock:STLogo").setTickRandomly(true);

    LanguageRegistry.addName(smartBlock, "SmartBlock");
    MinecraftForge.setBlockHarvestLevel(smartBlock, "shovel", 0);
    GameRegistry.registerBlock(smartBlock, "smartBlock");

    // Setup SmartBlockTileEntity
    GameRegistry.registerTileEntity(SmartBlockTileEntity.class, "SmartBlockTileEntityID");


    ItemStack quartzStack = new ItemStack(Item.netherQuartz);
    ItemStack redstoneStack = new ItemStack(Item.redstone);
    ItemStack comparatorStack = new ItemStack(Item.comparator);

    GameRegistry.addRecipe(new ItemStack(smartBlock), "xxx", "xzx", "xyx",
        'x', quartzStack, 'y', redstoneStack, 'z', comparatorStack);


  }

  @EventHandler // used in 1.6.2
  //@Init       // used in 1.5.2
  public void load(FMLInitializationEvent event) {
    proxy.registerRenderers();
  }

  @EventHandler // used in 1.6.2
  //@PostInit   // used in 1.5.2
  public void postInit(FMLPostInitializationEvent event) {
    // Stub Method
    try {
      Side side = FMLCommonHandler.instance().getEffectiveSide();
      if (side != Side.SERVER) {
        return;
      }

      new Thread(new Runnable() {
        public void run() {

          try {
            STRestServer.run();
          }
          catch (Exception e) {
            // TODO: figure out how to handle this
            e.printStackTrace();
            System.out.println("STRestServer stopped unexpectedly");
          }
        }
      }).start();

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
