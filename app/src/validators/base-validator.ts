import {type AnyElement} from '../models/elements/any-element';
import {CustomerInput} from "../models/customer-input";

export abstract class BaseValidator<E extends AnyElement> {
    abstract makeErrors(allElements: AnyElement[], idPrefix: string | undefined, id: string, element: E, customerInput: CustomerInput): string | null;
}
