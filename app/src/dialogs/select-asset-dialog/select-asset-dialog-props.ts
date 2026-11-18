import {type AssetVisibility} from '../../modules/assets/models/asset-visibility';

export interface SelectAssetDialogProps {
    id?: string;
    storageProviderId?: number | null;
    title: string;
    show: boolean;
    mimetype?: string | string[];
    onSelect: (assetKey: string) => void;
    onCancel: () => void;
    visibility: AssetVisibility;
}
