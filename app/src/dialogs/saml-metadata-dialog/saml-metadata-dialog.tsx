import React from 'react';
import {Dialog, DialogContent, DialogActions, Button, Box, Skeleton, DialogContentText} from '@mui/material';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import { useDispatch } from 'react-redux';
import {TextFieldComponent} from "../../components/text-field/text-field-component";
import {showSuccessSnackbar} from "../../slices/snackbar-slice";
import {DialogTitleWithClose} from "../../components/dialog-title-with-close/dialog-title-with-close";
import {AlertComponent} from "../../components/alert/alert-component";

interface SamlMetadataDialogProps {
    open: boolean;
    loading: boolean;
    fields: { label: string; value: string }[];
    error?: string | null;
    onClose: () => void;
}

export function SamlMetadataDialog({ open, loading, fields, error, onClose }: SamlMetadataDialogProps) {
    const dispatch = useDispatch();

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitleWithClose
                onClose={onClose}
                closeTooltip="Schließen"
            >
                Für Einrichtung notwendige Metadaten
            </DialogTitleWithClose>
            <DialogContent>
                {loading ? (
                    <Box>
                        {Array.from({ length: 4 }).map((_, idx) => (
                            <Skeleton key={idx} height={72} sx={{ my: 1 }} />
                        ))}
                    </Box>
                ) : error ? (
                    <AlertComponent sx={{my: 1}} color="error">{error}</AlertComponent>
                ) : <>
                        <DialogContentText sx={{mb: 2}}>
                            Nachfolgend finden Sie die für die Einrichtung des Nutzerkontenanbieters notwendigen Metadaten. Diese benötigen Sie, um sie dem Anbieter mitzuteilen oder selbst in einem Self Service Portal (SSP) zu hinterlegen. Bitte lesen Sie hierzu den Leitfaden zur Anbindung des jeweiligen Nutzerkontos.
                        </DialogContentText>
                        {
                            (
                                fields.map((field, idx) => (
                                    <Box key={idx} sx={{ mb: 2 }}>
                                        <TextFieldComponent
                                            label={field.label}
                                            value={field.value}
                                            disabled
                                            onChange={() => {}}
                                            endAction={{
                                                icon: <ContentPasteOutlinedIcon />,
                                                onClick: () => {
                                                    navigator.clipboard.writeText(field.value);
                                                    dispatch(showSuccessSnackbar('Link in die Zwischenablage kopiert.'));
                                                },
                                            }}
                                        />
                                    </Box>
                                ))
                            )
                        }
                    </>
                }
            </DialogContent>
            <DialogActions>
                <Button variant="contained" onClick={onClose}>Schließen</Button>
            </DialogActions>
        </Dialog>
    );
}
