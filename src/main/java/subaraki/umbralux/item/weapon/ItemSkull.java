package subaraki.umbralux.item.weapon;

import java.util.ConcurrentModificationException;

import javax.annotation.Nullable;

import lib.playerclass.capability.PlayerClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import subaraki.umbralux.config.ConfigurationHandler;
import subaraki.umbralux.entity.EntityNecroCloud;
import subaraki.umbralux.entity.minion.EntityMinionZombie;
import subaraki.umbralux.entity.minion.IMinion;
import subaraki.umbralux.entity.minion.MinionRegistry;
import subaraki.umbralux.item.UmbraLuxItems;

public class ItemSkull extends Item{

	public ItemSkull() {
		maxStackSize = 1;

		this.addPropertyOverride(new ResourceLocation("activate"), new IItemPropertyGetter()
		{
			@SideOnly(Side.CLIENT)
			public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
			{
				if (entityIn == null)
				{
					return 0.0F;
				}
				else
				{
					ItemStack itemstack = entityIn.getActiveItemStack();
					float timer = itemstack != ItemStack.EMPTY && itemstack.getItem() == UmbraLuxItems.skull? (float)(stack.getMaxItemUseDuration() - entityIn.getItemInUseCount()) / 20.0F : 0.0F;

					return timer;
				}
			}
		});
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft){

		if(!(entityLiving instanceof EntityPlayer))
			return;

		EntityPlayer player = (EntityPlayer)entityLiving;

		if(!PlayerClass.get(player).isPlayerClass(UmbraLuxItems.NECROMANCER_CLASS))
			return;

		float count = (float)(stack.getMaxItemUseDuration() - entityLiving.getItemInUseCount()) / 20.0F;
		if(count >= 0.2F && count < 1.5F){
			EntityNecroCloud cloud = new EntityNecroCloud(worldIn, entityLiving);
			cloud.setPosition(entityLiving.posX + entityLiving.getLookVec().x, entityLiving.posY + entityLiving.getEyeHeight() - 0.1, entityLiving.posZ + entityLiving.getLookVec().z);
			if(!worldIn.isRemote)
				worldIn.spawnEntity(cloud);
			if(entityLiving instanceof EntityPlayer){
				((EntityPlayer)entityLiving).getCooldownTracker().setCooldown(this, 50);
			}
		}
		else if (count < 0.2F){
			if(MinionRegistry.minionsForPlayer(player).size() < (PlayerClass.get(player).isShielded() ? 5 : 2))
				if(!player.getCooldownTracker().hasCooldown(this)){
					EntityMinionZombie emz = new EntityMinionZombie(player.world);
					emz.setPositionAndRotation(player.posX + player.getLook(1F).x, player.posY, player.posZ + player.getLook(1F).z, -player.getRotationYawHead(), -player.rotationPitch);

					if (!player.world.isRemote){
						emz.setOwnerId(player.getUniqueID());
						player.world.spawnEntity(emz);
					}else{
						for(int i = 0; i < 100; i++)
							player.world.spawnParticle(EnumParticleTypes.FLAME, emz.posX-0.5+player.world.rand.nextDouble(), emz.posY - player.world.rand.nextDouble(), emz.posZ-0.5+player.world.rand.nextDouble(), 0, 0.1D, 0, new int[0]);
					}
					player.getCooldownTracker().setCooldown(this, 50);
				}
		}
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase elb, int count) {
		float passedUsing = (float)(stack.getMaxItemUseDuration() - elb.getItemInUseCount()) / 20.0F;

		if(elb instanceof EntityPlayer){
			EntityPlayer player = (EntityPlayer)elb;
			if(passedUsing > 1.5F){
				try {

					if(!MinionRegistry.minionsForPlayer(player).isEmpty()){

						for(IMinion minion : MinionRegistry.minionsForPlayer(player)){
							EntityTameable theMinion = (EntityTameable)minion;

							if(player.world.getEntityByID(theMinion.getEntityId()) == null){
								//keep the map clean
								MinionRegistry.removeMinion(player, minion);
								break;
							}

							BlockPos minionPosition = new BlockPos(theMinion);
							BlockPos playerPosition = new BlockPos(player);

							Vec3d vec = new Vec3d(playerPosition.getX()-minionPosition.getX(),
									playerPosition.getY() - minionPosition.getY(),
									playerPosition.getZ() - minionPosition.getZ());

							if(elb.world.isRemote)
								for(int i = 0; i < 2; i++)
									elb.world.spawnParticle(EnumParticleTypes.CRIT, 
											theMinion.posX + elb.world.rand.nextDouble() - 0.25, 
											theMinion.posY + theMinion.getEyeHeight(), 
											theMinion.posZ + elb.world.rand.nextDouble() - 0.25,
											vec.x/1.2D, vec.y/1.2D, vec.z/1.2D, new int[0]);

							if(count % 5 == 0){
								minion.Harvest();
								player.heal(2);
								//break, to prevent concurrent exception, because this entity is dead, and was/will be removed from the list.
								if(theMinion.getHealth() <=0){
									MinionRegistry.removeMinion(player, minion);
									break;
								}
							}
						}
						if(elb.world.isRemote)
							for(int i = 0; i < 5; i++)
								elb.world.spawnParticle(EnumParticleTypes.DRIP_LAVA, 
										elb.posX + (0.5D + elb.world.rand.nextDouble()*2) - 1.25D, 
										elb.posY + elb.world.rand.nextDouble(), 
										elb.posZ + (0.5D + elb.world.rand.nextDouble()*2) - 1.25D,
										0, 0, 0, new int[0]);
					}

				} catch (ConcurrentModificationException e){
					//concurrent modification can occur when spawning a new entity, and draining at the same time.
					//better catch it than let it crash
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand){

		playerIn.setActiveHand(hand);
		return new ActionResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(hand));
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.NONE;
	}

	@Override
	public int getItemEnchantability() {
		return 0;
	}

	@Override
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {

		return super.onEntitySwing(entityLiving, stack);
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		if(entity instanceof EntityLivingBase)
		{
			EntityLivingBase elb = (EntityLivingBase)entity;
			elb.addPotionEffect(new PotionEffect(new PotionEffect(MobEffects.WITHER, 20, 1)));//wither potion effect
			elb.attackEntityFrom(DamageSource.causePlayerDamage(player), ConfigurationHandler.instance.skull_damage);
		}

		return super.onLeftClickEntity(stack, player, entity);
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return repair.getItem().equals(Blocks.BONE_BLOCK) ? true : super.getIsRepairable(toRepair, repair);
	}
}
