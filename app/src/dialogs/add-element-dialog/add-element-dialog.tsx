import {Box} from '@mui/material';
import {type AddElementDialogProps} from './add-element-dialog-props';
import React, {useEffect, useState} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {PresetTab} from './tabs/preset-tab';
import {ElementTab} from './tabs/element-tab';
import {ElementInfoTab} from './tabs/element-info-tab';
import {StoreTab} from './tabs/store-tab';
import {ModuleInfoTab} from './tabs/module-info-tab';
import {AnyElement} from '../../models/elements/any-element';
import {type Preset} from '../../models/entities/preset';
import {PresetInfoTab} from './tabs/preset-info-tab';
import Add from '@mui/icons-material/Add';
import {SelectionDialogShell} from '../../components/selection-dialog/selection-dialog-shell';
import {useRetainedDialogValue} from '../../hooks/use-retained-dialog-value';

const recentElementTypesStorageKey = 'gover-add-element-dialog-recent-element-types';
const maxRecentElementTypes = 3;

function loadRecentElementTypes(): ElementType[] {
    if (typeof window === 'undefined') {
        return [];
    }

    try {
        const value = window.localStorage.getItem(recentElementTypesStorageKey);
        if (value == null) {
            return [];
        }

        const parsedValue = JSON.parse(value);
        if (!Array.isArray(parsedValue)) {
            return [];
        }

        return parsedValue
            .map((entry) => Number(entry))
            .filter((entry): entry is ElementType => Number.isInteger(entry) && ElementType[entry] != null)
            .slice(0, maxRecentElementTypes);
    } catch {
        return [];
    }
}

function rememberElementType(recentElementTypes: ElementType[], elementType: ElementType): ElementType[] {
    return [elementType, ...recentElementTypes.filter((recentElementType) => recentElementType !== elementType)]
        .slice(0, maxRecentElementTypes);
}

function persistRecentElementTypes(recentElementTypes: ElementType[]): void {
    if (typeof window === 'undefined') {
        return;
    }

    try {
        window.localStorage.setItem(recentElementTypesStorageKey, JSON.stringify(recentElementTypes));
    } catch {
    }
}

export function AddElementDialog(props: AddElementDialogProps) {
    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showPresetInfo, setShowPresetInfo] = useState<Preset>();
    const [showModuleId, setShowModuleId] = useState<string>();
    const [recentElementTypes, setRecentElementTypes] = useState<ElementType[]>(loadRecentElementTypes);
    const renderTitle = useRetainedDialogValue(props.show, props.title ?? 'Formularelement hinzufügen');
    const renderPrimaryActionLabel = useRetainedDialogValue(props.show, props.primaryActionLabel ?? 'Hinzufügen');
    const renderPrimaryActionIcon = useRetainedDialogValue(props.show, props.primaryActionIcon ?? <Add sx={{fontSize: 18}}/>);
    const showElementDetailsPanel = currentTab === 0 && showElementInfo != null;
    const showPresetDetailsPanel = currentTab === 1 && showPresetInfo != null;
    const showStoreDetailsPanel = currentTab === 2 && showModuleId != null;
    const showDetailsPanel = showElementDetailsPanel || showPresetDetailsPanel || showStoreDetailsPanel;

    useEffect(() => {
        persistRecentElementTypes(recentElementTypes);
    }, [recentElementTypes]);

    const handleClose = () => {
        props.onClose();
    };

    const handleAddElement = (element: AnyElement) => {
        if (currentTab === 0) {
            setRecentElementTypes((previous) => rememberElementType(previous, element.type));
        }

        props.onAddElement(element);
    };

    return (
        <SelectionDialogShell
            open={props.show}
            onClose={handleClose}
            title={renderTitle}
            tabs={[
                {label: 'Elemente', value: 0},
                {label: 'Vorlagen', value: 1, hidden: props.hidePresets === true},
                {label: 'Gover Marktplatz', value: 2, hidden: props.hideGoverStore === true},
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
                setShowModuleId(undefined);
            }}
            detailsPanel={
                showElementDetailsPanel ? (
                    <ElementInfoTab
                        type={showElementInfo}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
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
                ) : showStoreDetailsPanel ? (
                    <ModuleInfoTab
                        moduleId={showModuleId}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        onClose={() => {
                            setShowModuleId(undefined);
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
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showElementInfo={setShowElementInfo}
                        highlightedElement={showElementInfo}
                        limitElementTypes={props.limitElementTypes}
                        recentElementTypes={recentElementTypes}
                    />
                }
                {
                    currentTab === 1 &&
                    <PresetTab
                        parentType={props.parentType}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showPresetInfo={setShowPresetInfo}
                        highlightedPresetKey={showPresetInfo?.key}
                    />
                }
                {
                    currentTab === 2 &&
                    <StoreTab
                        parentType={props.parentType}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showModuleId={setShowModuleId}
                        highlightedModuleId={showModuleId}
                    />
                }
            </Box>
        </SelectionDialogShell>
    );
}
