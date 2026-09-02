import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    FormControlLabel,
    Switch,
    Typography,
} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {IconPickerDialog} from '../../../dialogs/icon-picker-dialog/icon-picker-dialog';
import {CustomLinkIcons, getCustomLinkIcon} from '../data/custom-link-icons';
import {type CustomLink, type CustomLinkRequest, CustomLinkType} from '../models/custom-link';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';

interface CustomLinkDialogProps {
    open: boolean;
    link: CustomLink | null;
    isBusy: boolean;
    onClose: () => void;
    onSave: (request: CustomLinkRequest) => void;
}

const emptyRequest: CustomLinkRequest = {
    label: '',
    description: null,
    url: '',
    icon: 'link',
    type: CustomLinkType.Dashboard,
    enabled: true,
};

export function CustomLinkDialog(props: CustomLinkDialogProps) {
    const [request, setRequest] = useState<CustomLinkRequest>(emptyRequest);
    const [showIconPicker, setShowIconPicker] = useState(false);
    const [attemptedSubmit, setAttemptedSubmit] = useState(false);

    useEffect(() => {
        if (!props.open) return;
        setRequest(props.link == null ? emptyRequest : {
            label: props.link.label,
            description: props.link.description,
            url: props.link.url,
            icon: props.link.icon ?? 'link',
            type: props.link.type,
            enabled: props.link.enabled,
        });
        setAttemptedSubmit(false);
    }, [props.link, props.open]);

    const isValidUrl = useMemo(() => {
        try {
            const url = new URL(request.url);
            return url.protocol === 'http:' || url.protocol === 'https:';
        } catch {
            return false;
        }
    }, [request.url]);
    const labelLength = request.label.trim().length;
    const descriptionLength = request.description?.trim().length ?? 0;
    const urlLength = request.url.trim().length;
    const isValid = labelLength > 0 && labelLength <= 128 && descriptionLength <= 255 && urlLength <= 500 && isValidUrl;
    const SelectedIcon = getCustomLinkIcon(request.icon);

    const handleSave = () => {
        setAttemptedSubmit(true);
        if (!isValid) return;
        props.onSave({
            ...request,
            label: request.label.trim(),
            description: request.description?.trim() || null,
            url: request.url.trim(),
        });
    };

    return (
        <>
            <Dialog open={props.open} onClose={props.isBusy ? undefined : props.onClose} fullWidth maxWidth="sm">
                <DialogTitleWithClose onClose={props.onClose}>
                    {props.link == null ? 'Link hinzufügen' : 'Link bearbeiten'}
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    <Typography
                        sx={{
                            color: "text.secondary",
                            mb: 2,
                            maxWidth: 560
                        }}>
                        Aktivierte Links werden allen angemeldeten Mitarbeiter:innen im Bereich „Relevante Links“ auf der Übersicht angezeigt.
                    </Typography>
                    <TextFieldComponent
                        label="Bezeichnung *"
                        value={request.label}
                        onChange={(label) => setRequest((current) => ({...current, label: label ?? ''}))}
                        error={attemptedSubmit && (labelLength === 0 || labelLength > 128)
                            ? labelLength === 0 ? 'Bitte geben Sie eine Bezeichnung ein.' : 'Die Bezeichnung darf höchstens 128 Zeichen lang sein.'
                            : undefined}
                        disabled={props.isBusy}
                        controlSx={{mt: 0}}
                    />
                    <TextFieldComponent
                        label="Beschreibung"
                        value={request.description ?? ''}
                        onChange={(description) => setRequest((current) => ({...current, description: description ?? null}))}
                        hint="Eine kurze Einordnung hilft Nutzer:innen, das Ziel des Links zu erkennen."
                        error={attemptedSubmit && descriptionLength > 255 ? 'Die Beschreibung darf höchstens 255 Zeichen lang sein.' : undefined}
                        disabled={props.isBusy}
                    />
                    <TextFieldComponent
                        label="URL *"
                        value={request.url}
                        onChange={(url) => setRequest((current) => ({...current, url: url ?? ''}))}
                        error={attemptedSubmit && (!isValidUrl || urlLength > 500)
                            ? urlLength > 500 ? 'Die URL darf höchstens 500 Zeichen lang sein.' : 'Bitte geben Sie eine gültige HTTP- oder HTTPS-URL ein.'
                            : undefined}
                        disabled={props.isBusy}
                    />
                    <Box sx={{mt: 2.5, display: 'flex', alignItems: 'center', gap: 2}}>
                        <Button
                            variant="outlined"
                            startIcon={<SelectedIcon/>}
                            onClick={() => setShowIconPicker(true)}
                            disabled={props.isBusy}
                        >
                            Symbol auswählen
                        </Button>
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={request.enabled}
                                    onChange={(_, enabled) => setRequest((current) => ({...current, enabled}))}
                                    disabled={props.isBusy}
                                />
                            }
                            label="Auf der Übersicht anzeigen"
                        />
                    </Box>
                </DialogContent>
                <DialogActions sx={{justifyContent: 'flex-start'}}>
                    <Button variant="contained" startIcon={<Save/>} onClick={handleSave} disabled={props.isBusy}>
                        Speichern
                    </Button>
                    <Button variant="outlined" onClick={props.onClose} disabled={props.isBusy}>
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>

            <IconPickerDialog
                open={showIconPicker}
                onClose={() => setShowIconPicker(false)}
                onSelect={(icon) => setRequest((current) => ({...current, icon}))}
                selectedIconId={request.icon ?? 'link'}
                title="Symbol für den Link auswählen"
                showLabels
                icons={CustomLinkIcons}
            />
        </>
    );
}
