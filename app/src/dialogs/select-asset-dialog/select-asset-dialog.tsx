import {type PropsWithChildren} from 'react';
import {SelectAssetDialogProps} from './select-asset-dialog-props';
import {AssetPickerDialog} from '../asset-picker-dialog/asset-picker-dialog';

export function SelectAssetDialog(props: PropsWithChildren<SelectAssetDialogProps>) {
    const {
        title,
        show,
        mimetype,
        mode,
        onCancel,
        onSelect,
        children,
    } = props;

    return (
        <AssetPickerDialog
            title={title}
            show={show}
            mimeType={mimetype}
            mode={mode}
            onCancel={onCancel}
            onSelectAsset={(assetKey) => {
                onSelect(assetKey);
            }}
        >
            {children}
        </AssetPickerDialog>
    );
}
