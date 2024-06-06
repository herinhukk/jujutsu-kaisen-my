package radon.jujutsu_kaisen.entity.projectile;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import radon.jujutsu_kaisen.entity.registry.JJKEntities;

public class SharkShikigamiProjectile extends FishShikigamiProjectile {
    public SharkShikigamiProjectile(EntityType<? extends Projectile> pType, Level pLevel) {
        super(pType, pLevel);
    }

    public SharkShikigamiProjectile(LivingEntity owner, float power, float xOffset, float yOffset, LivingEntity target) {
        super(JJKEntities.SHARK_SHIKIGAMI.get(), owner, power, xOffset, yOffset, target);
    }

    public SharkShikigamiProjectile(LivingEntity owner, float power, float xOffset, float yOffset) {
        super(JJKEntities.SHARK_SHIKIGAMI.get(), owner, power, xOffset, yOffset);
    }
}
