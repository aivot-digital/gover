import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import SwapVert from '@aivot/mui-material-symbols-400-outlined/dist/swap-vert/SwapVert';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {FormVersionEntity} from '../entities/form-version-entity';
import {FormEntity} from '../entities/form-entity';

interface FormVersionsDialogRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    form: FormEntity;
    version: FormVersionEntity;

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
        version,

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
                form.draftedVersion !== version.version &&
                <MenuItem
                    onClick={() => {
                        onReuseFormVersionAsDraft(version.version);
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
                    onReuseFormVersionAsNewForm(version.version);
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
                    onExportFormVersion(version.version);
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
                form.draftedVersion === version.version &&
                <Divider />
            }

            {
                form.draftedVersion === version.version &&
                <MenuItem
                    onClick={() => {
                        onDeleteFormVersion(version.version);
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
