import {type AnyElement} from '../models/elements/any-element';

export abstract class BaseValidator<M extends AnyElement> {
    abstract makeErrors(allElements: AnyElement[], idPrefix: string | undefined, id: string, comp: M, userInput: any): string | null;
}
