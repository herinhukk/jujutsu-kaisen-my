package radon.jujutsu_kaisen.ability.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.data.ten_shadows.TenShadowsMode;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.List;

public abstract class Summon<T extends Entity> extends Ability implements Ability.IToggled {
    private final Class<T> clazz;

    public Summon(Class<T> clazz) {
        this.clazz = clazz;
    }

    public abstract List<EntityType<?>> getTypes();

    protected boolean canTame() {
        return false;
    }

    public boolean canDie() {
        return false;
    }

    public boolean display() {
        return true;
    }

    public boolean isTotality() {
        return false;
    }

    public List<EntityType<?>> getFusions() {
        return List.of();
    }

    public abstract boolean isTenShadows();

    public boolean isSpecificFusion() {
        return true;
    }

    protected boolean shouldRemove() {
        return true;
    }

    protected boolean isBottomlessWell() {
        return false;
    }

    public boolean isTamed(LivingEntity owner) {
        if (!this.canTame()) return true;

        ITenShadowsData data = owner.getData(JJKAttachmentTypes.TEN_SHADOWS);

        for (EntityType<?> type : this.getTypes()) {
            if (data.hasTamed(type)) return true;
        }
        return false;
    }

    public boolean isDead(LivingEntity owner) {
        for (EntityType<?> type : this.getTypes()) {
            if (this.isDead(owner, type)) return true;
        }
        return false;
    }

    @Override
    public boolean isValid(LivingEntity owner) {
        if (!super.isValid(owner)) return false;

        ISorcererData sorcererData = owner.getData(JJKAttachmentTypes.SORCERER);
        ITenShadowsData tenShadowsData = owner.getData(JJKAttachmentTypes.TEN_SHADOWS);

        if (sorcererData == null || tenShadowsData == null) return false;

        if (!sorcererData.hasToggled(this) && this.isTenShadows()) {
            if (tenShadowsData.getMode() != TenShadowsMode.SUMMON) return false;

            for (Ability ability : sorcererData.getToggled()) {
                if (!(ability instanceof Summon<?> summon)) continue;

                for (EntityType<?> type : this.getTypes()) {
                    if (summon.getTypes().contains(type)) return false;
                    if (summon.getFusions().contains(type)) return false;
                }
                for (EntityType<?> fusion : this.getFusions()) {
                    if (!tenShadowsData.hasTamed(fusion)) return false;
                    if (summon.getTypes().contains(fusion)) return false;
                    if (summon.getFusions().contains(fusion)) return false;
                }
            }

            List<EntityType<?>> fusions = this.getFusions();

            int dead = 0;

            for (int i = 0; i < fusions.size(); i++) {
                if (this.isBottomlessWell()) {
                    if (tenShadowsData.isDead(fusions.get(i)) || !tenShadowsData.hasTamed(fusions.get(i))) {
                        return false;
                    }
                } else {
                    if (this.isSpecificFusion()) {
                        if ((i == 0) == tenShadowsData.isDead(fusions.get(i))) {
                            return false;
                        }
                    } else {
                        if (tenShadowsData.isDead(fusions.get(i))) {
                            dead++;
                        }
                    }
                }
            }

            if (!this.isSpecificFusion() && (dead == 0 || dead == fusions.size())) {
                return false;
            }
        }
        return !this.isDead(owner);
    }

    protected boolean isDead(LivingEntity owner, EntityType<?> type) {
        if (!this.canDie()) return false;

        ITenShadowsData data = owner.getData(JJKAttachmentTypes.TEN_SHADOWS);

        return data.isDead(type);
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public void run(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        if (this.getActivationType(owner) == ActivationType.INSTANT) {
            this.spawn(owner, false);
        }
    }

    @Override
    public Status isTriggerable(LivingEntity owner) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);

        if ((this.isTenShadows() || this.getActivationType(owner) == ActivationType.TOGGLED) && data.hasSummonOfClass(this.clazz)) {
            return Status.FAILURE;
        }
        return super.isTriggerable(owner);
    }

    @Override
    public Status isStillUsable(LivingEntity owner) {
        if (!owner.level().isClientSide) {
            ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);

            if (!data.hasSummonOfClass(this.clazz)) {
                return Status.FAILURE;
            }
        }
        return super.isStillUsable(owner);
    }

    protected abstract T summon(LivingEntity owner);

    public void spawn(LivingEntity owner, boolean clone) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);

        T summon = this.summon(owner);

        if (summon instanceof TenShadowsSummon) {
            ((TenShadowsSummon) summon).setClone(clone);
        }
        owner.level().addFreshEntity(summon);

        data.addSummon(summon);

        if (owner instanceof ServerPlayer player) {
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(data.serializeNBT()), player);
        }
    }

    @Override
    public void onEnabled(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        this.spawn(owner, false);
    }

    @Override
    public void onDisabled(LivingEntity owner) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);

        if (this.shouldRemove()) {
            data.unsummonByClass(this.clazz);
        } else {
            data.removeSummonByClass(this.clazz);
        }

        if (owner instanceof ServerPlayer player) {
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(data.serializeNBT()), player);
        }
    }

    @Override
    public float getRealCost(LivingEntity owner) {
        ISorcererData data = owner.getData(JJKAttachmentTypes.SORCERER);
        return this.isTenShadows() && this.isTamed(owner) && data.hasToggled(JJKAbilities.CHIMERA_SHADOW_GARDEN.get()) ? 0.0F : super.getRealCost(owner);
    }
}
