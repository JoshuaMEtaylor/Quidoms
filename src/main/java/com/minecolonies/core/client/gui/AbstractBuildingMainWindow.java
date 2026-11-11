package com.minecolonies.core.client.gui;

import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.views.AbstractBuildingView;
import com.minecolonies.core.network.messages.server.colony.OpenInventoryMessage;
import com.minecolonies.core.network.messages.server.colony.building.BuildRequestMessage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * Base window class for the main window behind buildings.
 *
 * @param <B> Class extending {@link AbstractBuildingView}.
 */
public abstract class AbstractBuildingMainWindow<B extends IBuildingView> extends AbstractBuildingWindow<B>
{
    /**
     * The title displayed at the top of the window showing the building name.
     */
    private final Text title;

    /**
     * Opens build options / resource requirements.
     */
    private final Button buttonBuild;

    /**
     * Cancels an in-progress build, upgrade, repair, or deconstruction.
     */
    private final Button buttonCancelBuild;

    /**
     * Constructor for the windows that are associated with buildings.
     *
     * @param buildingView Class extending {@link AbstractBuildingView}.
     * @param resource     window resource location.
     */
    public AbstractBuildingMainWindow(final B buildingView, final ResourceLocation resource)
    {
        super(buildingView, resource);

        registerButton(BUTTON_BUILD, this::buildClicked);
        registerButton(BUTTON_CANCEL_BUILD, this::cancelBuildClicked);
        registerButton(BUTTON_INFO, this::infoClicked);
        registerButton(BUTTON_INVENTORY, this::inventoryClicked);
        registerButton(BUTTON_EDIT_NAME, this::editName);
        registerButton(BUTTON_ALLINVENTORY, this::allInventoryClicked);

        title = findPaneOfTypeByID(LABEL_BUILDING_NAME, Text.class);
        buttonBuild = findPaneOfTypeByID(BUTTON_BUILD, Button.class);
        buttonCancelBuild = findPaneOfTypeByID(BUTTON_CANCEL_BUILD, Button.class);
        Button buttonInfo = findPaneOfTypeByID(BUTTON_INFO, Button.class);
        if (buttonInfo != null)
        {
            buttonInfo.setVisible(I18n.exists(PARTIAL_INFO_TEXT + buildingView.getBuildingType().getTranslationKey().replace("com.minecolonies.building.", "") + ".0"));
        }
    }

    @Override
    public void setPage(final boolean relative, final int page)
    {
        super.setPage(relative, page);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    /**
     * Edit custom name action.
     */
    private void editName()
    {
        new WindowHutNameEntry(buildingView).open();
    }

    /**
     * Action when info button is clicked.
     */
    private void infoClicked()
    {
        new WindowInfo(buildingView).open();
    }

    /**
     * Action when allInventory button is clicked.
     */
    private void allInventoryClicked()
    {
        new WindowHutAllInventory(buildingView, this).open();
    }

    /**
     * Action when build options button is clicked.
     * Mid-build (or mid-repair/deconstruct) opens the builder contribution /
     * resource-input UI so players can deposit required materials. Otherwise
     * opens the style/requirements picker ({@link WindowBuildBuilding}).
     */
    private void buildClicked()
    {
        if (isWorkUnderway() && openContributionMenuIfPresent())
        {
            return;
        }

        new WindowBuildBuilding(buildingView.getColony(), buildingView).open();
    }

    /**
     * @return true when this hut has an active build, repair, or deconstruct.
     */
    private boolean isWorkUnderway()
    {
        return buildingView.isBuilding() || buildingView.isRepairing() || buildingView.isDeconstructing();
    }

    /**
     * Open {@code BuildingResourcesModuleView}'s contribution window when this
     * building tracks required resources (builder / structure-builder huts).
     *
     * @return true if a contribution window was opened.
     */
    private boolean openContributionMenuIfPresent()
    {
        final BuildingResourcesModuleView resources =
          buildingView.getModuleViewByType(BuildingResourcesModuleView.class);
        if (resources == null)
        {
            return false;
        }

        final BOWindow contribution = resources.getWindow();
        if (contribution == null)
        {
            return false;
        }

        contribution.open();
        return true;
    }

    /**
     * Action when cancel-build button is clicked.
     */
    private void cancelBuildClicked()
    {
        if (buildingView.isBuilding())
        {
            new BuildRequestMessage(buildingView, BuildRequestMessage.Mode.BUILD, BlockPos.ZERO).sendToServer();
        }
        else if (buildingView.isRepairing())
        {
            new BuildRequestMessage(buildingView, BuildRequestMessage.Mode.REPAIR, BlockPos.ZERO).sendToServer();
        }
        else if (buildingView.isDeconstructing())
        {
            new BuildRequestMessage(buildingView, BuildRequestMessage.Mode.REMOVE, BlockPos.ZERO).sendToServer();
        }
    }

    /**
     * Action when a button opening an inventory is clicked.
     */
    private void inventoryClicked()
    {
        new OpenInventoryMessage(buildingView).sendToServer();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        updateButtonBuild(buildingView);
    }

    /**
     * Keep Build Options available, and show Cancel beneath it while work is underway.
     *
     * @param buildingView the view to update from.
     */
    private void updateButtonBuild(final IBuildingView buildingView)
    {
        if (buttonBuild != null)
        {
            buttonBuild.setText(Component.translatableEscape(ACTION_BUILD_REPAIR));
        }

        if (buttonCancelBuild == null)
        {
            return;
        }

        if (buildingView.isBuilding())
        {
            buttonCancelBuild.setVisible(true);
            if (buildingView.getBuildingLevel() == 0)
            {
                buttonCancelBuild.setText(Component.translatableEscape(ACTION_CANCEL_BUILD));
            }
            else
            {
                buttonCancelBuild.setText(Component.translatableEscape(ACTION_CANCEL_UPGRADE));
            }
        }
        else if (buildingView.isRepairing())
        {
            buttonCancelBuild.setVisible(true);
            buttonCancelBuild.setText(Component.translatableEscape(ACTION_CANCEL_REPAIR));
        }
        else if (buildingView.isDeconstructing())
        {
            buttonCancelBuild.setVisible(true);
            buttonCancelBuild.setText(Component.translatableEscape(ACTION_CANCEL_DECONSTRUCTION));
        }
        else
        {
            buttonCancelBuild.setVisible(false);
        }
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        setPage(false, 0);

        if (title != null)
        {
            final MutableComponent component = Component.translatableEscape(buildingView.getBuildingDisplayName());
            final MutableComponent componentWithLevel = component.append(" ").append(String.valueOf(buildingView.getBuildingLevel()));
            title.setText(componentWithLevel);
        }

        updateButtonBuild(buildingView);
    }
}
