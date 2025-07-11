import {type ElementType} from '../../data/element-type/element-type';
import {type TestProtocolSet} from '../lib/test-protocol-set';
import {ElementMetadata} from './element-metadata';
import {ElementOverrideFunction} from './element-override-function';
import {ElementVisibilityFunction} from './element-visibility-function';

export interface BaseElement<T extends ElementType> {
    type: T;
    id: string;

    name: string | null | undefined;

    testProtocolSet: TestProtocolSet | null | undefined;

    visibility: ElementVisibilityFunction | null | undefined;
    override: ElementOverrideFunction | null | undefined;

    metadata: ElementMetadata | null | undefined;
}