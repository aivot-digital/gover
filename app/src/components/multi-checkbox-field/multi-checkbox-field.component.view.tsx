import {Box} from '@mui/material';
import {MultiChoiceCombobox} from './multi-choice-combobox';
import {MultiChoicePresentation} from '../../models/elements/form/input/multi-choice-presentation';
import React, {useMemo} from 'react';
import {type MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseViewProps} from '../../views/base-view';
import {MultiCheckboxComponent} from './multi-checkbox-component';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';

export function MultiCheckboxFieldComponentView(props: BaseViewProps<MultiCheckboxFieldElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled,
        options,
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const Component = element.presentation === MultiChoicePresentation.Combobox ? MultiChoiceCombobox : MultiCheckboxComponent;

    const count = value?.length ?? 0;
    const minimum = element.minimumRequiredOptions ?? 0;
    const remaining = minimum - count;
    const minimumHint = remaining > 0
        ? count === 0
            ? `${element.required ? 'Mindestens' : 'Bei Auswahl mindestens'} ${minimum} ${minimum === 1 ? 'Option' : 'Optionen'}`
            : `Noch mindestens ${remaining} ${remaining === 1 ? 'Option' : 'Optionen'}`
        : undefined;
    const error = errors?.filter(Boolean).join(' ');
    const message = error || element.hint;
    const helperContent = minimumHint ? (
        <Box sx={{display: 'flex', justifyContent: 'space-between', flexWrap: {xs: 'wrap', sm: 'nowrap'}, columnGap: 2, rowGap: 0.5, width: '100%'}}>
            {message && <><Box component="span" sx={{minWidth: 0}}>{message}</Box>{' '}</>}
            <Box component="span" sx={{ml: 'auto', maxWidth: '100%'}}>{minimumHint}</Box>
        </Box>
    ) : message;

    return (
        <Component
            label={element.label ?? ''}
            value={value}
            onChange={setValue}
            options={options ?? []}
            disabled={isDisabled}
            required={element.required ?? undefined}
            error={error ? helperContent : undefined}
            hint={!error ? helperContent : undefined}
            displayInline={element.displayInline ?? undefined}
            busy={isBusy}
        />
    );
}
