package radon.jujutsu_kaisen.ability.misc;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.client.particle.LightningParticle;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.config.ConfigHolder;
import radon.jujutsu_kaisen.effect.JJKEffects;
import radon.jujutsu_kaisen.entity.SimpleDomainEntity;
import radon.jujutsu_kaisen.util.DamageUtil;
import radon.jujutsu_kaisen.util.EntityUtil;
import radon.jujutsu_kaisen.util.HelperMethods;

public class QuickDraw extends Ability implements Ability.IToggled {
    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return target != null && !target.isDeadOrDying() && owner.hasLineOfSight(target);
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    private static void attack(LivingEntity owner, Entity entity) {
        if (entity instanceof AbstractArrow || entity instanceof ThrowableItemProjectile) {
            owner.lookAt(EntityAnchorArgument.Anchor.EYES, entity.position().add(0.0D, entity.getBbHeight() / 2.0F, 0.0D));
            owner.swing(InteractionHand.MAIN_HAND, true);
            entity.discard();
        } else if (entity instanceof LivingEntity) {
            if (entity.invulnerableTime > 0) return;

            owner.lookAt(EntityAnchorArgument.Anchor.EYES, entity.position().add(0.0D, entity.getBbHeight() / 2.0F, 0.0D));
            owner.swing(InteractionHand.MAIN_HAND, true);

            if (owner instanceof Player player) {
                player.attack(entity);
            } else {
                owner.doHurtTarget(entity);
            }

            for (int i = 0; i < 4; i++) {
                double x = owner.getX() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * (owner.getBbWidth() * 2);
                double y = owner.getY() + HelperMethods.RANDOM.nextDouble() * (owner.getBbHeight() * 1.25F);
                double z = owner.getZ() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * (owner.getBbWidth() * 2);
                ((ServerLevel) owner.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void run(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        owner.addEffect(new MobEffectInstance(JJKEffects.STUN.get(), 2, 0, false, false, false));

        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return;

        ISorcererData sorcererData = cap.getSorcererData();
        IAbilityData abilityData = cap.getAbilityData();

        if (abilityData.hasToggled(JJKAbilities.SIMPLE_DOMAIN.get())) {
            SimpleDomainEntity domain = sorcererData.getSummonByClass(SimpleDomainEntity.class);

            if (domain == null) return;

            for (Entity entity : owner.level().getEntities(owner, domain.getBoundingBox())) {
                if (entity == domain || entity.distanceTo(domain) > domain.getRadius()) continue;

                attack(owner, entity);
            }
        }
    }

    @Override
    public void onEnabled(LivingEntity owner) {

    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0;
    }

    @Override
    public boolean isValid(LivingEntity owner) {
        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return false;

        IAbilityData data = cap.getAbilityData();
        return (data.hasToggled(JJKAbilities.SIMPLE_DOMAIN.get()) || data.hasToggled(JJKAbilities.FALLING_BLOSSOM_EMOTION.get())) && super.isValid(owner);
    }

    @Nullable
    @Override
    public Ability getParent(LivingEntity owner) {
        return JJKAbilities.SIMPLE_DOMAIN.get();
    }

    @Override
    public int getPointsCost() {
        return ConfigHolder.SERVER.quickDrawCost.get();
    }

    @Override
    public boolean isTechnique() {
        return false;
    }

    @Mod.EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            IJujutsuCapability cap = victim.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) return;

            IAbilityData data = cap.getAbilityData();

            if (!data.hasToggled(JJKAbilities.QUICK_DRAW.get()) ||
                    !data.hasToggled(JJKAbilities.FALLING_BLOSSOM_EMOTION.get())) return;

            Entity attacker = event.getSource().getDirectEntity();

            QuickDraw.attack(victim, attacker);
        }

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            DamageSource source = event.getSource();

            if (!DamageUtil.isMelee(source)) return;

            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            IJujutsuCapability cap = victim.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) return;

            IAbilityData data = cap.getAbilityData();

            if (data.hasToggled(JJKAbilities.QUICK_DRAW.get())) return;

            Vec3 center = new Vec3(victim.getX(), victim.getY() + (victim.getBbHeight() / 2.0F), victim.getZ());
            ((ServerLevel) victim.level()).sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 0, 1.0D, 0.0D, 0.0D, 1.0D);

            victim.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 1.0F, 1.0F);

            event.setAmount(event.getAmount() * 2.0F);
        }
    }
}
