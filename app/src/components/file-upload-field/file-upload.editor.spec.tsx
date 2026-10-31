import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {FileUploadEditor} from './file-upload.editor';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../modules/system/system-api-service', () => ({
    SystemApiService: class {
        getFileExtensions = vi.fn().mockResolvedValue(['pdf']);
    },
}));

describe('FileUploadEditor labels', () => {
    it.each([
        {label: 'Anlage', isMultifile: false, nextLabel: 'Anlagen'},
        {label: 'Anlagen', isMultifile: true, nextLabel: 'Anlage'},
    ])('switches the default label from $label to $nextLabel', async ({label, isMultifile, nextLabel}) => {
        const user = userEvent.setup();
        const onPatch = vi.fn();
        render(<FileUploadEditor
            element={{...generateElementWithDefaultValues(ElementType.FileUpload), label, isMultifile}}
            onPatch={onPatch} editable hasSummaryLayoutParent={false} scope="application"
        />);

        expect(onPatch).not.toHaveBeenCalled();
        await user.click(screen.getByRole('checkbox', {name: 'Mehrere Anlagen zulässig'}));

        expect(onPatch).toHaveBeenCalledWith({
            isMultifile: !isMultifile,
            minFiles: undefined,
            maxFiles: undefined,
            label: nextLabel,
        });
    });

    it.each(['Nachweise zur Spende', 'Anlage(n)'])('preserves the existing label %s', async label => {
        const user = userEvent.setup();
        const onPatch = vi.fn();
        render(<FileUploadEditor
            element={{...generateElementWithDefaultValues(ElementType.FileUpload), label}}
            onPatch={onPatch} editable hasSummaryLayoutParent={false} scope="application"
        />);

        expect(onPatch).not.toHaveBeenCalled();
        await user.click(screen.getByRole('checkbox', {name: 'Mehrere Anlagen zulässig'}));

        expect(onPatch).toHaveBeenCalledWith({isMultifile: true, minFiles: undefined, maxFiles: undefined});
    });
});
