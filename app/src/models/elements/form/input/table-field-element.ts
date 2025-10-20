import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface TableFieldComponentColumnModel {
    key: string | null | undefined;
    label: string | null | undefined;
    datatype: 'string' | 'number' | null | undefined;
    placeholder: string | null | undefined;
    decimalPlaces: number | null | undefined;
    optional: boolean | null | undefined;
    disabled: boolean | null | undefined;
}

export interface TableFieldElement extends BaseInputElement<ElementType.Table> {
    fields: TableFieldComponentColumnModel[] | null | undefined;
    maximumRows: number | null | undefined;
    minimumRequiredRows: number | null | undefined;
}
