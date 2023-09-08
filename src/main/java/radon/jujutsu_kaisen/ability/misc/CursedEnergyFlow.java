package radon.jujutsu_kaisen.ability.misc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.ThornsEnchantment;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedEnergyNature;
import radon.jujutsu_kaisen.client.particle.LightningParticle;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.client.particle.VaporParticle;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.effect.JJKEffects;
import radon.jujutsu_kaisen.util.HelperMethods;

public class CursedEnergyFlow extends Ability implements Ability.IToggled {
    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return (target != null && owner.distanceTo(target) <= 5.0D) || !owner.level.getEntities(owner, owner.getBoundingBox().inflate(1.0D), entity -> entity instanceof Projectile).isEmpty();
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public void run(LivingEntity owner) {
        if (!(owner.level instanceof ServerLevel level)) return;

        double width = owner.getBbWidth();
        double height = owner.getBbHeight();

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            for (int i = 0; i < 8; i++) {
                double x = owner.getX() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * width - owner.getLookAngle().scale(0.35D).x();
                double y = owner.getY() + HelperMethods.RANDOM.nextDouble() * height;
                double z = owner.getZ() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * width - owner.getLookAngle().scale(0.35D).z();
                level.sendParticles(new VaporParticle.VaporParticleOptions(ParticleColors.getCursedEnergyColor(owner), (float) width * 3.0F, 0.5F, false, 1),
                        x, y, z, 0, 0.0D, HelperMethods.RANDOM.nextDouble(), 0.0D, 1.5D);
            }

            if (cap.getNature() == CursedEnergyNature.LIGHTNING) {
                for (int i = 0; i < 2; i++) {
                    double x = owner.getX() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * width;
                    double y = owner.getY() + HelperMethods.RANDOM.nextDouble() * height;
                    double z = owner.getZ() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * width;
                    level.sendParticles(new LightningParticle.LightningParticleOptions(ParticleColors.getCursedEnergyColor(owner), 0.2F),
                            x, y, z, 0, 0.0D, 0, 0.0D, 0.0D);
                }
            }
        });
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0.4F;
    }

    @Override
    public void onEnabled(LivingEntity owner) {

    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }

    @Mod.EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            // Damage
            DamageSource source = event.getSource();
            if (!(source.getEntity() instanceof LivingEntity attacker)) return;

            if (JJKAbilities.hasToggled(attacker, JJKAbilities.CURSED_ENERGY_FLOW.get())) {
                if (attacker.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
                    ISorcererData attackerCap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

                    boolean melee = source.getDirectEntity() == source.getEntity() && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK));

                    if (melee) {
                        switch (attackerCap.getNature()) {
                            case LIGHTNING, BASIC -> event.setAmount(event.getAmount() * 1.1F);
                            case ROUGH -> event.setAmount(event.getAmount() * 1.2F);
                        }
                    }
                }
            }

            // Shield
            LivingEntity victim = event.getEntity();

            if (JJKAbilities.hasToggled(victim, JJKAbilities.CURSED_ENERGY_FLOW.get())) {
                event.setAmount(event.getAmount() * 0.9F);

                if (victim.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
                    ISorcererData cap = victim.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

                    switch (cap.getNature()) {
                        case LIGHTNING -> attacker.addEffect(new MobEffectInstance(JJKEffects.STUN.get(), 20, 0,
                                false, false, false));
                        case ROUGH -> {
                            if (ThornsEnchantment.shouldHit(3, victim.getRandom())) {
                                attacker.hurt(JJKDamageSources.jujutsuAttack(victim, null), (float) ThornsEnchantment.getDamage(3, victim.getRandom()));
                            }
                        }
                    }
                }
            }

            if (!attacker.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;
            ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

            if (!JJKAbilities.hasToggled(attacker, JJKAbilities.CURSED_ENERGY_FLOW.get())) return;

            if (cap.getNature() == CursedEnergyNature.LIGHTNING) {
                victim.addEffect(new MobEffectInstance(JJKEffects.STUN.get(), 20, 0, false, false, false));

                victim.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0F, 0.5F + HelperMethods.RANDOM.nextFloat() * 0.2F);

                if (!attacker.level.isClientSide) {
                    for (int i = 0; i < 8; i++) {
                        double offsetX = HelperMethods.RANDOM.nextGaussian() * 1.5D;
                        double offsetY = HelperMethods.RANDOM.nextGaussian() * 1.5D;
                        double offsetZ = HelperMethods.RANDOM.nextGaussian() * 1.5D;
                        ((ServerLevel) attacker.level).sendParticles(new LightningParticle.LightningParticleOptions(ParticleColors.getCursedEnergyColor(attacker), 0.5F),
                                victim.getX() + offsetX, victim.getY() + offsetY, victim.getZ() + offsetZ,
                                0, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
    }
}
