import {Box} from '@mui/material';
import {type AddElementDialogProps} from './add-element-dialog-props';
import React, {useMemo, useState} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {PresetTab} from './tabs/preset-tab';
import {ElementTab} from './tabs/element-tab';
import {ElementInfoTab} from './tabs/element-info-tab';
import {MarketplaceTab} from './tabs/marketplace-tab';
import {MarketplaceModuleInfoTab} from './tabs/marketplace-module-info-tab';
import {AnyElement} from '../../models/elements/any-element';
import {type Preset} from '../../models/entities/preset';
import {PresetInfoTab} from './tabs/preset-info-tab';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {SelectionDialogShell} from '../../components/selection-dialog/selection-dialog-shell';
import {useRetainedDialogValue} from '../../hooks/use-retained-dialog-value';
import {
    useOptionalProcessNodeEditorContext,
} from '../../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {getSingleUseSectionAddDisabledReason} from '../../data/element-type/single-use-section-types';
import {createReusableUiDefinitionOptions} from './reusable-ui-definition-utils';
import {ReusableUiDefinitionsTab} from './tabs/reusable-ui-definitions-tab';


export function AddElementDialog(props: AddElementDialogProps) {
    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showPresetInfo, setShowPresetInfo] = useState<Preset>();
    const [showMarketplaceModuleId, setShowMarketplaceModuleId] = useState<string>();
    const renderTitle = useRetainedDialogValue(props.show, props.title ?? 'Formularelement hinzufügen');
    const renderPrimaryActionLabel = useRetainedDialogValue(props.show, props.primaryActionLabel ?? 'Hinzufügen');
    const renderPrimaryActionIcon = useRetainedDialogValue(props.show, props.primaryActionIcon ??
        <Add sx={{fontSize: 18}}/>);
    const showElementDetailsPanel = currentTab === 0 && showElementInfo != null;
    const showPresetDetailsPanel = currentTab === 1 && showPresetInfo != null;
    const showMarketplaceDetailsPanel = currentTab === 2 && showMarketplaceModuleId != null;
    const showDetailsPanel = showElementDetailsPanel || showPresetDetailsPanel || showMarketplaceDetailsPanel;

    const opec = useOptionalProcessNodeEditorContext();
    const reusableUiDefinitionOptions = useMemo(() => {
        if (opec == null || opec.incomingMetadata == null || opec.incomingMetadata.reusableUiDefinitions.length === 0) {
            return null;
        }
        const reusableDefinitions = createReusableUiDefinitionOptions(
            opec.incomingMetadata.reusableUiDefinitions,
            props,
        );

        return reusableDefinitions.length > 0 ? reusableDefinitions : null;
    }, [opec, props]);

    const handleClose = () => {
        props.onClose();
    };

    const handleAddElement = (element: AnyElement) => {
        if (getSingleUseSectionAddDisabledReason(props.parentElement, element.type) != null) {
            return;
        }

        props.onAddElements([element]);
    };

    return (
        <SelectionDialogShell
            open={props.show}
            onClose={handleClose}
            title={renderTitle}
            tabs={[
                {label: 'Elemente', value: 0},
                {label: 'Vorlagen', value: 1, hidden: props.hidePresets === true, disabled: true},
                {label: 'Prosuna Marktplatz', value: 2, hidden: props.hideProsunaMarketplace === true, disabled: true},
                {
                    label: 'UI-Definitionen',
                    value: 3,
                    hidden: reusableUiDefinitionOptions == null,
                    disabled: false,
                },
            ]}
            activeTab={currentTab}
            onTabChange={(value) => {
                setCurrentTab(Number(value));
            }}
            showDetailsPanel={showDetailsPanel}
            onExited={() => {
                // Keep the last active tab and detail selection alive until the close transition finishes.
                setCurrentTab(0);
                setShowElementInfo(undefined);
                setShowPresetInfo(undefined);
                setShowMarketplaceModuleId(undefined);
            }}
            detailsPanel={
                showElementDetailsPanel ? (
                    <ElementInfoTab
                        type={showElementInfo}
                        parentElement={props.parentElement}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        primaryActionDisabledReason={getSingleUseSectionAddDisabledReason(props.parentElement, showElementInfo)}
                        onClose={() => {
                            setShowElementInfo(undefined);
                        }}
                    />
                ) : showPresetDetailsPanel ? (
                    <PresetInfoTab
                        preset={showPresetInfo}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        onClose={() => {
                            setShowPresetInfo(undefined);
                        }}
                    />
                ) : showMarketplaceDetailsPanel ? (
                    <MarketplaceModuleInfoTab
                        marketplaceModuleId={showMarketplaceModuleId}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        onClose={() => {
                            setShowMarketplaceModuleId(undefined);
                        }}
                    />
                ) : undefined
            }
            closeTooltip="Schließen"
        >
            <Box
                sx={{
                    height: '100%',
                    overflowY: 'auto',
                }}
            >
                {
                    currentTab === 0 &&
                    <ElementTab
                        parentType={props.parentType}
                        parentElement={props.parentElement}
                        allParents={props.allParents}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showElementInfo={setShowElementInfo}
                        highlightedElement={showElementInfo}
                        limitElementTypes={props.limitElementTypes}
                        displayContext={props.displayContext}
                    />
                }
                {
                    currentTab === 1 &&
                    <PresetTab
                        parentType={props.parentType}
                        parentElement={props.parentElement}
                        allParents={props.allParents}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showPresetInfo={setShowPresetInfo}
                        highlightedPresetKey={showPresetInfo?.key}
                    />
                }
                {
                    currentTab === 2 &&
                    <MarketplaceTab
                        parentType={props.parentType}
                        parentElement={props.parentElement}
                        allParents={props.allParents}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showMarketplaceModuleId={setShowMarketplaceModuleId}
                        highlightedModuleId={showMarketplaceModuleId}
                    />
                }
                {
                    reusableUiDefinitionOptions != null &&
                    currentTab === 3 &&
                    <ReusableUiDefinitionsTab
                        options={reusableUiDefinitionOptions}
                        onAddElements={props.onAddElements}
                    />
                }
            </Box>
        </SelectionDialogShell>
    );
}
