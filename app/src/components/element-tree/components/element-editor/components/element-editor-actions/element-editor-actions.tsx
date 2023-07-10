import React, { useState } from 'react';
import { Box, Button, IconButton, ListItemIcon, ListItemText, Menu, MenuItem } from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faClone, faEllipsisVertical, faFloppyDisk, faLayerPlus, faTrashXmark } from '@fortawesome/pro-light-svg-icons';
import { type ElementEditorActionsProps } from './element-editor-actions-props';

export function ElementEditorActions(props: ElementEditorActionsProps): JSX.Element {
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = (): void => {
        setAnchorEl(null);
    };

    return (
        <>
            <Box
                sx={ {
                    p: 4,
                    mt: 'auto',
                    boxShadow: '0 -10px 20px rgba(0, 0, 0, 0.05)',
                    borderTop: '1px solid #E0E0E0',
                    display: 'flex',
                    justifyContent: 'space-between',
                } }
            >
                <Box>
                    <Button
                        color="primary"
                        variant="contained"
                        onClick={ props.onSave }
                        startIcon={ <FontAwesomeIcon
                            icon={ faFloppyDisk }
                            style={ {
                                marginTop: '-2px',
                            } }
                        /> }
                        sx={ {
                            ml: 'auto',
                        } }
                    >
                        Speichern
                    </Button>
                    <Button
                        color="primary"
                        onClick={ props.onCancel }
                        sx={ {
                            ml: 2,
                        } }
                    >
                        Abbrechen
                    </Button>
                    {
                        (
                            (props.onClone != null) ||
                            (props.onSaveAsPreset != null)
                        ) &&
                        <IconButton
                            color="primary"
                            onClick={ handleClick }
                        >
                            <FontAwesomeIcon
                                icon={ faEllipsisVertical }
                                fixedWidth
                            />
                        </IconButton>
                    }
                </Box>

                {
                    (props.onDelete != null) &&
                    <Button
                        color="error"
                        onClick={ props.onDelete }
                        variant="outlined"
                        startIcon={ <FontAwesomeIcon
                            icon={ faTrashXmark }
                            style={ {
                                marginTop: '-4px',
                            } }
                        /> }
                    >
                        Löschen
                    </Button>
                }
            </Box>
            <Menu
                open={ anchorEl != null }
                anchorEl={ anchorEl }
                onClose={ handleClose }
            >
                {
                    (props.onSaveAsPreset != null) &&
                    <MenuItem
                        onClick={ () => {
                            props.onSaveAsPreset!();
                            handleClose();
                        } }
                    >
                        <ListItemIcon>
                            <FontAwesomeIcon
                                icon={ faLayerPlus }
                                style={ {
                                    marginTop: '-4px',
                                } }
                            />
                        </ListItemIcon>
                        <ListItemText>
                            Als Vorlage speichern
                        </ListItemText>
                    </MenuItem>
                }
                {
                    (props.onClone != null) &&
                    <MenuItem
                        onClick={ () => {
                            props.onClone!();
                            handleClose();
                        } }
                    >
                        <ListItemIcon>
                            <FontAwesomeIcon
                                icon={ faClone }
                                style={ {
                                    marginTop: '-4px',
                                } }
                            />
                        </ListItemIcon>
                        <ListItemText>
                            Duplizieren
                        </ListItemText>
                    </MenuItem>
                }
            </Menu>
        </>
    );
}
