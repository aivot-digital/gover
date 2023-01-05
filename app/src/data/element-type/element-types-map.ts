import {ElementType} from './element-type';

export type ElementTypesMap<T> = {
    [Key in ElementType]: T;
}
