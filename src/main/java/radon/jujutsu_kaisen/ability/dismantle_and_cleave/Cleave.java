package radon.jujutsu_kaisen.ability.dismantle_and_cleave;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.DisplayType;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.sound.JJKSounds;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.concurrent.atomic.AtomicReference;

public class Cleave extends Ability implements Ability.IDomainAttack {
    public static final double RANGE = 30.0D;
    private static final float MAX_DAMAGE = 30.0F;

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return HelperMethods.RANDOM.nextInt(3) == 0 && target != null && owner.hasLineOfSight(target);
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    protected @Nullable LivingEntity getTarget(LivingEntity owner) {
        LivingEntity result = null;

        if (owner instanceof Player) {
            if (HelperMethods.getLookAtHit(owner, RANGE) instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
                if (owner.canAttack(target)) {
                    result = target;
                }
            }
        }
        return result;
    }

    private DamageSource getSource(LivingEntity owner, @Nullable DomainExpansionEntity domain) {
        return domain == null ? JJKDamageSources.jujutsuAttack(owner, this) : JJKDamageSources.indirectJujutsuAttack(domain, owner, this);
    }

    private static float getMaxDamage(LivingEntity owner) {
        AtomicReference<Float> result = new AtomicReference<>();

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap ->
                result.set(Math.min(MAX_DAMAGE, (cap.getGrade().ordinal() + 1) * 10.0F)));
        return result.get();
    }

    private static float calculateDamage(DamageSource source, LivingEntity owner, LivingEntity target) {
        float damage = target.getMaxHealth();
        float armor = (float) target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float f = 2.0F + toughness / 4.0F;
        float f1 = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        damage /= 1.0F - f1 / 25.0F;

        MobEffectInstance instance = target.getEffect(MobEffects.DAMAGE_RESISTANCE);

        if (instance != null) {
            int resistance = instance.getAmplifier();
            int i = (resistance + 1) * 5;
            int j = 25 - i;

            if (j == 0) {
                return damage;
            } else {
                float x = 25.0F / (float) j;
                damage = damage * x;
            }
        }

        int k = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);

        if (k > 0) {
            float f2 = Mth.clamp(k, 0.0F, 20.0F);
            damage /= 1.0F - f2 / 25.0F;
        }
        return Math.min(getMaxDamage(owner), damage);
    }

    @Override
    public void run(LivingEntity owner) {
        LivingEntity target = this.getTarget(owner);

        if (target != null) {
            owner.swing(InteractionHand.MAIN_HAND);
            this.perform(owner, null, target);
        }
    }

    @Override
    public float getCost(LivingEntity owner) {
        LivingEntity target = this.getTarget(owner);

        if (target != null && target.isAlive()) {
            return calculateDamage(this.getSource(owner, null), owner, target);
        }
        return 0.0F;
    }

    @Override
    public Status checkTriggerable(LivingEntity owner) {
        LivingEntity target = this.getTarget(owner);

        if (target == null) {
            return Status.FAILURE;
        }
        return super.checkTriggerable(owner);
    }

    @Override
    public void perform(LivingEntity owner, @Nullable DomainExpansionEntity domain, @Nullable LivingEntity target) {
        if (target != null && owner.level instanceof ServerLevel level) {
            AABB bounds = target.getBoundingBox();
            double minY = bounds.minY;
            double maxY = bounds.maxY;

            double randomY = minY + (maxY - minY) * HelperMethods.RANDOM.nextDouble();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), randomY, target.getZ(),
                    0, 0.0D, 0.0D, 0.0D, 0.0D);

            DamageSource source = this.getSource(owner, domain);
            float damage = calculateDamage(source, owner, target);
            owner.level.playSound(null, target.getX(), target.getY(), target.getZ(), JJKSounds.SLASH.get(), SoundSource.MASTER, 1.0F, 1.0F);
            target.hurt(source, damage);
        }
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public boolean isTechnique() {
        return true;
    }

    @Override
    public Classification getClassification() {
        return Classification.SLASH;
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.SCROLL;
    }
}
