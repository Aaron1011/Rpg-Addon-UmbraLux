package subaraki.umbralux.mod;

import java.util.Arrays;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import subaraki.umbralux.block.UmbraLuxBlocks;
import subaraki.umbralux.handler.event.MinionAttackEvent;
import subaraki.umbralux.handler.event.PaladinWeaponEvent;
import subaraki.umbralux.handler.proxy.ServerProxy;
import subaraki.umbralux.item.UmbraLuxItems;
import subaraki.umbralux.network.PacketHandler;

@Mod(modid = AddonUmbraLux.MODID, name = AddonUmbraLux.NAME, version = AddonUmbraLux.VERSION, dependencies = "required-after:subcommonlib")
public class AddonUmbraLux {

	public static final String MODID = "umbralux";
	public static final String NAME = "Umbra - Lux Addon for Rpg Inventory";
	public static final String VERSION = "1.10.2 v2";
	
	@SidedProxy(clientSide = "subaraki.umbralux.handler.proxy.ClientProxy", serverSide = "subaraki.umbralux.handler.proxy.ServerProxy")
	public static ServerProxy proxy;
	
	@EventHandler
	public void preInitEvent(FMLPreInitializationEvent event){
		ModMetadata modMeta = event.getModMetadata();
		modMeta.authorList = Arrays.asList(new String[] { "Subaraki" });
		modMeta.autogenerated = false;
		modMeta.credits = "";
		modMeta.description = "Class Armor for Necromancer and Paladin";
		modMeta.url = "https://github.com/ArtixAllMighty/Rpg-Inventory-2016/wiki";

		UmbraLuxItems.loadItems();
		UmbraLuxBlocks.loadBlocks();
		
		proxy.registerEntities();
		//items come before registering the renders
		proxy.registerRenders();
		
		new PacketHandler();
		new MinionAttackEvent();
		new PaladinWeaponEvent();
		
	}
	
	@EventHandler
	public void initEvent(FMLInitializationEvent event){
		proxy.registerColors();
	}
}
