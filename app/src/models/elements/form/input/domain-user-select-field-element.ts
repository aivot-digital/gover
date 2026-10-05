import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export type DomainAndUserSelectItemType = 'orgUnit' | 'team' | 'user';
export const DomainAndUserSelectItemTypes: DomainAndUserSelectItemType[] = ['orgUnit', 'team', 'user'];

export interface DomainAndUserSelectItem {
    type: DomainAndUserSelectItemType;
    id: string;
}

export interface DomainAndUserSelectProcessAccessConstraint {
    processId: number;
    processVersion: number;
    requiredPermissions: string[] | null | undefined;
}

export interface DomainUserSelectFieldElement extends BaseInputElement<ElementType.DomainAndUserSelect> {
    placeholder: string | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
    allowedTypes: DomainAndUserSelectItemType[] | null | undefined;
    processAccessConstraint: DomainAndUserSelectProcessAccessConstraint | null | undefined;
}
