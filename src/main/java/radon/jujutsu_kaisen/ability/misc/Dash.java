package radon.jujutsu_kaisen.ability.misc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.MenuType;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.client.particle.CursedEnergyParticle;
import radon.jujutsu_kaisen.client.particle.MirageParticle;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.sound.JJKSounds;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Dash extends Ability {
    public static final double RANGE = 30.0D;
    private static final double SPEED = 2.5D;

    @Override
    public boolean isScalable() {
        return false;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return target != null && (HelperMethods.getLookAtHit(owner, RANGE) instanceof EntityHitResult hit && hit.getEntity() == target);
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    @Override
    public Status checkTriggerable(LivingEntity owner) {
        if (!canDash(owner)) {
            return Status.FAILURE;
        }
        return super.checkTriggerable(owner);
    }

    private static boolean canDash(LivingEntity owner) {
        return HelperMethods.getLookAtHit(owner, RANGE) instanceof EntityHitResult || owner.isInWater() ||
                owner.onGround() || !owner.getFeetBlockState().getFluidState().isEmpty();
    }

    @Override
    public void run(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        if (!canDash(owner)) return;

        Vec3 look = owner.getLookAngle();

        if (HelperMethods.getLookAtHit(owner, RANGE) instanceof EntityHitResult hit) {
            Entity target = hit.getEntity();

            double distanceX = target.getX() - owner.getX();
            double distanceY = target.getY() - owner.getY();
            double distanceZ = target.getZ() - owner.getZ();

            double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
            double motionX = distanceX / distance * SPEED;
            double motionY = distanceY / distance * SPEED;
            double motionZ = distanceZ / distance * SPEED;

            owner.setDeltaMovement(motionX, motionY, motionZ);
            owner.hurtMarked = true;
        } else if (owner.onGround() || !owner.getFeetBlockState().getFluidState().isEmpty()) {
            owner.setDeltaMovement(owner.getDeltaMovement().add(look.normalize().scale(SPEED)));
            owner.hurtMarked = true;
        }

        Vec3 pos = owner.position().add(0.0D, owner.getBbHeight() / 2.0F, 0.0D);

        for (int i = 0; i < 32; i++) {
            double theta = HelperMethods.RANDOM.nextDouble() * 2 * Math.PI;
            double phi = HelperMethods.RANDOM.nextDouble() * Math.PI;
            double r = HelperMethods.RANDOM.nextDouble() * 0.8D;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            Vec3 speed = look.add(x, y, z).reverse();
            Vec3 offset = pos.add(look);
            level.sendParticles(ParticleTypes.CLOUD, offset.x(), offset.y(), offset.z(), 0, speed.x(), speed.y(), speed.z(), 1.0D);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), JJKSounds.DASH.get(), SoundSource.MASTER, 1.0F, 1.0F);
        owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false, false));
        level.sendParticles(new MirageParticle.MirageParticleOptions(owner.getId()), owner.getX(), owner.getY(), owner.getZ(),
                0, 0.0D, 0.0D, 0.0D, 1.0D);
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0;
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    @Override
    public int getRealCooldown(LivingEntity owner) {
        AtomicInteger cooldown = new AtomicInteger(this.getCooldown());

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            if (cap.hasTrait(Trait.HEAVENLY_RESTRICTION)) {
                cooldown.set(0);
            } else if (cap.hasTrait(Trait.SIX_EYES)) {
                cooldown.set(cooldown.get() / 2);
            }
        });
        return cooldown.get();
    }

    @Override
    public MenuType getMenuType() {
        return MenuType.NONE;
    }
}
