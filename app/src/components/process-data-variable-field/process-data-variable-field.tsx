import {type ReactNode, useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Radio,
    RadioGroup,
    Typography,
} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../text-field/text-field-component';
import type {FormFieldLayoutProps} from '../form-field';

export interface ProcessDataVariableOption {
    label: string;
    origin: string;
    path: string;
}

interface ProcessDataVariableFieldProps extends FormFieldLayoutProps {
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    hint?: string;
    error?: string;
    required?: boolean;
    label: string;
    onChange: (value: string | null) => void;
    options: ProcessDataVariableOption[];
    value: string | null;
}

function normalizePath(value: string): string {
    // TODO(input-modes): Validate path syntax, write permission and process-data scope on the backend. Client-side
    // normalization only supports the prototype interaction and must not become the authorization boundary.
    return value.trim().replace(/^\$\./, '');
}

export function ProcessDataVariableField(props: ProcessDataVariableFieldProps) {
    const [dialogOpen, setDialogOpen] = useState(false);
    const interactionDisabled = Boolean(props.disabled || props.readOnly || props.busy);

    return (
        <>
            <TextFieldComponent
                id={props.id}
                label={props.label}
                hint={props.hint}
                error={props.error}
                required={props.required}
                disabled={props.disabled}
                readonly={props.readOnly}
                busy={props.busy}
                value={props.value}
                onChange={(value) => props.onChange(value == null ? null : normalizePath(value))}
                bufferInputUntilBlur
                startIcon="$."
                endAction={interactionDisabled ? undefined : [
                    ...(props.value == null || props.value.length === 0 ? [] : [{
                        icon: <Close/>,
                        tooltip: 'Vorgangsdatenvariable leeren',
                        onClick: () => props.onChange(null),
                    }]),
                    {
                        icon: <DataObject/>,
                        tooltip: 'Vorgangsdatenvariable auswählen',
                        onClick: () => setDialogOpen(true),
                    },
                ]}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                labelAction={props.labelAction}
                margin={props.margin}
                showOptionalIndicator={props.showOptionalIndicator}
                sx={props.sx}
            />

            <ProcessDataVariableDialog
                open={dialogOpen && !interactionDisabled}
                options={props.options}
                value={props.value}
                onClose={() => setDialogOpen(false)}
                onApply={(value) => {
                    props.onChange(value);
                    setDialogOpen(false);
                }}
            />
        </>
    );
}

interface ProcessDataVariableDialogProps {
    open: boolean;
    options: ProcessDataVariableOption[];
    value: string | null;
    onApply: (value: string) => void;
    onClose: () => void;
}

function ProcessDataVariableDialog(props: ProcessDataVariableDialogProps) {
    const [search, setSearch] = useState('');
    const [draftPath, setDraftPath] = useState<string | null>(props.value);

    useEffect(() => {
        if (props.open) {
            setSearch('');
            setDraftPath(props.value);
        }
    }, [props.open, props.value]);

    const availableOptions = useMemo(() => {
        const currentPath = normalizePath(props.value ?? '');
        if (currentPath.length === 0 || props.options.some((option) => option.path === currentPath)) {
            return props.options;
        }

        return [
            {
                label: currentPath,
                origin: 'In diesem Prozesselement definiert',
                path: currentPath,
            },
            ...props.options,
        ];
    }, [props.options, props.value]);

    const normalizedSearch = normalizePath(search).toLocaleLowerCase('de');
    const filteredOptions = useMemo(() => {
        if (normalizedSearch.length === 0) {
            return availableOptions;
        }

        return availableOptions.filter((option) => (
            option.path.toLocaleLowerCase('de').includes(normalizedSearch) ||
            option.label.toLocaleLowerCase('de').includes(normalizedSearch) ||
            option.origin.toLocaleLowerCase('de').includes(normalizedSearch)
        ));
    }, [availableOptions, normalizedSearch]);
    const newPath = normalizePath(search);
    const canCreatePath = newPath.length > 0 && !availableOptions.some((option) => (
        option.path.toLocaleLowerCase('de') === newPath.toLocaleLowerCase('de')
    ));

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
            sx={{'& .MuiDialog-container': {alignItems: 'flex-start'}}}
            slotProps={{paper: {sx: {mt: {xs: 2, sm: 6}}}}}
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Vorgangsdatenvariable wählen
            </DialogTitleWithClose>
            <DialogContent sx={{p: 0}}>
                <Box sx={{p: 2}}>
                    <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                        Als Ziel sind nur beschreibbare Vorgangsdaten zulässig. Wähle eine vorhandene Variable oder
                        gib einen neuen Pfad ein.
                    </Typography>
                    <TextFieldComponent
                        label="Vorgangsdatenpfad durchsuchen oder neu anlegen"
                        placeholder="Zum Beispiel zaehler.aktuellerStand"
                        value={search}
                        onChange={(nextSearch) => {
                            setSearch(nextSearch ?? '');
                            setDraftPath(null);
                        }}
                        startIcon="$."
                        margin="none"
                        muiPassTroughProps={{autoFocus: true}}
                    />
                </Box>

                <RadioGroup
                    aria-label="Beschreibbare Vorgangsdatenvariablen"
                    value={draftPath ?? ''}
                    onChange={(event) => setDraftPath(event.target.value)}
                >
                    <List
                        disablePadding
                        subheader={(
                            <ListSubheader component="div">
                                {normalizedSearch.length === 0 ? 'Vorhandene Vorgangsdatenvariablen' : 'Ergebnisse'}
                            </ListSubheader>
                        )}
                        sx={{maxHeight: 'min(50vh, 420px)', overflowY: 'auto'}}
                    >
                        {canCreatePath && (
                            <SelectableVariableRow
                                value={newPath}
                                selected={draftPath === newPath}
                                icon={<Add sx={{color: 'text.secondary'}}/>}
                                primary={`$.${newPath}`}
                                secondary="Neue Vorgangsdatenvariable"
                            />
                        )}

                        {filteredOptions.map((option) => (
                            <SelectableVariableRow
                                key={option.path}
                                value={option.path}
                                selected={draftPath === option.path}
                                icon={<DataObject sx={{color: 'text.secondary'}}/>}
                                primary={option.label}
                                secondary={`${option.origin} - $.${option.path}`}
                            />
                        ))}
                    </List>
                </RadioGroup>

                {!canCreatePath && filteredOptions.length === 0 && (
                    <Box sx={{px: 3, py: 6, textAlign: 'center'}}>
                        <Typography color="text.secondary">
                            Keine passende Vorgangsdatenvariable gefunden
                        </Typography>
                    </Box>
                )}
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    disabled={draftPath == null || draftPath.length === 0}
                    onClick={() => draftPath != null && props.onApply(draftPath)}
                >
                    Übernehmen
                </Button>
                <Button onClick={props.onClose}>Abbrechen</Button>
            </DialogActions>
        </Dialog>
    );
}

interface SelectableVariableRowProps {
    value: string;
    selected: boolean;
    icon: ReactNode;
    primary: string;
    secondary: string;
}

function SelectableVariableRow(props: SelectableVariableRowProps) {
    return (
        <ListItem
            component="label"
            sx={{
                cursor: 'pointer',
                bgcolor: props.selected ? 'action.selected' : 'transparent',
                '&:hover': {bgcolor: 'action.hover'},
                '&:focus-within': {
                    outline: '2px solid',
                    outlineColor: 'primary.main',
                    outlineOffset: -2,
                },
            }}
        >
            <ListItemIcon>{props.icon}</ListItemIcon>
            <ListItemText primary={props.primary} secondary={props.secondary}/>
            <Radio value={props.value} size="small"/>
        </ListItem>
    );
}
