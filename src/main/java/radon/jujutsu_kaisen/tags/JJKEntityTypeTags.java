package radon.jujutsu_kaisen.tags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import radon.jujutsu_kaisen.JujutsuKaisen;

public class JJKEntityTypeTags {
    public static final TagKey<EntityType<?>> UNIQUE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(JujutsuKaisen.MOD_ID, "unique"));
}
