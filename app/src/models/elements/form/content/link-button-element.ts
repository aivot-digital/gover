import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export type LinkButtonElementVariant = 'contained' | 'text' | 'outlined';

export type LinkButtonElementColor = 'primary' | 'secondary';

export interface LinkButtonElement extends BaseFormElement<ElementType.LinkButton> {
    label: string | null | undefined;
    href: string | null | undefined;
    openInNewTab: boolean | null | undefined;
    staffTaskEvent: string | null | undefined;
    customerTaskEvent: string | null | undefined;
    variant: LinkButtonElementVariant | null | undefined;
    color: LinkButtonElementColor | null | undefined;
}
