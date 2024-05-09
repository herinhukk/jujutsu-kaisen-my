package radon.jujutsu_kaisen.ability.ten_shadows.summon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Summon;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.entity.registry.JJKEntities;
import radon.jujutsu_kaisen.entity.ten_shadows.ToadFusionEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.List;

public class ToadFusion extends Summon<ToadFusionEntity> {
    public ToadFusion() {
        super(ToadFusionEntity.class);
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

        IAbilityData data = cap.getAbilityData();

        if (data.hasToggled(this)) {
            return owner.level().getGameTime() % 20 != 0 || HelperMethods.RANDOM.nextInt(20) != 0;
        }
        return owner.level().getGameTime() % 20 == 0 && HelperMethods.RANDOM.nextInt(20) == 0;
    }

    @Override
    public List<EntityType<?>> getFusions() {
        return List.of(JJKEntities.TOAD.get(), JJKEntities.NUE.get());
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0.2F;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    protected boolean isBottomlessWell() {
        return true;
    }

    @Override
    public List<EntityType<?>> getTypes() {
        return List.of(JJKEntities.TOAD_FUSION.get());
    }

    @Override
    public boolean isTenShadows() {
        return true;
    }

    @Override
    protected ToadFusionEntity summon(LivingEntity owner) {
        return new ToadFusionEntity(owner, false);
    }
}
