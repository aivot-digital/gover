import {ElementType} from '../../../data/element-type/element-type';
import {getLiteralElementValue, type AuthoredElementValues, isDerivedRuntimeElementData} from '../../../models/element-data';
import type {SelectFieldElement} from '../../../models/elements/form/input/select-field-element';
import type {TaskView} from '../services/process-instance-task-api-service';

export const MANUAL_ASSIGNMENT_PROVIDER = 'de.aivot.core.assign_instance';
export const RECIPIENT_FIELD = 'assignedUserId';

export function getSelectedRecipient(values: AuthoredElementValues): string | undefined {
    const value = getLiteralElementValue(values, RECIPIENT_FIELD);
    return typeof value === 'string' && value !== '' ? value : undefined;
}

export function getRecipientOptions(view: TaskView) {
    const field = view.layout.children.find((element): element is SelectFieldElement =>
        element.id === RECIPIENT_FIELD && element.type === ElementType.Select);
    return field?.options ?? [];
}

export function hasRecipientOption(view: TaskView, recipient: string): boolean {
    return getRecipientOptions(view).some(option => (typeof option === 'string' ? option : option.value) === recipient);
}

export function getRecipientLabel(view: TaskView, recipient: string): string | undefined {
    const option = getRecipientOptions(view).find(option => typeof option !== 'string' && option.value === recipient);
    return typeof option === 'object' && option.label !== recipient ? option.label : undefined;
}

export function removeRecipientOption(view: TaskView, recipient: string): TaskView {
    const options = getRecipientOptions(view).filter(option => (typeof option === 'string' ? option : option.value) !== recipient);
    return {
        ...view,
        events: options.length === 0 ? view.events.filter(event => event.event !== 'assign') : view.events,
        layout: {
            ...view.layout,
            children: view.layout.children.map(element => element.id === RECIPIENT_FIELD && element.type === ElementType.Select
                ? {...element, options, disabled: options.length === 0 || element.disabled}
                : element),
        },
    };
}

export function getRejectedRecipient(details: unknown): string | undefined {
    const marker = isDerivedRuntimeElementData(details)
        ? details.elementStates[RECIPIENT_FIELD]?.errorDetails
        : details;
    if (marker != null && typeof marker === 'object' &&
        'code' in marker && marker.code === 'manual_assignment_recipient_unavailable' &&
        'fieldId' in marker && marker.fieldId === RECIPIENT_FIELD &&
        'rejectedValue' in marker && typeof marker.rejectedValue === 'string') {
        return marker.rejectedValue;
    }
}

export function clearRecipient(values: AuthoredElementValues): AuthoredElementValues {
    const updated = {...values};
    delete updated[RECIPIENT_FIELD];
    return updated;
}

export function unavailableRecipientMessage(name: string | undefined, hasAlternatives: boolean): string {
    const subject = name || 'Die ausgewählte Person';
    return `${subject} kann diesem Vorgang nicht mehr zugewiesen werden. ${hasAlternatives
        ? 'Bitte wählen Sie eine andere Person aus.'
        : 'Derzeit steht keine andere Person zur Auswahl.'}`;
}
