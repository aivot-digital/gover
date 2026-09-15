import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {
    DomainAndUserSelectItem,
    DomainAndUserSelectItemType,
    DomainAndUserSelectProcessAccessConstraint,
} from './domain-user-select-field-element';

export interface AssignmentContextValue {
    domainAndUserSelection: DomainAndUserSelectItem[] | null | undefined;
    generalAssigneePreference: GeneralAssigneePreference | null | undefined;
    repeatExecutionAssigneePreference: RepeatExecutionAssigneePreference | null | undefined;
}

export type GeneralAssigneePreference =
    | 'none'
    | 'previousProcessStepAssignee'
    | 'uninvolvedUser'
    | 'processInstanceAssignee';

export type RepeatExecutionAssigneePreference =
    | 'none'
    | 'previousIterationAssignee'
    | 'differentFromPreviousIterationAssignee';

export interface AssignmentContextFieldElement extends BaseInputElement<ElementType.AssignmentContext> {
    headline: string | null | undefined;
    text: string | null | undefined;
    placeholder: string | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
    allowedTypes: DomainAndUserSelectItemType[] | null | undefined;
    processAccessConstraint: DomainAndUserSelectProcessAccessConstraint | null | undefined;
}
