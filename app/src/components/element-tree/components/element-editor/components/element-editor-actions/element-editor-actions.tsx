import React, {useState} from 'react';
import {Box, Button, IconButton, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {type ElementEditorActionsProps} from './element-editor-actions-props';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import LibraryAddOutlinedIcon from '@mui/icons-material/LibraryAddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';

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
                sx={{
                    p: 4,
                    mt: 'auto',
                    boxShadow: '0 -10px 20px rgba(0, 0, 0, 0.05)',
                    borderTop: '1px solid #E0E0E0',
                    display: 'flex',
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    {
                        !props.editable &&
                        <Button
                            color="primary"
                            variant="contained"
                            onClick={props.onCancel}
                            startIcon={<CloseOutlinedIcon/>}
                            sx={{
                                mr: 2,
                            }}
                        >
                            Schließen
                        </Button>
                    }

                    <Button
                        color="primary"
                        variant="contained"
                        onClick={props.onSave}
                        startIcon={<SaveOutlinedIcon
                            sx={{
                                marginTop: '-2px',
                            }}
                        />}
                        sx={{
                            ml: 'auto',
                        }}
                        disabled={!props.editable}
                    >
                        Speichern
                    </Button>
                    <Button
                        color="primary"
                        onClick={props.onCancel}
                        sx={{
                            ml: 2,
                        }}
                        disabled={!props.editable}
                    >
                        Abbrechen
                    </Button>
                    {

                        props.editable &&
                        (
                            (props.onClone != null) ||
                            (props.onSaveAsPreset != null)
                        ) &&
                        <IconButton
                            color="primary"
                            onClick={handleClick}
                        >
                            <MoreVertOutlinedIcon/>
                        </IconButton>
                    }
                </Box>

                {
                    (props.onDelete != null) &&
                    <Button
                        color="error"
                        onClick={props.onDelete}
                        variant="outlined"
                        startIcon={<DeleteForeverOutlinedIcon
                            sx={{
                                marginTop: '-4px',
                            }}
                        />}
                        disabled={!props.editable}
                    >
                        Löschen
                    </Button>
                }
            </Box>
            <Menu
                open={anchorEl != null}
                anchorEl={anchorEl}
                onClose={handleClose}
            >
                {
                    (props.onSaveAsPreset != null) &&
                    <MenuItem
                        onClick={() => {
                            props.onSaveAsPreset!();
                            handleClose();
                        }}
                    >
                        <ListItemIcon>
                            <LibraryAddOutlinedIcon
                                sx={{
                                    marginTop: '-4px',
                                }}
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
                        onClick={() => {
                            props.onClone!();
                            handleClose();
                        }}
                    >
                        <ListItemIcon>
                            <ContentCopyOutlinedIcon
                                sx={{
                                    marginTop: '-4px',
                                }}
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
