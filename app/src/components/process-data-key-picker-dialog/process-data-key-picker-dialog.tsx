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
    Stack,
    Typography,
} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../text-field/text-field-component';

export interface ProcessDataKeyPickerOption {
    id: string;
    label: string;
    subLabel?: string;
}

interface ProcessDataKeyPickerDialogProps {
    disableWildCards: boolean;
    displayPrefix: string;
    onApply: (value: string) => void;
    onClose: () => void;
    open: boolean;
    options: ProcessDataKeyPickerOption[];
    value: string | null | undefined;
}

const PROCESS_DATA_KEY_PATTERN = /^[a-zA-Z0-9.*_]+$/;
const PROCESS_DATA_KEY_PATTERN_WITHOUT_WILDCARDS = /^[a-zA-Z0-9._]+$/;

function normalizePath(value: string, displayPrefix: string): string {
    const path = value.trim().replace(/^\$\./, '');
    return displayPrefix.length > 0 && path.startsWith(displayPrefix)
        ? path.substring(displayPrefix.length)
        : path;
}

function formatPath(path: string, displayPrefix: string): string {
    return `$.${displayPrefix}${path}`;
}

function getOptionText(option: ProcessDataKeyPickerOption, displayPrefix: string): {
    primary: string;
    secondary: string;
} {
    const path = formatPath(option.id, displayPrefix);
    return option.label === option.id
        ? {primary: path, secondary: option.subLabel ?? path}
        : {
            primary: option.label,
            secondary: [path, option.subLabel].filter((part): part is string => part != null).join(' · '),
        };
}

function isValidPath(path: string, disableWildCards: boolean): boolean {
    return path.length > 0 && (disableWildCards
        ? PROCESS_DATA_KEY_PATTERN_WITHOUT_WILDCARDS
        : PROCESS_DATA_KEY_PATTERN).test(path);
}

export function ProcessDataKeyPickerDialog(props: ProcessDataKeyPickerDialogProps) {
    const [search, setSearch] = useState('');
    const [draftPath, setDraftPath] = useState<string | null>(props.value ?? null);

    useEffect(() => {
        if (props.open) {
            setSearch('');
            setDraftPath(props.value ?? null);
        }
    }, [props.open, props.value]);

    const availableOptions = useMemo(() => {
        const currentPath = normalizePath(props.value ?? '', props.displayPrefix);
        if (currentPath.length === 0 || props.options.some((option) => option.id === currentPath)) {
            return props.options;
        }

        return [{
            id: currentPath,
            label: currentPath,
            subLabel: 'Aktuell eingetragener Pfad',
        }, ...props.options];
    }, [props.displayPrefix, props.options, props.value]);

    const normalizedSearch = normalizePath(search, props.displayPrefix);
    const comparableSearch = normalizedSearch.toLocaleLowerCase('de');
    const filteredOptions = useMemo(() => {
        if (comparableSearch.length === 0) {
            return availableOptions;
        }

        return availableOptions.filter((option) => [
            option.id,
            option.label,
            option.subLabel,
            formatPath(option.id, props.displayPrefix),
        ].filter((part): part is string => part != null)
            .some((part) => part.toLocaleLowerCase('de').includes(comparableSearch)));
    }, [availableOptions, comparableSearch, props.displayPrefix]);
    const canUseCustomPath = isValidPath(normalizedSearch, props.disableWildCards) &&
        // Search is case-insensitive, but process-data keys are not.
        !availableOptions.some((option) => option.id === normalizedSearch);

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
                Vorgangsdatenpfad wählen
            </DialogTitleWithClose>
            <DialogContent sx={{p: 0}}>
                <Box sx={{p: 2}}>
                    <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                        Die Vorschläge basieren auf Vorgangsdaten, die vorherige Prozesselemente bereitstellen können.
                        Abhängig vom ausgeführten Prozesspfad müssen sie zur Laufzeit nicht vorhanden sein. Sie können
                        auch einen eigenen Pfad verwenden.
                    </Typography>
                    <TextFieldComponent
                        label="Vorgangsdatenpfade durchsuchen oder eigenen Pfad eingeben"
                        placeholder="Zum Beispiel zaehler.aktuellerStand"
                        value={search}
                        onChange={(nextSearch) => {
                            setSearch(nextSearch ?? '');
                            setDraftPath(null);
                        }}
                        startIcon={`$.${props.displayPrefix}`}
                        margin="none"
                        muiPassTroughProps={{autoFocus: true}}
                    />
                </Box>

                <RadioGroup
                    aria-label="Vorgeschlagene Vorgangsdatenpfade"
                    value={draftPath ?? ''}
                    onChange={(event) => setDraftPath(event.target.value)}
                >
                    <List
                        disablePadding
                        subheader={(
                            <ListSubheader component="div">
                                Vorgeschlagene Vorgangsdatenpfade
                            </ListSubheader>
                        )}
                        sx={{maxHeight: 'min(50vh, 420px)', overflowY: 'auto'}}
                    >
                        {canUseCustomPath && (
                            <SelectablePathRow
                                value={normalizedSearch}
                                selected={draftPath === normalizedSearch}
                                icon={<Add sx={{color: 'text.secondary'}}/>}
                                primary={formatPath(normalizedSearch, props.displayPrefix)}
                                secondary="Eigenen Pfad verwenden"
                            />
                        )}

                        {filteredOptions.map((option) => {
                            const text = getOptionText(option, props.displayPrefix);
                            return <SelectablePathRow
                                key={option.id}
                                value={option.id}
                                selected={draftPath === option.id}
                                icon={<DataObject sx={{color: 'text.secondary'}}/>}
                                primary={text.primary}
                                secondary={text.secondary}
                            />;
                        })}
                    </List>
                </RadioGroup>

                {!canUseCustomPath && filteredOptions.length === 0 && (
                    <Box sx={{px: 3, py: 6, textAlign: 'center'}}>
                        <Typography color="text.secondary">
                            Keine passenden Vorschläge gefunden
                        </Typography>
                    </Box>
                )}
            </DialogContent>
            <DialogActions>
                <Stack direction="row" spacing={1}>
                    <Button
                        variant="contained"
                        disabled={draftPath == null || draftPath.length === 0}
                        onClick={() => draftPath != null && props.onApply(draftPath)}
                    >
                        Pfad übernehmen
                    </Button>
                    <Button onClick={props.onClose}>Abbrechen</Button>
                </Stack>
            </DialogActions>
        </Dialog>
    );
}

interface SelectablePathRowProps {
    value: string;
    selected: boolean;
    icon: ReactNode;
    primary: string;
    secondary: string;
}

function SelectablePathRow(props: SelectablePathRowProps) {
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
