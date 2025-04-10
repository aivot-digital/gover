import {type ElementType} from '../../data/element-type/element-type';
import {type TestProtocolSet} from '../lib/test-protocol-set';
import {type Function as AppFunction} from '../functions/function';
import {ElementMetadata} from './element-metadata';
import {NoCodeExpression} from '../functions/no-code-expression';
import {JavascriptCode} from '../functions/javascript-code';

export interface BaseElement<T extends ElementType> {
    type: T;
    id: string;
    appVersion: string;
    name?: string;
    isVisible?: AppFunction;
    patchElement?: AppFunction;
    testProtocolSet?: TestProtocolSet;
    metadata?: ElementMetadata;

    visibilityExpression?: NoCodeExpression | null;
    visibilityCode?: JavascriptCode | null;

    overrideExpression?: NoCodeExpression | null;
    overrideCode?: JavascriptCode | null;

    visibilityReferencedIds?: string[];
    overrideReferencedIds?: string[];
}
