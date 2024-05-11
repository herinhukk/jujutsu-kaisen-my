package radon.jujutsu_kaisen.client.visual.visual;

import radon.jujutsu_kaisen.cursed_technique.CursedTechnique;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.ability.registry.JJKAbilities;
import radon.jujutsu_kaisen.client.visual.ClientVisualHandler;
import radon.jujutsu_kaisen.client.visual.base.IVisual;
import radon.jujutsu_kaisen.util.HelperMethods;

public class AzureGlideVisual implements IVisual {
    @Override
    public boolean isValid(LivingEntity entity, ClientVisualHandler.ClientData client) {
        return client.channeled == JJKAbilities.AZURE_GLIDE.get();
    }

    @Override
    public void tick(LivingEntity entity, ClientVisualHandler.ClientData client) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) return;

        for (int i = 0; i < 12; i++) {
            double x = entity.getX() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * (entity.getBbWidth() * 1.5F) - entity.getLookAngle().scale(0.35D).x;
            double y = entity.getY() + HelperMethods.RANDOM.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (HelperMethods.RANDOM.nextDouble() - 0.5D) * (entity.getBbWidth() * 1.5F) - entity.getLookAngle().scale(0.35D).z;
            BlueFistsVisual.spawn(mc.level, new Vec3(x, y, z));
        }
    }
}
