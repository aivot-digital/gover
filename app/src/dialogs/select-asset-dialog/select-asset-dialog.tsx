import {type PropsWithChildren} from 'react';
import {SelectAssetDialogProps} from './select-asset-dialog-props';
import {AssetPickerDialog} from '../asset-picker-dialog/asset-picker-dialog';

export function SelectAssetDialog(props: PropsWithChildren<SelectAssetDialogProps>) {
    const {
        id,
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
            id={id}
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
