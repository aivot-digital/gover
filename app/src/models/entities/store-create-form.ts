import {RootElement} from "../elements/root-element";

export interface StoreCreateForm {
    version: string;
    title: string;
    description: string;
    description_short: string;
    is_public: boolean;
    leika_ids: string[];
    gover_root: RootElement;
}