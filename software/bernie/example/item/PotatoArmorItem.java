package software.bernie.example.item;


import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import software.bernie.example.GeckoLibMod;
import software.bernie.example.registry.ItemRegistry;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.item.GeoArmorItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//This is an example of animated armor. Make sure to read the comments thoroughly and also check out PotatoArmorRenderer.
public class PotatoArmorItem extends GeoArmorItem implements IAnimatable
{
	private AnimationFactory factory = new AnimationFactory(this);

	public PotatoArmorItem(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot slot)
	{
		super(materialIn, renderIndexIn, slot);

		this.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());
	}

	// Predicate runs every frame
	private <P extends IAnimatable> PlayState predicate(AnimationEvent<P> event)
	{
		//This is all the extradata this event carries. The livingentity is the entity that's wearing the armor. The itemstack and equipmentslottype are self explanatory.
		List<EntityEquipmentSlot> slotData = event.getExtraDataOfType(EntityEquipmentSlot.class);
		List<ItemStack> stackData = event.getExtraDataOfType(ItemStack.class);
		EntityLivingBase livingEntity = event.getExtraDataOfType(EntityLivingBase.class).get(0);

		//Always loop the animation but later on in this method we'll decide whether or not to actually play it
		event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.potato_armor.new", true));

		//If the living entity is an armorstand just play the animation nonstop
		if (livingEntity instanceof EntityArmorStand)
		{
			return PlayState.CONTINUE;
		}

		//The entity is a player, so we want to only play if the player is wearing the full set of armor
		else if (livingEntity instanceof EntityPlayerSP)
		{
			EntityPlayerSP client = (EntityPlayerSP) livingEntity;

			//Get all the equipment, aka the armor, currently held item, and offhand item
			List<Item> equipmentList = new ArrayList<>();
			client.getEquipmentAndArmor().forEach((x) -> equipmentList.add(x.getItem()));

			//elements 2 to 6 are the armor so we take the sublist. Armorlist now only contains the 4 armor slots
			List<Item> armorList = equipmentList.subList(2, 6);

			//Make sure the player is wearing all the armor. If they are, continue playing the animation, otherwise stop
			boolean isWearingAll = armorList.containsAll(Arrays.asList(ItemRegistry.POTATO_BOOTS, ItemRegistry.POTATO_LEGGINGS, ItemRegistry.POTATO_CHEST, ItemRegistry.POTATO_HEAD));
			return isWearingAll ? PlayState.CONTINUE : PlayState.STOP;
		}
		return PlayState.STOP;
	}


	//All you need to do here is add your animation controllers to the AnimationData
	@Override
	public void registerControllers(AnimationData data)
	{
		data.addAnimationController(new AnimationController(this, "controller", 20, this::predicate));
	}


	@Override
	public AnimationFactory getFactory()
	{
		return this.factory;
	}
}
