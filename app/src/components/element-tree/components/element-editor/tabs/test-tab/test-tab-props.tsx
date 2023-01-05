import {AnyElement} from '../../../../../../models/elements/any-element';

export interface TestTabProps<T extends AnyElement> {
    elementModel: T;
    onPatch: (data: Pick<T, 'technicalTest' | 'professionalTest'>) => void;
}
