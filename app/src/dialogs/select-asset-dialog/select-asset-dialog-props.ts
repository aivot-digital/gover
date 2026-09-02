export interface SelectAssetDialogProps {
    id?: string;
    title: string;
    show: boolean;
    mimetype?: string;
    onSelect: (assetKey: string) => void;
    onCancel: () => void;
    mode: 'public' | 'all';
}
