import {
    Alert,
    AlertTitle,
    Box,
    Dialog,
    DialogContent,
    FormControlLabel, Grid, IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Switch,
    Tab,
    Tabs,
    Tooltip,
} from '@mui/material';
import {AddElementDialogProps} from './add-element-dialog-props';
import React, {useEffect, useState} from 'react';
import {ElementChildOptions} from '../../data/element-type/element-child-options';
import {ElementIcons} from '../../data/element-type/element-icons';
import {ElementNames} from '../../data/element-type/element-names';
import {Preset} from '../../models/entities/preset';
import {PresetsService} from '../../services/presets.service';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome'
import {ElementType} from '../../data/element-type/element-type';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {faInfoCircle, faLayerPlus} from '@fortawesome/pro-light-svg-icons';
import {ElementTypesMap} from '../../data/element-type/element-types-map';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {cloneElement} from "../../utils/clone-element";
import {PresetTab} from "./tabs/preset-tab";
import {ElementTab} from "./tabs/element-tab";
import {ElementInfoTab} from "./tabs/element-info-tab";
import {StoreTab} from "./tabs/store-tab";
import {ModuleInfoTab} from "./tabs/module-info-tab";


export function AddElementDialog({parentType, onAddElement, onClose}: AddElementDialogProps) {
    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showModuleId, setShowModuleId] = useState<string>();

    return (
        <Dialog
            open={true}
            onClose={onClose}
            fullWidth
            maxWidth="xl"
        >
            <DialogTitleWithClose
                id="add-component-dialog-title"
                onClose={onClose}
                closeTooltip="Schließen"
            >
                Neues Element hinzufügen
            </DialogTitleWithClose>

            <Tabs
                value={currentTab}
                onChange={(evt, val) => setCurrentTab(val)}
                sx={{borderBottom: '1px solid #E0E0E0', mt: -1}}
            >
                <Tab
                    label="Basis-Elemente"
                    value={0}
                />
                <Tab
                    label="Vorlagen"
                    value={1}
                />
                <Tab
                    label="Gover Store"
                    value={2}
                />
            </Tabs>

            <Grid container>
                <Grid
                    item
                    xs={(currentTab === 0 && showElementInfo != null) || (currentTab === 2 && showModuleId != null) ? 6 : 12}
                >
                    <Box
                        sx={{
                            height: '50vh',
                            overflowY: 'auto',
                        }}
                    >
                        {
                            currentTab === 0 &&
                            <ElementTab
                                parentType={parentType}
                                onAddElement={onAddElement}
                                showElementInfo={setShowElementInfo}
                                highlightedElement={showElementInfo}
                            />
                        }
                        {
                            currentTab === 1 &&
                            <PresetTab
                                parentType={parentType}
                                onAddElement={onAddElement}
                            />
                        }
                        {
                            currentTab === 2 &&
                            <StoreTab
                                parentType={parentType}
                                onAddElement={onAddElement}
                                showModuleId={setShowModuleId}
                                hightlightedModuleId={showModuleId}
                            />
                        }
                    </Box>
                </Grid>

                {
                    currentTab === 0 &&
                    showElementInfo != null &&
                    <Grid
                        item
                        xs={6}
                    >
                        <Box
                            sx={{
                                height: '50vh',
                                overflowY: 'auto',
                            }}
                        >
                            <ElementInfoTab
                                type={showElementInfo}
                                onClose={() => setShowElementInfo(undefined)}
                            />
                        </Box>
                    </Grid>
                }

                {
                    currentTab === 2 &&
                    showModuleId != null &&
                    <Grid
                        item
                        xs={6}
                    >
                        <Box
                            sx={{
                                height: '50vh',
                                overflowY: 'auto',
                            }}
                        >
                            <ModuleInfoTab
                                moduleId={showModuleId}
                                onClose={() => setShowModuleId(undefined)}
                            />
                        </Box>
                    </Grid>
                }
            </Grid>
        </Dialog>
    );
}
