package com.quidoms.colony;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

public final class ColonyPermissions {
    public enum Rank {
        OWNER,
        OFFICER,
        MEMBER,
        VISITOR
    }

    private UUID owner;
    private final Map<UUID, Rank> ranks = new HashMap<>();

    public ColonyPermissions(UUID owner) {
        this.owner = owner;
        this.ranks.put(owner, Rank.OWNER);
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        ranks.put(owner, Rank.OWNER);
    }

    public Rank rankOf(UUID player) {
        return ranks.getOrDefault(player, Rank.VISITOR);
    }

    public boolean canManage(UUID player) {
        Rank rank = rankOf(player);
        return rank == Rank.OWNER || rank == Rank.OFFICER;
    }

    public boolean canAccess(UUID player) {
        return rankOf(player) != Rank.VISITOR || owner.equals(player);
    }

    public void setRank(UUID player, Rank rank) {
        if (rank == Rank.VISITOR) {
            ranks.remove(player);
        } else {
            ranks.put(player, rank);
        }
    }

    public Map<UUID, Rank> ranks() {
        return Map.copyOf(ranks);
    }

    public static final Codec<ColonyPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ColonyPermissions::owner),
            Codec.unboundedMap(UUIDUtil.CODEC, Codec.STRING.xmap(ColonyPermissions::rankFromString, Rank::name))
                    .fieldOf("ranks").forGetter(p -> p.ranks)
    ).apply(instance, ColonyPermissions::fromCodec));

    private static ColonyPermissions fromCodec(UUID owner, Map<UUID, Rank> ranks) {
        ColonyPermissions permissions = new ColonyPermissions(owner);
        permissions.ranks.clear();
        permissions.ranks.putAll(ranks);
        if (!permissions.ranks.containsKey(owner)) {
            permissions.ranks.put(owner, Rank.OWNER);
        }
        return permissions;
    }

    private static Rank rankFromString(String value) {
        try {
            return Rank.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Rank.VISITOR;
        }
    }
}
