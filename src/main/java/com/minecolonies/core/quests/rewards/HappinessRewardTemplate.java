package com.minecolonies.core.quests.rewards;

import com.google.gson.JsonObject;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.quests.IQuestInstance;
import com.minecolonies.api.quests.IQuestRewardTemplate;
import net.minecraft.world.entity.player.Player;

import static com.minecolonies.api.quests.QuestParseConstant.*;

/**
 * Quest reward type kept for JSON compatibility. Quidoms QD-109: happiness system removed — apply is a no-op.
 */
public class HappinessRewardTemplate implements IQuestRewardTemplate
{
    private final int qty;
    private final int days;
    private final int target;

    public HappinessRewardTemplate(final int target, final int qty, final int days)
    {
        this.target = target;
        this.qty = qty;
        this.days = days;
    }

    public static IQuestRewardTemplate createReward(final JsonObject jsonObject)
    {
        final JsonObject details = jsonObject.getAsJsonObject(DETAILS_KEY);
        final int target = details.get(TARGET_KEY).getAsInt();
        final int qty = details.get(QUANTITY_KEY).getAsInt();
        final int days = details.get(DAYS_KEY).getAsInt();
        return new HappinessRewardTemplate(target, qty, days);
    }

    @Override
    public void applyReward(final IColony colony, final Player player, final IQuestInstance colonyQuest)
    {
        // QD-109: no-op (individual happiness removed).
    }
}
