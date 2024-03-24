import {Box, Dialog, Grid, Tab, Tabs} from '@mui/material';
import {type AddElementDialogProps} from './add-element-dialog-props';
import React, {useState} from 'react';
import {type ElementType} from '../../data/element-type/element-type';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {PresetTab} from './tabs/preset-tab';
import {ElementTab} from './tabs/element-tab';
import {ElementInfoTab} from './tabs/element-info-tab';
import {StoreTab} from './tabs/store-tab';
import {ModuleInfoTab} from './tabs/module-info-tab';
import {AnyElement} from "../../models/elements/any-element";


export function AddElementDialog(props: AddElementDialogProps): JSX.Element {
    const [currentTab, setCurrentTab] = useState(0);
    const [showElementInfo, setShowElementInfo] = useState<ElementType>();
    const [showModuleId, setShowModuleId] = useState<string>();

    const handleClose = () => {
        setShowModuleId(undefined);
        props.onClose();
    };

    const handleAddElement = (element: AnyElement) => {
        setShowModuleId(undefined);
        props.onAddElement(element);
    };

    return (
        <Dialog
            open={props.show}
            onClose={handleClose}
            fullWidth
            maxWidth="xl"
        >
            <DialogTitleWithClose
                onClose={handleClose}
                closeTooltip="Schließen"
            >
                Neues Element hinzufügen
            </DialogTitleWithClose>

            <Tabs
                value={currentTab}
                onChange={(evt, val) => {
                    setCurrentTab(val);
                }}
                sx={{
                    borderBottom: '1px solid #E0E0E0',
                    mt: -1,
                }}
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
                                parentType={props.parentType}
                                onAddElement={handleAddElement}
                                showElementInfo={setShowElementInfo}
                                highlightedElement={showElementInfo}
                            />
                        }
                        {
                            currentTab === 1 &&
                            <PresetTab
                                parentType={props.parentType}
                                onAddElement={handleAddElement}
                            />
                        }
                        {
                            currentTab === 2 &&
                            <StoreTab
                                parentType={props.parentType}
                                onAddElement={handleAddElement}
                                showModuleId={setShowModuleId}
                                highlightedModuleId={showModuleId}
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
                                onClose={() => {
                                    setShowElementInfo(undefined);
                                }}
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
                                onClose={() => {
                                    setShowModuleId(undefined);
                                }}
                            />
                        </Box>
                    </Grid>
                }
            </Grid>
        </Dialog>
    );
}
