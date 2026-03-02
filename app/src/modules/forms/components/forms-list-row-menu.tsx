import React from 'react';
import {Divider, Link, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {createCustomerPath} from '../../../utils/url-path-utils';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {downloadQrCode} from '../../../utils/download-qrcode';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import ContentPaste from '@aivot/mui-material-symbols-400-outlined/dist/content-paste/ContentPaste';
import MoveItem from '@aivot/mui-material-symbols-400-outlined/dist/move-item/MoveItem';
import QrCode from '@aivot/mui-material-symbols-400-outlined/dist/qr-code/QrCode';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import ApprovalDelegation from '@aivot/mui-material-symbols-400-outlined/dist/approval-delegation/ApprovalDelegation';
import {FormEntity} from '../entities/form-entity';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';

interface FormsListRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    form: FormEntity;

    onManageAccess: (form: FormEntity) => void;
    onMoveFormToDepartment: (form: FormEntity) => void;
    onDeleteForm: (form: FormEntity) => void;
}

export function FormsListRowMenu(props: FormsListRowMenuProps) {
    const {
        anchorEl,
        onClose,
        form,

        onManageAccess,
        onMoveFormToDepartment,
        onDeleteForm,
    } = props;

    const dispatch = useAppDispatch();

    const handleFormLinkCopy = async () => {
        try {
            const success = await copyToClipboardText(createCustomerPath(form.slug));
            if (!success) {
                throw new Error('copy failed');
            }
            dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Formularlink konnte nicht kopiert werden'));
        }

        onClose();
    };

    const handleDownloadQrCode = async () => {
        try {
            await downloadQrCode(
                createCustomerPath(form.slug),
                `qr-code-${form.slug}.png`,
            );
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }

        onClose();
    };

    const handleExternalLink = (url: string) => {
        window.open(url, '_blank');
        onClose();
    };

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            {form.publishedVersion != null &&
                <MenuItem
                    component={Link}
                    href={createCustomerPath(form.slug)}
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    <ListItemIcon>
                        <OpenInNew />
                    </ListItemIcon>
                    <ListItemText>
                        Veröffentlichtes Formular öffnen (neuer Tab)
                    </ListItemText>
                </MenuItem>
            }


            <MenuItem
                onClick={handleFormLinkCopy}
            >
                <ListItemIcon>
                    <ContentPaste />
                </ListItemIcon>
                <ListItemText>
                    Formularlink in Zwischenablage kopieren
                </ListItemText>
            </MenuItem>

            <MenuItem
                onClick={handleDownloadQrCode}
            >
                <ListItemIcon>
                    <QrCode />
                </ListItemIcon>
                <ListItemText>
                    QR-Code mit Formularlink herunterladen
                </ListItemText>
            </MenuItem>

            <Divider />

            <MenuItem
                onClick={() => {
                    onMoveFormToDepartment(form);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <MoveItem />
                </ListItemIcon>
                <ListItemText>
                    Formular an Fachbereich übertragen
                </ListItemText>
            </MenuItem>

            <MenuItem
                onClick={() => {
                    onManageAccess(form);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <ApprovalDelegation />
                </ListItemIcon>
                <ListItemText>
                    Zugriffsberechtigungen verwalten
                </ListItemText>
            </MenuItem>

            <Divider />

            <MenuItem
                onClick={() => {
                    onDeleteForm(form);
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
                    Formular löschen
                </ListItemText>
            </MenuItem>
        </Menu>
    );
}
