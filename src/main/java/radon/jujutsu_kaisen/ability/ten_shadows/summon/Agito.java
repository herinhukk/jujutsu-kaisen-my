package radon.jujutsu_kaisen.ability.ten_shadows.summon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.ten_shadows.AgitoEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.List;

public class Agito extends Summon<AgitoEntity> {
    public Agito() {
        super(AgitoEntity.class);
    }

    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        if (target == null || target.isDeadOrDying()) return false;

        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return false;

        ISorcererData data = cap.getSorcererData();

        if (data.hasToggled(this)) {
            return owner.level().getGameTime() % 20 != 0 || HelperMethods.RANDOM.nextInt(10) != 0;
        }
        return HelperMethods.RANDOM.nextInt(10) == 0;
    }

    @Override
    public boolean isTotality() {
        return true;
    }

    @Override
    public List<EntityType<?>> getFusions() {
        return List.of(JJKEntities.NUE.get(), JJKEntities.GREAT_SERPENT.get(), JJKEntities.TRANQUIL_DEER.get());
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0.3F;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public List<EntityType<?>> getTypes() {
        return List.of(JJKEntities.AGITO.get());
    }

    @Override
    public boolean isTenShadows() {
        return true;
    }

    @Override
    protected AgitoEntity summon(LivingEntity owner) {
        return new AgitoEntity(owner);
    }
}
