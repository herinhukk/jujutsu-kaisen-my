package radon.jujutsu_kaisen.ability.idle_transfiguration;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.registry.JJKAbilities;
import radon.jujutsu_kaisen.ability.IAttack;
import radon.jujutsu_kaisen.ability.IToggled;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.effect.registry.JJKEffects;
import radon.jujutsu_kaisen.entity.idle_transfiguration.base.TransfiguredSoulEntity;
import radon.jujutsu_kaisen.util.DamageUtil;

public class SoulDecimation extends Ability implements IToggled, IAttack {
    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return target != null;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    private void run(LivingEntity owner, LivingEntity target) {
        if (IdleTransfiguration.checkSukuna(owner, target)) return;

        MobEffectInstance existing = target.getEffect(JJKEffects.TRANSFIGURED_SOUL);

        int amplifier = 0;

        if (existing != null) {
            amplifier = existing.getAmplifier() + 2;
        }

        MobEffectInstance instance = new MobEffectInstance(JJKEffects.TRANSFIGURED_SOUL, 60 * 20, amplifier, false, true, true);
        target.addEffect(instance);
    }

    @Override
    public void run(LivingEntity owner) {

    }

    @Override
    public float getCost(LivingEntity owner) {
        return JJKAbilities.IDLE_TRANSFIGURATION.get().getCost(owner) * 2;
    }

    @Override
    public void onEnabled(LivingEntity owner) {
        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return;

        IAbilityData data = cap.getAbilityData();

        if (data.hasToggled(JJKAbilities.IDLE_TRANSFIGURATION.get())) {
            data.toggle(JJKAbilities.IDLE_TRANSFIGURATION.get());
        }
    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }

    @Override
    public boolean attack(DamageSource source, LivingEntity owner, LivingEntity target) {
        if (owner.level().isClientSide) return false;
        if (!DamageUtil.isMelee(source)) return false;
        if (!owner.getMainHandItem().isEmpty()) return false;

        this.run(owner, target);

        return true;
    }

    @EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            DamageSource source = event.getSource();

            if (!DamageUtil.isMelee(source)) return;

            if (!(source.getEntity() instanceof LivingEntity attacker)) return;

            IJujutsuCapability cap = attacker.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) return;

            IAbilityData data = cap.getAbilityData();

            if (!data.hasToggled(JJKAbilities.SOUL_DECIMATION.get())) return;

            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            MobEffectInstance existing = victim.getEffect(JJKEffects.TRANSFIGURED_SOUL);

            if (existing == null) return;

            int amplifier = existing.getAmplifier();

            float attackerStrength = IdleTransfiguration.calculateStrength(attacker);
            float victimStrength = IdleTransfiguration.calculateStrength(victim);

            int required = Math.round((victimStrength / attackerStrength) * 2);

            if (victim instanceof TransfiguredSoulEntity || amplifier >= required) {
                victim.hurt(JJKDamageSources.soulAttack(attacker), victim.getMaxHealth());
            }
        }
    }
}
