export interface SelectAssetDialogProps {
    title: string;
    show: boolean;
    mimetype?: string;
    onSelect: (assetKey: string) => void;
    onCancel: () => void;
}