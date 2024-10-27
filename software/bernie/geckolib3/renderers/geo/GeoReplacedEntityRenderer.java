package software.bernie.geckolib3.renderers.geo;

import com.eliotlash.mclib.utils.Interpolations;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GeoReplacedEntityRenderer<T extends IAnimatable> extends Render<EntityLivingBase> implements IGeoRenderer
{
	private final AnimatedGeoModel<T> modelProvider;
	private final T animatable;
	protected final List<GeoLayerRenderer> layerRenderers = Lists.newArrayList();
	private IAnimatable currentAnimatable;
	private static Map<Class<? extends IAnimatable>, GeoReplacedEntityRenderer> renderers = new ConcurrentHashMap<>();

	static
	{
		AnimationController.addModelFetcher((IAnimatable object) ->
		{
			GeoReplacedEntityRenderer renderer = renderers.get(object.getClass());
			return renderer == null ? null : renderer.getGeoModelProvider();
		});
	}

	protected GeoReplacedEntityRenderer(RenderManager renderManager, AnimatedGeoModel<T> modelProvider, T animatable)
	{
		super(renderManager);
		this.modelProvider = modelProvider;
		this.animatable = animatable;
	}

	public static void registerReplacedEntity(Class<? extends IAnimatable> itemClass, GeoReplacedEntityRenderer renderer)
	{
		renderers.put(itemClass, renderer);
	}

	public static GeoReplacedEntityRenderer getRenderer(Class<? extends IAnimatable> item)
	{
		return renderers.get(item);
	}

	@Override
	public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks)
	{
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);

		// TODO: entity.isPassenger() looks redundant here
		boolean shouldSit = /* entity.isPassenger() && */ (entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit());
		EntityModelData entityModelData = new EntityModelData();
		entityModelData.isSitting = shouldSit;
		entityModelData.isChild = entity.isChild();

		float f = Interpolations.lerpYaw(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
		float f1 = Interpolations.lerpYaw(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
		float netHeadYaw = f1 - f;
		if (shouldSit && entity.getRidingEntity() instanceof EntityLivingBase)
		{
			EntityLivingBase livingentity = (EntityLivingBase) entity.getRidingEntity();
			f = Interpolations.lerpYaw(livingentity.prevRenderYawOffset, livingentity.renderYawOffset, partialTicks);
			netHeadYaw = f1 - f;
			float f3 = MathHelper.wrapDegrees(netHeadYaw);
			if (f3 < -85.0F)
			{
				f3 = -85.0F;
			}

			if (f3 >= 85.0F)
			{
				f3 = 85.0F;
			}

			f = f1 - f3;
			if (f3 * f3 > 2500.0F)
			{
				f += f3 * 0.2F;
			}

			netHeadYaw = f1 - f;
		}

		float headPitch = Interpolations.lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);
		/* TODO: vanilla mobs can't sleep in beds in 1.12.2 and below
		if (entity.getPose() == Pose.SLEEPING)
		{
			Direction direction = entity.getBedDirection();
			if (direction != null)
			{
				float f4 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
				stack.translate((double) ((float) (-direction.getXOffset()) * f4), 0.0D, (double) ((float) (-direction.getZOffset()) * f4));
			}
		} */
		float f7 = this.handleRotationFloat(entity, partialTicks);
		this.applyRotations(entity, f7, f, partialTicks);

		float limbSwingAmount = 0.0F;
		float limbSwing = 0.0F;
		if (!shouldSit && entity.isEntityAlive())
		{
			limbSwingAmount = Interpolations.lerp(entity.prevLimbSwingAmount, entity.limbSwingAmount, partialTicks);
			limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
			if (entity.isChild())
			{
				limbSwing *= 3.0F;
			}

			if (limbSwingAmount > 1.0F)
			{
				limbSwingAmount = 1.0F;
			}
		}
		entityModelData.headPitch = -headPitch;
		entityModelData.netHeadYaw = -netHeadYaw;

		AnimationEvent predicate = new AnimationEvent(animatable, limbSwing, limbSwingAmount, partialTicks, !(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F), Collections.singletonList(entityModelData));
		GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(animatable));
		if (modelProvider instanceof IAnimatableModel)
		{
			((IAnimatableModel<T>) modelProvider).setLivingAnimations(animatable, this.getUniqueID(entity), predicate);
		}

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0.01f, 0);
		Minecraft.getMinecraft().renderEngine.bindTexture(getEntityTexture(entity));
		Color renderColor = getRenderColor(entity, partialTicks);
		render(model, entity, partialTicks, (float) renderColor.getRed() / 255f, (float) renderColor.getBlue() / 255f, (float) renderColor.getGreen() / 255f, (float) renderColor.getAlpha() / 255);

		if (entity instanceof EntityPlayer && !((EntityPlayer) entity).isSpectator())
		{
			for (GeoLayerRenderer layerRenderer : this.layerRenderers)
			{
				layerRenderer.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, f7, netHeadYaw, headPitch, 1 / 16F);
			}
		}
		GlStateManager.popMatrix();
		GlStateManager.popMatrix();

		// super.doRender(entity, x, y, z, entityYaw, partialTicks);
	}

	protected void preRenderCallback(EntityLivingBase entitylivingbaseIn, float partialTickTime)
	{
	}

	@Nullable
	@Override
	protected ResourceLocation getEntityTexture(EntityLivingBase entity)
	{
		return getTextureLocation(currentAnimatable);
	}

	@Override
	public AnimatedGeoModel getGeoModelProvider()
	{
		return this.modelProvider;
	}

	protected void applyRotations(EntityLivingBase entityLiving, float ageInTicks, float rotationYaw, float partialTicks)
	{
		if (!entityLiving.isPlayerSleeping())
		{
			GlStateManager.rotate(180.0F - rotationYaw, 0, 1, 0);
		}

		if (entityLiving.deathTime > 0)
		{
			float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
			f = MathHelper.sqrt(f);
			if (f > 1.0F)
			{
				f = 1.0F;
			}

			GlStateManager.rotate(f * this.getDeathMaxRotation(entityLiving), 0, 0, 1);
		}
		/* TODO: probably doesn't exist in 1.12.2 as well
		else if (entityLiving.isSpinAttacking())
		{
			matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-90.0F - entityLiving.rotationPitch));
			matrixStackIn.rotate(Vector3f.YP.rotationDegrees(((float) entityLiving.ticksExisted + partialTicks) * -75.0F));
		}
		else if (pose == Pose.SLEEPING)
		{
			Direction direction = entityLiving.getBedDirection();
			float f1 = direction != null ? getFacingAngle(direction) : rotationYaw;
			matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f1));
			matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(this.getDeathMaxRotation(entityLiving)));
			matrixStackIn.rotate(Vector3f.YP.rotationDegrees(270.0F));
		} */
		else if (entityLiving.hasCustomName() || entityLiving instanceof EntityPlayer)
		{
			String s = TextFormatting.getTextWithoutFormattingCodes(entityLiving.getName());
			if (("Dinnerbone".equals(s) || "Grumm".equals(s)) && (!(entityLiving instanceof EntityPlayer) || ((EntityPlayer) entityLiving).isWearing(EnumPlayerModelParts.CAPE)))
			{
				GlStateManager.translate(0.0D, (double) (entityLiving.height + 0.1F), 0.0D);
				GlStateManager.rotate(180, 0, 0, 1);
			}
		}
	}

	protected boolean isVisible(EntityLivingBase livingEntityIn)
	{
		return !livingEntityIn.isInvisible();
	}

	private static float getFacingAngle(EnumFacing facingIn)
	{
		switch (facingIn)
		{
			case SOUTH:
				return 90.0F;
			case WEST:
				return 0.0F;
			case NORTH:
				return 270.0F;
			case EAST:
				return 180.0F;
			default:
				return 0.0F;
		}
	}

	protected float getDeathMaxRotation(EntityLivingBase entityLivingBaseIn)
	{
		return 90.0F;
	}

	/**
	 * Returns where in the swing animation the living entity is (from 0 to 1).  Args : entity, partialTickTime
	 */
	protected float getSwingProgress(EntityLivingBase livingBase, float partialTickTime)
	{
		return livingBase.getSwingProgress(partialTickTime);
	}

	/**
	 * Defines what float the third param in setRotationAngles of ModelBase is
	 */
	protected float handleRotationFloat(EntityLivingBase livingBase, float partialTicks)
	{
		return (float) livingBase.ticksExisted + partialTicks;
	}

	public final boolean addLayer(GeoLayerRenderer<? extends EntityLivingBase> layer)
	{
		return this.layerRenderers.add(layer);
	}

	@Override
	public ResourceLocation getTextureLocation(Object instance)
	{
		return this.modelProvider.getTextureLocation(this.animatable);
	}
}
