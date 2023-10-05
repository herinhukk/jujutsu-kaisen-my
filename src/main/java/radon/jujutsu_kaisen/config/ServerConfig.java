package radon.jujutsu_kaisen.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.entity.JJKEntities;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServerConfig {
    private static final Map<ResourceLocation, Float> MAX_CURSED_ENERGY_NPC = new HashMap<>();

    static {
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.RIKA.getId(), Float.POSITIVE_INFINITY);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.MAHORAGA.getId(), 6000.0F);

        MAX_CURSED_ENERGY_NPC.put(JJKEntities.JOGO.getId(), 1000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.DAGON.getId(), 1000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.HANAMI.getId(), 1000.0F);

        MAX_CURSED_ENERGY_NPC.put(JJKEntities.SUKUNA_RYOMEN.getId(), 6000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.MEGUNA_RYOMEN.getId(), 6000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.HEIAN_SUKUNA.getId(), 6000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.SATORU_GOJO.getId(), 2000.0F);
        MAX_CURSED_ENERGY_NPC.put(JJKEntities.YUTA_OKKOTSU.getId(), 4000.0F);
    }

    private static final Map<SorcererGrade, Float> REQUIRED_EXPERIENCE = new HashMap<>();

    static {
        REQUIRED_EXPERIENCE.put(SorcererGrade.GRADE_4, 0.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.GRADE_3, 100.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.SEMI_GRADE_2, 300.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.GRADE_2, 500.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.SEMI_GRADE_1, 1000.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.GRADE_1, 1500.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.SPECIAL_GRADE_1, 2000.0F);
        REQUIRED_EXPERIENCE.put(SorcererGrade.SPECIAL_GRADE, 2500.0F);
    }

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> cursedEnergyAmounts;
    public final ForgeConfigSpec.DoubleValue cursedEnergyAmount;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> requiredExperience;
    public final ForgeConfigSpec.DoubleValue sorcererHealingAmount;
    public final ForgeConfigSpec.DoubleValue curseHealingAmount;
    public final ForgeConfigSpec.DoubleValue maximumExperienceAmount;
    public final ForgeConfigSpec.DoubleValue cursedObjectEnergyForGrade;

    public ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Server configuration settings")
                .push("server");

        this.cursedEnergyAmounts = builder.comment("Cursed energy amounts for NPCs (scales with experience)")
                .defineList("maxCursedEnergyNPC", MAX_CURSED_ENERGY_NPC
                        .entrySet()
                        .stream()
                        .map(x -> String.format(Locale.ROOT, "%s=%f", x.getKey().toString(), x.getValue()))
                        .toList(), obj -> obj instanceof String);
        this.cursedEnergyAmount = builder.comment("Cursed energy amount (scales with experience)")
                .defineInRange("maxCursedEnergyDefault", 500.0F, 0.0F, 100000.0F);
        this.requiredExperience = builder.comment("Required experience for grade")
                .defineList("requiredExperience", REQUIRED_EXPERIENCE
                        .entrySet()
                        .stream()
                        .map(x -> String.format(Locale.ROOT, "%s=%f", x.getKey().name(), x.getValue()))
                        .toList(), obj -> obj instanceof String);
        this.sorcererHealingAmount = builder.comment("The maximum amount of health sorcerers can heal per tick (scales with experience)")
                .defineInRange("sorcererHealingAmount", 0.05F, 0.0F, 100.0F);
        this.curseHealingAmount = builder.comment("The maximum amount of health curses can heal per tick (scales with experience)")
                .defineInRange("curseHealingAmount", 0.1F, 0.0F, 100.0F);
        this.maximumExperienceAmount = builder.comment("The maximum amount of experience one can obtain")
                .defineInRange("maximumExperienceAmount", 10000.0F, 1.0F, 100000.0F);
        this.cursedObjectEnergyForGrade = builder.comment("The amount of energy consuming cursed objects gives to curses (multiplied by the grade of the object)")
                .defineInRange("cursedObjectEnergyForGrade", 100.0F, 1.0F, 1000.0F);

        builder.pop();
    }

    public Map<ResourceLocation, Float> getCursedEnergyAmounts() {
        Map<ResourceLocation, Float> amounts = new HashMap<>();

        for (String line : this.cursedEnergyAmounts.get()) {
            String[] parts = line.split("=");
            String key = parts[0];
            float value = Float.parseFloat(parts[1]);
            amounts.put(new ResourceLocation(key), value);
        }
        return amounts;
    }

    public Map<SorcererGrade, Float> getRequiredExperience() {
        Map<SorcererGrade, Float> required = new HashMap<>();

        for (String line : this.requiredExperience.get()) {
            String[] parts = line.split("=");
            String key = parts[0];
            float value = Float.parseFloat(parts[1]);
            required.put(SorcererGrade.valueOf(key), value);
        }
        return required;
    }
}
