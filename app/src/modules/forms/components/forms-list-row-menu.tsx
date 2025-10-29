import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {FormListResponseDTO} from '../dtos/form-list-response-dto';
import {createCustomerPath} from '../../../utils/url-path-utils';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {downloadQrCode} from '../../../utils/download-qrcode';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import DeleteForever from '@aivot/mui-material-symbols-400-outlined/dist/delete-forever/DeleteForever';
import ContentPaste from '@aivot/mui-material-symbols-400-outlined/dist/content-paste/ContentPaste';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import QrCode2 from '@aivot/mui-material-symbols-400-outlined/dist/qr-code-2/QrCode2';
import MoveItem from '@aivot/mui-material-symbols-400-outlined/dist/move-item/MoveItem';
import QrCode from '@aivot/mui-material-symbols-400-outlined/dist/qr-code/QrCode';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';

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
                    onClick={() => handleExternalLink(form.slug)}
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

            <Divider/>

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

            <Divider/>

            <MenuItem
                onClick={() => {
                    onDeleteForm(form);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <Delete color="error" />
                </ListItemIcon>
                <ListItemText sx={{
                    color: "error.main"
                }}>
                    Formular löschen
                </ListItemText>
            </MenuItem>
        </Menu>
    );
}
