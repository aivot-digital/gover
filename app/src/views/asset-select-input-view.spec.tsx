import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssetSelectInputView} from './asset-select-input-view';
import {ElementType} from '../data/element-type/element-type';
import {AssetVisibility} from '../modules/assets/models/asset-visibility';

const {assetSelectorSpy} = vi.hoisted(() => ({
    assetSelectorSpy: vi.fn(),
}));

vi.mock('../modules/assets/components/asset-selector', () => ({
    AssetSelector: (props: {label: string}) => {
        assetSelectorSpy(props);
        return <button type="button">{props.label}</button>;
    },
}));

describe('AssetSelectInputView', () => {
    it('renders one asset picker with the configured MIME types and visibility', () => {
        const setValue = vi.fn();

        render(
            <AssetSelectInputView
                {...({
                    element: {
                        id: 'clientCertificate',
                        type: ElementType.AssetSelectInput,
                        weight: 12,
                        label: 'Client-Zertifikat',
                        dialogTitle: 'Client-Zertifikat auswählen',
                        placeholder: 'PEM-Datei auswählen',
                        allowedMimeTypes: ['application/x-pem-file'],
                        assetVisibility: AssetVisibility.Private,
                        required: true,
                        disabled: false,
                    },
                    value: null,
                    setValue,
                    errors: [],
                    isBusy: false,
                    isDeriving: false,
                } as any)}
            />,
        );

        expect(screen.getAllByRole('button')).toHaveLength(1);
        expect(assetSelectorSpy).toHaveBeenCalledWith(expect.objectContaining({
            label: 'Client-Zertifikat',
            selectLabel: 'Client-Zertifikat auswählen',
            placeholder: 'PEM-Datei auswählen',
            mimetype: ['application/x-pem-file'],
            visibility: AssetVisibility.Private,
            required: true,
            onChange: setValue,
        }));
    });
});
