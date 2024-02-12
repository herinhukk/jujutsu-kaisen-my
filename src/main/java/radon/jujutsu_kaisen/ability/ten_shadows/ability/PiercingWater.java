package radon.jujutsu_kaisen.ability.ten_shadows.ability;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.data.ten_shadows.TenShadowsMode;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.effect.PiercingWaterEntity;
import radon.jujutsu_kaisen.util.HelperMethods;


public class PiercingWater extends Ability {


    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return HelperMethods.RANDOM.nextInt(3) == 0 && target != null && owner.hasLineOfSight(target);
    }

    @Override
    public boolean isValid(LivingEntity owner) {
        if (!super.isValid(owner)) return false;

        ISorcererData sorcererData = owner.getData(JJKAttachmentTypes.SORCERER);
        ITenShadowsData tenShadowsData = owner.getData(JJKAttachmentTypes.TEN_SHADOWS);

        if (sorcererData == null || tenShadowsData == null) return false;

        return !sorcererData.hasToggled(JJKAbilities.MAX_ELEPHANT.get()) &&
                tenShadowsData.hasTamed(JJKEntities.MAX_ELEPHANT.get()) &&
                tenShadowsData.getMode() == TenShadowsMode.ABILITY;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    @Override
    public void run(LivingEntity owner) {
        owner.swing(InteractionHand.MAIN_HAND);

        PiercingWaterEntity piercing = new PiercingWaterEntity(owner, this.getPower(owner));
        owner.level().addFreshEntity(piercing);
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 100.0F;
    }


}
