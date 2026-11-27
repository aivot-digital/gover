import {FormLayoutElement} from '../elements/form-layout-element';

export interface MarketplaceCreateForm {
    version: string;
    title: string;
    description: string;
    description_short: string;
    is_public: boolean;
    leika_ids: string[];
    prosuna_root: FormLayoutElement;
}