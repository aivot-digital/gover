import {AnyElement} from '../models/elements/any-element';

export abstract class BaseValidator<M extends AnyElement> {
    abstract makeErrors(id: string, comp: M, userInput: any): string | null;
}
