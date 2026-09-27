import {type AssetVisibility} from '../../modules/assets/models/asset-visibility';

export interface SelectAssetDialogProps {
    id?: string;
    title: string;
    show: boolean;
    mimetype?: string | string[];
    onSelect: (assetKey: string) => void;
    onCancel: () => void;
    visibility: AssetVisibility;
}
