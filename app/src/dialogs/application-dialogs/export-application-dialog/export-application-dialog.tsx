import React from 'react';
import { Dialog, DialogContent, DialogContentText, DialogActions, Button } from '@mui/material';
import {ExportApplicationDialogProps} from "./export-application-dialog-props";
import {DialogTitleWithClose} from "../../../components/dialog-title-with-close/dialog-title-with-close";

export function ExportApplicationDialog({ open, onCancel, onExport }: ExportApplicationDialogProps): JSX.Element {
    return (
        <Dialog
            open={open}
            onClose={onCancel}
            aria-labelledby="export-dialog-title"
            aria-describedby="export-dialog-description"
        >
            <DialogTitleWithClose
                id="export-dialog-title"
                onClose={onCancel}
                closeTooltip="Schließen"
            >
                Formular exportieren
            </DialogTitleWithClose>
            <DialogContent id="export-dialog-description">
                <DialogContentText>
                    Sie können das Formular exportieren, um es z. B. in einem anderen System weiterzuverwenden oder zu archivieren. Der Export erfolgt im offenen .gov-Format, das auf JSON basiert.
                </DialogContentText>
                <DialogContentText sx={{ mt: 2 }}>
                    <b>Wichtig:</b> Zum Schutz Ihrer Daten werden bestimmte Informationen aus dem Export ausgeschlossen und sind für die importierende Person nicht sichtbar.
                    Dazu zählen u. a. Formular-ID, Schnittstellen, Status, Fachbereiche, rechtliche Hinweise, Support-Angaben, PDF-Vorlagen, Farbschema, Zahlungsdetails, Test-Protokolle und die Historie.
                </DialogContentText>
                <DialogContentText sx={{ mt: 2 }}>
                    Bei Bedarf müssen Sie diese Informationen nach einem Import im Zielsystem neu konfigurieren.
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={onExport} variant="contained" color="primary" autoFocus>
                    Formular als .gov-Datei herunterladen
                </Button>
                <Button onClick={onCancel} color="inherit">
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default ExportApplicationDialog;