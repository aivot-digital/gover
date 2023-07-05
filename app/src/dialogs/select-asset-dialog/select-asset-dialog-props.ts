export interface SelectAssetDialogProps {
    title: string;
    show: boolean;

    onSelect: (assetName: string) => void;
    onCancel: () => void;
}