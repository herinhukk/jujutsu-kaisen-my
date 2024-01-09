package radon.jujutsu_kaisen.ability.boogie_woogie;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.entity.effect.CursedEnergyImbuedItem;

public class CEThrow extends Ability {
    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return false;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    @Override
    public void run(LivingEntity owner) {
        owner.swing(InteractionHand.MAIN_HAND);

        if (owner.level().isClientSide) return;

        ItemStack stack = owner.getItemInHand(InteractionHand.MAIN_HAND).copy();
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        CursedEnergyImbuedItem item = new CursedEnergyImbuedItem(owner, stack);
        owner.level().addFreshEntity(item);

        SwapOthers.setTarget(owner, item);
    }

    @Override
    public Status isTriggerable(LivingEntity owner) {
        if (owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return Status.FAILURE;
        }
        return super.isTriggerable(owner);
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 10.0F;
    }
}