import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {FormDetailsResponseDTO} from '../dtos/form-details-response-dto';
import SwapVert from '@aivot/mui-material-symbols-400-outlined/dist/swap-vert/SwapVert';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';

interface FormVersionsDialogRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    form: FormDetailsResponseDTO;

    onReuseFormVersionAsDraft: (version: number) => void;
    onReuseFormVersionAsNewForm: (version: number) => void;
    onExportFormVersion: (version: number) => void;
    onDeleteFormVersion: (version: number) => void;
}

export function FormVersionsDialogRowMenu(props: FormVersionsDialogRowMenuProps) {
    const {
        anchorEl,
        onClose,
        form,

        onReuseFormVersionAsDraft,
        onReuseFormVersionAsNewForm,
        onExportFormVersion,
        onDeleteFormVersion,
    } = props;

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            {
                form.draftedVersion !== form.version &&
                <MenuItem
                    onClick={() => {
                        onReuseFormVersionAsDraft(form.version);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Edit />
                    </ListItemIcon>
                    <ListItemText>
                        Als Entwurf verwenden
                    </ListItemText>
                </MenuItem>
            }

            <MenuItem
                onClick={() => {
                    onReuseFormVersionAsNewForm(form.version);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <ContentCopy />
                </ListItemIcon>
                <ListItemText>
                    Als neues Formular kopieren
                </ListItemText>
            </MenuItem>

            <MenuItem
                onClick={() => {
                    onExportFormVersion(form.version);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <SwapVert />
                </ListItemIcon>
                <ListItemText>
                    Als Formular exportieren
                </ListItemText>
            </MenuItem>

            {
                form.draftedVersion === form.version &&
                <Divider />
            }

            {
                form.draftedVersion === form.version &&
                <MenuItem
                    onClick={() => {
                        onDeleteFormVersion(form.version);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Delete color="error" />
                    </ListItemIcon>
                    <ListItemText
                        sx={{
                            color: 'error.main',
                        }}
                    >
                        Entwurf löschen
                    </ListItemText>
                </MenuItem>
            }
        </Menu>
    );
}
