package radon.jujutsu_kaisen.data.chant;


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.registry.JJKAbilities;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ChantData implements IChantData {
    private final Map<Ability, LinkedHashSet<String>> chants;

    public ChantData() {
        this.chants = new LinkedHashMap<>();
    }

    @Override
    public void tick() {

    }

    @Override
    public void addChant(Ability ability, String chant) {
        if (!this.chants.containsKey(ability)) {
            this.chants.put(ability, new LinkedHashSet<>());
        }
        this.chants.get(ability).add(chant);
    }

    @Override
    public void addChants(Ability ability, LinkedHashSet<String> chants) {
        this.chants.put(ability, chants);
    }

    @Override
    public void removeChant(Ability ability, String chant) {
        if (this.chants.containsKey(ability)) {
            this.chants.get(ability).remove(chant);

            if (this.chants.get(ability).isEmpty()) {
                this.chants.remove(ability);
            }
        }
    }

    @Override
    public boolean hasChant(Ability ability, String chant) {
        List<String> chants = new ArrayList<>(this.chants.getOrDefault(ability, new LinkedHashSet<>()));

        if (chants.contains(chant)) return true;

        chants.add(chant);

        for (Map.Entry<Ability, LinkedHashSet<String>> entry : this.chants.entrySet()) {
            if (entry.getKey() == ability) continue;

            List<String> current = new ArrayList<>(entry.getValue());

            for (int i = 0; i < chants.size(); i++) {
                if (i > current.size() - 1) break;
                if (chants.get(i).equals(current.get(i))) return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasChants(Ability ability) {
        return this.chants.containsKey(ability);
    }

    @Override
    public boolean isChantsAvailable(Set<String> chants) {
        for (Set<String> entry : this.chants.values()) {
            if (entry.containsAll(chants)) return false;
        }
        return true;
    }

    @Override
    @Nullable
    public Ability getAbility(String chant) {
        for (Map.Entry<Ability, LinkedHashSet<String>> entry : this.chants.entrySet()) {
            if (entry.getValue().contains(chant)) return entry.getKey();
        }
        return null;
    }

    @Override
    @Nullable
    public Ability getAbility(Set<String> chants) {
        for (Map.Entry<Ability, LinkedHashSet<String>> entry : this.chants.entrySet()) {
            if (entry.getValue().equals(chants)) return entry.getKey();
        }
        return null;
    }

    @Override
    public Set<String> getFirstChants() {
        return this.chants.values().stream().map(set -> set.stream().findFirst().orElseThrow()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getFirstChants(Ability ability) {
        return this.chants.getOrDefault(ability, new LinkedHashSet<>());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag nbt = new CompoundTag();

        ListTag chantsTag = new ListTag();

        for (Map.Entry<Ability, LinkedHashSet<String>> entry : this.chants.entrySet()) {
            ResourceLocation key = JJKAbilities.getKey(entry.getKey());

            if (key == null) continue;

            CompoundTag data = new CompoundTag();
            data.putString("technique", key.toString());

            ListTag chants = new ListTag();

            for (String chant : entry.getValue()) {
                chants.add(StringTag.valueOf(chant));
            }
            data.put("entries", chants);

            chantsTag.add(data);
        }
        nbt.put("chants", chantsTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        this.chants.clear();

        for (Tag key : nbt.getList("chants", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) key;

            LinkedHashSet<String> chants = new LinkedHashSet<>();

            for (Tag entry : data.getList("entries", Tag.TAG_STRING)) {
                chants.add(entry.getAsString());
            }
            this.chants.put(JJKAbilities.getValue(new ResourceLocation(data.getString("technique"))), chants);
        }
    }
}
