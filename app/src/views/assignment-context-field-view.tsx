import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    AssignmentContextFieldElement,
    AssignmentContextValue,
} from '../models/elements/form/input/assignment-context-field-element';
import {AssignmentContextFieldComponent} from '../components/assignment-context-field/assignment-context-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function AssignmentContextFieldView(props: BaseViewProps<AssignmentContextFieldElement, AssignmentContextValue>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <AssignmentContextFieldComponent
            value={value ?? undefined}
            onChange={setValue}
            title={element.headline ?? undefined}
            description={element.text ?? undefined}
            domainAndUserSelectionLabel={element.label ?? ''}
            domainAndUserSelectionPlaceholder={element.placeholder ?? undefined}
            domainAndUserSelectionHint={element.hint ?? undefined}
            domainAndUserSelectionError={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            allowedTypes={element.allowedTypes ?? undefined}
            processAccessConstraint={element.processAccessConstraint ?? undefined}
        />
    );
}
