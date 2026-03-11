import {Box, Dialog, DialogContent, Grid, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import React, {PropsWithChildren, useState} from 'react';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {FunctionSelector} from './components/function-selector/function-selector';
import {BaseCodeTabProps} from './base-code-tab-props';
import {Actions} from '../actions/actions';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';

export function BaseCodeTab(props: PropsWithChildren<BaseCodeTabProps>) {
    const [anchorEl, setAnchorEl] = useState<Element>();
    const [showFunctionSelector, setShowFunctionSelector] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const [confirmChangeFunction, setConfirmChangeFunction] = useState<() => void>();

    const notEditable = !props.editable;

    return (
        <>
            <Box>
                <ElementEditorSectionHeader
                    title={props.label}
                    disableMarginTop
                >
                    {props.description}
                </ElementEditorSectionHeader>

                <ElementEditorSectionHeader
                    title="Fachliche Anforderungen"
                    variant={"h5"}
                >
                    Definieren Sie hier schriftlich, welche Anforderungen an die Logik gestellt werden. Diese Beschreibung hilft bei der technischen Umsetzung und späteren Nachvollziehbarkeit der entsprechenden No-/Low-Code Funktion.
                </ElementEditorSectionHeader>

                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6
                        }}>
                        <RichTextEditorComponentView
                            value={props.requirements ?? ''}
                            onChange={(req) => {
                                props.onRequirementsChange(req);
                            }}
                            disabled={notEditable}
                        />
                    </Grid>
                </Grid>

                {
                    props.editable &&
                    !props.hasFunction &&
                    <FunctionSelector
                        fullWidth={false}
                        allowNoCode={props.allowsNoCode}
                        allowExpression={props.allowsExpression}

                        onSelectFunctionCode={() => {
                            props.onSelectFunction('legacy-code');
                        }}
                        onSelectNoCode={() => {
                            props.onSelectFunction('legacy-condition');
                        }}
                        onSelectCloudCode={() => {
                            props.onSelectFunction('code');
                        }}
                        onSelectNoCodeExpression={() => {
                            props.onSelectFunction('expression');
                        }}
                    />
                }

                {
                    props.hasFunction &&
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <ElementEditorSectionHeader
                            title={"Funktion"}
                            variant={"h5"}
                            sx={{
                                flexGrow: 1,
                            }}
                        >
                            Konfigurieren Sie die Logik, die zur Umsetzung der fachlichen Anforderungen erforderlich ist.
                        </ElementEditorSectionHeader>

                        {
                            props.editable &&
                            <Actions
                                sx={{
                                    ml: 'auto',
                                    flexShrink: 0,
                                }}
                                actions={[
                                    {
                                        tooltip: 'Mehr',
                                        icon: <MoreVertOutlinedIcon />,
                                        onClick: (event) => {
                                            setAnchorEl(event.currentTarget);
                                        },
                                    },
                                ]}
                            />
                        }
                    </Box>
                }

                {
                    props.hasFunction &&
                    <Box
                        sx={{
                            mt: 2,
                        }}
                    >
                        {props.children}
                    </Box>
                }
            </Box>
            <Menu
                open={anchorEl != null}
                anchorEl={anchorEl}
                onClose={() => {
                    setAnchorEl(undefined);
                }}
            >
                <MenuItem
                    onClick={() => {
                        setAnchorEl(undefined);
                        setConfirmDelete(() => () => {
                            props.onDeleteFunction();
                            setConfirmDelete(undefined);
                        });
                    }}
                >
                    <ListItemIcon>
                        <Delete />
                    </ListItemIcon>
                    <ListItemText>
                        Funktion löschen
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        setAnchorEl(undefined);
                        setShowFunctionSelector(true);
                    }}
                >
                    <ListItemIcon>
                        <SwapHorizOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Anderen Funktionstyp auswählen
                    </ListItemText>
                </MenuItem>
            </Menu>
            <Dialog
                open={showFunctionSelector}
                onClose={() => {
                    setShowFunctionSelector(false);
                }}
            >
                <DialogTitleWithClose
                    onClose={() => {
                        setShowFunctionSelector(false);
                    }}
                >
                    Funktionstyp auswählen
                </DialogTitleWithClose>
                <DialogContent>
                    <FunctionSelector
                        fullWidth={true}
                        allowNoCode={props.allowsNoCode}
                        allowExpression={props.allowsExpression}

                        onSelectFunctionCode={() => {
                            setConfirmChangeFunction(() => () => {
                                props.onSelectFunction('legacy-code');
                                setShowFunctionSelector(false);
                                setConfirmChangeFunction(undefined);
                            });
                        }}
                        onSelectNoCode={() => {
                            setConfirmChangeFunction(() => () => {
                                props.onSelectFunction('legacy-condition');
                                setShowFunctionSelector(false);
                                setConfirmChangeFunction(undefined);
                            });
                        }}
                        onSelectCloudCode={() => {
                            setConfirmChangeFunction(() => () => {
                                props.onSelectFunction('code');
                                setShowFunctionSelector(false);
                                setConfirmChangeFunction(undefined);
                            });
                        }}
                        onSelectNoCodeExpression={() => {
                            setConfirmChangeFunction(() => () => {
                                props.onSelectFunction('expression');
                                setShowFunctionSelector(false);
                                setConfirmChangeFunction(undefined);
                            });
                        }}
                    />
                </DialogContent>
            </Dialog>
            <ConfirmDialog
                title="Funktion löschen"
                onConfirm={confirmDelete}
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
            >
                <Typography>
                    Möchten Sie die Funktion wirklich löschen?
                    Bitte beachten Sie, dass diese Aktion nicht rückgängig gemacht werden kann.
                </Typography>
            </ConfirmDialog>
            <ConfirmDialog
                title="Funktionstyp ändern"
                onConfirm={confirmChangeFunction}
                onCancel={() => {
                    setConfirmChangeFunction(undefined);
                }}
            >
                <Typography>
                    Möchten Sie den Typ der Funktion wirklich ändern?
                    Bitte beachten Sie, dass diese Aktion nicht rückgängig gemacht werden kann.
                </Typography>
            </ConfirmDialog>
        </>
    );
}
