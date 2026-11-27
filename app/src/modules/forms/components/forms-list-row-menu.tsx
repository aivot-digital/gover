import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import QrCode from '@aivot/mui-material-symbols-400-n25-outlined/QrCode';
import PictureAsPdf from '@aivot/mui-material-symbols-400-n25-outlined/PictureAsPdf';
import type {FormOverviewItem} from '../services/form-trigger-api-service';

interface FormsListRowMenuProps {
    anchorEl: HTMLElement;
    form: FormOverviewItem;
    canDownloadPrintablePdf: boolean;
    onClose: () => void;
    onCopyPublicLink: (form: FormOverviewItem) => void;
    onDownloadQrCode: (form: FormOverviewItem) => void;
    onDownloadPrintablePdf: (form: FormOverviewItem) => void;
}

export function FormsListRowMenu({
    anchorEl,
    form,
    canDownloadPrintablePdf,
    onClose,
    onCopyPublicLink,
    onDownloadQrCode,
    onDownloadPrintablePdf,
}: FormsListRowMenuProps): React.ReactElement {
    const hasPublicLink = form.status === 'Published' && form.publicUrl != null;

    return (
        <Menu anchorEl={anchorEl} open onClose={onClose}>
            {hasPublicLink && (
                <MenuItem onClick={() => {
                    onClose();
                    onCopyPublicLink(form);
                }}>
                    <ListItemIcon><ContentCopy /></ListItemIcon>
                    <ListItemText>Öffentlichen Link kopieren</ListItemText>
                </MenuItem>
            )}
            {hasPublicLink && (
                <MenuItem onClick={() => {
                    onClose();
                    onDownloadQrCode(form);
                }}>
                    <ListItemIcon><QrCode /></ListItemIcon>
                    <ListItemText>QR-Code mit öffentlichem Link herunterladen</ListItemText>
                </MenuItem>
            )}
            {hasPublicLink && canDownloadPrintablePdf && <Divider />}
            {canDownloadPrintablePdf && (
                <MenuItem onClick={() => {
                    onClose();
                    onDownloadPrintablePdf(form);
                }}>
                    <ListItemIcon><PictureAsPdf /></ListItemIcon>
                    <ListItemText>Vordruck herunterladen (PDF)</ListItemText>
                </MenuItem>
            )}
        </Menu>
    );
}
