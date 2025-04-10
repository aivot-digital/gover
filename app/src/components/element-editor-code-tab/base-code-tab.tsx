import {Box, Dialog, DialogContent, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import React, {PropsWithChildren, useState} from 'react';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {FunctionSelector} from './components/function-selector/function-selector';
import {BaseCodeTabProps} from './base-code-tab-props';
import {Actions} from '../actions/actions';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

export function BaseCodeTab(props: PropsWithChildren<BaseCodeTabProps>): JSX.Element {
    const [anchorEl, setAnchorEl] = useState<Element>();
    const [showFunctionSelector, setShowFunctionSelector] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const [confirmChangeFunction, setConfirmChangeFunction] = useState<() => void>();

    const notEditable = !props.editable;

    return (
        <>
            <Box
                sx={{
                    m: 4,
                }}
            >
                <Typography
                    sx={{
                        mb: 2,
                    }}
                    variant="subtitle1"
                >
                    Fachliche Anforderungen
                </Typography>

                <Box
                    sx={{
                        mb: 4,
                    }}
                >
                    <RichTextEditorComponentView
                        value={props.requirements ?? ''}
                        onChange={(req) => {
                            props.onRequirementsChange(req);
                        }}
                        disabled={notEditable}
                    />
                </Box>

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
                            mt: 4,
                        }}
                    >
                        <Typography>
                            {props.label}
                        </Typography>

                        <Actions
                            sx={{
                                ml: 'auto',
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
                        <DeleteForeverOutlinedIcon />
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
