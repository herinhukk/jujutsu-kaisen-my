package radon.jujutsu_kaisen.ability.shrine;


import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.IAttack;
import radon.jujutsu_kaisen.ability.IDomainAttack;
import radon.jujutsu_kaisen.ability.IToggled;
import radon.jujutsu_kaisen.ability.registry.JJKAbilities;
import radon.jujutsu_kaisen.client.particle.registry.JJKParticles;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.registry.JJKAttachmentTypes;
import radon.jujutsu_kaisen.entity.domain.DomainExpansionEntity;
import radon.jujutsu_kaisen.sound.JJKSounds;
import radon.jujutsu_kaisen.util.DamageUtil;
import radon.jujutsu_kaisen.util.EntityUtil;
import radon.jujutsu_kaisen.util.HelperMethods;

public class Cleave extends Ability implements IDomainAttack, IAttack, IToggled {
    public static final double RANGE = 30.0D;
    private static final float DAMAGE = 30.0F;

    private static DamageSource getSource(LivingEntity owner, @Nullable DomainExpansionEntity domain) {
        return domain == null ? JJKDamageSources.jujutsuAttack(owner, JJKAbilities.CLEAVE.get()) : JJKDamageSources.indirectJujutsuAttack(domain, owner, JJKAbilities.CLEAVE.get());
    }

    public static void perform(LivingEntity owner, LivingEntity target, @Nullable DomainExpansionEntity domain, DamageSource source, boolean instant) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        owner.level().playSound(null, target.getX(), target.getY(), target.getZ(), JJKSounds.SLASH.get(), SoundSource.MASTER,
                1.0F, 1.0F);

        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return;

        IAbilityData data = cap.getAbilityData();

        for (int i = 1; i <= 20; i++) {
            data.delayTickEvent(() -> {
                if (target.isDeadOrDying()) return;

                level.sendParticles(JJKParticles.SLASH.get(), target.getX(), target.getY(), target.getZ(), 0, target.getId(),
                        0.0D, 0.0D, 1.0D);

                Vec3 center = target.position().add(0.0D, target.getBbHeight() / 2, 0.0D);
                Vec3 offset = center.add((HelperMethods.RANDOM.nextDouble() - 0.5D) * target.getBbWidth(),
                        (HelperMethods.RANDOM.nextDouble() - 0.5D) * target.getBbHeight(),
                        (HelperMethods.RANDOM.nextDouble() - 0.5D) * target.getBbWidth());
                ((ServerLevel) owner.level()).sendParticles(ParticleTypes.EXPLOSION, offset.x, offset.y, offset.z, 0, 1.0D, 0.0D, 0.0D, 1.0D);
            }, i);
        }

        for (int i = 1; i <= 10; i++) {
            data.delayTickEvent(() -> {
                if (target.isDeadOrDying()) return;

                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), JJKSounds.SLASH.get(), SoundSource.MASTER,
                        domain == null ? 1.0F : 0.05F / 10, 1.0F);
            }, i * 2);
        }

        data.delayTickEvent(() -> {
            float power = domain == null ? Ability.getOutput(JJKAbilities.CLEAVE.get(), owner) : Ability.getOutput(JJKAbilities.CLEAVE.get(), owner) *
                    (instant ? 0.5F : 1.0F);

            float damage = EntityUtil.calculateDamage(source, target);
            damage = Math.min(DAMAGE * power, damage);

            boolean success = target.hurt(source, damage);

            if (!success || !(target instanceof Mob) && !(target instanceof Player)) return;

            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    JJKSounds.CLEAVE.get(), SoundSource.MASTER, 1.0F, 1.0F);

            target.setData(JJKAttachmentTypes.CLEAVED, false);
        }, 20);
    }

    public static void perform(LivingEntity owner, LivingEntity target, @Nullable DomainExpansionEntity domain, boolean instant) {
        DamageSource source = getSource(owner, domain);
        perform(owner, target, domain, source, instant);
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return target != null;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public void run(LivingEntity owner) {

    }

    @Override
    public float getCost(LivingEntity owner) {
        return 100.0F;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public Classification getClassification() {
        return Classification.SLASHING;
    }

    @Override
    public void performEntity(LivingEntity owner, LivingEntity target, DomainExpansionEntity domain, boolean instant) {
        perform(owner, target, domain, instant);
    }

    @Override
    public boolean attack(DamageSource source, LivingEntity owner, LivingEntity target) {
        if (owner.level().isClientSide) return false;
        if (!DamageUtil.isMelee(source)) return false;

        perform(owner, target, null, false);

        return true;
    }

    @Override
    public void onEnabled(LivingEntity owner) {

    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }
}
