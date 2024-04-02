package radon.jujutsu_kaisen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.block.entity.CurseSpawnerBlockEntity;
import radon.jujutsu_kaisen.block.entity.JJKBlockEntities;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.mission.IMissionData;
import radon.jujutsu_kaisen.data.mission.Mission;
import radon.jujutsu_kaisen.entity.curse.base.CursedSpirit;
import radon.jujutsu_kaisen.tags.JJKEntityTypeTags;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;

public class CurseSpawnerBlock extends Block implements EntityBlock {
    public static final BooleanProperty IS_BOSS = BooleanProperty.create("is_boss");

    public CurseSpawnerBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void tick(@NotNull BlockState pState, @NotNull ServerLevel pLevel, @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        IMissionData data = pLevel.getData(JJKAttachmentTypes.MISSION);

        if (!(pLevel.getBlockEntity(pPos) instanceof CurseSpawnerBlockEntity be)) return;

        Mission mission = data.getMission(be.getPos());

        List<EntityType<?>> spawnsPool = new ArrayList<>();
        List<EntityType<?>> bossesPool = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!type.is(JJKEntityTypeTags.SPAWNABLE_CURSE)) continue;

            if (!((type.create(pLevel)) instanceof CursedSpirit curse)) continue;

            int diff = mission.getGrade().toSorcererGrade().ordinal() - curse.getGrade().ordinal();

            if (diff >= 1 && diff < 3) {
                bossesPool.add(type);
                continue;
            }

            if (curse.getGrade().ordinal() > mission.getGrade().toSorcererGrade().ordinal()) continue;

            spawnsPool.add(type);
        }

        if (pState.getValue(IS_BOSS)) {
            if (!bossesPool.isEmpty()) {
                EntityType<?> type = bossesPool.get(HelperMethods.RANDOM.nextInt(bossesPool.size()));

                if (pLevel.noCollision(type.getAABB(pPos.getX() + 0.5D , pPos.getY(), pPos.getZ() + 0.5D))) {
                    type.spawn(pLevel, pPos, MobSpawnType.SPAWNER);
                }
            }
        } else {
            if (!spawnsPool.isEmpty()) {
                EntityType<?> type = spawnsPool.get(HelperMethods.RANDOM.nextInt(spawnsPool.size()));

                if (!pLevel.noCollision(type.getAABB(pPos.getX() + 0.5D, pPos.getY(), pPos.getZ() + 0.5D))) {
                    type.spawn(pLevel, pPos, MobSpawnType.SPAWNER);
                }
            }
        }

        pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 11);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(IS_BOSS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return JJKBlockEntities.CURSE_SPAWNER.get().create(pPos, pState);
    }
}