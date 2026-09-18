import {type BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AssetVisibility} from '../../../../modules/assets/models/asset-visibility';

export interface AssetSelectInputElement extends BaseInputElement<ElementType.AssetSelectInput> {
    placeholder: string | null | undefined;
    dialogTitle: string | null | undefined;
    allowedMimeTypes: string[] | null | undefined;
    assetVisibility: AssetVisibility | null | undefined;
}
