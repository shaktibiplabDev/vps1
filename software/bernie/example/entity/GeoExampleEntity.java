package software.bernie.example.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class GeoExampleEntity extends EntityCreature implements IAnimatable
{
	private AnimationFactory factory = new AnimationFactory(this);

	private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event)
	{
		if (((EntityCreature) event.getAnimatable()).world.isRaining())
		{
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.magmaspider.attack", true));
		}
		else
		{
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.magmaspider.run", true));
		}
		return PlayState.CONTINUE;
	}

	public GeoExampleEntity(World worldIn)
	{
		super(worldIn);
		this.ignoreFrustumCheck = true;
		this.setSize(0.7F, 1.3F);
	}

	@Override
	public void registerControllers(AnimationData data)
	{
		data.addAnimationController(new AnimationController(this, "controller", 50, this::predicate));
	}

	@Override
	public AnimationFactory getFactory()
	{
		return this.factory;
	}

	@Override
	protected void initEntityAI()
	{
		this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		super.initEntityAI();
	}
}
