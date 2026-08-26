import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    ButtonBase,
    Dialog,
    DialogActions,
    DialogContent,
    Divider,
    InputAdornment,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Menu,
    MenuItem,
    Radio,
    RadioGroup,
    Stack,
    Tab,
    Tabs,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Function from '@aivot/mui-material-symbols-400-n25-outlined/Function';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Search from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import TextFields from '@aivot/mui-material-symbols-400-n25-outlined/TextFields';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {TextFieldComponent} from '../text-field/text-field-component';
import type {EndAction} from '../text-field/text-field-component-props';
import {FieldLayout, type FieldLayoutControlContext} from '../field-layout/field-layout';

export type InputMode = 'literal' | 'variable' | 'noCode' | 'lowCode';
export type InputModeVariableCategory = 'processData' | 'elementData' | 'elementMetadata' | 'protectedProcessData';

export interface InputModeVariable {
    id: string;
    label: string;
    path: string;
    origin: string;
    category: InputModeVariableCategory;
    description?: string;
}

export interface InputModeNoCodeValue {
    sourceVariableId: string | null;
    operator: string;
    operand: string;
}

export interface InputModeValue<T> {
    // TODO(input-modes): Replace this draft-preserving prototype shape with a versioned, discriminated authored-value
    // envelope. Inactive mode drafts should remain UI state instead of becoming part of the persisted value.
    mode: InputMode;
    literal: T | null;
    variableId: string | null;
    noCode: InputModeNoCodeValue;
    lowCode: string;
}

interface LiteralRenderContext<T> {
    value: T | null;
    onChange: (value: T | null) => void;
    variableInsertAction?: EndAction;
    control: FieldLayoutControlContext;
}

interface InputModeFieldProps<T> {
    label: string;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    allowedModes?: InputMode[];
    variables: InputModeVariable[];
    value: InputModeValue<T>;
    onChange: (value: InputModeValue<T>) => void;
    onInsertVariable?: (variable: InputModeVariable) => void;
    renderLiteral: (context: LiteralRenderContext<T>) => React.ReactNode;
}

interface ModeDefinition {
    label: string;
    description: string;
}

const MODE_DEFINITIONS: Record<InputMode, ModeDefinition> = {
    literal: {
        label: 'Wert',
        description: 'Direkten Wert eingeben',
    },
    variable: {
        label: 'Variable',
        description: 'Eine im Prozess mögliche Variable referenzieren',
    },
    noCode: {
        label: 'Ausdruck (No-Code)',
        description: 'Wert visuell ableiten',
    },
    lowCode: {
        label: 'Skript (Low-Code)',
        description: 'Wert mit JavaScript bestimmen',
    },
};

const VARIABLE_CATEGORY_DEFINITIONS: Record<InputModeVariableCategory, {label: string; prefix: string}> = {
    processData: {
        label: 'Vorgangsdaten',
        prefix: '$.',
    },
    elementData: {
        label: 'Elementdaten',
        prefix: '_.',
    },
    elementMetadata: {
        label: 'Element-Metadaten',
        prefix: '$$.taskMetadata.',
    },
    protectedProcessData: {
        label: 'Geschützte Vorgangsdaten',
        prefix: '$$.',
    },
};

const VARIABLE_CATEGORIES = Object.keys(VARIABLE_CATEGORY_DEFINITIONS) as InputModeVariableCategory[];

const ALL_MODES: InputMode[] = ['literal', 'variable', 'noCode', 'lowCode'];

export function getInputModeVariableReference(variable: InputModeVariable): string {
    return `${VARIABLE_CATEGORY_DEFINITIONS[variable.category].prefix}${variable.path}`;
}

export function getInputModeVariableCategoryLabel(category: InputModeVariableCategory): string {
    return VARIABLE_CATEGORY_DEFINITIONS[category].label;
}

const NO_CODE_OPERATORS = [
    {value: 'add', label: 'plus', symbol: '+'},
    {value: 'subtract', label: 'minus', symbol: '-'},
    {value: 'multiply', label: 'multipliziert mit', symbol: 'x'},
    {value: 'divide', label: 'geteilt durch', symbol: '/'},
    {value: 'fallback', label: 'oder ersatzweise', symbol: '??'},
];

function renderModeIcon(mode: InputMode) {
    const iconProps = {
        fontSize: 'small' as const,
        sx: {color: 'text.secondary'},
    };

    switch (mode) {
        case 'variable':
            return <DataObject {...iconProps}/>;
        case 'noCode':
            return <Functions {...iconProps}/>;
        case 'lowCode':
            return <Code {...iconProps}/>;
        case 'literal':
        default:
            return <TextFields {...iconProps}/>;
    }
}

export function InputModeField<T>(props: InputModeFieldProps<T>) {
    const {
        label,
        hint,
        error,
        required,
        disabled,
        readOnly,
        busy,
        variables,
        value,
        onChange,
        onInsertVariable,
        renderLiteral,
    } = props;

    const allowedModes = props.allowedModes ?? ALL_MODES;
    const interactionDisabled = Boolean(disabled || readOnly || busy);
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
    const [variablePickerPurpose, setVariablePickerPurpose] = useState<'mapping' | 'placeholder' | null>(null);
    const [noCodeEditorOpen, setNoCodeEditorOpen] = useState(false);
    const [lowCodeEditorOpen, setLowCodeEditorOpen] = useState(false);

    const selectedVariable = useMemo(() => {
        return variables.find((variable) => variable.id === value.variableId) ?? null;
    }, [value.variableId, variables]);

    const variableInsertAction: EndAction | undefined = onInsertVariable == null ? undefined : {
        icon: <DataObject/>,
        tooltip: 'Variablenreferenz einfügen',
        onClick: () => setVariablePickerPurpose('placeholder'),
    };

    const handleModeChange = (mode: InputMode) => {
        setMenuAnchor(null);
        if (mode === value.mode) {
            return;
        }
        onChange({
            ...value,
            mode,
        });
    };

    const renderModeContent = (control: FieldLayoutControlContext) => {
        switch (value.mode) {
            case 'variable': {
                const variablePrimary = selectedVariable?.label ?? 'Variable referenzieren';
                return (
                    <SourceSummaryField
                        id={control.inputId}
                        ariaDescribedBy={control.helperTextId}
                        ariaLabel={`${variablePrimary}. ${label}: Variable referenzieren`}
                        primary={variablePrimary}
                        secondary={selectedVariable == null
                            ? 'Keine Referenz festgelegt'
                            : `Erzeugt von ${selectedVariable.origin} - ${getInputModeVariableReference(selectedVariable)}`}
                        empty={selectedVariable == null}
                        disabled={interactionDisabled}
                        invalid={control.invalid}
                        required={control.required}
                        onClick={() => setVariablePickerPurpose('mapping')}
                    />
                );
            }
            case 'noCode': {
                const noCodePrimary = getNoCodeSummary(value.noCode, variables);
                return (
                    <SourceSummaryField
                        id={control.inputId}
                        ariaDescribedBy={control.helperTextId}
                        ariaLabel={`${noCodePrimary}. ${label}: Ausdruck bearbeiten`}
                        primary={noCodePrimary}
                        secondary="No-Code-Ausdruck"
                        empty={value.noCode.sourceVariableId == null}
                        disabled={interactionDisabled}
                        invalid={control.invalid}
                        required={control.required}
                        onClick={() => setNoCodeEditorOpen(true)}
                    />
                );
            }
            case 'lowCode': {
                const lowCodePrimary = value.lowCode.trim().length === 0
                    ? 'Kein Skript definiert'
                    : 'Benutzerdefiniertes Skript';
                return (
                    <SourceSummaryField
                        id={control.inputId}
                        ariaDescribedBy={control.helperTextId}
                        ariaLabel={`${lowCodePrimary}. ${label}: Skript bearbeiten`}
                        primary={lowCodePrimary}
                        secondary={getLowCodeSummary(value.lowCode)}
                        empty={value.lowCode.trim().length === 0}
                        disabled={interactionDisabled}
                        invalid={control.invalid}
                        required={control.required}
                        onClick={() => setLowCodeEditorOpen(true)}
                    />
                );
            }
            case 'literal':
            default:
                return renderLiteral({
                    value: value.literal,
                    onChange: (literal) => onChange({...value, literal}),
                    variableInsertAction,
                    control,
                });
        }
    };

    const modeSelector = (
        <Button
            size="small"
            variant="text"
            disabled={interactionDisabled || allowedModes.length < 2}
            aria-label={`${MODE_DEFINITIONS[value.mode].label}: Eingabemodus für ${label} ändern`}
            aria-haspopup="menu"
            aria-expanded={menuAnchor != null}
            startIcon={renderModeIcon(value.mode)}
            endIcon={<KeyboardArrowDown fontSize="small"/>}
            onClick={(event) => setMenuAnchor(event.currentTarget)}
            sx={{
                minWidth: 0,
                minHeight: 28,
                px: 0.75,
                py: 0.25,
                color: 'text.secondary',
                fontSize: '0.8125rem',
                lineHeight: 1.25,
                '&:hover': {
                    bgcolor: 'action.hover',
                    color: 'text.primary',
                },
            }}
        >
            {MODE_DEFINITIONS[value.mode].label}
        </Button>
    );
    const labelAction = (
        <Stack direction="row" spacing={0.5} sx={{alignItems: 'center'}}>
            {onInsertVariable != null && value.mode === 'literal' && (
                <Tooltip title="Dieser dynamische Text unterstützt Variablen und Bedingungen." arrow>
                    <Box
                        component="span"
                        role="img"
                        aria-label="Dieser dynamische Text unterstützt Variablen und Bedingungen."
                        sx={{
                            display: 'inline-flex',
                            width: 20,
                            height: 20,
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'text.disabled',
                        }}
                    >
                        <Function sx={{fontSize: 16}}/>
                    </Box>
                </Tooltip>
            )}
            {modeSelector}
        </Stack>
    );

    return (
        <Box data-input-mode={value.mode}>
            <FieldLayout
                label={label}
                hint={hint}
                error={error}
                required={required}
                disabled={disabled}
                readOnly={readOnly}
                busy={busy}
                labelAction={labelAction}
            >
                {(control) => renderModeContent(control)}
            </FieldLayout>

            <Menu
                anchorEl={menuAnchor}
                open={menuAnchor != null}
                onClose={() => setMenuAnchor(null)}
                slotProps={{
                    paper: {
                        sx: {minWidth: 260},
                    },
                }}
            >
                {allowedModes.map((mode) => (
                    <MenuItem
                        key={mode}
                        data-testid={`input-mode-option-${mode}`}
                        selected={mode === value.mode}
                        onClick={() => handleModeChange(mode)}
                    >
                        <ListItemIcon>
                            {renderModeIcon(mode)}
                        </ListItemIcon>
                        <ListItemText
                            primary={MODE_DEFINITIONS[mode].label}
                            secondary={MODE_DEFINITIONS[mode].description}
                        />
                        {mode === value.mode && <Check fontSize="small"/>}
                    </MenuItem>
                ))}
            </Menu>

            <VariablePickerDialog
                open={variablePickerPurpose != null}
                variables={variables}
                selectedId={variablePickerPurpose === 'mapping' ? value.variableId : null}
                allowClear={variablePickerPurpose === 'mapping'}
                title="Variable referenzieren"
                onClose={() => setVariablePickerPurpose(null)}
                onClear={() => {
                    onChange({...value, variableId: null});
                    setVariablePickerPurpose(null);
                }}
                onSelect={(variable) => {
                    if (variablePickerPurpose === 'placeholder') {
                        onInsertVariable?.(variable);
                    } else {
                        onChange({...value, variableId: variable.id});
                    }
                    setVariablePickerPurpose(null);
                }}
            />

            <NoCodeEditorDialog
                open={noCodeEditorOpen}
                label={label}
                variables={variables}
                value={value.noCode}
                onClose={() => setNoCodeEditorOpen(false)}
                onApply={(noCode) => {
                    onChange({...value, noCode});
                    setNoCodeEditorOpen(false);
                }}
            />

            <LowCodeEditorDialog
                open={lowCodeEditorOpen}
                label={label}
                value={value.lowCode}
                onClose={() => setLowCodeEditorOpen(false)}
                onApply={(lowCode) => {
                    onChange({...value, lowCode});
                    setLowCodeEditorOpen(false);
                }}
            />
        </Box>
    );
}

interface SourceSummaryFieldProps {
    id: string;
    ariaDescribedBy?: string;
    ariaLabel: string;
    primary: string;
    secondary: string;
    empty?: boolean;
    disabled?: boolean;
    invalid?: boolean;
    required?: boolean;
    onClick: () => void;
}

function SourceSummaryField(props: SourceSummaryFieldProps) {
    const secondaryId = React.useId();
    const describedBy = [secondaryId, props.ariaDescribedBy].filter(Boolean).join(' ');

    return (
        <ButtonBase
            id={props.id}
            aria-label={props.ariaLabel}
            aria-describedby={describedBy}
            aria-invalid={props.invalid || undefined}
            aria-required={props.required || undefined}
            disabled={props.disabled}
            onClick={props.onClick}
            sx={{
                width: '100%',
                minHeight: 48,
                px: 1.5,
                py: 0.625,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 1.25,
                textAlign: 'left',
                opacity: props.disabled ? 0.6 : 1,
                '&:hover': {
                    borderColor: 'text.secondary',
                    bgcolor: 'action.hover',
                },
                '&.Mui-focusVisible': {
                    outline: '2px solid',
                    outlineColor: 'primary.main',
                    outlineOffset: 2,
                },
            }}
        >
            <Box sx={{minWidth: 0, flex: 1}}>
                <Typography
                    variant="body2"
                    sx={{
                        color: props.empty ? 'text.secondary' : 'text.primary',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                    title={props.primary}
                >
                    {props.primary}
                </Typography>
                <Typography
                    id={secondaryId}
                    variant="caption"
                    component="div"
                    sx={{
                        color: 'text.secondary',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                    title={props.secondary}
                >
                    {props.secondary}
                </Typography>
            </Box>
        </ButtonBase>
    );
}

interface VariablePickerDialogProps {
    open: boolean;
    title: string;
    variables: InputModeVariable[];
    selectedId: string | null;
    allowClear: boolean;
    onSelect: (variable: InputModeVariable) => void;
    onClear: () => void;
    onClose: () => void;
}

function VariablePickerDialog(props: VariablePickerDialogProps) {
    const [search, setSearch] = useState('');
    const [selectedCategory, setSelectedCategory] = useState<InputModeVariableCategory | 'all'>('all');
    const [draftSelectedId, setDraftSelectedId] = useState<string | null>(props.selectedId);
    const tabsId = React.useId();
    const panelId = `${tabsId}-panel`;
    const activeTabId = `${tabsId}-tab-${selectedCategory}`;

    useEffect(() => {
        if (props.open) {
            setSearch('');
            setSelectedCategory('all');
            setDraftSelectedId(props.selectedId);
        }
    }, [props.open, props.selectedId]);

    const filteredVariables = useMemo(() => {
        const normalizedSearch = search.trim().toLocaleLowerCase('de');
        return props.variables.filter((variable) => {
            if (selectedCategory !== 'all' && variable.category !== selectedCategory) {
                return false;
            }
            if (normalizedSearch.length === 0) {
                return true;
            }

            return variable.label.toLocaleLowerCase('de').includes(normalizedSearch) ||
                getInputModeVariableReference(variable).toLocaleLowerCase('de').includes(normalizedSearch) ||
                variable.origin.toLocaleLowerCase('de').includes(normalizedSearch) ||
                VARIABLE_CATEGORY_DEFINITIONS[variable.category].label.toLocaleLowerCase('de').includes(normalizedSearch) ||
                variable.description?.toLocaleLowerCase('de').includes(normalizedSearch);
        });
    }, [props.variables, search, selectedCategory]);

    const groups = useMemo(() => {
        return VARIABLE_CATEGORIES
            .map((category) => ({
                category,
                variables: filteredVariables.filter((variable) => variable.category === category),
            }))
            .filter((group) => group.variables.length > 0);
    }, [filteredVariables]);

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
            sx={{
                '& .MuiDialog-container': {
                    alignItems: 'flex-start',
                },
            }}
            slotProps={{
                paper: {
                    sx: {
                        mt: {xs: 2, sm: 6},
                    },
                },
            }}
        >
            <DialogTitleWithClose onClose={props.onClose}>
                {props.title}
            </DialogTitleWithClose>
            <DialogContent sx={{p: 0}}>
                <Box sx={{p: 2}}>
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mb: 2}}
                    >
                        Die Vorschläge zeigen Variablen, die im Prozess erzeugt werden können. Ob zur Laufzeit ein Wert
                        vorliegt, hängt vom ausgeführten Prozesspfad ab.
                    </Typography>
                    <FieldLayout label="Variablenvorschläge durchsuchen">
                        {({inputId}) => (
                            <TextField
                                id={inputId}
                                autoFocus
                                fullWidth
                                placeholder="Name, Pfad oder Prozesselement"
                                margin="none"
                                size="small"
                                value={search}
                                onChange={(event) => {
                                    setSearch(event.target.value);
                                    setDraftSelectedId(null);
                                }}
                                slotProps={{
                                    input: {
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <Search/>
                                            </InputAdornment>
                                        ),
                                    },
                                }}
                            />
                        )}
                    </FieldLayout>
                </Box>

                <Tabs
                    value={selectedCategory}
                    onChange={(_, category: InputModeVariableCategory | 'all') => {
                        setSelectedCategory(category);
                        setDraftSelectedId(null);
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                    allowScrollButtonsMobile
                    aria-label="Kategorie der Variablenvorschläge"
                    sx={{
                        minHeight: 42,
                        borderBottom: '1px solid',
                        borderColor: 'divider',
                        '& .MuiTab-root': {
                            minHeight: 42,
                            py: 1,
                        },
                    }}
                >
                    <Tab
                        id={`${tabsId}-tab-all`}
                        aria-controls={panelId}
                        value="all"
                        label="Alle"
                        data-testid="variable-category-all"
                    />
                    {VARIABLE_CATEGORIES.map((category) => (
                        <Tab
                            key={category}
                            id={`${tabsId}-tab-${category}`}
                            aria-controls={panelId}
                            value={category}
                            label={VARIABLE_CATEGORY_DEFINITIONS[category].label}
                            data-testid={`variable-category-${category}`}
                        />
                    ))}
                </Tabs>

                <Box
                    id={panelId}
                    role="tabpanel"
                    aria-labelledby={activeTabId}
                >
                    {groups.length > 0 ? (
                        <RadioGroup
                            aria-label="Variablenvorschläge"
                            value={draftSelectedId ?? ''}
                            onChange={(event) => setDraftSelectedId(event.target.value)}
                        >
                            <List disablePadding sx={{maxHeight: 'min(56vh, 560px)', overflowY: 'auto'}}>
                                {groups.map((group, groupIndex) => (
                                    <React.Fragment key={group.category}>
                                        {groupIndex > 0 && <Divider/>}
                                        <ListSubheader
                                            component="div"
                                            sx={{
                                                lineHeight: '36px',
                                                bgcolor: 'background.paper',
                                                color: 'text.secondary',
                                            }}
                                        >
                                            {VARIABLE_CATEGORY_DEFINITIONS[group.category].label}
                                        </ListSubheader>
                                        {group.variables.map((variable) => {
                                            const selected = variable.id === draftSelectedId;
                                            return (
                                                <ListItem
                                                    key={variable.id}
                                                    component="label"
                                                    sx={{
                                                        cursor: 'pointer',
                                                        bgcolor: selected ? 'action.selected' : 'transparent',
                                                        '&:hover': {bgcolor: 'action.hover'},
                                                        '&:focus-within': {
                                                            outline: '2px solid',
                                                            outlineColor: 'primary.main',
                                                            outlineOffset: -2,
                                                        },
                                                    }}
                                                >
                                                    <ListItemIcon>
                                                        <DataObject sx={{color: 'text.secondary'}}/>
                                                    </ListItemIcon>
                                                    <ListItemText
                                                        primary={variable.label}
                                                        secondary={`${variable.origin} - ${getInputModeVariableReference(variable)}${variable.description ? ` - ${variable.description}` : ''}`}
                                                    />
                                                    <Radio value={variable.id} size="small"/>
                                                </ListItem>
                                            );
                                        })}
                                    </React.Fragment>
                                ))}
                            </List>
                        </RadioGroup>
                    ) : (
                        <Box sx={{px: 3, py: 6, textAlign: 'center'}}>
                            <Typography color="text.secondary">
                                Keine passenden Vorschläge gefunden
                            </Typography>
                        </Box>
                    )}
                </Box>
            </DialogContent>
            <DialogActions>
                <Stack
                    direction="row"
                    spacing={1}
                    data-testid="variable-dialog-primary-actions"
                >
                    <Button
                        data-testid="use-variable-reference"
                        variant="contained"
                        disabled={draftSelectedId == null}
                        onClick={() => {
                            const selectedVariable = props.variables.find((variable) => variable.id === draftSelectedId);
                            if (selectedVariable != null) {
                                props.onSelect(selectedVariable);
                            }
                        }}
                    >
                        Referenz verwenden
                    </Button>
                    <Button onClick={props.onClose}>Abbrechen</Button>
                </Stack>
                {props.allowClear && (
                    <Button color="inherit" onClick={props.onClear}>
                        Referenz entfernen
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
}

interface NoCodeEditorDialogProps {
    open: boolean;
    label: string;
    variables: InputModeVariable[];
    value: InputModeNoCodeValue;
    onApply: (value: InputModeNoCodeValue) => void;
    onClose: () => void;
}

function NoCodeEditorDialog(props: NoCodeEditorDialogProps) {
    const [draft, setDraft] = useState<InputModeNoCodeValue>(props.value);

    useEffect(() => {
        if (props.open) {
            setDraft({...props.value});
        }
    }, [props.open, props.value]);

    const preview = getNoCodeSummary(draft, props.variables);

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Ausdruck für „{props.label}“
            </DialogTitleWithClose>
            <DialogContent>
                <Stack spacing={3} sx={{pt: 2}}>
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.25,
                            px: 1.5,
                            py: 1.25,
                            bgcolor: 'action.hover',
                            borderRadius: 1,
                        }}
                    >
                        <Functions sx={{color: 'text.secondary'}}/>
                        <Typography variant="body2" sx={{minWidth: 0, overflowWrap: 'anywhere'}}>
                            {preview}
                        </Typography>
                    </Box>

                    <Stack
                        direction={{xs: 'column', md: 'row'}}
                        spacing={2}
                        sx={{alignItems: {md: 'flex-start'}}}
                    >
                        <Box sx={{flex: 1, minWidth: 0}}>
                            <FieldLayout label="Variablenreferenz">
                                {({inputId, labelId}) => (
                                    <SelectFieldComponent
                                        label=""
                                        ariaLabelledBy={labelId}
                                        value={draft.sourceVariableId}
                                        onChange={(sourceVariableId) => setDraft({...draft, sourceVariableId})}
                                        options={props.variables.map((variable) => ({
                                            value: variable.id,
                                            label: variable.label,
                                            subLabel: `${VARIABLE_CATEGORY_DEFINITIONS[variable.category].label} - ${getInputModeVariableReference(variable)}`,
                                        }))}
                                        placeholder="Variable referenzieren"
                                        size="small"
                                        sx={{minHeight: 48}}
                                        muiPassTroughProps={{id: inputId, margin: 'none'}}
                                    />
                                )}
                            </FieldLayout>
                        </Box>
                        <Box sx={{flex: 0.85, minWidth: 0}}>
                            <FieldLayout label="Operator">
                                {({inputId, labelId}) => (
                                    <SelectFieldComponent
                                        label=""
                                        ariaLabelledBy={labelId}
                                        value={draft.operator}
                                        onChange={(operator) => setDraft({...draft, operator: operator ?? 'add'})}
                                        options={NO_CODE_OPERATORS.map((operator) => ({
                                            value: operator.value,
                                            label: operator.label,
                                        }))}
                                        includeEmptyOption={false}
                                        size="small"
                                        sx={{minHeight: 48}}
                                        muiPassTroughProps={{id: inputId, margin: 'none'}}
                                    />
                                )}
                            </FieldLayout>
                        </Box>
                        <Box sx={{flex: 0.7, minWidth: 0}}>
                            <FieldLayout label="Wert">
                                {({inputId, labelId}) => (
                                    <TextFieldComponent
                                        id={inputId}
                                        label=""
                                        ariaLabelledBy={labelId}
                                        value={draft.operand}
                                        onChange={(operand) => setDraft({...draft, operand: operand ?? ''})}
                                        size="small"
                                        sx={{m: 0, '& .MuiInputBase-root': {minHeight: 48}}}
                                        muiPassTroughProps={{margin: 'none'}}
                                    />
                                )}
                            </FieldLayout>
                        </Box>
                    </Stack>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button variant="contained" onClick={() => props.onApply(draft)}>
                    Übernehmen
                </Button>
                <Button onClick={props.onClose}>Abbrechen</Button>
            </DialogActions>
        </Dialog>
    );
}

interface LowCodeEditorDialogProps {
    open: boolean;
    label: string;
    value: string;
    onApply: (value: string) => void;
    onClose: () => void;
}

function LowCodeEditorDialog(props: LowCodeEditorDialogProps) {
    const [draft, setDraft] = useState(props.value);

    useEffect(() => {
        if (props.open) {
            setDraft(props.value);
        }
    }, [props.open, props.value]);

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth
            maxWidth="lg"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Skript für „{props.label}“
            </DialogTitleWithClose>
            <DialogContent sx={{pt: 2}}>
                <FieldLayout label="JavaScript">
                    {({inputId}) => (
                        <TextField
                            id={inputId}
                            autoFocus
                            fullWidth
                            multiline
                            minRows={12}
                            margin="none"
                            value={draft}
                            onChange={(event) => setDraft(event.target.value)}
                            slotProps={{
                                htmlInput: {
                                    'aria-label': `JavaScript für ${props.label}`,
                                    spellCheck: false,
                                },
                            }}
                            sx={{
                                '& textarea': {
                                    fontFamily: 'monospace',
                                    fontSize: '0.875rem',
                                    lineHeight: 1.6,
                                },
                            }}
                        />
                    )}
                </FieldLayout>
            </DialogContent>
            <DialogActions>
                <Button variant="contained" onClick={() => props.onApply(draft)}>
                    Übernehmen
                </Button>
                <Button onClick={props.onClose}>Abbrechen</Button>
            </DialogActions>
        </Dialog>
    );
}

function getNoCodeSummary(value: InputModeNoCodeValue, variables: InputModeVariable[]): string {
    const variable = variables.find((candidate) => candidate.id === value.sourceVariableId);
    if (variable == null) {
        return 'Kein Ausdruck definiert';
    }

    const operator = NO_CODE_OPERATORS.find((candidate) => candidate.value === value.operator);
    return `${variable.label} ${operator?.symbol ?? operator?.label ?? value.operator} ${value.operand || '…'}`;
}

function getLowCodeSummary(value: string): string {
    const firstLine = value
        .split('\n')
        .map((line) => line.trim())
        .find((line) => line.length > 0);
    return firstLine ?? 'Kein Skript definiert';
}
