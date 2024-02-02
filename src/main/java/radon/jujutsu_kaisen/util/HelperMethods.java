package radon.jujutsu_kaisen.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.block.entity.DomainBlockEntity;
import radon.jujutsu_kaisen.config.ConfigHolder;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.entity.curse.KuchisakeOnnaEntity;
import radon.jujutsu_kaisen.entity.projectile.ThrownChainProjectile;
import radon.jujutsu_kaisen.entity.projectile.base.JujutsuProjectile;
import radon.jujutsu_kaisen.item.JJKItems;

import java.util.*;

public class HelperMethods {
    public static final RandomSource RANDOM = RandomSource.createThreadSafe();

    public static boolean isBlockable(LivingEntity target, Projectile projectile) {
        if (projectile.getOwner() == target) return false;

        if (projectile instanceof ThrownChainProjectile chain) {
            if (chain.getStack().is(JJKItems.INVERTED_SPEAR_OF_HEAVEN.get())) return false;
        }

        for (KuchisakeOnnaEntity curse : target.level().getEntitiesOfClass(KuchisakeOnnaEntity.class, AABB.ofSize(target.position(),
                KuchisakeOnnaEntity.RANGE, KuchisakeOnnaEntity.RANGE, KuchisakeOnnaEntity.RANGE))) {
            Optional<UUID> identifier = curse.getCurrent();
            if (identifier.isEmpty()) continue;
            if (identifier.get() == target.getUUID() && projectile.getOwner() == curse) return false;
        }

        if (projectile instanceof JujutsuProjectile jujutsu) {
            return jujutsu.isDomain();
        }
        return true;
    }

    public static boolean isBlockable(LivingEntity target, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

        if (source.getDirectEntity() instanceof Projectile projectile && !isBlockable(target, projectile)) return false;

        if (source.getDirectEntity() instanceof DomainExpansionEntity) return false;

        if (source.getEntity() == target) return false;

        if (source.getEntity() instanceof LivingEntity living && HelperMethods.isMelee(source)) {
            return !JJKAbilities.hasToggled(living, JJKAbilities.DOMAIN_AMPLIFICATION.get());
        }
        return true;
    }

    public static boolean isMelee(DamageSource source) {
        return !source.isIndirect() && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK) || source.is(JJKDamageSources.SPLIT_SOUL_KATANA)) ||
                source instanceof JJKDamageSources.JujutsuDamageSource jujutsu && jujutsu.getAbility() != null && jujutsu.getAbility().isMelee();
    }

    public static int getRGB24(Vector3f rgb) {
        return FastColor.ARGB32.color(255, Math.round(rgb.x * 255.0F), Math.round(rgb.y * 255.0F), Math.round(rgb.z * 255.0F));
    }

    public static boolean isDestroyable(BlockGetter getter, @Nullable LivingEntity source, BlockPos pos) {
        if (!ConfigHolder.SERVER.destruction.get()) return false;

        if (source != null && !(source instanceof Player) && !source.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;

        BlockState state = getter.getBlockState(pos);
        boolean destroyable = !state.isAir() && state.getBlock().defaultDestroyTime() > Block.INDESTRUCTIBLE;

        if (!destroyable && source != null && source.level() instanceof ServerLevel level && getter.getBlockEntity(pos) instanceof DomainBlockEntity be) {
            UUID identifier = be.getIdentifier();
            destroyable = identifier == null || !(level.getEntity(identifier) instanceof DomainExpansionEntity domain) ||
                    !domain.isInsideBarrier(source.blockPosition());
        }
        return destroyable;
    }

    public static <E> E getWeightedRandom(Map<E, Double> weights, RandomSource random) {
        E result = null;
        double bestValue = Double.MAX_VALUE;

        for (E element : weights.keySet()) {
            double value = -Math.log(random.nextDouble()) / weights.get(element);

            if (value < bestValue) {
                bestValue = value;
                result = element;
            }
        }

        return result;
    }

    public static int getLevenshteinDistance(String x, String y) {
        int m = x.length();
        int n = y.length();

        int[][] T = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            T[i][0] = i;
        }

        for (int j = 1; j <= n; j++) {
            T[0][j] = j;
        }

        int cost;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                cost = x.charAt(i - 1) == y.charAt(j - 1) ? 0: 1;
                T[i][j] = Integer.min(Integer.min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }
        return T[m][n];
    }

    public static float strcmp(String x, String y) {
        float max = Float.max(x.length(), y.length());

        if (max > 0) {
            return 1.0F - ((max - getLevenshteinDistance(x, y)) / max);
        }
        return 0.0F;
    }

    public static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[RANDOM.nextInt(clazz.getEnumConstants().length)];
    }

    public static <T extends Enum<T>> T randomEnum(Class<T> clazz, Set<T> excluded) {
        if (!excluded.isEmpty()) {
            EnumSet<T> available = EnumSet.complementOf(EnumSet.copyOf(excluded));

            if (!available.isEmpty()) {
                return (T) available.toArray()[RANDOM.nextInt(available.size())];
            }
        }
        return clazz.getEnumConstants()[RANDOM.nextInt(clazz.getEnumConstants().length)];
    }

    public static int toRGB24(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF));
    }
}
