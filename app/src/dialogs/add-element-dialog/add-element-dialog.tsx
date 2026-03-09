import {Box, Dialog, Tab, Tabs} from '@mui/material';
import {type AddElementDialogProps} from './add-element-dialog-props';
import React, {useState} from 'react';
import {type ElementType} from '../../data/element-type/element-type';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {PresetTab} from './tabs/preset-tab';
import {ElementTab} from './tabs/element-tab';
import {ElementInfoTab} from './tabs/element-info-tab';
import {StoreTab} from './tabs/store-tab';
import {ModuleInfoTab} from './tabs/module-info-tab';
import {AnyElement} from '../../models/elements/any-element';
import {type Preset} from '../../models/entities/preset';
import {PresetInfoTab} from './tabs/preset-info-tab';
import Add from '@mui/icons-material/Add';


export function AddElementDialog(props: AddElementDialogProps) {
    const title = props.title ?? 'Formularelement hinzufügen';
    const primaryActionLabel = props.primaryActionLabel ?? 'Hinzufügen';
    const primaryActionIcon = props.primaryActionIcon ?? <Add sx={{fontSize: 18}}/>;
    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showPresetInfo, setShowPresetInfo] = useState<Preset>();
    const [showModuleId, setShowModuleId] = useState<string>();
    const showElementDetailsPanel = currentTab === 0 && showElementInfo != null;
    const showPresetDetailsPanel = currentTab === 1 && showPresetInfo != null;
    const showStoreDetailsPanel = currentTab === 2 && showModuleId != null;
    const showDetailsPanel = showElementDetailsPanel || showPresetDetailsPanel || showStoreDetailsPanel;

    const handleClose = () => {
        props.onClose();
    };

    const handleAddElement = (element: AnyElement) => {
        props.onAddElement(element);
    };

    return (
        <Dialog
            open={props.show}
            onClose={handleClose}
            fullWidth
            maxWidth={showDetailsPanel ? 'lg' : 'md'}
            TransitionProps={{
                onExited: () => {
                    // Keep the last active tab and detail selection alive until the close transition finishes.
                    setCurrentTab(0);
                    setShowElementInfo(undefined);
                    setShowPresetInfo(undefined);
                    setShowModuleId(undefined);
                },
            }}
        >
            <DialogTitleWithClose
                onClose={handleClose}
                closeTooltip="Schließen"
            >
                {title}
            </DialogTitleWithClose>
            <Tabs
                value={currentTab}
                onChange={(evt, val) => {
                    setCurrentTab(val);
                }}
                sx={{
                    px: 2,
                    borderBottom: '1px solid #E0E0E0',
                    mt: -1.5,
                }}
            >
                <Tab
                    label="Elemente"
                    value={0}
                />
                {
                    props.hidePresets !== true &&
                    <Tab
                        label="Vorlagen"
                        value={1}
                    />
                }
                {
                    props.hideGoverStore !== true &&
                    <Tab
                        label="Gover Marktplatz"
                        value={2}
                    />
                }
            </Tabs>
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: showDetailsPanel ? 'minmax(0, 1.2fr) minmax(320px, 0.8fr)' : 'minmax(0, 1fr)',
                    height: 'min(74vh, 820px)',
                }}
            >
                <Box
                    sx={{
                        minWidth: 0,
                        minHeight: 0,
                        overflow: 'hidden',
                    }}
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
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                showElementInfo={setShowElementInfo}
                                highlightedElement={showElementInfo}
                                limitElementTypes={props.limitElementTypes}
                            />
                        }
                        {
                            currentTab === 1 &&
                            <PresetTab
                                parentType={props.parentType}
                                onAddElement={handleAddElement}
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                showPresetInfo={setShowPresetInfo}
                                highlightedPresetKey={showPresetInfo?.key}
                            />
                        }
                        {
                            currentTab === 2 &&
                            <StoreTab
                                parentType={props.parentType}
                                onAddElement={handleAddElement}
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                showModuleId={setShowModuleId}
                                highlightedModuleId={showModuleId}
                            />
                        }
                    </Box>
                </Box>

                {
                    showElementDetailsPanel &&
                    <Box
                        sx={{
                            minWidth: 0,
                            minHeight: 0,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            overflow: 'hidden',
                        }}
                    >
                        <Box
                            sx={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                overflow: 'hidden',
                            }}
                        >
                            <ElementInfoTab
                                type={showElementInfo}
                                onAddElement={handleAddElement}
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                onClose={() => {
                                    setShowElementInfo(undefined);
                                }}
                            />
                        </Box>
                    </Box>
                }

                {
                    showPresetDetailsPanel &&
                    <Box
                        sx={{
                            minWidth: 0,
                            minHeight: 0,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            overflow: 'hidden',
                        }}
                    >
                        <Box
                            sx={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                overflow: 'hidden',
                            }}
                        >
                            <PresetInfoTab
                                preset={showPresetInfo}
                                onAddElement={handleAddElement}
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                onClose={() => {
                                    setShowPresetInfo(undefined);
                                }}
                            />
                        </Box>
                    </Box>
                }

                {
                    showStoreDetailsPanel &&
                    <Box
                        sx={{
                            minWidth: 0,
                            minHeight: 0,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            overflow: 'hidden',
                        }}
                    >
                        <Box
                            sx={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                overflow: 'hidden',
                            }}
                        >
                            <ModuleInfoTab
                                moduleId={showModuleId}
                                onAddElement={handleAddElement}
                                primaryActionLabel={primaryActionLabel}
                                primaryActionIcon={primaryActionIcon}
                                onClose={() => {
                                    setShowModuleId(undefined);
                                }}
                            />
                        </Box>
                    </Box>
                }
            </Box>
        </Dialog>
    );
}
