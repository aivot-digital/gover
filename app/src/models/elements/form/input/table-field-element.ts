import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface TableFieldComponentColumnModel {
    label: string;
    datatype: 'string' | 'number';
    placeholder?: string;
    decimalPlaces?: number;
    optional?: boolean;
    disabled?: boolean;
}

export interface TableFieldElement extends BaseInputElement<{[key: string]: string}[], ElementType.Table> {
    fields?: TableFieldComponentColumnModel[];
    maximumRows?: number;
    minimumRequiredRows?: number;
}
