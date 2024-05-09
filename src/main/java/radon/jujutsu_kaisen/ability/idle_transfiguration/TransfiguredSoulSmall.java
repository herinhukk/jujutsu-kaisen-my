package radon.jujutsu_kaisen.ability.idle_transfiguration;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.idle_transfiguration.base.TransfiguredSoul;
import radon.jujutsu_kaisen.entity.registry.JJKEntities;
import radon.jujutsu_kaisen.entity.idle_transfiguration.TransfiguredSoulSmallEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.List;

public class TransfiguredSoulSmall extends TransfiguredSoul<TransfiguredSoulSmallEntity> {
    public TransfiguredSoulSmall() {
        super(TransfiguredSoulSmallEntity.class);
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        if (target == null) return false;

        return HelperMethods.RANDOM.nextInt(20) == 0;
    }

    @Override
    public List<EntityType<?>> getTypes() {
        return List.of(JJKEntities.TRANSFIGURED_SOUL_SMALL.get());
    }

    @Override
    public boolean isTenShadows() {
        return false;
    }

    @Override
    protected TransfiguredSoulSmallEntity summon(LivingEntity owner) {
        return new TransfiguredSoulSmallEntity(owner);
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 10.0F;
    }

    @Override
    public int getSoulCost() {
        return 1;
    }
}
