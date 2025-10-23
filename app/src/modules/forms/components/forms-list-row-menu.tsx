import React from 'react';
import {ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {FormListResponseDTO} from '../dtos/form-list-response-dto';
import {createCustomerPath} from '../../../utils/url-path-utils';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {downloadQrCode} from '../../../utils/download-qrcode';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import DeleteForever from '@aivot/mui-material-symbols-400-outlined/dist/delete-forever/DeleteForever';
import ContentPaste from '@aivot/mui-material-symbols-400-outlined/dist/content-paste/ContentPaste';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import QrCode2 from '@aivot/mui-material-symbols-400-outlined/dist/qr-code-2/QrCode2';

interface FormsListRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    form: FormListResponseDTO;

    onMoveFormToDepartment: (form: FormListResponseDTO) => void;
    onDeleteForm: (form: FormListResponseDTO) => void;
}

export function FormsListRowMenu(props: FormsListRowMenuProps) {
    const {
        anchorEl,
        onClose,
        form,

        onMoveFormToDepartment,
        onDeleteForm,
    } = props;

    const dispatch = useAppDispatch();

    const handleFormLinkCopy = async () => {
        try {
            await navigator
                .clipboard
                .writeText(createCustomerPath(form.slug));
            dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
        } catch (err) {
            console.error(err);
            dispatch(showSuccessSnackbar('Formularlink konnte nicht kopiert werden'));
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

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            <MenuItem
                onClick={() => {
                    onMoveFormToDepartment(form);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <MoveGroup />
                </ListItemIcon>
                <ListItemText>
                    Formular an Fachbereich übertragen
                </ListItemText>
            </MenuItem>

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
                    <QrCode2 />
                </ListItemIcon>
                <ListItemText>
                    QR-Code mit Formularlink herunterladen
                </ListItemText>
            </MenuItem>

            <MenuItem
                onClick={() => {
                    onDeleteForm(form);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <DeleteForever />
                </ListItemIcon>
                <ListItemText>
                    Formular löschen
                </ListItemText>
            </MenuItem>
        </Menu>
    );
}
