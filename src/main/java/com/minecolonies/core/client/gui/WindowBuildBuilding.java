package com.minecolonies.core.client.gui;

import com.ldtteam.blockui.Color;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.DropDownList;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.storage.ClientFutureProcessor;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.workers.util.IBuilderUndestroyable;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.LoadOnlyStructureHandler;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.views.AbstractBuildingBuilderView;
import com.minecolonies.core.network.messages.server.colony.building.BuildPickUpMessage;
import com.minecolonies.core.network.messages.server.colony.building.BuildRequestMessage;
import com.minecolonies.core.network.messages.server.colony.building.BuildingSetStyleMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;
import static com.minecolonies.api.util.constant.TranslationConstants.ACTION_BUILD;
import static com.minecolonies.api.util.constant.TranslationConstants.ACTION_UPGRADE;
import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for selecting the style and confirming the resources.
 */
public class WindowBuildBuilding extends AbstractWindowSkeleton
{
    /**
     * Predicate defining things we don't want the builders to ever touch.
     */
    protected TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> DONT_TOUCH_PREDICATE = (info, worldPos, handler) ->
    {
        final BlockState worldState = handler.getWorld().getBlockState(worldPos);

        return worldState.getBlock() instanceof IBuilderUndestroyable
                 || worldState.getBlock() == Blocks.BEDROCK
                 || (info.getBlockInfo().getState().getBlock() instanceof AbstractBlockHut && handler.getCenterPos().equals(worldPos));
    };

    /**
     * The view of the current building.
     */
    private final IBuildingView building;

    /**
     * Contains all resources needed for a certain build.
     */
    private final Map<String, ItemStorage> resources = new HashMap<>();

    /**
     * Drop down list for style.
     */
    private DropDownList stylesDropDownList;

    /**
     * Drop down list for style.
     */
    private DropDownList buildersDropDownList;

    /**
     * Ink on parchment (opaque black — BlockUI name colors are RGB|0xFF000000).
     */
    private static final int INK = Color.getByName("black", 0xFF000000);

    /**
     * List of style for the section.
     */
    @NotNull
    private List<String> styles = new ArrayList<>();

    /**
     * List of style for the section.
     */
    @NotNull
    private List<Tuple<String, BlockPos>> builders = new ArrayList<>();

    /**
     * Update delay.
     */
    private int tick;

    /**
     * Monotonic id so stale async blueprint callbacks cannot overwrite a newer style selection.
     */
    private int resourceRequestId = 0;

    /**
     * Constructor for the window when the player wants to hire a worker for a certain job.
     *
     * @param c        the colony view.
     * @param building the building.
     */
    public WindowBuildBuilding(final IColonyView c, final IBuildingView building)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/windowbuildbuilding.xml"));
        this.building = building;

        initStyleNavigation();

        PaneBuilders.singleLineTooltip(Component.translatable(building.getHoverWarningForLevel()), findPaneOfTypeByID(BUTTON_BUILD, Button.class));

        registerButton(BUTTON_BUILD, this::confirmClicked);
        registerButton(BUTTON_CANCEL, this::cancelClicked);
        registerButton(BUTTON_REPAIR, this::repairClicked);
        registerButton(BUTTON_DECONSTRUCT_BUILDING, this::deconstructBuildingClicked);
        registerButton(BUTTON_PICKUP_BUILDING, this::pickUpBuilding);

        final Button buttonBuild = findPaneOfTypeByID(BUTTON_BUILD, Button.class);

        if (building.getBuildingLevel() == 0)
        {
            buttonBuild.setText(Component.translatableEscape("com.minecolonies.coremod.gui.workerhuts.build"));
            findPaneOfTypeByID(BUTTON_REPAIR, Button.class).hide();
            findPaneOfTypeByID(BUTTON_DECONSTRUCT_BUILDING, Button.class).hide();
            findPaneOfTypeByID(BUTTON_PICKUP_BUILDING, Button.class).show();
        }
        else if (!canBeUpgraded())
        {
            buttonBuild.hide();
        }
        else
        {
            buttonBuild.setText(Component.translatableEscape(ACTION_UPGRADE));
        }

        if (building.isDeconstructed())
        {
            findPaneOfTypeByID(BUTTON_REPAIR, Button.class).setText(Component.translatableEscape(ACTION_BUILD));
            findPaneOfTypeByID(BUTTON_PICKUP_BUILDING, Button.class).show();
        }
    }

    /**
     * Check if this one can be upgraded.
     * @return true if so.
     */
    public boolean canBeUpgraded()
    {
        final IBuildingView parentBuilding = building.getColony().getClientBuildingManager().getBuilding(building.getParent());
        return building.getBuildingLevel() < building.getBuildingMaxLevel() && (parentBuilding == null || building.getBuildingLevel() < parentBuilding.getBuildingLevel() || parentBuilding.getBuildingLevel() >= parentBuilding.getBuildingMaxLevel());
    }

    /**
     * When the pickup building button was clicked.
     */
    private void pickUpBuilding()
    {
        new BuildPickUpMessage(building).sendToServer();
        close();
    }

    /**
     * When the deconstruct building button has been clicked.
     */
    private void deconstructBuildingClicked()
    {
        final BlockPos builder = buildersDropDownList.getSelectedIndex() == 0 ? BlockPos.ZERO : builders.get(buildersDropDownList.getSelectedIndex()).getB();
        new BuildRequestMessage(building, BuildRequestMessage.Mode.REMOVE, builder).sendToServer();
        cancelClicked();
    }

    /**
     * On cancel button.
     */
    private void cancelClicked()
    {
        building.openGui(false);
    }

    /**
     * On confirm button.
     */
    private void confirmClicked()
    {
        final BlockPos builder = buildersDropDownList.getSelectedIndex() == 0 ? BlockPos.ZERO : builders.get(buildersDropDownList.getSelectedIndex()).getB();

        if (!building.getHoverWarningForLevel().isEmpty())
        {
            new WindowConfirm(this, () -> triggerConfirmAction(builder), "com.minecolonies.core.gui.build.confirm.title", building.getHoverWarningForLevel()).open();
            return;
        }
        triggerConfirmAction(builder);
    }

    /**
     * Trigger confirm action.
     * @param builder the position of the builder that was selected.
     */
    private void triggerConfirmAction(final BlockPos builder)
    {
        final int styleIndex = stylesDropDownList.getSelectedIndex();
        if (styleIndex < 0 || styleIndex >= styles.size())
        {
            Log.getLogger().warn("Build Building: no valid style selected; refusing empty claim");
            return;
        }
        final String stylePack = styles.get(styleIndex);
        final String blueprintPath = resolveBlueprintPathForPack(stylePack);
        // Always push pack+path so orphan/empty-path huts still create a work order.
        new BuildingSetStyleMessage(building, stylePack, blueprintPath == null ? "" : blueprintPath).sendToServer();
        if (building.getBuildingLevel() == building.getBuildingMaxLevel())
        {
            new BuildRequestMessage(building, BuildRequestMessage.Mode.REPAIR, builder).sendToServer();
        }
        else
        {
            new BuildRequestMessage(building, BuildRequestMessage.Mode.BUILD, builder).sendToServer();
        }
        cancelClicked();
    }

    /**
     * Action when repair button is clicked.
     */
    private void repairClicked()
    {
        final BlockPos builder = buildersDropDownList.getSelectedIndex() == 0 ? BlockPos.ZERO : builders.get(buildersDropDownList.getSelectedIndex()).getB();
        new BuildRequestMessage(building, BuildRequestMessage.Mode.REPAIR, builder).sendToServer();
        cancelClicked();
    }

    /**
     * Update the builders list but try to keep the same one.
     */
    private void updateBuilders()
    {
        builders.clear();
        builders.add(new Tuple<>(Component.translatableEscape(ModJobs.builder.get().getTranslationKey()).getString() + ":", BlockPos.ZERO));
        builders.addAll(building.getColony().getClientBuildingManager().getBuildings().values().stream()
                          .filter(build -> build instanceof AbstractBuildingBuilderView && !((AbstractBuildingBuilderView) build).getWorkerName().isEmpty()
                                             && build.getBuildingType() != ModBuildings.miner.get())
                          .map(build -> new Tuple<>(((AbstractBuildingBuilderView) build).getWorkerName(), build.getPosition()))
                          .sorted(Comparator.comparing(item -> item.getB().distSqr(building.getPosition())))
                          .collect(Collectors.toList()));

        initBuilderNavigation();
    }

    /** Preferred fallback packs when the building's stored pack is invalid (e.g. Decorations). */
    private static final List<String> PREFERRED_PACKS = List.of(
      "Medieval Oak",
      "Minecolonies Original",
      "Colonial",
      "Nordic Spruce",
      "Desert Oasis");

    /**
     * Update the styles list but try to keep the same one.
     * Quidoms: only packs that actually contain this hut's blueprint — never keep Decorations /
     * decoration-only packs or empty paths on the Build UI.
     */
    private void updateStyles()
    {
        styles = new ArrayList<>();

        final IBuildingView parentBuilding = building.getParent().equals(BlockPos.ZERO)
                                               ? null
                                               : building.getColony().getClientBuildingManager().getBuilding(building.getParent());
        final List<String> pathCandidates = blueprintPathCandidates();
        if (pathCandidates.isEmpty())
        {
            Log.getLogger().warn("Build UI: no blueprint path for building at {} (pack='{}' path='{}')",
              building.getPosition(), building.getStructurePack(), building.getStructurePath());
        }

        if (parentBuilding != null)
        {
            final String parentPack = resolveValidPackName(parentBuilding.getStructurePack(), pathCandidates);
            if (parentPack != null)
            {
                styles.add(parentPack);
            }
        }
        else
        {
            for (final StructurePackMeta pack : StructurePacks.getPackMetas())
            {
                if (packHasAnyBlueprint(pack, pathCandidates))
                {
                    styles.add(pack.getName());
                }
            }
            styles.sort(String.CASE_INSENSITIVE_ORDER);
        }

        // Never fall back to an unverified pack name (that stuck us on "Decorations").
        if (styles.isEmpty())
        {
            for (final String preferred : PREFERRED_PACKS)
            {
                final StructurePackMeta pack = StructurePacks.getStructurePack(preferred);
                if (pack != null && packHasAnyBlueprint(pack, pathCandidates))
                {
                    styles.add(pack.getName());
                    break;
                }
            }
        }

        if (!styles.isEmpty())
        {
            int newIndex = indexOfValidStoredPack(pathCandidates);
            if (newIndex < 0)
            {
                newIndex = preferredStyleIndex();
            }
            stylesDropDownList.setSelectedIndex(Math.max(0, newIndex));
        }
        else
        {
            stylesDropDownList.setSelectedIndex(-1);
        }

        final boolean canSwitch = styles.size() > 1;
        findPaneOfTypeByID(BUTTON_PREVIOUS_STYLE_ID, Button.class).setEnabled(canSwitch);
        findPaneOfTypeByID(DROPDOWN_STYLE_ID, DropDownList.class).setEnabled(canSwitch || !styles.isEmpty());
        findPaneOfTypeByID(BUTTON_NEXT_STYLE_ID, Button.class).setEnabled(canSwitch);
    }

    /**
     * Index of the building's stored pack if it is in {@link #styles}; else -1.
     */
    private int indexOfValidStoredPack(final List<String> pathCandidates)
    {
        final String stored = building.getStructurePack();
        if (stored == null || stored.isEmpty())
        {
            return -1;
        }
        int idx = styles.indexOf(stored);
        if (idx >= 0)
        {
            return idx;
        }
        final StructurePackMeta resolved = StructurePacks.getStructurePack(stored);
        if (resolved != null)
        {
            idx = styles.indexOf(resolved.getName());
            if (idx >= 0 && packHasAnyBlueprint(resolved, pathCandidates))
            {
                return idx;
            }
        }
        return -1;
    }

    private int preferredStyleIndex()
    {
        for (final String preferred : PREFERRED_PACKS)
        {
            final int idx = styles.indexOf(preferred);
            if (idx >= 0)
            {
                return idx;
            }
        }
        return 0;
    }

    @Nullable
    private static String resolveValidPackName(@Nullable final String packName, final List<String> pathCandidates)
    {
        if (packName == null || packName.isEmpty())
        {
            return null;
        }
        final StructurePackMeta pack = StructurePacks.getStructurePack(packName);
        if (pack != null && packHasAnyBlueprint(pack, pathCandidates))
        {
            return pack.getName();
        }
        return null;
    }

    /**
     * Blueprint sub-paths to probe. Derives {@code fundamentals/&lt;hut&gt;N.blueprint} when the
     * building's stored path is empty/corrupt (recreated orphan huts).
     */
    @NotNull
    private List<String> blueprintPathCandidates()
    {
        final List<String> candidates = new ArrayList<>();
        final String structurePath = building.getStructurePath();
        if (structurePath != null && !structurePath.isBlank())
        {
            candidates.add(structurePath.trim());
            addLeveledVariants(candidates, structurePath.trim());
        }

        final String derived = deriveHutBlueprintBase();
        if (derived != null)
        {
            addLeveledVariants(candidates, derived + ".blueprint");
            // Flat legacy packs (Stone): hut file at pack root.
            if (derived.contains("/"))
            {
                final String hutFile = derived.substring(derived.lastIndexOf('/') + 1);
                addLeveledVariants(candidates, hutFile + ".blueprint");
            }
        }

        final List<String> withAlts = new ArrayList<>(candidates);
        for (final String path : candidates)
        {
            withAlts.addAll(StructurePacks.alternateBlueprintPaths(building.getStructurePack(), path));
        }
        return withAlts.stream().filter(p -> p != null && !p.isBlank()).distinct().collect(Collectors.toList());
    }

    private void addLeveledVariants(final List<String> candidates, final String structurePath)
    {
        String name = structurePath.replace(".blueprint", "");
        if (name.isEmpty())
        {
            return;
        }
        // Strip trailing level digit when present (townhall1 → townhall).
        final char last = name.charAt(name.length() - 1);
        final String base = Character.isDigit(last) ? name.substring(0, name.length() - 1) : name;

        int nextLevel = building.getBuildingLevel();
        if (canBeUpgraded())
        {
            nextLevel = building.getBuildingLevel() + 1;
        }
        if (nextLevel < 1)
        {
            nextLevel = 1;
        }
        candidates.add(base + nextLevel + ".blueprint");
        candidates.add(base + "1.blueprint");
    }

    /**
     * Default blueprint folder/name for this hut type when TE path is missing.
     */
    @Nullable
    private String deriveHutBlueprintBase()
    {
        try
        {
            if (building.getBuildingType() == null || building.getBuildingType().getRegistryName() == null)
            {
                return null;
            }
            final String hut = building.getBuildingType().getRegistryName().getPath();
            if (hut.isEmpty())
            {
                return null;
            }
            final String mapped = BlueprintMapping.getPathMapping("", hut);
            if (mapped == null || mapped.isEmpty())
            {
                return "fundamentals/" + hut;
            }
            return mapped;
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    /**
     * Blueprint path used for the currently selected style (first existing candidate in that pack).
     */
    @Nullable
    private String resolveBlueprintPathForPack(@NotNull final String stylePack)
    {
        final StructurePackMeta pack = StructurePacks.getStructurePack(stylePack);
        if (pack == null)
        {
            return null;
        }
        for (final String path : blueprintPathCandidates())
        {
            if (packHasBlueprint(pack, path))
            {
                return path;
            }
        }
        // Still return best leveled guess so StructurePacks can try aliases.
        final List<String> candidates = blueprintPathCandidates();
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean packHasAnyBlueprint(@NotNull final StructurePackMeta pack, @NotNull final List<String> paths)
    {
        for (final String path : paths)
        {
            if (packHasBlueprint(pack, path))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the pack contains the given blueprint sub-path.
     */
    private static boolean packHasBlueprint(@NotNull final StructurePackMeta pack, @Nullable final String structurePath)
    {
        if (structurePath == null || structurePath.isBlank())
        {
            return false;
        }
        try
        {
            return Files.exists(pack.getPath().resolve(pack.getNormalizedSubPath(structurePath)));
        }
        catch (final Exception ignored)
        {
            return false;
        }
    }

    /**
     * Clears and resets/updates all resources for the currently selected style pack.
     */
    private void updateResources()
    {
        tick = 20;

        if (stylesDropDownList.getSelectedIndex() == -1 || styles.isEmpty())
        {
            resources.clear();
            updateResourceList();
            return;
        }

        final Level world = Minecraft.getInstance().level;
        final String stylePack = styles.get(stylesDropDownList.getSelectedIndex());
        final String blueprintPath = resolveBlueprintPathForPack(stylePack);
        if (blueprintPath == null || blueprintPath.isBlank())
        {
            // Selected pack cannot serve this hut — drop it and jump to a valid pack.
            skipInvalidStyle(stylePack);
            return;
        }

        final int requestId = ++resourceRequestId;

        // Drop stale rows immediately so a slow/failed load cannot leave the previous style's list up.
        resources.clear();
        updateResourceList();

        // Soft-fail + pack/path alias inside StructurePacks.getBlueprint so Alternative packs still fill the list.
        ClientFutureProcessor.queueBlueprint(new ClientFutureProcessor.BlueprintProcessingData(
          StructurePacks.getBlueprintFuture(stylePack, blueprintPath, true, world.registryAccess()), (blueprint -> {
            if (requestId != resourceRequestId)
            {
                return;
            }

            resources.clear();
            if (blueprint == null)
            {
                skipInvalidStyle(stylePack);
                return;
            }

            // Restore action buttons after a previous missing-blueprint style hid them.
            if (building.getBuildingLevel() == 0)
            {
                findPaneOfTypeByID(BUTTON_BUILD, Button.class).show();
                findPaneOfTypeByID(BUTTON_REPAIR, Button.class).hide();
                findPaneOfTypeByID(BUTTON_PICKUP_BUILDING, Button.class).show();
            }
            else
            {
                if (canBeUpgraded())
                {
                    findPaneOfTypeByID(BUTTON_BUILD, Button.class).show();
                }
                findPaneOfTypeByID(BUTTON_REPAIR, Button.class).show();
                if (!building.isDeconstructed())
                {
                    findPaneOfTypeByID(BUTTON_PICKUP_BUILDING, Button.class).hide();
                }
            }

            blueprint.setRotationMirror(building.getRotationMirror(), Minecraft.getInstance().level);
            StructurePlacer placer = new StructurePlacer(new LoadOnlyStructureHandler(Minecraft.getInstance().level, building.getPosition(), blueprint, RotationMirror.NONE));
            StructurePhasePlacementResult result;
            BlockPos progressPos = NULL_POS;

            do
            {
                result = placer.executeStructureStep(world, null, progressPos, StructurePlacer.Operation.GET_RES_REQUIREMENTS,
                  () -> placer.getIterator().increment(DONT_TOUCH_PREDICATE.and((info, pos, handler) -> false)), true);

                progressPos = result.getIteratorPos();
                for (final ItemStack stack : result.getBlockResult().getRequiredItems())
                {
                    addNeededResource(stack, stack.getCount());
                }
            }
            while (result != null && result.getBlockResult().getResult() != BlockPlacementResult.Result.FINISHED);

            if (requestId != resourceRequestId)
            {
                return;
            }

            window.findPaneOfTypeByID(LIST_RESOURCES, ScrollingList.class).refreshElementPanes();
            updateResourceList();

        })));

    }

    /**
     * Drop a pack that cannot load this hut's blueprint and select the next valid style.
     */
    private void skipInvalidStyle(@Nullable final String badPack)
    {
        if (badPack != null)
        {
            styles.removeIf(s -> s.equalsIgnoreCase(badPack));
            Log.getLogger().info("Build UI: skipped style '{}' (no hut blueprint); {} pack(s) remain",
              badPack, styles.size());
        }
        if (styles.isEmpty())
        {
            updateStyles();
        }
        if (styles.isEmpty())
        {
            resources.clear();
            updateResourceList();
            findPaneOfTypeByID(BUTTON_BUILD, Button.class).hide();
            findPaneOfTypeByID(BUTTON_REPAIR, Button.class).hide();
            findPaneOfTypeByID(BUTTON_PREVIOUS_STYLE_ID, Button.class).setEnabled(false);
            findPaneOfTypeByID(BUTTON_NEXT_STYLE_ID, Button.class).setEnabled(false);
            return;
        }
        final boolean canSwitch = styles.size() > 1;
        findPaneOfTypeByID(BUTTON_PREVIOUS_STYLE_ID, Button.class).setEnabled(canSwitch);
        findPaneOfTypeByID(DROPDOWN_STYLE_ID, DropDownList.class).setEnabled(true);
        findPaneOfTypeByID(BUTTON_NEXT_STYLE_ID, Button.class).setEnabled(canSwitch);
        stylesDropDownList.setSelectedIndex(preferredStyleIndex());
        // Force resource refresh for the newly selected pack.
        updateResources();
    }

    /**
     * Add a new resource to the needed list.
     *
     * @param res    the resource.
     * @param amount the amount.
     */
    public void addNeededResource(@Nullable final ItemStack res, final int amount)
    {
        if (ItemStackUtils.isEmpty(res) || amount == 0)
        {
            return;
        }
        final int hashCode = res.getComponentsPatch().hashCode();
        final String key = res.getItem().getDescriptionId() + "-" + hashCode;
        ItemStorage resource = resources.get(key);
        if (resource == null)
        {
            resource = new ItemStorage(res);
            resource.setAmount(amount);
        }
        else
        {
            resource.setAmount(resource.getAmount() + amount);
        }
        resources.put(key, resource);
    }

    /**
     * Initialise the previous/next and drop down list for style.
     */
    private void initStyleNavigation()
    {
        registerButton(BUTTON_PREVIOUS_STYLE_ID, this::previousStyle);
        registerButton(BUTTON_NEXT_STYLE_ID, this::nextStyle);
        stylesDropDownList = findPaneOfTypeByID(DROPDOWN_STYLE_ID, DropDownList.class);
        stylesDropDownList.setHandler(this::onStyleDropDownChanged);
        stylesDropDownList.setDataProvider(new DropDownList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return styles.size();
            }

            @Override
            public MutableComponent getLabel(final int index)
            {
                if (index >= 0 && index < styles.size())
                {
                    return Component.literal(styles.get(index));
                }
                return Component.empty();
            }
        });
    }

    /**
     * Initialise the builder setup..
     */
    private void initBuilderNavigation()
    {
        buildersDropDownList = findPaneOfTypeByID(DROPDOWN_BUILDER_ID, DropDownList.class);
        buildersDropDownList.setDataProvider(new DropDownList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return builders.size();
            }

            @Override
            public MutableComponent getLabel(final int index)
            {
                if (index >= 0 && index < builders.size())
                {
                    return Component.literal(builders.get(index).getA());
                }
                return Component.empty();
            }
        });
        buildersDropDownList.setSelectedIndex(0);
    }

    /**
     * called every time one of the dropdownlist changed.
     *
     * @param list the dropdown list which change
     */
    private void onStyleDropDownChanged(final DropDownList list)
    {
        updateResources();
    }

    /**
     * Change to the next style.
     */
    private void nextStyle()
    {
        if (styles.isEmpty())
        {
            return;
        }
        final int current = stylesDropDownList.getSelectedIndex();
        final int next = (Math.max(current, 0) + 1) % styles.size();
        stylesDropDownList.setSelectedIndex(next);
        // Handler fires on index change; force refresh if setSelectedIndex no-ops (same index edge cases).
        if (stylesDropDownList.getSelectedIndex() == current)
        {
            updateResources();
        }
    }

    /**
     * Change to the previous style.
     */
    private void previousStyle()
    {
        if (styles.isEmpty())
        {
            return;
        }
        final int current = stylesDropDownList.getSelectedIndex();
        final int prev = current <= 0 ? styles.size() - 1 : current - 1;
        stylesDropDownList.setSelectedIndex(prev);
        if (stylesDropDownList.getSelectedIndex() == current)
        {
            updateResources();
        }
    }

    /**
     * Called when the GUI has been opened. Will fill the fields and lists.
     */
    @Override
    public void onOpened()
    {
        updateStyles();
        updateBuilders();
        updateResources();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (resources.isEmpty() && tick > 0 && --tick == 0)
        {
            updateResources();
        }
    }

    public void updateResourceList()
    {
        final ScrollingList recourseList = findPaneOfTypeByID(LIST_RESOURCES, ScrollingList.class);
        recourseList.enable();
        recourseList.show();
        final List<ItemStorage> tempRes = new ArrayList<>(resources.values());

        //Creates a dataProvider for the unemployed recourseList.
        recourseList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return tempRes.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStorage resource = tempRes.get(index);
                final Text resourceLabel = rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class);
                final Text quantityLabel = rowPane.findPaneOfTypeByID(RESOURCE_QUANTITY_MISSING, Text.class);
                final ItemIcon icon = rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class);
                final ItemStack displayStack = resource.getItemStack();
                // Plain translated string (Domum nested components) + force panes visible/opaque.
                resourceLabel.setText(Component.literal(displayStack.getHoverName().getString()));
                quantityLabel.setText(Component.literal(Integer.toString(resource.getAmount())));
                resourceLabel.setColors(INK);
                quantityLabel.setColors(INK);
                resourceLabel.show();
                quantityLabel.show();
                final ItemStack itemIcon = displayStack.copy();
                itemIcon.setCount(1);
                icon.setItem(itemIcon);
                icon.show();
            }
        });
    }
}
