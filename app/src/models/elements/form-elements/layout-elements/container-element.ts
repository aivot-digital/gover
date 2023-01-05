import {ElementType} from '../../../../data/element-type/element-type';
import {BaseLayoutElement} from './base-layout-element';
import {AnyContentElement} from '../content-elements/any-content-element';

export interface ContainerElement extends BaseLayoutElement<AnyContentElement, ElementType.Container> {
}
