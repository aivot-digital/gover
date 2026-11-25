import {
    Box,
    Dialog,
    DialogContent,
    Divider,
    Grid,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Typography,
} from '@mui/material';
import React, {PropsWithChildren, useState} from 'react';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {FunctionSelector} from './components/function-selector/function-selector';
import {BaseCodeTabProps} from './base-code-tab-props';
import {Actions} from '../actions/actions';
import MoreVertOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import SwapHorizOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';

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
                    variant={'h5'}
                >
                    Definieren Sie hier schriftlich, welche Anforderungen an die Logik gestellt werden. Diese
                    Beschreibung hilft bei der technischen Umsetzung und späteren Nachvollziehbarkeit der entsprechenden
                    No-/Low-Code-Funktion.
                </ElementEditorSectionHeader>

                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}>
                        <RichTextInputComponent
                            value={props.requirements ?? ''}
                            onChange={(req) => {
                                props.onRequirementsChange(req ?? undefined);
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
                        role="group"
                        aria-label="Funktion"
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            flexWrap: 'wrap',
                            gap: 1,
                            mt: 4,
                            mb: 2,
                        }}
                    >
                        <ElementEditorSectionHeader
                            title={'Funktion'}
                            variant={'h5'}
                            disableMarginTop
                            disableMarginBottom
                            maxWidth="none"
                            sx={{
                                flexGrow: 1,
                            }}
                        />

                        {props.functionToolbarRef != null && <Box ref={props.functionToolbarRef}
                                                                  sx={{minWidth: 0}}/>}

                        {
                            props.editable && !props.functionActionsDisabled &&
                            <Actions
                                sx={{
                                    flexShrink: 0,
                                    ...(props.functionToolbarRef != null ? {
                                        borderLeft: 1,
                                        borderColor: 'divider',
                                        pl: 1,
                                    } : {}),
                                }}
                                actions={[
                                    {
                                        tooltip: 'Mehr',
                                        icon: <MoreVertOutlinedIcon/>,
                                        onClick: (event) => {
                                            setAnchorEl(event.currentTarget);
                                        },
                                    },
                                ]}
                            />
                        }
                        <Typography sx={{flexBasis: '100%'}}>
                            Konfigurieren Sie die Logik, die zur Umsetzung der fachlichen Anforderungen erforderlich
                            ist.
                        </Typography>
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
                        setShowFunctionSelector(true);
                    }}
                >
                    <ListItemIcon>
                        <SwapHorizOutlinedIcon/>
                    </ListItemIcon>
                    <ListItemText>
                        Anderen Funktionstyp auswählen
                    </ListItemText>
                </MenuItem>
                <Divider/>
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
                        <Delete color="error"/>
                    </ListItemIcon>
                    <ListItemText sx={{color: 'error.main'}}>
                        Funktion löschen
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
                        mode="change"
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
                isDestructive
                title="Funktion löschen"
                onConfirm={confirmDelete}
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
            >
                <Typography>
                    Die konfigurierte Funktion wird entfernt.
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
                    Die bisherige Funktion wird durch eine neue Funktion des ausgewählten Typs ersetzt.
                </Typography>
            </ConfirmDialog>
        </>
    );
}
