SmartThingsBlock
================

Setup
================
0) Make a backup of ~/Library/Application Support/minecraft before starting any of this.

1) Download the minecraftForge installer( http://steve.vlaminck.com/T7zE ) and run it. I think it will overwrite some stuff in your minecraft folder, but it will definitely put a new version under minecraft/versions like this: http://steve.vlaminck.com/T7mV. 

2) Then open the regular Minecraft launcher and edit your profiles. Create a new one named “forge” or something and point it to your new version. Here’s what mine looks like: http://steve.vlaminck.com/T825

3) Download SmartBlock-<versionNumber>.jar from the /LatestBuild directory above and drop it in ~/Library/Application Support/minecraft/mods like this: http://steve.vlaminck.com/T7Cl

4) enjoy :D 


More Setup info
================
If you copy/paste the SmartBlockManager SmartApp and the SmartBlock DeviceType to make your own, you will need to add your client_id and client_secret to ~/Library/Application Support/minecraft/ in files named "SmartThings-<Your MC Username>.clientId" and "SmartThings-<Your MC Username>.clientSecret" respectively.

When you place your first block it should take you through the oauth flow. After you get back, destroy that first block because it didn’t register. Any block you place after that should create a new SmartBlock device in the location you chose during the oauth flow.

The SmartBlock is technically a switch so any SmartApps that listen to switches will work with it. I’ve create a SmartApp that will make a switch do exactly what another switch does: https://gist.github.com/vlaminck/fb0f61bb3174beb59ec4. You can use that to turn lights on/off or dim with redstone. Throw a light sensor on top of your SmartBlock to make a dimmer go with the daylight in the game.
