package radon.jujutsu_kaisen.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ExplosionHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.client.particle.BetterSmokeParticle;
import radon.jujutsu_kaisen.client.particle.FireParticle;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.client.particle.TravelParticle;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.sound.JJKSounds;
import radon.jujutsu_kaisen.util.HelperMethods;

public class FireArrowProjectile extends JujutsuProjectile {
    private static final float DAMAGE = 25.0F;
    private static final float SPEED = 5.0F;
    private static final float EXPLOSIVE_POWER = 2.5F;
    private static final float MAX_EXPLOSION = 15.0F;
    public static final int DELAY = 20;
    public static final int STILL_FRAMES = 2;
    public static final int STARTUP_FRAMES = 4;
    private static final double OFFSET = 2.0D;

    public int animation;

    public FireArrowProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FireArrowProjectile(LivingEntity owner, float power) {
        super(JJKEntities.FIRE_ARROW.get(), owner.level(), owner, power);

        Vec3 look = owner.getLookAngle();
        Vec3 spawn = new Vec3(owner.getX(), owner.getEyeY() - (this.getBbHeight() / 2.0F), owner.getZ()).add(look.scale(OFFSET));
        this.moveTo(spawn.x, spawn.y, spawn.z, owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        super.onHitEntity(pResult);

        Entity entity = pResult.getEntity();

        if (this.getOwner() instanceof LivingEntity owner) {
            if (!(entity instanceof LivingEntity living) || !owner.canAttack(living) || entity == owner) return;

            entity.hurt(JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.FIRE_ARROW.get()), DAMAGE * this.getPower());
        }
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);

        if (this.level().isClientSide) return;

        this.playSound(JJKSounds.FLAME_EXPLOSION.get(), 3.0F, 1.0F);

        Vec3 location = result.getLocation();

        Vec3 center = new Vec3(location.x, location.y + (this.getBbHeight() / 2.0F), location.z);

        int pillarCount = (int) (this.getFlamePillarRadius() * Math.PI * 2) * 8;

        for (int i = 0; i < pillarCount; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = this.random.nextDouble() * Math.PI;

            double xOffset = this.getFlamePillarRadius() * Math.sin(phi) * Math.cos(theta);
            double yOffset = this.getFlamePillarRadius() * Math.sin(phi) * Math.sin(theta);
            double zOffset = this.getFlamePillarRadius() * Math.cos(phi);

            int lifetime = 2 * 20;

            for (int j = 0; j < 2; j++) {
                double x = center.x + xOffset * this.getFlamePillarRadius() * this.random.nextDouble();
                double y = center.y + yOffset * (this.getFlamePillarRadius() * 10.0F) * this.random.nextDouble();
                double z = center.z + zOffset * this.getFlamePillarRadius() * this.random.nextDouble();

                Vec3 start = new Vec3(center.x + xOffset * (this.getFlamePillarRadius() * 0.1F), center.y, center.z + zOffset * (this.getFlamePillarRadius() * 0.1F));
                Vec3 end = new Vec3(x, y, z);
                Vec3 speed = start.subtract(end).scale((double) 1 / lifetime * 2.0D);

                switch (j) {
                    case 0:
                        HelperMethods.sendParticles((ServerLevel) this.level(), new FireParticle.FireParticleOptions(this.getFlamePillarRadius() * 0.3F, true, lifetime), true,
                                start.x, start.y, start.z, speed.x, speed.y, speed.z);
                        break;
                    case 1:
                        HelperMethods.sendParticles((ServerLevel) this.level(), new BetterSmokeParticle.BetterSmokeParticleOptions(this.getFlamePillarRadius() * 0.3F, lifetime), true,
                                start.x, start.y, start.z, speed.x, speed.y, speed.z);
                        break;
                }
            }
        }

        int shockwaveCount = (int) (this.getFlamePillarRadius() * 2 * Math.PI * 2) * 8;

        for (int i = 0; i < shockwaveCount; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = this.random.nextDouble() * Math.PI;

            double xOffset = this.getFlamePillarRadius() * 2 * Math.sin(phi) * Math.cos(theta);
            double zOffset = this.getFlamePillarRadius() * 2 * Math.cos(phi);

            int lifetime = 2 * 20;

            for (int j = 0; j < 2; j++) {
                double x = center.x + xOffset * this.getFlamePillarRadius() * 2 * this.random.nextDouble();
                double z = center.z + zOffset * this.getFlamePillarRadius() * 2 * this.random.nextDouble();

                Vec3 start = new Vec3(center.x + xOffset * (this.getFlamePillarRadius() * 0.1F), center.y, center.z + zOffset * (this.getFlamePillarRadius() * 0.1F));
                Vec3 end = new Vec3(x, start.y, z);
                Vec3 speed = start.subtract(end).scale((double) 1 / lifetime * 2.0D);

                switch (j) {
                    case 0:
                        HelperMethods.sendParticles((ServerLevel) this.level(), new FireParticle.FireParticleOptions(this.getFlamePillarRadius() * 0.3F, true, lifetime), true,
                                start.x, start.y, start.z, speed.x, speed.y, speed.z);
                        break;
                    case 1:
                        HelperMethods.sendParticles((ServerLevel) this.level(), new BetterSmokeParticle.BetterSmokeParticleOptions(this.getFlamePillarRadius() * 0.3F, lifetime), true,
                                start.x, start.y, start.z, speed.x, speed.y, speed.z);
                        break;
                }
            }
        }

        if (this.getOwner() instanceof LivingEntity owner) {
            ExplosionHandler.spawn(this.level().dimension(), location, this.getExplosionRadius(), 2 * 20, this.getPower() * 0.5F,
                    owner, JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.FIRE_ARROW.get()), true);
        }
        this.discard();
    }

    private float getFlamePillarRadius() {
        return this.getExplosionRadius() * 0.25F;
    }

    private float getExplosionRadius() {
        return Math.min(MAX_EXPLOSION, EXPLOSIVE_POWER * this.getPower());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount >= DELAY) {
            if (this.animation < STILL_FRAMES) {
                this.animation++;
            } else {
                this.animation = 0;
            }
        } else {
            if (this.tickCount > 0 && this.tickCount % (DELAY / STARTUP_FRAMES) == 0) {
                if (this.animation < STARTUP_FRAMES) {
                    this.animation++;
                }
            }
        }

        if (this.getOwner() instanceof LivingEntity owner) {
            for (int i = 0; i < 2; i++) {
                Vec3 dir = owner.getLookAngle().reverse().scale(0.1D);
                double dx = dir.x + ((this.random.nextDouble() - 0.5D) * 0.5D);
                double dy = dir.y + ((this.random.nextDouble() - 0.5D) * 0.5D);
                double dz = dir.z + ((this.random.nextDouble() - 0.5D) * 0.5D);

                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ(), dx, dy, dz);
            }

            if (this.tickCount < DELAY) {
                Vec3 look = owner.getLookAngle();
                double d0 = look.horizontalDistance();
                this.setYRot((float) (Mth.atan2(look.x, look.z) * (double) (180.0F / (float) Math.PI)));
                this.setXRot((float) (Mth.atan2(look.y, d0) * (double) (180.0F / (float) Math.PI)));
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();

                if (!owner.isAlive()) {
                    this.discard();
                } else {
                    if (this.tickCount % 5 == 0) {
                        owner.swing(InteractionHand.MAIN_HAND);
                    }
                    Vec3 spawn = new Vec3(owner.getX(), owner.getEyeY() - (this.getBbHeight() / 2.0F), owner.getZ()).add(look.scale(OFFSET));
                    this.setPos(spawn.x, spawn.y, spawn.z);
                }
            } else if (this.tickCount == DELAY) {
                this.setDeltaMovement(owner.getLookAngle().scale(SPEED));
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.MASTER, 1.0F, 1.0F);
            }
        }
    }
}
