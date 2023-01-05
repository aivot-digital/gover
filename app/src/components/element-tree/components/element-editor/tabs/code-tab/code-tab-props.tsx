import {AnyElement} from '../../../../../../models/elements/any-element';

export interface CodeTabProps<T extends AnyElement> {
    element: T;
    field: keyof T & ('visibility' | 'validate' | 'patch');
    onChange: (data: T) => void;
}
