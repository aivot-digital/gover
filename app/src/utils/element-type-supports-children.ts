import {ElementType} from '../data/element-type/element-type';
import {ElementChildOptions} from '../data/element-type/element-child-options';

/**
 * @deprecated
 */
export function elementTypeSupportsChildren(type: ElementType) {
    return ElementChildOptions[type] != null;
}

