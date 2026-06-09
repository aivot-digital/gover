import {Box} from '@mui/material';
import {type AddElementDialogProps} from './add-element-dialog-props';
import React, {useMemo, useState} from 'react';
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
import {
    useOptionalProcessNodeEditorContext,
} from '../../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {SelectionListRow} from '../../components/selection-dialog/selection-list-row';
import {getElementIcon, getElementIconForType} from '../../data/element-type/element-icons';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';


export function AddElementDialog(props: AddElementDialogProps) {
    const dispatch = useAppDispatch();

    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showPresetInfo, setShowPresetInfo] = useState<Preset>();
    const [showModuleId, setShowModuleId] = useState<string>();
    const renderTitle = useRetainedDialogValue(props.show, props.title ?? 'Formularelement hinzufügen');
    const renderPrimaryActionLabel = useRetainedDialogValue(props.show, props.primaryActionLabel ?? 'Hinzufügen');
    const renderPrimaryActionIcon = useRetainedDialogValue(props.show, props.primaryActionIcon ??
        <Add sx={{fontSize: 18}}/>);
    const showElementDetailsPanel = currentTab === 0 && showElementInfo != null;
    const showPresetDetailsPanel = currentTab === 1 && showPresetInfo != null;
    const showStoreDetailsPanel = currentTab === 2 && showModuleId != null;
    const showDetailsPanel = showElementDetailsPanel || showPresetDetailsPanel || showStoreDetailsPanel;

    const opec = useOptionalProcessNodeEditorContext();
    const reusableUiDefinitions = useMemo(() => {
        if (opec == null || opec.incomingMetadata == null || opec.incomingMetadata.reusableUiDefinitions.length === 0) {
            return null;
        }
        return opec
            .incomingMetadata
            .reusableUiDefinitions;
    }, [opec]);

    const handleClose = () => {
        props.onClose();
    };

    const handleAddElement = (element: AnyElement) => {
        props.onAddElement(element);
    };

    return (
        <SelectionDialogShell
            open={props.show}
            onClose={handleClose}
            title={renderTitle}
            tabs={[
                {label: 'Elemente', value: 0},
                {label: 'Vorlagen', value: 1, hidden: props.hidePresets === true, disabled: true},
                {label: 'Gover Marktplatz', value: 2, hidden: props.hideGoverStore === true, disabled: true},
                {
                    label: 'Wiederverwendbare UI-Definitionen',
                    value: 3,
                    hidden: reusableUiDefinitions == null,
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
                setShowModuleId(undefined);
            }}
            detailsPanel={
                showElementDetailsPanel ? (
                    <ElementInfoTab
                        type={showElementInfo}
                        parentElement={props.parentElement}
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
                    <StoreTab
                        parentType={props.parentType}
                        parentElement={props.parentElement}
                        allParents={props.allParents}
                        onAddElement={handleAddElement}
                        primaryActionLabel={renderPrimaryActionLabel}
                        primaryActionIcon={renderPrimaryActionIcon}
                        showModuleId={setShowModuleId}
                        highlightedModuleId={showModuleId}
                    />
                }
                {
                    reusableUiDefinitions != null &&
                    currentTab === 3 &&
                    <>
                        {
                            reusableUiDefinitions.map((def) => {
                                const Icon = getElementIconForType(def.uiDefinition.type);
                                return (
                                    <SelectionListRow
                                        icon={<Icon/>}
                                        title={def.label}
                                        description={
                                            <span>Ein UI-Element aus Prozesselement <strong>{def.origin.name}</strong></span>
                                        }
                                        primaryActionLabel="Kopieren und einfügen"
                                        primaryActionIcon={<ContentCopy/>}
                                        onPrimaryAction={() => {
                                            handleAddElement(def.uiDefinition);
                                            dispatch(showSuccessSnackbar('UI-Element wurde kopiert und eingefügt'));
                                        }}
                                    />
                                );
                            })
                        }
                    </>
                }
            </Box>
        </SelectionDialogShell>
    );
}
