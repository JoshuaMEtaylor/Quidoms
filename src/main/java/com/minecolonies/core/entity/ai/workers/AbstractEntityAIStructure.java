package com.minecolonies.core.entity.ai.workers;

import com.ldtteam.structurize.blocks.schematic.BlockFluidSubstitution;
import com.ldtteam.structurize.placement.AbstractBlueprintIterator;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.workorders.IBuilderWorkOrder;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AIEventTarget;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.IBuilderUndestroyable;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule;
import com.minecolonies.core.colony.buildings.utils.BuilderBucket;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.colony.jobs.AbstractJobStructure;
import com.minecolonies.core.entity.ai.workers.util.BuildingProgressStage;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import com.minecolonies.core.tileentities.TileEntityDecorationController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_PLACE_SPEED;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner.FILL_BLOCK;
import static com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure.ItemCheckResult.*;
import static com.minecolonies.core.entity.ai.workers.util.BuildingProgressStage.*;

/**
 * This base ai class is used by ai's who need to build entire structures. These structures have to be supplied as schematics files.
 * <p>
 * Once an ai starts building a structure, control over it is only given back once that is done.
 * <p>
 * If the ai resets, the structure is gone, so just restart building and no progress will be reset.
 *
 * @param <J> the job type this AI has to do.
 */
public abstract class AbstractEntityAIStructure<J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> extends AbstractEntityAIInteract<J, B>
{
    /**
     * Building block delay (Quidoms: near-instant place cadence; research can still shave ticks).
     */
    protected static final int BUILD_BLOCK_DELAY = 1;

    /**
     * Soft leaves/grass/flowers cleared per structure tick.
     * QD-108: keep small — 24 removeBlocks/tick hitch the server like a section clear.
     */
    protected static final int SOFT_CLEAR_BATCH = 2;

    /**
     * 2D reach for instant soft-obstruction clears without repathing.
     */
    protected static final int SOFT_CLEAR_REACH = 12;

    /**
     * Quidoms: max hard-break instructions committed per scan (partial prefetch).
     * Selection used to re-run StructurePlacer after every mine (~1s gap); queue removes that.
     */
    protected static final int ACTION_QUEUE_SIZE = 16;

    /**
     * Quidoms: iterator cells visited while filling the action queue in one scan.
     */
    protected static final int ACTION_PREFETCH_SCAN_BUDGET = 256;

    /**
     * The current structure task to be build.
     */
    protected Tuple<StructurePlacer, BuildingStructureHandler<J, B>> structurePlacer;

    /**
     * Quidoms: predetermined hard-break path (scan → commit → execute successively).
     */
    private final Deque<QueuedBreakAction> actionQueue = new ArrayDeque<>();

    /**
     * If the structure state is currently reached limit rather than block placement.
     */
    protected boolean limitReached = false;

    /**
     * We're waiting for the blueprint to finish loading.
     */
    protected boolean loadingBlueprint = false;

    /**
     * LOAD_STRUCTURE ticks spent with {@link #loadingBlueprint} true — reset if stuck.
     */
    protected int blueprintLoadWaitTicks = 0;

    /**
     * Whether the citizen already announced BUILD_START for the current WO claim.
     */
    protected boolean buildStartAnnounced = false;

    /**
     * Consecutive BUILDING_STEP ticks where walk/ready returned false (QD-101 walk harden).
     */
    protected int walkNotReadyStreak = 0;

    /**
     * Rate-limit in-game wall chat so RECALC/path spam does not flood (QD-102).
     */
    private long lastWallChatMs = 0L;
    private String lastWallChatKey = "";

    /**
     * Different item check result possibilities.
     */
    public enum ItemCheckResult
    {
        FAIL,
        SUCCESS,
        RECALC
    }

    /**
     * Predicate defining things we don't want the builders to ever touch.
     */
    protected TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> DONT_TOUCH_PREDICATE = (info, worldPos, handler) ->
    {
        final BlockState worldState = handler.getWorld().getBlockState(worldPos);

        return worldState.getBlock() instanceof IBuilderUndestroyable
                 || worldState.getBlock() == Blocks.BEDROCK
                 || (info.getBlockInfo().getState().getBlock() instanceof AbstractBlockHut && handler.getCenterPos().equals(worldPos)
                       && worldState.getBlock() instanceof AbstractBlockHut);
    };

    /**
     * Position where the Builders constructs from.
     */
    protected BlockPos workFrom;

    /**
     * Previous Position where the Builders constructed from.
     */
    protected BlockPos prevBlockPosition;

    /**
     * Block to mine.
     */
    protected BlockPos blockToMine;

    /**
     * Block to go to when building
     */
    protected BlockPos gotoPos = null;

    /**
     * The id in the list of the last picked up item.
     */
    private int pickUpCount = 0;

    /**
     * Creates this ai base class and set's up important things.
     * <p>
     * Always use this constructor!
     *
     * @param job the job class of the ai using this base class.
     */
    protected AbstractEntityAIStructure(@NotNull final J job)
    {
        super(job);
        this.registerTargets(
          /*
           * Check if tasks should be executed.
           */
          new AIEventTarget(AIBlockingEventType.STATE_BLOCKING, this::checkIfCanceled, IDLE, 1),
          /*
           * Select the appropriate State to do next.
           */
          new AITarget(LOAD_STRUCTURE, this::loadRequirements, 5),
          /*
           * Select the appropriate State to do next.
           */
          new AITarget(START_BUILDING, this::startBuilding, 1),
          /*
           * Select the appropriate State to do next.
           */
          // Quidoms: hard-break cadence matches place/clear loop (was 10).
          new AITarget(MINE_BLOCK, this::doMining, 1),
          /*
           * Check if we have to build something.
           */
          new AITarget(IDLE, START_WORKING, 10),
          /*
           * Start working at the building.
           */
          new AITarget(START_WORKING, this::startWorkingAtOwnBuilding, TICKS_SECOND),
          /*
           * Build the structure and foundation of the building.
           */
          // Quidoms: structure tick every AI pulse — selection delay lived here (was STANDARD_DELAY).
          new AITarget(BUILDING_STEP, this::structureStep, 1),
          /*
           * Finalize the building and give back control to the ai.
           */
          new AITarget(COMPLETE_BUILD, this::completeBuild, STANDARD_DELAY),
          new AITarget(PICK_UP, this::pickUpMaterial, 5)
        );
    }

    /**
     * One committed hard-break instruction from a structure scan.
     */
    private static final class QueuedBreakAction
    {
        private final BlockPos worldPos;
        private final BlockPos iteratorPos;
        private final BuildingProgressStage stage;

        private QueuedBreakAction(final BlockPos worldPos, final BlockPos iteratorPos, final BuildingProgressStage stage)
        {
            this.worldPos = worldPos;
            this.iteratorPos = iteratorPos;
            this.stage = stage;
        }
    }

    /**
     * Start working at own building. Override for worker specific implementations.
     * @return next state.
     */
    protected IAIState startWorkingAtOwnBuilding()
    {
        if (isThereAStructureToBuild())
        {
            return START_BUILDING;
        }
        return IDLE;
    }

    /**
     * State to pick up material before going back to work.
     *
     * @return the next state to go to.
     */
    public IAIState pickUpMaterial()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint())
        {
            return IDLE;
        }

        if (structurePlacer.getB().getStage() == null || structurePlacer.getB().getStage() == BuildingProgressStage.CLEAR)
        {
            pickUpCount = 0;
            return START_WORKING;
        }

        final List<Tuple<Predicate<ItemStack>, Integer>> neededItemsList = new ArrayList<>();

        final BuilderBucket neededRessourcesMap = building.getRequiredResources();
        final BuildingResourcesModule module = building.getFirstModuleOccurance(BuildingResourcesModule.class);
        if (neededRessourcesMap != null)
        {
            for (final Map.Entry<String, Integer> entry : neededRessourcesMap.getResourceMap().entrySet())
            {
                final BuildingBuilderResource res = module.getResourceFromIdentifier(entry.getKey());
                if (res != null)
                {
                    int amount = entry.getValue();
                    neededItemsList.add(new Tuple<>(itemstack -> ItemStackUtils.compareItemStacksIgnoreStackSize(res.getItemStack(), itemstack, true, true), amount));
                }
            }
        }

        if (neededItemsList.size() <= pickUpCount || InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= MIN_OPEN_SLOTS)
        {
            building.checkOrRequestBucket(building.getRequiredResources(), worker.getCitizenData());
            building.checkOrRequestBucket(building.getNextBucket(), worker.getCitizenData());
            pickUpCount = 0;
            return START_WORKING;
        }

        needsCurrently = neededItemsList.get(pickUpCount);
        pickUpCount++;

        if (InventoryUtils.hasItemInProvider(building.getTileEntity(), needsCurrently.getA()))
        {
            return GATHERING_REQUIRED_MATERIALS;
        }

        return PICK_UP;
    }

    /**
     * Completition logic.
     *
     * @return the final state after completition.
     */
    protected IAIState completeBuild()
    {
        incrementActionsDoneAndDecSaturation();
        executeSpecificCompleteActions();
        worker.getCitizenExperienceHandler().addExperience(XP_EACH_BUILDING);
        resetCurrentStructure();
        return IDLE;
    }

    /**
     * Start building this StructureIterator.
     * <p>
     * Will determine where to start.
     *
     * @return the new State to start in.
     */
    @NotNull
    protected IAIState startBuilding()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint())
        {
            return LOAD_STRUCTURE;
        }
        return BUILDING_STEP;
    }

    /**
     * The next state after structure loading.
     *
     * @return the next state.
     */
    public IAIState afterStructureLoading()
    {
        return START_BUILDING;
    }

    /**
     * Walk to the current construction site.
     * <p>
     * Calculates and caches the position where to walk to.
     *
     * @param currentBlock the current block it is working on.
     * @return true while walking to the site.
     */
    public abstract boolean walkToConstructionSite(final BlockPos currentBlock);

    /**
     * Checks for blocks that need to be treated as deco
     */
    protected static boolean isDecoItem(Block block)
    {
        return block.defaultBlockState().is(ModTags.decorationItems) || block instanceof BlockFluidSubstitution;
    }

    /**
     * The Structure step to execute the actual placement actions etc.
     *
     * @return the next step to go to.
     */
    protected IAIState structureStep()
    {
        if (!isThereAStructureToBuild())
        {
            return IDLE;
        }
        if (structurePlacer.getB().getStage() == null)
        {
            resetCurrentStructure();
            return IDLE;
        }

        if (!worker.getInventoryCitizen().hasSpace())
        {
            return INVENTORY_FULL;
        }

        checkForExtraBuildingActions();

        // some things to do first! then we go to the actual phase!

        //Fill workFrom with the position from where the builder should build.
        //also ensure we are at that position.
        BlockPos progress = getProgressPos() == null ? NULL_POS : getProgressPos().getA();
        final BlockPos worldPos = getPosToWorkAt();

        if (getProgressPos() != null)
        {
            structurePlacer.getB().setStage(getProgressPos().getB());
        }

        // Quidoms: soft clears / in-reach hard breaks skip slow repath; otherwise approach site.
        if (!limitReached && !isReadyForStructureAction(worldPos, blockToMine))
        {
            walkNotReadyStreak++;
            // TEMP QD-101 debug
            if (walkNotReadyStreak == 1 || walkNotReadyStreak % 40 == 0)
            {
                Log.getLogger().info(
                  "[QuidomsBuilder] BUILDING_STEP not-ready streak={} citizen={} workFrom={} worldPos={} minePos={}",
                  walkNotReadyStreak,
                  worker.getCitizenData() == null ? "?" : worker.getCitizenData().getName(),
                  workFrom,
                  worldPos,
                  blockToMine);
                wallChat("path",
                  Component.literal("can't reach the next build spot — repathing (streak " + walkNotReadyStreak + ")."));
            }
            // Soft recover: after sustained path failure, drop workFrom so walkToConstructionSite repaths
            // (or works in-place when already within reach — see EntityAIStructureBuilder).
            if (walkNotReadyStreak >= 60)
            {
                workFrom = null;
                walkNotReadyStreak = 0;
                Log.getLogger().info("[QuidomsBuilder] BUILDING_STEP walk recover: cleared workFrom");
                wallChat("path-recover", Component.literal("path stuck — resetting work position."));
            }
            return getState();
        }
        walkNotReadyStreak = 0;

        limitReached = false;

        final BuildingProgressStage currentStage = structurePlacer.getB().getStage();
        StructurePhasePlacementResult result = executeCurrentStructureStage(progress, currentStage);

        // Quidoms: spam-cycle soft obstructions (leaves/grass/flowers) without walk/mine AI.
        int softCleared = 0;
        while (result.getBlockResult().getResult() == BlockPlacementResult.Result.BREAK_BLOCK
                 && softCleared < SOFT_CLEAR_BATCH
                 && isSoftObstruction(world.getBlockState(result.getBlockResult().getWorldPos())))
        {
            final BlockPos softPos = result.getBlockResult().getWorldPos();
            if (!isWithinSoftClearReach(softPos) && !walkToConstructionSite(softPos))
            {
                blockToMine = softPos;
                worker.getCitizenData().setStatusPosition(softPos);
                return getState();
            }

            instantClearSoftObstruction(softPos);
            softCleared++;
            worker.swing(InteractionHand.MAIN_HAND);

            if (!result.getIteratorPos().equals(NULL_POS))
            {
                this.storeProgressPos(result.getIteratorPos(), currentStage);
                progress = result.getIteratorPos();
            }

            result = executeCurrentStructureStage(progress, currentStage);
        }

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FAIL)
        {
            Log.getLogger().error("Failed placement at: " + result.getBlockResult().getWorldPos().toShortString());
        }

        final boolean firstIteration = building.getProgress() == null;
        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FINISHED)
        {

            building.nextStage();
            if (!goToNextStage(result))
            {
                building.setProgressPos(null, null);
                worker.getCitizenData().setStatusPosition(null);
                return COMPLETE_BUILD;
            }
            this.storeProgressPos(NULL_POS, structurePlacer.getB().getStage());
            if (currentStage == CLEAR)
            {
                building.checkOrRequestBucket(building.getRequiredResources(), worker.getCitizenData());
            }
        }
        else if (result.getBlockResult().getResult() == BlockPlacementResult.Result.LIMIT_REACHED)
        {
            this.limitReached = true;
            this.storeProgressPos(result.getIteratorPos(), structurePlacer.getB().getStage());
        }
        else
        {
            if (structurePlacer.getB().getStage() != CLEAR_WATER && !result.getIteratorPos().equals(NULL_POS))
            {
                gotoPos = result.getIteratorPos();
            }
            else
            {
                gotoPos = null;
            }

            this.storeProgressPos(result.getIteratorPos(), structurePlacer.getB().getStage());
        }

        if (firstIteration)
        {
            building.checkOrRequestBucket(building.getRequiredResources(), worker.getCitizenData());
        }

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.MISSING_ITEMS)
        {
            actionQueue.clear();
            final List<ItemStack> required = result.getBlockResult().getRequiredItems();
            // QuidCraft may have already provisioned the hut — pull/buy before parking in NEEDS_ITEM.
            final String citizenName = worker.getCitizenData() == null ? "" : worker.getCitizenData().getName();
            com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryProvisionNeededResources(
              building, citizenName, worker.getCitizenData());
            // QD-116: provision/buy RAWS only; grant finished from credit after raw deposit — no craft.
            for (final ItemStack need : required)
            {
                if (ItemStackUtils.isEmpty(need))
                {
                    continue;
                }
                final boolean convertible =
                  com.minecolonies.core.util.BuilderInventoryCraft.mustNotRequestFinished(worker.level(), need);
                if (convertible)
                {
                    for (final ItemStack raw : com.minecolonies.core.util.BuilderInventoryCraft.expandToRaw(
                      worker.level(), need, Math.max(1, need.getCount())))
                    {
                        com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryBuyForBuilding(
                          building, raw, Math.max(1, raw.getCount()), false, citizenName, worker.getCitizenData());
                    }
                }
                else
                {
                    com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryBuyForBuilding(
                      building, need, Math.max(1, need.getCount()), false, citizenName, worker.getCitizenData());
                }
                com.minecolonies.core.util.BuilderFinishedGrant.ensureFinishedInPerson(
                  building, worker.getCitizenData(), need.copy());
            }

            final ItemCheckResult check = hasListOfResInInvOrRequest(this, required, required.size() > 1);
            // TEMP QD-101 debug
            Log.getLogger().info(
              "[QuidomsBuilder] MISSING_ITEMS citizen={} check={} required={} openSync={}",
              worker.getCitizenData() == null ? "?" : worker.getCitizenData().getName(),
              check,
              required.size(),
              worker.getCitizenData() != null && building.hasOpenSyncRequest(worker.getCitizenData()));

            if (check == SUCCESS)
            {
                return BUILDING_STEP;
            }

            // Hut holds finished placeables — gather. Raws wait for deposit completeness (QD-116), no craft.
            for (final ItemStack need : required)
            {
                if (ItemStackUtils.isEmpty(need))
                {
                    continue;
                }
                final Predicate<ItemStack> match =
                  candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, false);
                final int person = InventoryUtils.getItemCountInItemHandler(getInventory(), match);
                final int hut = countInHut(building, match);
                if (person > 0)
                {
                    continue;
                }
                if (hut > 0 || building.hasResourceInBucket(need))
                {
                    Log.getLogger().info(
                      "[QuidomsBuilder] MISSING_ITEMS hut-has finished person={} hut={} item={} → GATHER",
                      person, hut, need.getItem());
                    wallChat("gather",
                      Component.literal("picking up " + need.getHoverName().getString() + " from the hut."));
                    if (!worker.getInventoryCitizen().hasSpace())
                    {
                        wallChat("inv-full", Component.literal("inventory full — dumping before pickup."));
                        return INVENTORY_FULL;
                    }
                    needsCurrently = new Tuple<>(match, Math.max(1, need.getCount()));
                    if (InventoryUtils.hasItemInProvider(building.getTileEntity(), match) || hut > 0)
                    {
                        return GATHERING_REQUIRED_MATERIALS;
                    }
                    return PICK_UP;
                }
            }

            // QD-102: never LOAD_STRUCTURE on RECALC — that looped forever after QuidCraft provisioned
            // (MISSING_ITEMS check=RECALC → LOAD_STRUCTURE → …). Stay on BUILDING_STEP / gather / retry.
            if (check == RECALC)
            {
                Log.getLogger().info("[QuidomsBuilder] MISSING_ITEMS RECALC → BUILDING_STEP retry (no LOAD_STRUCTURE)");
                wallChat("wait-raws",
                  Component.literal("retrying material check (not reloading schematic)."));
                setDelay(40);
                return BUILDING_STEP;
            }
            // QD-101/119: stay on BUILDING_STEP; chat shortages match Required Resources (or stay silent).
            Log.getLogger().info("[QuidomsBuilder] MISSING_ITEMS → BUILDING_STEP retry (no NEEDS_ITEM park)");
            final com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule resModule =
              building.getFirstModuleOccurance(com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule.class);
            if (resModule != null)
            {
                // Menu complete → convert once; already-granted stays quiet (QD-121).
                if (resModule.describeMissingRaws(worker.getCitizenData()).isEmpty())
                {
                    if (!resModule.isFinishedConverted()
                          && resModule.tryConvertRawsToFinishedCredit(worker.getCitizenData()))
                    {
                        wallChat("grant", Component.literal("raw deposits complete — granting finished placeables."));
                    }
                    // else already converted / nothing to say — continue BUILDING_STEP
                }
                else
                {
                    wallChat("wait-raws",
                      Component.literal("waiting on raw deposits: " + resModule.describeMissingRaws(worker.getCitizenData()) + "."));
                }
            }
            setDelay(20);
            return BUILDING_STEP;
        }

        worker.swing(InteractionHand.MAIN_HAND);
        worker.queueSound(SoundEvents.BAMBOO_HIT, worker.blockPosition(), 10, 0, 0.5f, 0.1f);

        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.BREAK_BLOCK)
        {
            final BlockPos firstBreak = result.getBlockResult().getWorldPos();
            this.storeProgressPos(result.getIteratorPos(), currentStage);

            // Quidoms: scan ahead and commit a break queue (wood→wood→dirt…), not one-by-one mid-build.
            prefetchHardBreakQueue(result.getIteratorPos(), currentStage, firstBreak);

            blockToMine = firstBreak;
            worker.getCitizenData().setStatusPosition(blockToMine);

            // Hard blocks: always go through MINE_BLOCK so out-of-range walks closer, then tool-break.
            // Soft clears already handled above; do not spam-insta hard solids.
            return MINE_BLOCK;
        }
        else
        {
            blockToMine = null;
        }

        // Quidoms: after a soft-spam batch, resume immediately (no place-delay stall).
        if (softCleared > 0)
        {
            setDelay(1);
            return getState();
        }

        // Quidoms: pure scan chunks must not pay place-delay — that was part of the ~1s selection gap.
        if (result.getBlockResult().getResult() == BlockPlacementResult.Result.LIMIT_REACHED)
        {
            setDelay(1);
            return getState();
        }

        // Quidoms: place delay skill-decoupled (no Adaptability); fixed high place speed.
        final double decrease = 1 - worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_PLACE_SPEED);
        setDelay(Math.max(1, (int) (BUILD_BLOCK_DELAY * decrease)));

        return getState();
    }

    /**
     * Run one StructurePlacer step for the active building stage.
     */
    private StructurePhasePlacementResult executeCurrentStructureStage(final BlockPos progress, final BuildingProgressStage currentStage)
    {
        final StructurePlacer placer = structurePlacer.getA();
        switch (currentStage)
        {
            case BUILD_SOLID:
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment(this::skipBuilding), false);
            case WEAK_SOLID:
                return placer.executeStructureStep(world,
                  null,
                  progress,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator()
                          .increment(((info, pos, handler) -> !BlockUtils.isWeakSolidBlock(info.getBlockInfo().getState()) || DONT_TOUCH_PREDICATE.test(info, pos, handler))),
                  false);
            case CLEAR_WATER:
                return placer.clearWaterStep(world, progress);
            case CLEAR_NON_SOLIDS:
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().decrement((info, pos, handler) ->
                                                         !(info.getBlockInfo().getState().getBlock() instanceof AirBlock)
                                                           || (handler.getWorld().isEmptyBlock(pos))
                                                           || DONT_TOUCH_PREDICATE.test(info, pos, handler)), false);
            case DECORATE:
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment(this::skipDecorate), false);
            case SPAWN:
                if (placer.getHandler().getBluePrint().getEntities().length == 0)
                {
                    return new StructurePhasePlacementResult(BlockPos.ZERO, new BlockPlacementResult(BlockPos.ZERO, BlockPlacementResult.Result.FINISHED));
                }
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.SPAWN_ENTITY,
                  () -> placer.getIterator().increment((info, pos, handler) -> info.getEntities().length == 0 || DONT_TOUCH_PREDICATE.test(info, pos, handler)), true);
            case REMOVE_WATER:
                placer.getIterator().setRemoving();
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.WATER_REMOVAL,
                  () -> placer.getIterator().decrement((info, pos, handler) -> info.getBlockInfo().getState().getFluidState().isEmpty()), false);
            case REMOVE:
                placer.getIterator().setRemoving();
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_REMOVAL,
                  () -> placer.getIterator().decrement(this::skipRemoval), true);
            case CLEAR:
            default:
                return placer.executeStructureStep(world, null, progress, StructurePlacer.Operation.BLOCK_REMOVAL,
                  () -> placer.getIterator().decrement(this::skipClearing), false);
        }
    }

    /**
     * Choose approach vs act-in-place: soft clears and in-reach hard breaks skip repath.
     */
    private boolean isReadyForStructureAction(@Nullable final BlockPos worldPos, @Nullable final BlockPos minePos)
    {
        if (minePos != null)
        {
            if (isSoftObstruction(world.getBlockState(minePos)) && isWithinSoftClearReach(minePos))
            {
                return true;
            }
            return walkToConstructionSite(minePos);
        }

        if (worldPos == null)
        {
            return true;
        }

        // Already standing in a good work pocket — scan/place/break without repathing each cell.
        if (workFrom != null && BlockPosUtil.getDistance2D(worker.blockPosition(), worldPos) <= SOFT_CLEAR_REACH)
        {
            return true;
        }

        return walkToConstructionSite(worldPos);
    }

    /**
     * Leaves, grass, flowers, and other cheap replaceable foliage — spam-cleared.
     */
    protected boolean isSoftObstruction(@NotNull final BlockState state)
    {
        if (state.isAir() || state.getBlock() instanceof AirBlock || state.getBlock() instanceof IBuilderUndestroyable
              || state.getBlock() == Blocks.BEDROCK || !state.getFluidState().isEmpty())
        {
            return false;
        }

        if (state.is(BlockTags.LEAVES)
              || state.is(BlockTags.FLOWERS)
              || state.is(BlockTags.SAPLINGS)
              || state.is(BlockTags.REPLACEABLE)
              || state.is(BlockTags.WOOL_CARPETS)
              || state.getBlock() == Blocks.SHORT_GRASS
              || state.getBlock() == Blocks.TALL_GRASS
              || state.getBlock() == Blocks.FERN
              || state.getBlock() == Blocks.LARGE_FERN
              || state.getBlock() == Blocks.DEAD_BUSH
              || state.getBlock() == Blocks.VINE
              || state.getBlock() == Blocks.GLOW_LICHEN
              || state.getBlock() == Blocks.SNOW
              || state.getBlock() == Blocks.MOSS_CARPET
              || state.getBlock() == Blocks.PINK_PETALS
              || state.getBlock() == Blocks.SEAGRASS
              || state.getBlock() == Blocks.TALL_SEAGRASS
              || state.getBlock() == Blocks.KELP
              || state.getBlock() == Blocks.KELP_PLANT)
        {
            return true;
        }

        if (state.canBeReplaced())
        {
            return true;
        }

        // Very soft solids (e.g. leftover foliage) — not dirt/wood/stone.
        final float hardness = state.getDestroySpeed(world, worker.blockPosition());
        return hardness >= 0.0F && hardness <= 0.25F && !BlockUtils.isAnySolid(state);
    }

    private boolean isWithinSoftClearReach(@NotNull final BlockPos pos)
    {
        return BlockPosUtil.getDistance2D(worker.blockPosition(), pos) <= SOFT_CLEAR_REACH
                 && Math.abs(worker.blockPosition().getY() - pos.getY()) <= SOFT_CLEAR_REACH;
    }

    private void instantClearSoftObstruction(@NotNull final BlockPos pos)
    {
        final BlockState state = world.getBlockState(pos);
        if (!isSoftObstruction(state))
        {
            return;
        }

        world.removeBlock(pos, false);
        worker.getCitizenExperienceHandler().addExperience(0.05D);
        this.incrementActionsDone();
    }

    /**
     * Get the position the citizen needs to walk to in order to build
     *
     * @return the position to go to
     */
    protected BlockPos getPosToWorkAt()
    {
        if (gotoPos != null)
        {
            return structurePlacer.getB().getProgressPosInWorld(gotoPos);
        }
        else
        {
            return building.getWorkOrder().getLocation();
        }
    }

    /**
     * This method is called when a stage is finished, and it is needed to go to the next stage
     *
     * @return whether it succeeded, i.e. there was a next stage to go to.
     */
    protected boolean goToNextStage(StructurePhasePlacementResult result)
    {
        return structurePlacer.getB().nextStage();
    }

    /**
     * Checks which blocks are skipped on decorate
     *
     * @param info
     * @param pos
     * @param handler
     * @return
     */
    private boolean skipDecorate(final BlueprintPositionInfo info, final BlockPos pos, final IStructureHandler handler)
    {
        final BlockState blockInfoState = info.getBlockInfo().getState();
        return (!isDecoItem(blockInfoState.getBlock()) && BlockUtils.isAnySolid(blockInfoState)) || DONT_TOUCH_PREDICATE.test(info, pos, handler);
    }

    /**
     * Checks wich blocks are skipped on building
     *
     * @param info
     * @param pos
     * @param handler
     * @return
     */
    protected boolean skipBuilding(final BlueprintPositionInfo info, final BlockPos pos, final IStructureHandler handler)
    {
        final BlockState blockInfoState = info.getBlockInfo().getState();
        return !BlockUtils.canBlockFloatInAir(blockInfoState)
                 || isDecoItem(blockInfoState.getBlock())
                 || DONT_TOUCH_PREDICATE.test(info, pos, handler);
    }

    /**
     * Checks which blocks are skipped on clearing
     *
     * @param info
     * @param pos
     * @param handler
     * @return
     */
    protected boolean skipClearing(final BlueprintPositionInfo info, final BlockPos pos, final IStructureHandler handler)
    {
        if (info.getBlockInfo().getState().getBlock() == com.ldtteam.structurize.blocks.ModBlocks.blockFluidSubstitution.get())
        {
            return true;
        }

        final BlockState state = handler.getWorld().getBlockState(pos);
        return state.getBlock() instanceof IBuilderUndestroyable
                 || state.getBlock() == Blocks.BEDROCK
                 || state.isAir()
                 || !state.getFluidState().isEmpty();
    }

    /**
     * Checks which blocks are skipped on removal on building upgrade
     *
     * @param info
     * @param pos
     * @param handler
     * @return
     */
    private boolean skipRemoval(final BlueprintPositionInfo info, final BlockPos pos, final IStructureHandler handler)
    {
        final BlockState infoBlockState = info.getBlockInfo().getState();
        final Block infoBlock = infoBlockState.getBlock();
        if (infoBlockState.isAir()
              || infoBlock == com.ldtteam.structurize.blocks.ModBlocks.blockSolidSubstitution.get()
              || infoBlock == com.ldtteam.structurize.blocks.ModBlocks.blockSubstitution.get()
              || infoBlock == com.ldtteam.structurize.blocks.ModBlocks.blockFluidSubstitution.get())
        {
            return true;
        }

        final BlockState state = handler.getWorld().getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof IBuilderUndestroyable || state.getBlock() == Blocks.WATER;
    }

    /**
     * Get the level that affects the place speed.
     *
     * @return the level.
     */
    public abstract int getPlaceSpeedLevel();

    @Override
    public IAIState getStateAfterPickUp()
    {
        return PICK_UP;
    }

    @Override
    public IAIState afterRequestPickUp()
    {
        return INVENTORY_FULL;
    }

    @Override
    public IAIState afterDump()
    {
        return PICK_UP;
    }

    /**
     * Separate step for mining.
     *
     * @return the next state to go to.
     */
    public IAIState doMining()
    {
        if (blockToMine == null)
        {
            return continueQueuedBreaks();
        }

        if (structurePlacer == null)
        {
            actionQueue.clear();
            return IDLE;
        }

        final BlockState worldState = world.getBlockState(blockToMine);
        if (worldState.getBlock() instanceof AirBlock || worldState.getBlock() == Blocks.WATER)
        {
            blockToMine = null;
            return continueQueuedBreaks();
        }

        // Quidoms: soft leftovers clear instantly; schedule next queue item for the following tick.
        if (isSoftObstruction(worldState) && isWithinSoftClearReach(blockToMine))
        {
            instantClearSoftObstruction(blockToMine);
            blockToMine = null;
            worker.swing(InteractionHand.MAIN_HAND);
            return continueQueuedBreaks();
        }

        if (!walkToConstructionSite(blockToMine))
        {
            return getState();
        }

        if (workFrom == null)
        {
            return getState();
        }

        if (!mineBlock(blockToMine, workFrom))
        {
            worker.swing(InteractionHand.MAIN_HAND);
            return getState();
        }
        worker.decreaseSaturationForContinuousAction();
        blockToMine = null;

        // QD-108: arm next queued break for the *next* AI tick — never recurse doMining()
        // (Eff5 netherite instamine would otherwise drain the whole queue in one hitch).
        return continueQueuedBreaks();
    }

    /**
     * After a successful hard break, arm the next queued break for a subsequent AI tick.
     * Soft leftovers may clear up to {@link #SOFT_CLEAR_BATCH} here; hard mines never recurse.
     */
    @NotNull
    private IAIState continueQueuedBreaks()
    {
        int softCleared = 0;
        while (!actionQueue.isEmpty())
        {
            final QueuedBreakAction next = actionQueue.poll();
            this.storeProgressPos(next.iteratorPos, next.stage);

            final BlockState state = world.getBlockState(next.worldPos);
            if (state.getBlock() instanceof AirBlock || state.getBlock() == Blocks.WATER)
            {
                continue;
            }

            if (isSoftObstruction(state) && isWithinSoftClearReach(next.worldPos))
            {
                if (softCleared >= SOFT_CLEAR_BATCH)
                {
                    actionQueue.addFirst(next);
                    blockToMine = null;
                    return MINE_BLOCK;
                }
                instantClearSoftObstruction(next.worldPos);
                worker.swing(InteractionHand.MAIN_HAND);
                softCleared++;
                continue;
            }

            // Hard break — always defer to next AI tick (QD-108). Never call doMining() here.
            blockToMine = next.worldPos;
            worker.getCitizenData().setStatusPosition(blockToMine);
            return MINE_BLOCK;
        }

        return BUILDING_STEP;
    }

    /**
     * Dry-scan the blueprint from {@code progressAfterFirst} and commit further hard breaks into {@link #actionQueue}.
     * {@code firstBreak} is already being mined and is treated as virtual air so the scan can walk past it.
     */
    private void prefetchHardBreakQueue(
      @NotNull final BlockPos progressAfterFirst,
      @NotNull final BuildingProgressStage stage,
      @NotNull final BlockPos firstBreak)
    {
        actionQueue.clear();

        if (structurePlacer == null || structurePlacer.getA() == null || structurePlacer.getB() == null)
        {
            return;
        }

        // Placement stages that don't use the block iterator the same way — skip prefetch.
        if (stage == CLEAR_WATER || stage == SPAWN)
        {
            return;
        }

        final AbstractBlueprintIterator iterator = structurePlacer.getA().getIterator();
        final BlockPos savedProgress = iterator.getProgressPos() == null ? NULL_POS : iterator.getProgressPos().immutable();
        final Set<Long> virtualAir = new HashSet<>();
        virtualAir.add(firstBreak.asLong());

        if (stage == REMOVE || stage == REMOVE_WATER)
        {
            iterator.setRemoving();
        }

        BlockPos scanProgress = progressAfterFirst;
        int scanned = 0;

        while (actionQueue.size() < ACTION_QUEUE_SIZE && scanned < ACTION_PREFETCH_SCAN_BUDGET)
        {
            iterator.setProgressPos(scanProgress);
            final AbstractBlueprintIterator.Result iterResult = advanceIteratorForStage(stage, virtualAir);
            scanned++;

            if (iterResult != AbstractBlueprintIterator.Result.NEW_BLOCK)
            {
                break;
            }

            final BlockPos iterPos = iterator.getProgressPos();
            final BlockPos prevPos = iterator.getPrevProgressPos();
            if (iterPos == null || iterPos.equals(NULL_POS))
            {
                break;
            }

            final BlockPos worldPos = structurePlacer.getB().getProgressPosInWorld(iterPos);
            final BlockState realState = world.getBlockState(worldPos);
            final boolean treatedAir = virtualAir.contains(worldPos.asLong());
            final BlockState effectiveState = treatedAir ? Blocks.AIR.defaultBlockState() : realState;

            // QD-108: do not soft-clear during prefetch (piles removeBlocks onto the find-break tick).
            // Enqueue softs so continueQueuedBreaks clears them under SOFT_CLEAR_BATCH.
            if (!treatedAir && isSoftObstruction(realState))
            {
                actionQueue.addLast(new QueuedBreakAction(worldPos.immutable(), iterPos.immutable(), stage));
                virtualAir.add(worldPos.asLong());
                scanProgress = iterPos;
                continue;
            }

            final BlueprintPositionInfo info = iterator.getBluePrintPositionInfo(iterPos);
            final BlockInfo blockInfo = info == null ? null : info.getBlockInfo();
            if (blockInfo == null || blockInfo.getState() == null)
            {
                // Skip malformed cell — advance past it.
                scanProgress = iterPos;
                continue;
            }

            final boolean clearLike = stage == CLEAR || stage == REMOVE || stage == REMOVE_WATER;
            if (clearLike)
            {
                if (effectiveState.getBlock() instanceof AirBlock
                      || effectiveState.getBlock() == Blocks.WATER
                      || !effectiveState.getFluidState().isEmpty()
                      || effectiveState.getBlock() instanceof IBuilderUndestroyable
                      || effectiveState.getBlock() == Blocks.BEDROCK)
                {
                    scanProgress = iterPos;
                    continue;
                }

                // QD-108: store this cell's iterPos (not streak start) so progress advances one-by-one.
                actionQueue.addLast(new QueuedBreakAction(worldPos.immutable(), iterPos.immutable(), stage));
                virtualAir.add(worldPos.asLong());
                scanProgress = iterPos;
                continue;
            }

            // Placement stages: already-correct cells (or virtually cleared + assumed placed) → walk past.
            if (treatedAir || IPlacementHandler.doesWorldStateMatchBlueprintState(blockInfo, worldPos, structurePlacer.getB()))
            {
                scanProgress = iterPos;
                continue;
            }

            if (!(effectiveState.getBlock() instanceof AirBlock))
            {
                actionQueue.addLast(new QueuedBreakAction(worldPos.immutable(), iterPos.immutable(), stage));
                virtualAir.add(worldPos.asLong());
                // Treat as cleared+placed and continue so wood→wood→dirt can commit in one scan.
                scanProgress = iterPos;
                continue;
            }

            // Needs place at this cell — commit the break streak; placer handles placement next.
            break;
        }

        iterator.setProgressPos(savedProgress);
    }

    /**
     * Advance the blueprint iterator one step with the stage skip predicate.
     * Clear-like stages also skip {@code virtualAir} (already committed breaks).
     * Placement stages must still visit those cells so we can walk past them as virtual-placed.
     */
    private AbstractBlueprintIterator.Result advanceIteratorForStage(
      @NotNull final BuildingProgressStage stage,
      @NotNull final Set<Long> virtualAir)
    {
        final StructurePlacer placer = structurePlacer.getA();
        final boolean clearLike = stage == CLEAR || stage == REMOVE || stage == REMOVE_WATER;
        final Supplier<AbstractBlueprintIterator.Result> advance = () -> switch (stage)
        {
            case BUILD_SOLID -> placer.getIterator().increment(this::skipBuilding);
            case WEAK_SOLID -> placer.getIterator().increment((info, pos, handler) ->
                                                                !BlockUtils.isWeakSolidBlock(info.getBlockInfo().getState())
                                                                  || DONT_TOUCH_PREDICATE.test(info, pos, handler));
            case CLEAR_NON_SOLIDS -> placer.getIterator().decrement((info, pos, handler) ->
                                                                      !(info.getBlockInfo().getState().getBlock() instanceof AirBlock)
                                                                        || (handler.getWorld().isEmptyBlock(pos))
                                                                        || DONT_TOUCH_PREDICATE.test(info, pos, handler));
            case DECORATE -> placer.getIterator().increment(this::skipDecorate);
            case REMOVE -> placer.getIterator().decrement((info, pos, handler) ->
                                                            (clearLike && virtualAir.contains(pos.asLong())) || skipRemoval(info, pos, handler));
            case REMOVE_WATER -> placer.getIterator().decrement((info, pos, handler) ->
                                                                  (clearLike && virtualAir.contains(pos.asLong()))
                                                                    || info.getBlockInfo().getState().getFluidState().isEmpty());
            case CLEAR -> placer.getIterator().decrement((info, pos, handler) ->
                                                           virtualAir.contains(pos.asLong()) || skipClearing(info, pos, handler));
            default -> AbstractBlueprintIterator.Result.AT_END;
        };
        return advance.get();
    }

    /**
     * Loads the structure given the name, rotation and position.
     *
     * @param workOrder   the work order.
     * @param position    the position to set it.
     * @param removal     if removal step.
     */
    public void loadStructure(@NotNull final IBuilderWorkOrder workOrder, final BlockPos position, final boolean removal)
    {
        this.loadingBlueprint = true;
        workOrder.loadBlueprint(world, (blueprint -> {
            if (blueprint == null)
            {
                // Keep the claim — in-flight/pack-not-ready misses used to cancel the WO (QD-94).
                // Clear placer and let LOAD_STRUCTURE retry on a later tick.
                Log.getLogger()
                  .warn("Couldn't find structure with name: " + workOrder.getStructurePath() + " in: " + workOrder.getStructurePack()
                          + ". Will retry; work order kept claimed.");
                this.loadingBlueprint = false;
                resetCurrentStructure();
                return;
            }

            final BuildingStructureHandler<J, B> structure;
            IBuilding colonyBuilding = worker.getCitizenColonyHandler().getColonyOrRegister().getServerBuildingManager().getBuilding(position);
            final BlockEntity entity = world.getBlockEntity(position);

            if (removal)
            {
                structure = new BuildingStructureHandler<>(world,
                    workOrder,
                    this, new BuildingProgressStage[] {REMOVE_WATER, REMOVE});
                building.setTotalStages(2);
            }
            else if ((colonyBuilding != null && (colonyBuilding.getBuildingLevel() > 0 || colonyBuilding.hasParent())) ||
                       (entity instanceof TileEntityDecorationController && Utils.getBlueprintLevel(((TileEntityDecorationController) entity).getBlueprintPath()) != -1))
            {
                structure = new BuildingStructureHandler<>(world,
                    workOrder,
                    this, new BuildingProgressStage[] {BUILD_SOLID, WEAK_SOLID, CLEAR_WATER, CLEAR_NON_SOLIDS, DECORATE, SPAWN});
                building.setTotalStages(5);
            }
            else
            {
                structure = new BuildingStructureHandler<>(world,
                    workOrder,
                    this, new BuildingProgressStage[] {CLEAR, BUILD_SOLID, WEAK_SOLID, CLEAR_WATER, CLEAR_NON_SOLIDS, DECORATE, SPAWN});
                building.setTotalStages(6);
            }

            setStructurePlacer(structure);

            if (getProgressPos() != null)
            {
                structure.setStage(getProgressPos().getB());
            }
            this.loadingBlueprint = false;
        }));
    }

    /**
     * Set the structure placer.
     *
     * @param structure the placer.
     */
    public void setStructurePlacer(final BuildingStructureHandler<J, B> structure)
    {
        structurePlacer = new Tuple<>(new StructurePlacer(structure), structure);
    }

    /**
     * Check the placers inventory for the items in the itemList and remove it of the list if found.
     *
     * @param placer   the placer.
     * @param itemList the list to check.
     * @param force    if force insertion.
     * @return true if need to request.
     */
    public static <J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> ItemCheckResult hasListOfResInInvOrRequest(
      @NotNull final AbstractEntityAIStructure<J, B> placer,
      final List<ItemStack> itemList,
      final boolean force)
    {
        final Map<ItemStorage, Integer> requestedMap = new HashMap<>();
        for (final ItemStack stack : itemList)
        {
            if (stack.getItem() instanceof BlockItem && isBlockFree(((BlockItem) stack.getItem()).getBlock().defaultBlockState()))
            {
                continue;
            }

            ItemStorage tempStorage = new ItemStorage(stack.copy());
            if (requestedMap.containsKey(tempStorage))
            {
                final int oldSize = requestedMap.get(tempStorage);
                tempStorage.setAmount(tempStorage.getAmount() + oldSize);
            }
            requestedMap.put(tempStorage, tempStorage.getAmount());
        }

        final net.minecraft.world.level.Level world = placer.getWorker().level();
        for (final ItemStorage stack : new HashSet<>(requestedMap.keySet()))
        {
            final ItemStack need = stack.getItemStack();
            final int want = Math.max(1, stack.getAmount());
            final Predicate<ItemStack> match =
              candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, false);
            final boolean convertible =
              com.minecolonies.core.util.BuilderInventoryCraft.mustNotRequestFinished(world, need);
            final java.util.List<ItemStack> raws =
              convertible
                ? com.minecolonies.core.util.BuilderInventoryCraft.expandToRaw(world, need, want)
                : java.util.List.of();

            // QD-112: cancel any open delivery requests for convertible finished products (oak planks, framed…).
            if (convertible)
            {
                cancelFinishedConvertibleRequests(placer, need);
            }

            // QD-118: finished already in person (exact count) → satisfied, do not wait on raws.
            if (InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match) >= want
                  || (!convertible && placer.building.hasResourceInBucket(need)))
            {
                requestedMap.remove(stack);
                continue;
            }

            // Dual inventory: pull finished product from hut TE + racks.
            pullFromHutIntoWorker(placer, match, want);

            if (InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match) >= want)
            {
                requestedMap.remove(stack);
                continue;
            }

            // QD-116/118: convert dual-inv raws → finished credit, inject into person (no runtime craft).
            final String citizenName = placer.getWorker().getCitizenData() == null ? "" : placer.getWorker().getCitizenData().getName();
            if (convertible)
            {
                for (final ItemStack raw : raws)
                {
                    com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryBuyForBuilding(
                      placer.building,
                      raw,
                      Math.max(1, raw.getCount()),
                      false,
                      citizenName,
                      placer.getWorker().getCitizenData());
                }
            }
            else
            {
                com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryBuyForBuilding(
                  placer.building,
                  need,
                  want,
                  false,
                  citizenName,
                  placer.getWorker().getCitizenData());
                pullFromHutIntoWorker(placer, match, want);
            }

            if (com.minecolonies.core.util.BuilderFinishedGrant.ensureFinishedInPerson(
              placer.building, placer.getWorker().getCitizenData(), need.copyWithCount(want)))
            {
                Log.getLogger().info("[QuidomsBuilder] hasList granted/has finished {} x{}", need.getItem(), want);
                requestedMap.remove(stack);
                continue;
            }

            if (InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match) >= want
                  || (!convertible && placer.building.hasResourceInBucket(need)))
            {
                requestedMap.remove(stack);
                continue;
            }

            // Still missing — do NOT return RECALC (QD-102). Leave in map → raw async request + FAIL.
            Log.getLogger().info(
              "[QuidomsBuilder] hasList missing person+hut item={} convertible={} → FAIL path (no RECALC)",
              need.getItem(), convertible);
        }
        // QD-118: clear satisfied finished by count compare (avoid ItemStorage key mismatches).
        for (final Map.Entry<ItemStorage, Integer> pending : new HashMap<>(requestedMap).entrySet())
        {
            final ItemStack need = pending.getKey().getItemStack();
            final Predicate<ItemStack> match =
              candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, false);
            final int want = Math.max(1, pending.getValue());
            int personHave = InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match);
            if (personHave < want && countInHut(placer.building, match) > 0)
            {
                pullFromHutIntoWorker(placer, match, want - personHave);
                personHave = InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match);
            }
            if (personHave >= want)
            {
                requestedMap.remove(pending.getKey());
            }
            else if (force && personHave > 0)
            {
                requestedMap.put(pending.getKey(), want - personHave);
            }
        }

        final ICitizenData citizenData = placer.getWorker().getCitizenData();
        final int citizenId = citizenData.getId();
        for (final Map.Entry<ItemStorage, Integer> placedStack : requestedMap.entrySet())
        {
            final ItemStack stack = placedStack.getKey().getItemStack();
            if (ItemStackUtils.isEmpty(stack))
            {
                return FAIL;
            }

            final int want = Math.max(1, placedStack.getValue());
            final Predicate<ItemStack> match =
              candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, candidate, true, false);

            final boolean convertible =
              com.minecolonies.core.util.BuilderInventoryCraft.mustNotRequestFinished(world, stack);

            // QD-112: never open a delivery request for convertible finished products (oak planks / framed).
            if (convertible)
            {
                cancelFinishedConvertibleRequests(placer, stack);
            }

            // Person already has finished — should have been cleared above; safety net.
            if (InventoryUtils.getItemCountInItemHandler(placer.getInventory(), match) >= want)
            {
                continue;
            }

            // Hut already has finished product — gather, do not open a courier request.
            if (countInHut(placer.building, match) > 0 || (!convertible && placer.building.hasResourceInBucket(stack)))
            {
                return FAIL;
            }

            final com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule resModule =
              placer.building.getFirstModuleOccurance(com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule.class);
            // QD-120/121: after one-shot grant, never open new raw requests / top-ups mid-build.
            if (resModule != null && resModule.isFinishedConverted())
            {
                return FAIL;
            }

            // Request RAW only (BOM). Person+hut already holding raws → no new request.
            final java.util.List<ItemStack> toRequest = convertible
              ? com.minecolonies.core.util.BuilderInventoryCraft.expandToRaw(world, stack, want)
              : java.util.List.of(stack.copyWithCount(want));

            boolean filedAny = false;
            String rawNames = "";
            for (final ItemStack reqStack : toRequest)
            {
                if (ItemStackUtils.isEmpty(reqStack))
                {
                    continue;
                }
                if (convertible && ItemStackUtils.compareItemStacksIgnoreStackSize(reqStack, stack, true, false))
                {
                    Log.getLogger().info(
                      "[QuidomsBuilder] skip finished request for convertible {} (no distinct raw BOM)",
                      stack.getItem());
                    continue;
                }
                final Predicate<ItemStack> rawMatch =
                  candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(reqStack, candidate, true, false);
                final int personRaw = InventoryUtils.getItemCountInItemHandler(placer.getInventory(), rawMatch);
                final int hutRaw = countInHut(placer.building, rawMatch);
                if (personRaw + hutRaw > 0 || placer.building.hasResourceInBucket(reqStack))
                {
                    continue;
                }

                final List<IRequest<?>> requests = placer.building.getOpenRequestsOfCitizenOrBuilding(citizenId,
                  e -> e.getRequest() instanceof IDeliverable deliverable && deliverable.matches(reqStack));
                final List<IRequest<?>> completedRequests = placer.building.getCompletedRequestsOfCitizenOrBuilding(citizenData,
                  e -> e.getRequest() instanceof IDeliverable deliverable && deliverable.matches(reqStack));

                if (requests.isEmpty() && completedRequests.isEmpty())
                {
                    final int count = convertible
                      ? Math.max(1, reqStack.getCount())
                      : placer.getTotalAmount(reqStack).getCount();
                    final Stack stackRequest = new Stack(reqStack, count, 1);
                    placer.getWorker().getCitizenData().createRequestAsync(stackRequest);
                    placer.registerBlockAsNeeded(reqStack);
                    filedAny = true;
                    final String rawLabel = com.minecolonies.core.util.BuilderInventoryCraft.localizedDepositName(reqStack);
                    rawNames = rawNames.isEmpty() ? rawLabel : rawNames + ", " + rawLabel;
                    Log.getLogger().info(
                      "[QuidomsBuilder] createRequestAsync raw={} count={} forFinished={} citizen={}",
                      reqStack.getItem(), stackRequest.getCount(), stack.getItem(), citizenData.getName());
                }
                else
                {
                    Log.getLogger().info(
                      "[QuidomsBuilder] hasList open={} completed={} raw={} forFinished={} (left async, no sync promote)",
                      requests.size(), completedRequests.size(), reqStack.getItem(), stack.getItem());
                }
            }
            // QD-119/121: wait chat matches Required Resources; grant chat only on fresh convert.
            String missingRaws = resModule == null ? "" : resModule.describeMissingRaws(citizenData);
            if (missingRaws.isEmpty() && !rawNames.isEmpty())
            {
                missingRaws = rawNames;
            }
            if (!missingRaws.isEmpty())
            {
                placer.wallChat("wait-raws",
                  Component.literal((filedAny ? "need raw deposits: " : "waiting on raw deposits: ") + missingRaws + "."));
            }
            else if (resModule != null && resModule.tryConvertRawsToFinishedCredit(citizenData))
            {
                placer.wallChat("grant", Component.literal("raw deposits complete — granting finished placeables."));
            }
            return FAIL;
        }
        return SUCCESS;
    }

    /**
     * QD-112: cancel open/completed-async Stack requests for a convertible finished product
     * (e.g. oak planks) so the clipboard never asks for craftables covered by the raw BOM.
     */
    private static <J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> void cancelFinishedConvertibleRequests(
      @NotNull final AbstractEntityAIStructure<J, B> placer,
      @NotNull final ItemStack finished)
    {
        final ICitizenData citizenData = placer.getWorker().getCitizenData();
        if (citizenData == null || ItemStackUtils.isEmpty(finished))
        {
            return;
        }
        final int citizenId = citizenData.getId();
        final List<IRequest<?>> open = new ArrayList<>(placer.building.getOpenRequestsOfCitizenOrBuilding(citizenId,
          e -> e.getRequest() instanceof IDeliverable deliverable && deliverable.matches(finished)));
        for (final IRequest<?> request : open)
        {
            try
            {
                placer.building.getColony().getRequestManager().updateRequestState(request.getId(),
                  com.minecolonies.api.colony.requestsystem.request.RequestState.CANCELLED);
                Log.getLogger().info("[QuidomsBuilder] cancelled spurious finished request item={} id={}",
                  finished.getItem(), request.getId());
            }
            catch (final Exception ex)
            {
                Log.getLogger().debug("[QuidomsBuilder] cancel finished request failed: {}", ex.toString());
            }
        }
    }

    /**
     * Short rate-limited citizen chat so players see walls without grepping latest.log (QD-102).
     */
    protected void wallChat(@NotNull final String key, @NotNull final net.minecraft.network.chat.Component detail)
    {
        final long now = System.currentTimeMillis();
        // QD-118: throttle wait-raws harder (20s) so deposit waits do not spam chat.
        final long throttleMs = "wait-raws".equals(key) ? 20000L : 8000L;
        if (key.equals(lastWallChatKey) && now - lastWallChatMs < throttleMs)
        {
            return;
        }
        lastWallChatKey = key;
        lastWallChatMs = now;
        try
        {
            MessageUtils.forCitizen(worker, detail)
              .sendTo(worker.getCitizenColonyHandler().getColonyOrRegister().getMessagePlayerEntities());
        }
        catch (final Exception e)
        {
            Log.getLogger().debug("[QuidomsBuilder] wallChat failed: {}", e.toString());
        }
    }

    /**
     * Count matching stacks in the hut TE inventory + registered racks (combined handler).
     */
    private static int countInHut(@NotNull final AbstractBuildingStructureBuilder building, @NotNull final Predicate<ItemStack> match)
    {
        return InventoryUtils.hasBuildingEnoughElseCount(building, match, 1);
    }

    /**
     * Pull matching stacks from hut (TE + racks) into the worker's on-person inventory.
     */
    private static <J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> void pullFromHutIntoWorker(
      @NotNull final AbstractEntityAIStructure<J, B> placer,
      @NotNull final Predicate<ItemStack> match,
      final int amount)
    {
        if (amount <= 0)
        {
            return;
        }
        final net.neoforged.neoforge.items.IItemHandler hut = placer.building.getItemHandlerCap();
        if (hut == null)
        {
            return;
        }
        InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
          hut, match, amount, placer.getInventory());
    }

    /**
     * Load all requirements of the structure.
     *
     * @return the next state to go to.
     */
    public IAIState loadRequirements()
    {
        return START_WORKING;
    }

    /**
     * Iterates through all the required resources and stores them in the building.
     *
     * @return true if finished.
     */
    public boolean requestMaterials()
    {
        /*
         *  Override if needed.
         */
        return true;
    }

    /**
     * Register the block as needed at the building if possible.
     *
     * @param stack the stack.
     */
    public void registerBlockAsNeeded(final ItemStack stack)
    {
        /*
         * Override in child if possible.
         */
    }

    /**
     * Store the progressPos in the building if possible for the worker.
     *
     * @param blockPos the progressResult.
     * @param stage    the current stage.
     */
    public void storeProgressPos(final BlockPos blockPos, final BuildingProgressStage stage)
    {
        /*
         * Override if needed.
         */
    }

    /**
     * Calculates the working position.
     * <p>
     * Takes a min distance from width and length.
     * <p>
     * Then finds the floor level at that distance and then check if it does contain two air levels.
     *
     * @param targetPosition the position to work at.
     * @return BlockPos position to work from.
     */
    @Override
    public BlockPos getWorkingPosition(final BlockPos targetPosition)
    {
        //get length or width either is larger.
        final int length = structurePlacer.getB().getBluePrint().getSizeX();
        final int width = structurePlacer.getB().getBluePrint().getSizeZ();
        final int distance = Math.max(width, length) + MIN_ADDITIONAL_RANGE_TO_BUILD;

        return getWorkingPosition(distance, targetPosition, 0);
    }

    /**
     * Defines blocks that can be built for free.
     *
     * @param block The block to check if it is free.
     * @return true or false.
     */
    public static boolean isBlockFree(@Nullable final BlockState block)
    {
        return block == null
                 || BlockUtils.isWater(block)
                 || block.is(BlockTags.LEAVES)
                 || block.getBlock() == ModBlocks.blockDecorationPlaceholder;
    }

    /**
     * Let childs overwrite this if necessary.
     *
     * @return true if so.
     */
    protected boolean isAlreadyCleared()
    {
        return false;
    }

    /**
     * Get the current block being placed/removed. May be null if nothing is loaded yet.
     *
     * @return the current building position, or null.
     */
    @Nullable
    public BlockPos getCurrentBuildingPosition()
    {
        if (structurePlacer == null || structurePlacer.getA() == null || structurePlacer.getB() == null)
        {
            return null;
        }

        final BlockPos progressPos = structurePlacer.getA().getIterator().getProgressPos();
        if (progressPos == null || progressPos.equals(NULL_POS))
        {
            return null;
        }

        return structurePlacer.getB().getProgressPosInWorld(progressPos);
    }

    /**
     * Get the current working position for the worker. If workFrom is null calculate a new one.
     *
     * @return the current working position.
     */
    protected BlockPos getCurrentWorkingPosition()
    {
        return workFrom == null ? getWorkingPosition(getCurrentBuildingPosition()) : workFrom;
    }

    /**
     * Check if there is a StructureIterator to be build.
     *
     * @return true if we should start building.
     */
    protected boolean isThereAStructureToBuild()
    {
        if (structurePlacer == null || !structurePlacer.getB().hasBluePrint() || building.getWorkOrder() == null)
        {
            return false;
        }
        return true;
    }

    /**
     * Reduces the needed resources by 1.
     *
     * @param stack the stack which has been used now.
     */
    public void reduceNeededResources(final ItemStack stack)
    {
        /*
         * Nothing to be done here. Workers overwrite this if necessary.
         */
    }

    /**
     * Check for extra building options to do with each block.
     */
    public void checkForExtraBuildingActions()
    {
        /*
         * Override by worker if necessary.
         */
    }

    /**
     * Specific actions to handle a cancellation of a structure.
     */
    public void handleSpecificCancelActions()
    {
        /*
         * Child classes have to override this.
         */
    }

    /**
     * Check how much of a certain stuck is actually required.
     *
     * @param stack the stack to check.
     * @return the new stack with the correct amount.
     */
    @Nullable
    public ItemStack getTotalAmount(@Nullable final ItemStack stack)
    {
        return stack;
    }

    /**
     * Set the currentStructure to null.
     */
    public void resetCurrentStructure()
    {
        workFrom = null;
        structurePlacer = null;
        actionQueue.clear();
        blockToMine = null;
        blueprintLoadWaitTicks = 0;
        buildStartAnnounced = false;
        walkNotReadyStreak = 0;
        building.setProgressPos(null, null);
        worker.getCitizenData().setStatusPosition(null);
    }

    /**
     * Get the worker of the AI.
     *
     * @return the EntityCitizen object.
     */
    public AbstractEntityCitizen getWorker()
    {
        return this.worker;
    }

    /**
     * Get the current structure progress,
     *
     * @return the progress with the current stage.
     */
    public abstract Tuple<BlockPos, BuildingProgressStage> getProgressPos();

    /**
     * Check if a solid substitution block should be overwritten in a specific case.
     *
     * @param worldBlock    the worldblock.
     * @param worldMetadata the world metadata.
     * @return true if should be overwritten.
     */
    public abstract boolean shallReplaceSolidSubstitutionBlock(final Block worldBlock, final BlockState worldMetadata);

    /**
     * Searches a handy block to substitute a non-solid space which should be guaranteed solid.
     *
     * @param ignored the location the block should be at.
     * @param virtualBlocks blueprint blocks, fnc may return null if virtual block is not available (then use level instead for getting surrounding block states), pos argument is using world coords
     * @return the Block.
     */
    public BlockState getSolidSubstitution(final BlockPos ignored, final Function<BlockPos, @Nullable BlockState> virtualBlocks)
    {
        return building.getSetting(FILL_BLOCK).getValue().getBlock().defaultBlockState();
    }

    /**
     * Execute specific actions on loading a structure.
     */
    protected abstract void executeSpecificCompleteActions();

    /**
     * Check if the structure tusk has been canceled.
     *
     * @return true if reset to idle.
     */
    protected abstract boolean checkIfCanceled();
}
