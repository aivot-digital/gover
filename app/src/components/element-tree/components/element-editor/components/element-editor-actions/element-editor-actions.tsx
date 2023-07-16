import {Box, Button, IconButton, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import React from 'react';
import {ElementEditorActionsProps} from './element-editor-actions-props';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import LibraryAddOutlinedIcon from '@mui/icons-material/LibraryAddOutlined';

export function ElementEditorActions(props: ElementEditorActionsProps) {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };
    const handleClose = () => {
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
                    <Button
                        color="primary"
                        variant="contained"
                        onClick={props.onSave}
                        startIcon={<SaveOutlinedIcon
                            sx={{marginTop: '-2px'}}
                        />}
                        sx={{ml: 'auto'}}
                    >
                        Speichern
                    </Button>
                    <Button
                        color="primary"
                        onClick={props.onCancel}
                        sx={{ml: 2}}
                    >
                        Abbrechen
                    </Button>
                    {
                        (
                            props.onClone ||
                            props.onSaveAsPreset
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
                    props.onDelete &&
                    <Button
                        color="error"
                        onClick={props.onDelete}
                        variant={'outlined'}
                        startIcon={<DeleteForeverOutlinedIcon
                            sx={{marginTop: '-4px'}}
                        />}
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
                    props.onSaveAsPreset &&
                    <MenuItem
                        onClick={() => {
                            props.onSaveAsPreset!();
                            handleClose();
                        }}
                    >
                        <ListItemIcon>
                            <LibraryAddOutlinedIcon
                                sx={{marginTop: '-4px'}}
                            />
                        </ListItemIcon>
                        <ListItemText>
                            Als Vorlage Speichern
                        </ListItemText>
                    </MenuItem>
                }
                {
                    props.onClone &&
                    <MenuItem
                        onClick={() => {
                            props.onClone!();
                            handleClose();
                        }}
                    >
                        <ListItemIcon>
                            <ContentCopyOutlinedIcon
                                sx={{marginTop: '-4px'}}
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
