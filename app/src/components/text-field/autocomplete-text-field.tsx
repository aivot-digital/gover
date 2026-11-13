import {useEffect, useMemo, useRef, useState} from 'react';
import {Autocomplete, Box, ListItemText} from '@mui/material';
import {TextFieldComponent} from './text-field-component';
import {type TextFieldComponentProps} from './text-field-component-props';
import {createOptionFilter} from '../select-field/option-search';

interface TextSuggestion {
    id: string;
    label: string;
    subLabel?: string;
}

export interface AutocompleteTextFieldProps extends TextFieldComponentProps {
    suggestions: string[] | TextSuggestion[];
    disablePortal?: boolean;
}

const filterSuggestions = createOptionFilter<TextSuggestion>((option) => [option.label, option.subLabel, option.id]);

/** Text suggestions insert text; structured suggestions deliberately insert their technical id. */
export function AutocompleteTextField({suggestions, disablePortal = true, ...props}: AutocompleteTextFieldProps) {
    const [draft, setDraft] = useState(props.value ?? '');
    const draftRef = useRef(draft);
    const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastEmitted = useRef(props.value ?? null);
    const locked = Boolean(props.disabled || props.readonly || props.busy);
    const options = useMemo(() => {
        const unique = new Map<string, TextSuggestion>();
        for (const suggestion of suggestions) {
            const option = typeof suggestion === 'string'
                ? {id: suggestion.trim(), label: suggestion.trim()}
                : suggestion;
            if (option.id.length > 0 && !unique.has(option.id)) unique.set(option.id, option);
        }
        return [...unique.values()];
    }, [suggestions]);

    const cancelPending = () => {
        if (timer.current != null) clearTimeout(timer.current);
        timer.current = null;
    };
    const updateDraft = (value: string) => {
        draftRef.current = value;
        setDraft(value);
    };
    const emit = (value: string | null) => {
        if (lastEmitted.current !== value) {
            lastEmitted.current = value;
            props.onChange(value);
        }
    };

    useEffect(() => {
        cancelPending();
        lastEmitted.current = props.value ?? null;
        updateDraft(props.value ?? '');
    }, [props.value]);
    useEffect(() => {
        if (locked) cancelPending();
        return cancelPending;
    }, [locked]);

    const changeText = (value: string | null) => {
        if (locked) return;
        updateDraft(value ?? '');
        cancelPending();
        if (props.bufferInputUntilBlur) return;
        if (props.debounce) timer.current = setTimeout(() => emit(value), props.debounce);
        else emit(value);
    };

    return <Autocomplete<TextSuggestion, false, false, true>
        id={props.id}
        freeSolo
        fullWidth
        disablePortal={disablePortal}
        disabled={props.disabled}
        readOnly={props.readonly || props.busy}
        options={options}
        value={options.find((option) => option.id === props.value) ?? props.value ?? null}
        inputValue={draft}
        filterOptions={filterSuggestions}
        getOptionKey={(option) => typeof option === 'string' ? option : option.id}
        getOptionLabel={(option) => typeof option === 'string' ? option : option.label}
        isOptionEqualToValue={(option, value) => option.id === (typeof value === 'string' ? value : value.id)}
        onCompositionStart={() => {
            // The debounce lives here, while TextFieldComponent protects the browser's composition.
            if (props.normalizeInput) cancelPending();
        }}
        onInputChange={(event, value, reason) => {
            if (!locked && reason === 'input') {
                if (props.normalizeInput && (event?.nativeEvent as InputEvent)?.isComposing) cancelPending();
                updateDraft(value);
            }
        }}
        onChange={(_, option) => {
            if (locked) return;
            cancelPending();
            const value = typeof option === 'string' ? option.trim() || null : option?.id ?? null;
            updateDraft(value ?? '');
            if (!props.bufferInputUntilBlur) emit(value);
        }}
        renderOption={({key, ...optionProps}, option) => <Box component="li" key={key} {...optionProps}>
            <ListItemText primary={option.label} secondary={option.subLabel}/>
        </Box>}
        renderInput={(params) => <TextFieldComponent
            {...props}
            value={draft}
            debounce={undefined}
            bufferInputUntilBlur={false}
            onChange={changeText}
            onBlur={() => {
                if (locked) return;
                cancelPending();
                const value = draftRef.current.trim() || null;
                updateDraft(value ?? '');
                emit(value);
                props.onBlur?.(value);
            }}
            muiPassTroughProps={params}
        />}
    />;
}
