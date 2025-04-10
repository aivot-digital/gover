import {type ElementType} from './element-type';

/**
 * @deprecated Use Record<ElementType, T> instead
 */
export type ElementTypesMap<T> = Record<ElementType, T>;
