package radon.jujutsu_kaisen.ability.mimicry;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.entity.curse.RikaEntity;
import radon.jujutsu_kaisen.entity.effect.PureLoveBeamEntity;

public class CommandPureLove extends Ability {
    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);
        
        if (data == null) return false;

        return target != null && !target.isDeadOrDying() && (data.getType() == JujutsuType.CURSE || data.isUnlocked(JJKAbilities.RCT1.get()) ? owner.getHealth() / owner.getMaxHealth() < 0.9F : owner.getHealth() / owner.getMaxHealth() < 0.4F);
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    @Override
    public void run(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);
        

        RikaEntity rika = data.getSummonByClass(RikaEntity.class);

        if (rika == null) return;

        if (JJKAbilities.SHOOT_PURE_LOVE.get().getStatus(rika) != Status.SUCCESS) return;

        rika.setOpen(PureLoveBeamEntity.CHARGE + PureLoveBeamEntity.DURATION + PureLoveBeamEntity.FRAMES);
    }

    @Override
    public boolean isValid(LivingEntity owner) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);

        if (data == null) return false;

        return data.hasToggled(JJKAbilities.RIKA.get()) && super.isValid(owner);
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0;
    }
}
