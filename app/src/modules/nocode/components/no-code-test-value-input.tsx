import {useState} from 'react';
import {visuallyHidden} from '@mui/utils';
import {Box, Button, Menu, MenuItem, Tooltip, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import MoreHoriz from '@aivot/mui-material-symbols-400-n25-outlined/MoreHoriz';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {NoCodeTestJsonInput} from './no-code-test-json-input';
import {NoCodeDataType as T, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {AutocompleteTextField} from '../../../components/text-field/autocomplete-text-field';
import {MultiCheckboxComponent} from '../../../components/multi-checkbox-field/multi-checkbox-component';
import {NO_CODE_BOOLEAN_OPTIONS} from '../utils/no-code-options';
import {
    NoCodeTestField,
    NoCodeTestValueDraft,
    NoCodeTestValueMode,
    parseTestDraft,
    testDraftFromValue,
} from '../utils/no-code-test-data';

export const NO_CODE_TEST_FIELDS_SX = {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
    gap: 1.5,
    alignItems: 'start',
    '@container no-code-test (max-width: 719px)': {gridTemplateColumns: 'minmax(0, 1fr)'},
};

interface Props {
    field: NoCodeTestField;
    disabled?: boolean;
    value: NoCodeTestValueDraft;
    onChange: (value: NoCodeTestValueDraft) => void;
    showError: boolean;
    onBlur: () => void;
}

const HINTS: Partial<Record<T, string>> = {
    [T.Number]: 'Zum Beispiel 42 oder 3,5.',
    [T.Date]: 'Im Format JJJJ-MM-TT, zum Beispiel 2026-11-19.',
    [T.Time]: 'Im Format HH:MM, zum Beispiel 09:30.',
    [T.DateTime]: 'Mit Zeitzone, zum Beispiel 2026-11-19T09:30:00+01:00.',
    [T.List]: 'Als JSON-Liste, zum Beispiel ["a", "b"].',
    [T.Object]: 'Als JSON-Objekt, zum Beispiel {"name": "Müller"}.',
};

export function NoCodeTestValueInput({
                                         field,
                                         disabled,
                                         value,
                                         onChange,
                                         showError,
                                         onBlur,
                                     }: Props) {
    const [toolbarContainer, setToolbarContainer] = useState<HTMLElement | null>(null);
    const inputId = useNormalizedReactId();
    const referenceId = `${inputId}-reference`;
    const [menu, setMenu] = useState<{ kind: 'type' | 'mode'; anchor: HTMLElement; open: boolean } | null>(null);
    const closeMenu = () => setMenu(current => current == null ? null : {
        ...current,
        open: false,
    });
    const parsed = parseTestDraft(value);
    const json = value.mode === 'json';
    const multiple = value.type === T.List && field.options.length > 0 && !json;
    const usesJson = json || value.mode === 'value' && (value.type === T.Object || value.type === T.List && !multiple);
    const options = [...field.options];
    // Keep JSON-authored selections visible even when they are absent from today's option list.
    if (multiple && Array.isArray(parsed.value)) for (const item of parsed.value) {
        if (typeof item === 'string' && !options.some(option => option.value === item)) options.push({
            value: item,
            label: item,
        });
    }
    const common = {
        disabled,
        id: inputId,
        label: '',
        ariaLabel: `Testwert für „${field.label}“`,
        ariaDescribedBy: field.reference ? referenceId : undefined,
        margin: 'none' as const,
        value: value.text,
        showOptionalIndicator: false,
        hint: [field.hint, json ? undefined : HINTS[value.type]].filter(Boolean).join(' '),
        error: showError ? parsed.error : undefined,
        onBlur,
        onChange: (text: string | null) => onChange({
            ...value,
            text: text ?? '',
        }),
    };
    const switchMode = (mode: NoCodeTestValueMode) => {
        if (mode === 'json') {
            onChange({
                ...value,
                mode,
                type: field.type === T.Runtime ? T.Runtime : value.type,
                text: parsed.error ? value.text : parsed.value === undefined ? '' : JSON.stringify(parsed.value, null, 2),
            });
        } else if (value.mode === 'json' && mode === 'value' && !parsed.error) {
            const next = testDraftFromValue(parsed.value, field.type);
            onChange({
                ...next,
                mode: next.mode === 'json' ? 'value' : next.mode,
            });
        } else if (value.mode === 'json' && mode === 'value' && parsed.error) {
            onBlur();
            return;
        } else if (mode === 'missing' || mode === 'null' || mode === 'empty') {
            onChange({
                text: '',
                mode,
                type: value.type === T.Runtime ? T.String : value.type,
            });
        } else onChange({
            ...value,
            mode,
        });
    };
    const types: Partial<Record<T, string>> = {
        [T.String]: 'Text',
        [T.Number]: 'Zahl',
        [T.Boolean]: 'Wahrheitswert',
        [T.Date]: 'Datum',
        [T.Time]: 'Uhrzeit',
        [T.DateTime]: 'Datum und Uhrzeit',
        [T.List]: 'Liste',
        [T.Object]: 'Objekt',
    };
    const modes: { value: NoCodeTestValueMode; label: string }[] = [
        {
            value: 'value',
            label: 'Wert eingeben',
        }, {
            value: 'missing',
            label: 'Kein Wert (nicht vorhanden)',
        },
        ...(field.type === T.Runtime || field.type === T.String ? [{
            value: 'empty' as const,
            label: 'Leerer Text',
        }] : []),
        {
            value: 'null',
            label: 'Leerer Wert (null)',
        }, {
            value: 'json',
            label: 'JSON',
        },
    ];
    return <Box sx={{
        minWidth: 0,
        p: 1.5,
        border: 1,
        borderColor: 'divider',
        borderRadius: 1,
    }}>
        <Box sx={{
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 1,
            mb: 1,
        }}>
            <Box sx={{
                flex: '1 1 180px',
                minWidth: 0,
            }}>
                <Typography id={field.reference === field.label ? referenceId : undefined}
                            component="label"
                            htmlFor={inputId}
                            onClick={() => document.getElementById(inputId)?.focus()}
                            variant="body2"
                            sx={{
                                fontWeight: field.label === field.reference ? 400 : 600,
                                color: field.label === field.reference ? 'text.secondary' : 'text.primary',
                                overflowWrap: 'anywhere',
                            }}>{field.label}</Typography>
                {field.reference && field.reference !== field.label && <Typography id={referenceId}
                                                                                   variant="caption"
                                                                                   color="text.secondary"
                                                                                   component="div"
                                                                                   sx={{
                                                                                       fontFamily: 'monospace',
                                                                                       color: 'text.secondary',
                                                                                       fontWeight: 400,
                                                                                       overflowWrap: 'anywhere',
                                                                                   }}>{field.reference}</Typography>}
            </Box>
            {value.mode === 'value' && (field.type === T.Runtime
                ? <Tooltip arrow
                           title="Der Datentyp ist nicht vorgegeben. Wählen Sie den Typ Ihres Testwerts.">
                    <span><Button size="small"
                                  color="inherit"
                                  disabled={disabled}
                                  endIcon={<KeyboardArrowDown/>}
                                  aria-label={`Datentyp für „${field.label}“: ${NoCodeDataTypeLabels[value.type]}`}
                                  aria-haspopup="menu"
                                  aria-expanded={menu?.kind === 'type' && menu.open || undefined}
                                  onClick={event => setMenu({
                                      kind: 'type',
                                      anchor: event.currentTarget,
                                      open: true,
                                  })}>
                        {types[value.type] ?? NoCodeDataTypeLabels[value.type]}
                    </Button></span>
                </Tooltip>
                : <Typography variant="caption"
                              color="text.secondary">{types[value.type] ?? NoCodeDataTypeLabels[value.type]}</Typography>)}
            {json && <Typography variant="caption"
                                 color="text.secondary">JSON</Typography>}
            {usesJson && <Box ref={setToolbarContainer}
                              sx={{display: 'inline-flex'}}/>}
            <Button size="small"
                    color="inherit"
                    disabled={disabled}
                    endIcon={<MoreHoriz/>}
                    aria-label={`Wertoptionen für „${field.label}“`}
                    aria-haspopup="menu"
                    aria-expanded={menu?.kind === 'mode' && menu.open || undefined}
                    onClick={event => setMenu({
                        kind: 'mode',
                        anchor: event.currentTarget,
                        open: true,
                    })}>Wertoptionen</Button>
        </Box>
        <Menu open={menu?.open ?? false}
              anchorEl={menu?.anchor}
              onClose={closeMenu}
              slotProps={{
                  list: {'aria-label': `${menu?.kind === 'type' ? 'Datentyp' : 'Wertoptionen'} für „${field.label}“`},
                  transition: {onExited: () => setMenu(null)},
              }}>
            {menu?.kind === 'type'
                ? Object.values(T).filter(type => type !== T.Runtime).map(type => <MenuItem key={type}
                                                                                            role="menuitemradio"
                                                                                            aria-checked={value.type === type}
                                                                                            selected={value.type === type}
                                                                                            onClick={() => {
                                                                                                onChange({
                                                                                                    ...value,
                                                                                                    type,
                                                                                                });
                                                                                                closeMenu();
                                                                                            }}>{NoCodeDataTypeLabels[type]}</MenuItem>)
                : modes.map(mode => <MenuItem key={mode.value}
                                              role="menuitemradio"
                                              aria-checked={value.mode === mode.value}
                                              selected={value.mode === mode.value}
                                              onClick={() => {
                                                  switchMode(mode.value);
                                                  closeMenu();
                                              }}>{mode.label}</MenuItem>)}
        </Menu>
        {value.mode === 'null' && <Typography variant="body2"
                                              color="text.secondary">Der Wert ist vorhanden und ausdrücklich leer
                                                                     (null).</Typography>}
        {value.mode === 'missing' && <Typography variant="body2"
                                                 color="text.secondary">Für diesen Verweis wird kein Testwert
                                                                        bereitgestellt.</Typography>}
        {value.mode === 'empty' && <Typography variant="body2"
                                               color="text.secondary">Es wird ein Text ohne Zeichen
                                                                      bereitgestellt.</Typography>}
        {usesJson
            ? <NoCodeTestJsonInput toolbarContainer={toolbarContainer}
                                   id={inputId}
                                   ariaDescribedBy={common.ariaDescribedBy}
                                   ariaLabel={common.ariaLabel}
                                   disabled={disabled}
                                   value={value.text}
                                   error={common.error}
                                   hint={field.hint}
                                   onBlur={onBlur}
                                   onChange={text => onChange({
                                       ...value,
                                       text,
                                   })}/>
            : value.mode === 'value' && (
            multiple && (!Array.isArray(parsed.value) || parsed.value.every(item => typeof item === 'string'))
                ? <MultiCheckboxComponent id={inputId}
                                          ariaDescribedBy={common.ariaDescribedBy}
                                          sx={{'& > legend': visuallyHidden}}
                                          disabled={disabled}
                                          label={field.label}
                                          margin="none"
                                          showOptionalIndicator={false}
                                          options={options}
                                          value={Array.isArray(parsed.value) ? parsed.value : []}
                                          hint={field.hint}
                                          error={common.error}
                                          onChange={(selected) => {
                                              const next = selected ?? [];
                                              // A test list can be ordered; toggling a checkbox must not reorder its existing entries.
                                              const previous: string[] = Array.isArray(parsed.value) ? parsed.value : [];
                                              onChange({
                                                  ...value,
                                                  text: JSON.stringify([...previous.filter(item => next.includes(item)), ...next.filter(item => !previous.includes(item))]),
                                              });
                                          }}/>

                : value.type === T.Boolean && !json
                    ? <SelectFieldComponent {...common} value={value.text}
                                            options={NO_CODE_BOOLEAN_OPTIONS}
                                            emptyOptionLabel="Kein Wert"/>
                    : value.type === T.String && options.length > 0 && !json
                        ? <AutocompleteTextField {...common} suggestions={options.map(option => ({
                            id: option.value,
                            label: option.label,
                            subLabel: option.value,
                        }))}/>
                        :
                        <TextFieldComponent {...common} multiline={json || value.type === T.List || value.type === T.Object}
                                            rows={json || value.type === T.List || value.type === T.Object ? 3 : undefined}
                                            muiPassTroughProps={{slotProps: {htmlInput: {inputMode: value.type === T.Number && !json ? 'decimal' : 'text'}}}}/>
        )}
    </Box>;
}
