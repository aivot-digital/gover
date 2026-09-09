import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    ButtonBase,
    Dialog,
    DialogActions,
    DialogContent,
    Divider,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Radio,
    RadioGroup,
    Stack,
    Tab,
    Tabs,
    Typography,
} from '@mui/material';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import {NoCodeIcon} from '../../modules/nocode/data/no-code-icon';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Search from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {TextFieldComponent} from '../text-field/text-field-component';
import type {EndAction} from '../text-field/text-field-component-props';
import {FormField, type FormFieldControlContext, type FormFieldLayoutProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {DynamicTextIndicator, InputModeSelector} from '../input-mode-selector';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';
import {
    type AuthoredInputValue,
    getInputVariableKey,
    getInputVariableReference,
    type InputMode,
    type InputVariableReference,
    type InputVariableSource,
    type InputVariableSuggestion,
    normalizeAuthoredInputValue,
} from '../../models/input-mode';
import {isNoCodeStaticValue, type NoCodeOperand} from '../../models/functions/no-code-expression';
import {NoCodeEditorWrapper} from '../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {type AnyElement} from '../../models/elements/any-element';
import {CodeEditor} from '../code-editor/code-editor';
import {useNoCodePreview} from '../../modules/nocode/hooks/use-no-code-preview';

export type {InputMode} from '../../models/input-mode';
export interface InputModeVariable extends Omit<InputVariableSuggestion, 'source' | 'origin'> {
    source: InputVariableSource;
    origin?: InputVariableSuggestion['origin'] | string;
    id?: string;
}
export type InputModeValue<T> = AuthoredInputValue<T>;

export interface InputModeLiteralFieldProps extends Omit<FormFieldLayoutProps, 'labelAction'> {
    label: string;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    labelAction?: React.ReactNode;
}

export interface InputModeLiteralRenderContext<T> {
    value: T | null;
    onChange: (value: T | null, triggeringElementIds?: string[]) => void;
    variableInsertAction?: EndAction;
    fieldProps: InputModeLiteralFieldProps;
}

export interface InputModeFieldProps<T> extends Omit<FormFieldLayoutProps, 'labelAction'> {
    label: string;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    allowedModes?: InputMode[];
    allowedVariableSources?: InputVariableSource[];
    dynamicTextVariableSources?: InputVariableSource[];
    variables: InputModeVariable[];
    value: InputModeValue<T> | T | null;
    onChange: (value: InputModeValue<T>, triggeringElementIds?: string[]) => void;
    onInsertVariable?: (variable: InputModeVariable) => void;
    renderLiteral: (context: InputModeLiteralRenderContext<T>) => React.ReactNode;
    rootElement?: AnyElement;
    noCodeReturnType?: NoCodeDataType;
}

const VARIABLE_SOURCE_DEFINITIONS: Record<InputVariableSource, {label: string; prefix: string; pathHint: string}> = {
    ProcessData: {
        label: 'Vorgangsdaten',
        prefix: '$.',
        pathHint: 'Pfad innerhalb der Vorgangsdaten, zum Beispiel person.vorname.',
    },
    ElementData: {
        label: 'Elementdaten',
        prefix: '_.',
        pathHint: 'Zuerst den Element-Datenschlüssel, danach den Pfad angeben, zum Beispiel antragPruefen.ergebnis.',
    },
    ElementMetadata: {
        label: 'Element-Metadaten',
        prefix: '$$.taskMetadata.',
        pathHint: 'Zuerst den Element-Datenschlüssel, danach den Pfad angeben, zum Beispiel antragPruefen.finished.',
    },
    ProtectedProcessData: {
        label: 'Geschützte Vorgangsdaten',
        prefix: '$$.',
        pathHint: 'Pfad innerhalb der geschützten Vorgangsdaten, zum Beispiel caseNumber.',
    },
};

const VARIABLE_SOURCES = Object.keys(VARIABLE_SOURCE_DEFINITIONS) as InputVariableSource[];

export function getInputModeVariableReference(variable: InputModeVariable): string {
    return getInputVariableReference(toVariableReference(variable));
}

export function getInputModeVariableCategoryLabel(source: InputVariableSource): string {
    return VARIABLE_SOURCE_DEFINITIONS[source].label;
}

interface ModeDrafts<T> {
    Literal: T | null;
    Variable: InputVariableReference;
    NoCode: NoCodeOperand;
    LowCode: string;
}

const EMPTY_NO_CODE: NoCodeOperand = {type: 'NoCodeStaticValue', value: null};

function emptyVariableReference(allowedSources: InputVariableSource[]): InputVariableReference {
    return {source: allowedSources[0] ?? 'ProcessData', path: ''};
}

function createModeDrafts<T>(value: AuthoredInputValue<T>, allowedSources: InputVariableSource[]): ModeDrafts<T> {
    return {
        Literal: value.type === 'Literal' ? value.value : null,
        Variable: value.type === 'Variable' ? value.reference : emptyVariableReference(allowedSources),
        NoCode: value.type === 'NoCode' ? value.operand : EMPTY_NO_CODE,
        LowCode: value.type === 'LowCode' ? value.code : '',
    };
}

function valueForMode<T>(mode: InputMode, drafts: ModeDrafts<T>): AuthoredInputValue<T> {
    switch (mode) {
        case 'Variable':
            return {type: 'Variable', reference: drafts.Variable};
        case 'NoCode':
            return {type: 'NoCode', operand: drafts.NoCode};
        case 'LowCode':
            return {type: 'LowCode', code: drafts.LowCode};
        case 'Literal':
            return {type: 'Literal', value: drafts.Literal};
    }
}

export function InputModeField<T>(props: InputModeFieldProps<T>) {
    const {label, hint, error, required, disabled, readOnly, busy, variables, onChange, onInsertVariable, renderLiteral} = props;
    const generatedId = useNormalizedReactId();
    const controlId = props.id ?? `input-mode-${generatedId}`;
    const value = normalizeInputModeValue<T>(props.value);
    const editingDisabled = Boolean(disabled || readOnly || busy);
    const allowedVariableSources = props.allowedVariableSources ?? VARIABLE_SOURCES;
    const dynamicTextVariableSources = props.dynamicTextVariableSources ?? VARIABLE_SOURCES;
    const [drafts, setDrafts] = useState<ModeDrafts<T>>(() => createModeDrafts(value, allowedVariableSources));
    const [variablePickerPurpose, setVariablePickerPurpose] = useState<'mapping' | 'placeholder' | null>(null);
    const [noCodeEditorOpen, setNoCodeEditorOpen] = useState(false);
    const [lowCodeEditorOpen, setLowCodeEditorOpen] = useState(false);

    // Server refreshes update the active value without discarding drafts the author created in other modes.
    useEffect(() => {
        setDrafts((current) => ({
            ...current,
            ...(value.type === 'Literal' ? {Literal: value.value} : {}),
            ...(value.type === 'Variable' ? {Variable: value.reference} : {}),
            ...(value.type === 'NoCode' ? {NoCode: value.operand} : {}),
            ...(value.type === 'LowCode' ? {LowCode: value.code} : {}),
        }));
    }, [props.value]);

    const selectedVariable = useMemo(() => {
        if (value.type !== 'Variable') return null;
        const key = getInputVariableKey(value.reference);
        return variables.find((variable) => getInputVariableKey(toVariableReference(variable)) === key) ?? null;
    }, [value, variables]);

    const variableInsertAction: EndAction | undefined = onInsertVariable == null ? undefined : {
        icon: <DataObject/>,
        tooltip: 'Variablenreferenz einfügen',
        onClick: () => setVariablePickerPurpose('placeholder'),
    };

    const updateDraft = <M extends keyof ModeDrafts<T>>(
        mode: M,
        draft: ModeDrafts<T>[M],
        triggeringElementIds?: string[],
    ) => {
        const nextDrafts = {...drafts, [mode]: draft};
        setDrafts(nextDrafts);
        onChange(valueForMode(mode, nextDrafts), triggeringElementIds);
    };

    const changeMode = (mode: InputMode) => {
        if (mode === value.type) return;

        if (mode === 'Variable' && !allowedVariableSources.includes(drafts.Variable.source)) {
            const nextDrafts = {
                ...drafts,
                Variable: emptyVariableReference(allowedVariableSources),
            };
            setDrafts(nextDrafts);
            onChange(valueForMode(mode, nextDrafts));
            return;
        }

        onChange(valueForMode(mode, drafts));
    };

    const renderModeContent = (control: FormFieldControlContext) => {
        if (value.type === 'Variable') {
            const hasReference = value.reference.path.length > 0;
            const reference = getInputVariableReference(value.reference);
            const variablePrimary = selectedVariable?.label ??
                (hasReference ? reference : 'Keine Variable referenziert');
            const origin = selectedVariable == null ? '' : resolveRawOrigin(selectedVariable);
            return <SourceSummaryField
                id={control.controlId}
                ariaDescribedBy={control.ariaProps['aria-describedby']}
                ariaLabel={`${label}: ${variablePrimary}. Variable ${editingDisabled ? 'ansehen' : 'referenzieren'}`}
                icon={<DataObject/>}
                primary={variablePrimary}
                secondary={hasReference
                    ? [
                        variablePrimary === reference ? getInputModeVariableCategoryLabel(value.reference.source) : reference,
                        origin,
                    ].filter(Boolean).join(' · ')
                    : undefined}
                empty={!hasReference}
                disabled={Boolean(busy)}
                invalid={control.invalid}
                required={control.required}
                onClick={() => setVariablePickerPurpose('mapping')}
            />;
        }

        if (value.type === 'NoCode') {
            const hasExpression = !isNoCodeStaticValue(value.operand) || value.operand.value != null;
            const primary = hasExpression ? 'Ausdruck definiert' : 'Kein Ausdruck definiert';
            return <NoCodeSummaryField
                id={control.controlId}
                ariaDescribedBy={control.ariaProps['aria-describedby']}
                ariaLabel={`${label}: ${primary}. Ausdruck ${editingDisabled ? 'ansehen' : 'bearbeiten'}`}
                operand={value.operand}
                rootElement={props.rootElement}
                icon={<NoCodeIcon/>}
                primary={primary}
                empty={!hasExpression}
                disabled={Boolean(busy)}
                invalid={control.invalid}
                required={control.required}
                onClick={() => setNoCodeEditorOpen(true)}
            />;
        }

        if (value.type === 'LowCode') {
            const hasCode = value.code.trim().length > 0;
            const primary = hasCode ? 'Skript definiert' : 'Kein Skript definiert';
            return <SourceSummaryField
                id={control.controlId}
                ariaDescribedBy={control.ariaProps['aria-describedby']}
                ariaLabel={`${label}: ${primary}. Skript ${editingDisabled ? 'ansehen' : 'bearbeiten'}`}
                icon={<Code/>}
                primary={primary}
                secondary={hasCode ? getLowCodeSummary(value.code) : undefined}
                empty={!hasCode}
                disabled={Boolean(busy)}
                invalid={control.invalid}
                required={control.required}
                onClick={() => setLowCodeEditorOpen(true)}
            />;
        }

        return null;
    };

    const showsModeSelector = (props.allowedModes?.length ?? 4) > 1;
    const labelAction = (onInsertVariable != null && value.type === 'Literal') || showsModeSelector
        ? <Stack direction="row" spacing={0.5} sx={{alignItems: 'center'}}>
            {onInsertVariable != null && value.type === 'Literal' && <DynamicTextIndicator/>}
            {showsModeSelector && <InputModeSelector
                fieldLabel={label}
                controlledFieldId={controlId}
                value={value.type}
                allowedModes={props.allowedModes}
                disabled={editingDisabled}
                onChange={changeMode}
            />}
        </Stack>
        : undefined;
    const fieldProps: InputModeLiteralFieldProps = {
        id: controlId,
        label,
        hint,
        error,
        required,
        disabled,
        readOnly,
        busy,
        ariaLabel: props.ariaLabel,
        ariaDescribedBy: props.ariaDescribedBy,
        externalAction: props.externalAction,
        labelAction,
        margin: props.margin,
        showOptionalIndicator: props.showOptionalIndicator,
        sx: props.sx,
    };
    const variablePickerSources = variablePickerPurpose === 'placeholder'
        ? dynamicTextVariableSources
        : allowedVariableSources;

    return <Box data-input-mode={value.type}>
        {value.type === 'Literal' ? renderLiteral({
            value: value.value,
            onChange: (literal, triggeringElementIds) => updateDraft('Literal', literal, triggeringElementIds),
            variableInsertAction,
            fieldProps,
        }) : <FormField {...fieldProps}>{renderModeContent}</FormField>}

        <VariablePickerDialog
            open={variablePickerPurpose != null}
            variables={variables}
            allowedSources={variablePickerSources}
            selectedReference={variablePickerPurpose === 'mapping' && value.type === 'Variable' ? value.reference : null}
            allowClear={variablePickerPurpose === 'mapping'}
            readOnly={editingDisabled}
            title="Variable referenzieren"
            onClose={() => setVariablePickerPurpose(null)}
            onClear={() => {
                updateDraft('Variable', emptyVariableReference(allowedVariableSources));
                setVariablePickerPurpose(null);
            }}
            onSelect={(reference, suggestion) => {
                if (variablePickerPurpose === 'placeholder') {
                    onInsertVariable?.(suggestion ?? {...reference, label: getInputVariableReference(reference)});
                } else {
                    updateDraft('Variable', reference);
                }
                setVariablePickerPurpose(null);
            }}
        />

        <NoCodeEditorDialog
            open={noCodeEditorOpen}
            label={label}
            rootElement={props.rootElement}
            returnType={props.noCodeReturnType ?? NoCodeDataType.Runtime}
            value={drafts.NoCode}
            readOnly={editingDisabled}
            onClose={() => setNoCodeEditorOpen(false)}
            onApply={(noCode) => {
                updateDraft('NoCode', noCode);
                setNoCodeEditorOpen(false);
            }}
        />

        <LowCodeEditorDialog
            open={lowCodeEditorOpen}
            label={label}
            value={drafts.LowCode}
            readOnly={editingDisabled}
            onClose={() => setLowCodeEditorOpen(false)}
            onApply={(lowCode) => {
                updateDraft('LowCode', lowCode);
                setLowCodeEditorOpen(false);
            }}
        />
    </Box>;
}

interface SourceSummaryFieldProps {
    id: string;
    ariaDescribedBy?: string;
    ariaLabel: string;
    icon: React.ReactNode;
    primary: string;
    secondary?: string;
    empty?: boolean;
    disabled?: boolean;
    invalid?: boolean;
    required?: boolean;
    onClick: () => void;
}

function NoCodeSummaryField({operand, rootElement, ...props}: SourceSummaryFieldProps & {
    operand: NoCodeOperand;
    rootElement?: AnyElement;
}) {
    const preview = useNoCodePreview(props.empty ? null : operand, rootElement);
    return <SourceSummaryField {...props} secondary={preview}/>;
}

function SourceSummaryField(props: SourceSummaryFieldProps) {
    const secondaryId = `input-mode-summary-${useNormalizedReactId()}`;
    const hasSecondary = Boolean(props.secondary);
    const describedBy = [hasSecondary ? secondaryId : undefined, props.ariaDescribedBy].filter(Boolean).join(' ') || undefined;

    return <ButtonBase
        id={props.id}
        aria-label={`${props.ariaLabel}${props.required ? '. Pflichtfeld' : ''}`}
        aria-describedby={describedBy}
        aria-haspopup="dialog"
        aria-invalid={props.invalid || undefined}
        disabled={props.disabled}
        onClick={props.onClick}
        sx={{
            width: '100%',
            minWidth: 0,
            minHeight: hasSecondary ? FormFieldTokens.controlWithSecondaryTextMinHeight : FormFieldTokens.controlMinHeight,
            px: 1.5,
            py: 0.5,
            border: '1px solid',
            borderColor: props.invalid ? 'error.main' : 'divider',
            borderRadius: 1,
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
            textAlign: 'left',
            opacity: props.disabled ? 0.6 : 1,
            '&:hover': {borderColor: props.invalid ? 'error.main' : 'text.secondary', bgcolor: 'action.hover'},
            '&.Mui-focusVisible': {outline: '2px solid', outlineColor: props.invalid ? 'error.main' : 'primary.main', outlineOffset: 2},
        }}
    >
        <Box component="span" aria-hidden="true" sx={{display: 'inline-flex', flexShrink: 0, color: 'text.secondary'}}>
            {props.icon}
        </Box>
        <Box component="span" sx={{minWidth: 0, flex: 1, display: 'flex', flexDirection: 'column', gap: hasSecondary ? 0.25 : 0}}>
            <Typography
                component="span"
                variant="body2"
                title={props.primary}
                sx={{
                    color: props.empty ? 'text.secondary' : 'text.primary',
                    fontSize: '1rem',
                    lineHeight: 1.25,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {props.primary}
            </Typography>
            {hasSecondary && <Typography
                id={secondaryId}
                variant="caption"
                component="span"
                title={props.secondary}
                sx={{
                    color: 'text.secondary',
                    fontSize: '0.75rem',
                    lineHeight: 1.2,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {props.secondary}
            </Typography>}
        </Box>
        <ChevronRight aria-hidden="true" fontSize="small" sx={{flexShrink: 0, color: 'action.active'}}/>
    </ButtonBase>;
}

interface VariablePickerDialogProps {
    open: boolean;
    title: string;
    variables: InputModeVariable[];
    allowedSources: InputVariableSource[];
    selectedReference: InputVariableReference | null;
    allowClear: boolean;
    readOnly: boolean;
    onSelect: (reference: InputVariableReference, suggestion?: InputModeVariable) => void;
    onClear: () => void;
    onClose: () => void;
}

function VariablePickerDialog(props: VariablePickerDialogProps) {
    const [search, setSearch] = useState('');
    const [selectedSource, setSelectedSource] = useState<InputVariableSource | 'all'>('all');
    const [draftSelectedKey, setDraftSelectedKey] = useState<string | null>(null);
    const [customSource, setCustomSource] = useState<InputVariableSource>('ProcessData');
    const [customPath, setCustomPath] = useState('');
    const tabsId = useNormalizedReactId();
    const panelId = `${tabsId}-panel`;

    useEffect(() => {
        if (props.open) {
            setSearch('');
            setSelectedSource('all');
            setDraftSelectedKey(props.selectedReference == null ? null : getInputVariableKey(props.selectedReference));
            const initialReference = props.selectedReference != null && props.allowedSources.includes(props.selectedReference.source)
                ? props.selectedReference
                : emptyVariableReference(props.allowedSources);
            setCustomSource(initialReference.source);
            setCustomPath(toCustomVariablePath(initialReference));
        }
    }, [props.open, props.selectedReference, props.allowedSources]);

    const filteredVariables = useMemo(() => {
        const normalizedSearch = search.trim().toLocaleLowerCase('de');
        return props.variables.filter((variable) => {
            const source = resolveVariableSource(variable);
            if (!props.allowedSources.includes(source) || selectedSource !== 'all' && source !== selectedSource) return false;
            const origin = resolveRawOrigin(variable);
            return normalizedSearch.length === 0 || [variable.label, getInputModeVariableReference(variable), origin, variable.description]
                .filter((part): part is string => typeof part === 'string')
                .some((part) => part.toLocaleLowerCase('de').includes(normalizedSearch));
        });
    }, [props.allowedSources, props.variables, search, selectedSource]);

    const groups = props.allowedSources
        .map((source) => ({source, variables: filteredVariables.filter((variable) => resolveVariableSource(variable) === source)}))
        .filter((group) => group.variables.length > 0);
    const selectedSuggestion = props.variables.find((variable) => getInputVariableKey(toVariableReference(variable)) === draftSelectedKey);
    const customReference = parseCustomVariableReference(customSource, customPath);
    const selectedReference = selectedSuggestion == null ? customReference : toVariableReference(selectedSuggestion);

    return <Dialog
        open={props.open}
        onClose={props.onClose}
        fullWidth
        maxWidth="sm"
        sx={{'& .MuiDialog-container': {alignItems: 'flex-start'}}}
        slotProps={{paper: {sx: {mt: {xs: 2, sm: 6}}}}}
    >
        <DialogTitleWithClose onClose={props.onClose}>{props.title}</DialogTitleWithClose>
        <DialogContent sx={{p: 0}}>
            <Box sx={{p: 2}}>
                <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                    Die Vorschläge zeigen Variablen, die im Prozess erzeugt werden können. Ob zur Laufzeit ein Wert vorliegt, hängt vom ausgeführten Prozesspfad ab.
                </Typography>
                <TextFieldComponent
                    label="Variablenvorschläge durchsuchen"
                    placeholder="Name, Pfad oder Prozesselement"
                    value={search}
                    onChange={(nextSearch) => setSearch(nextSearch ?? '')}
                    startIcon={<Search/>}
                    margin="none"
                    muiPassTroughProps={{autoFocus: true}}
                />
            </Box>

            <Tabs
                value={selectedSource}
                onChange={(_, source: InputVariableSource | 'all') => setSelectedSource(source)}
                variant="scrollable"
                scrollButtons="auto"
                allowScrollButtonsMobile
                aria-label="Kategorie der Variablenvorschläge"
                sx={{minHeight: 42, borderBottom: '1px solid', borderColor: 'divider', '& .MuiTab-root': {minHeight: 42, py: 1}}}
            >
                <Tab id={`${tabsId}-tab-all`} aria-controls={panelId} value="all" label="Alle" data-testid="variable-category-all"/>
                {props.allowedSources.map((source) => <Tab
                    key={source}
                    id={`${tabsId}-tab-${source}`}
                    aria-controls={panelId}
                    value={source}
                    label={VARIABLE_SOURCE_DEFINITIONS[source].label}
                    data-testid={`variable-category-${source}`}
                />)}
            </Tabs>

            <Box id={panelId} role="tabpanel" aria-labelledby={`${tabsId}-tab-${selectedSource}`}>
                {groups.length > 0 ? <RadioGroup aria-label="Variablenvorschläge" value={draftSelectedKey ?? ''} onChange={(event) => setDraftSelectedKey(event.target.value)}>
                    <List disablePadding sx={{maxHeight: 'min(38vh, 380px)', overflowY: 'auto'}}>
                        {groups.map((group, groupIndex) => <React.Fragment key={group.source}>
                            {groupIndex > 0 && <Divider/>}
                            <ListSubheader component="div" sx={{lineHeight: '36px', bgcolor: 'background.paper', color: 'text.secondary'}}>
                                {VARIABLE_SOURCE_DEFINITIONS[group.source].label}
                            </ListSubheader>
                            {group.variables.map((variable) => {
                                const key = getInputVariableKey(toVariableReference(variable));
                                return <ListItem key={key} component="label" sx={{cursor: 'pointer', bgcolor: key === draftSelectedKey ? 'action.selected' : 'transparent', '&:hover': {bgcolor: 'action.hover'}}}>
                                    <ListItemIcon><DataObject sx={{color: 'text.secondary'}}/></ListItemIcon>
                                    <ListItemText primary={variable.label} secondary={`${resolveOriginLabel(variable)}${getInputModeVariableReference(variable)}${variable.description ? ` - ${variable.description}` : ''}`}/>
                                    <Radio value={key} size="small" disabled={props.readOnly}/>
                                </ListItem>;
                            })}
                        </React.Fragment>)}
                    </List>
                </RadioGroup> : <Box sx={{px: 3, py: 4, textAlign: 'center'}}><Typography color="text.secondary">Keine passenden Vorschläge gefunden</Typography></Box>}
            </Box>

            <Divider/>
            <Box sx={{p: 2}}>
                <Typography variant="subtitle2" sx={{mb: 1.5}}>Eigene Referenz</Typography>
                <Stack spacing={1.5}>
                    <SelectFieldComponent
                        label="Variablenquelle"
                        value={customSource}
                        options={props.allowedSources.map((source) => ({value: source, label: VARIABLE_SOURCE_DEFINITIONS[source].label}))}
                        includeEmptyOption={false}
                        disabled={props.readOnly}
                        margin="none"
                        onChange={(source) => {
                            if (source != null) {
                                setDraftSelectedKey(null);
                                setCustomSource(source as InputVariableSource);
                            }
                        }}
                    />
                    <TextFieldComponent
                        label="Variablenpfad"
                        hint={VARIABLE_SOURCE_DEFINITIONS[customSource].pathHint}
                        startIcon={VARIABLE_SOURCE_DEFINITIONS[customSource].prefix}
                        value={customPath}
                        disabled={props.readOnly}
                        margin="none"
                        onChange={(path) => {
                            setDraftSelectedKey(null);
                            setCustomPath(path ?? '');
                        }}
                    />
                </Stack>
            </Box>
        </DialogContent>
        <DialogActions>
            <Stack direction="row" spacing={1} data-testid="variable-dialog-primary-actions">
                {!props.readOnly && <Button
                    data-testid="use-variable-reference"
                    variant="contained"
                    disabled={selectedReference == null}
                    onClick={() => selectedReference != null && props.onSelect(selectedReference, selectedSuggestion)}
                >
                    Referenz verwenden
                </Button>}
                <Button onClick={props.onClose}>{props.readOnly ? 'Schließen' : 'Abbrechen'}</Button>
            </Stack>
            {props.allowClear && !props.readOnly && <Button color="inherit" onClick={props.onClear}>Referenz entfernen</Button>}
        </DialogActions>
    </Dialog>;
}

interface NoCodeEditorDialogProps {
    open: boolean;
    label: string;
    rootElement?: AnyElement;
    returnType: NoCodeDataType;
    value: NoCodeOperand;
    readOnly: boolean;
    onApply: (value: NoCodeOperand) => void;
    onClose: () => void;
}

function NoCodeEditorDialog(props: NoCodeEditorDialogProps) {
    const [draft, setDraft] = useState<NoCodeOperand>(props.value);

    useEffect(() => {
        if (props.open) setDraft(props.value);
    }, [props.open, props.value]);

    return <Dialog open={props.open} onClose={props.onClose} fullWidth maxWidth="md">
        <DialogTitleWithClose onClose={props.onClose}>Ausdruck für „{props.label}“</DialogTitleWithClose>
        <DialogContent sx={{pt: 2}}>
            {props.rootElement == null ? <Typography color="text.secondary">Der Ausdruckseditor ist in diesem Kontext nicht verfügbar.</Typography> : <NoCodeEditorWrapper
                parents={[props.rootElement]}
                noCode={draft}
                onChange={(operand) => setDraft(operand ?? EMPTY_NO_CODE)}
                editable={!props.readOnly}
                desiredReturnType={props.returnType}
                contextType="PROCESS"
                label="Ausdruck"
            />}
        </DialogContent>
        <DialogActions>
            {!props.readOnly && <Button variant="contained" onClick={() => props.onApply(draft)} disabled={props.rootElement == null}>Übernehmen</Button>}
            <Button onClick={props.onClose}>{props.readOnly ? 'Schließen' : 'Abbrechen'}</Button>
        </DialogActions>
    </Dialog>;
}

interface LowCodeEditorDialogProps {
    open: boolean;
    label: string;
    value: string;
    readOnly: boolean;
    onApply: (value: string) => void;
    onClose: () => void;
}

function LowCodeEditorDialog(props: LowCodeEditorDialogProps) {
    const [draft, setDraft] = useState(props.value);

    useEffect(() => {
        if (props.open) setDraft(props.value);
    }, [props.open, props.value]);

    return <Dialog open={props.open} onClose={props.onClose} fullWidth maxWidth="lg">
        <DialogTitleWithClose onClose={props.onClose}>Skript für „{props.label}“</DialogTitleWithClose>
        <DialogContent sx={{pt: 2}}>
            <CodeEditor
                ariaLabel={`JavaScript für ${props.label}`}
                value={draft}
                readOnly={props.readOnly}
                onChange={setDraft}
                height="min(55vh, 520px)"
                wordWrap
                actions={[]}
            />
        </DialogContent>
        <DialogActions>
            {!props.readOnly && <Button variant="contained" onClick={() => props.onApply(draft)}>Übernehmen</Button>}
            <Button onClick={props.onClose}>{props.readOnly ? 'Schließen' : 'Abbrechen'}</Button>
        </DialogActions>
    </Dialog>;
}

function resolveOriginLabel(variable: InputModeVariable | null | undefined): string {
    const origin = variable == null ? '' : resolveRawOrigin(variable);
    return origin == null || origin.trim().length === 0 ? '' : `Erzeugt von ${origin} - `;
}

function resolveRawOrigin(variable: InputModeVariable): string {
    return typeof variable.origin === 'string' ? variable.origin : variable.origin?.name ?? '';
}

function resolveVariableSource(variable: InputModeVariable): InputVariableSource {
    return variable.source;
}

function toVariableReference(variable: InputModeVariable): InputVariableReference {
    return {source: resolveVariableSource(variable), path: variable.path, nodeDataKey: variable.nodeDataKey};
}

function normalizeInputModeValue<T>(value: InputModeValue<T> | T | null): AuthoredInputValue<T> {
    return normalizeAuthoredInputValue<T>(value);
}

function requiresNodeDataKey(reference: InputVariableReference): boolean {
    return sourceRequiresNodeDataKey(reference.source);
}

function sourceRequiresNodeDataKey(source: InputVariableSource): boolean {
    return source === 'ElementData' || source === 'ElementMetadata';
}

function toCustomVariablePath(reference: InputVariableReference): string {
    return requiresNodeDataKey(reference)
        ? [reference.nodeDataKey, reference.path].filter((segment) => segment != null && segment.length > 0).join('.')
        : reference.path;
}

function parseCustomVariableReference(source: InputVariableSource, path: string): InputVariableReference | null {
    const normalizedPath = path.trim().split('.').map((segment) => segment.trim()).join('.');
    if (normalizedPath.length === 0) {
        return null;
    }

    if (!sourceRequiresNodeDataKey(source)) {
        return {source, path: normalizedPath};
    }

    const separatorIndex = normalizedPath.indexOf('.');
    if (separatorIndex <= 0 || separatorIndex === normalizedPath.length - 1) {
        return null;
    }

    return {
        source,
        nodeDataKey: normalizedPath.slice(0, separatorIndex),
        path: normalizedPath.slice(separatorIndex + 1),
    };
}

function getLowCodeSummary(value: string): string | undefined {
    return value.split('\n').map((line) => line.trim()).find((line) => line.length > 0);
}
