import {AnyStepElement} from './step-elements/any-step-element';
import {RootElement} from './root-element';
import {AnyFormElement} from './form-elements/any-form-element';

export type AnyElement = (
    AnyFormElement |
    AnyStepElement |
    RootElement
    );
