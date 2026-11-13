package com.minecolonies.core.entity.citizen.citizenhandlers;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHappinessHandler;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.ITimeBasedHappinessModifier;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.minecolonies.api.util.constant.HappinessConstants.MAX_HAPPINESS;
import static com.minecolonies.api.util.constant.HappinessConstants.TAG_NEW_HAPPINESS;

/**
 * Quidoms QD-109: happiness tracking removed. Handler kept as a no-op stub so accessors,
 * networking field order, and legacy call sites compile without individual happiness.
 */
public class CitizenHappinessHandler implements ICitizenHappinessHandler
{
    /**
     * Inert modifier so validators that cast to {@link ITimeBasedHappinessModifier} never NPE/CCE.
     * Days stay high enough that homelessness/unemployment/idle complaints never fire;
     * slepttonight days stay &gt; 0 so sleep complaints never fire.
     */
    private static final IHappinessModifier NOOP_MODIFIER = new NoOpModifier();

    /**
     * Fixed score used wherever callers still ask for happiness (skills, sounds, colony average).
     */
    public static final double FIXED_HAPPINESS = MAX_HAPPINESS;

    public CitizenHappinessHandler(final ICitizenData data)
    {
        // Quidoms: no default modifiers registered.
    }

    public CitizenHappinessHandler()
    {
        // Client empty ctor.
    }

    @Override
    public void addModifier(final IHappinessModifier modifier)
    {
        // no-op — Quidoms does not track individual happiness.
    }

    @Override
    public void resetModifier(final String name)
    {
        // no-op
    }

    @Override
    public IHappinessModifier getModifier(final String name)
    {
        return NOOP_MODIFIER;
    }

    @Override
    public void processDailyHappiness(final ICitizenData citizenData)
    {
        // no-op — no daily happiness processing or happiness chat.
    }

    @Override
    public double getHappiness(final IColony colony, final ICitizenData citizenData)
    {
        return FIXED_HAPPINESS;
    }

    @Override
    public void read(@NotNull final HolderLookup.Provider provider, final CompoundTag compound, final boolean persist)
    {
        // Ignore persisted/networked happiness modifiers.
    }

    @Override
    public void write(@NotNull final HolderLookup.Provider provider, final CompoundTag compound, final boolean persist)
    {
        // Keep TAG_NEW_HAPPINESS present (empty) for packet/NBT shape stability.
        compound.put(TAG_NEW_HAPPINESS, new ListTag());
    }

    @Override
    public List<String> getModifiers()
    {
        return Collections.emptyList();
    }

    // --------------------------------------------- Static Utility Methods --------------------------------------------- //
    // Kept for ModHappinessFactorTypeInitializer dynamic suppliers; unused by stubbed score.

    public static double getSocialModifier(final IColony colony)
    {
        return 1.0;
    }

    public static double getGuardFactor(final IColony colony)
    {
        double guards = 1;
        double workers = 1;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (citizen.getJob() instanceof AbstractJobGuard)
            {
                guards++;
            }
            else
            {
                workers++;
            }
        }
        return Math.min(guards / (workers * 2 / 3), 2);
    }

    public static double getFoodFactor(final ICitizenData citizenData)
    {
        final int homeBuildingLevel = citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent();
        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        if (homeBuildingLevel == 0 || !foodHandler.hasFullFoodHistory())
        {
            return 1.0;
        }

        final ICitizenFoodHandler.CitizenFoodStats happinessStats = foodHandler.getFoodHappinessStats();
        final double diversityFactor = Math.min(5.0, (double) happinessStats.diversity() / homeBuildingLevel);
        final double qualityFactor = Math.min(5.0, (double) happinessStats.quality() / (Math.max(1.0, homeBuildingLevel - 2.0)));
        return (diversityFactor + qualityFactor) / 2.0;
    }

    public static double getMysticalSiteFactor(final IColony colony)
    {
        return Math.max(1, ((double) colony.getServerBuildingManager().getMysticalSiteMaxBuildingLevel() / 2.0));
    }

    private static final class NoOpModifier implements ITimeBasedHappinessModifier
    {
        @Override
        public void dayEnd(final ICitizenData data)
        {
        }

        @Override
        public void reset()
        {
        }

        @Override
        public int getDays()
        {
            // > DEMANDS thresholds and > 0 for slepttonight validators.
            return 100;
        }

        @Override
        public String getId()
        {
            return "noop";
        }

        @Override
        public double getFactor(final ICitizenData citizenData)
        {
            return 1.0;
        }

        @Override
        public double getWeight()
        {
            return 1.0;
        }

        @Override
        public void read(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundTag, final boolean persist)
        {
        }

        @Override
        public void write(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundTag, final boolean persist)
        {
        }
    }
}
