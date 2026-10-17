import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {FileUploadComponent} from './file-upload-component';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

function getFileInput(label: string): HTMLInputElement {
    const labelElement = screen.getByTitle(label);
    const input = document.getElementById(labelElement.getAttribute('for')!);
    if (!(input instanceof HTMLInputElement)) {
        throw new Error(`Expected ${label} to reference a native file input.`);
    }

    return input;
}

describe('FileUploadComponent', () => {
    it('associates the external label and helper with the native file input', () => {
        const {container} = render(
            <FileUploadComponent
                label="Anlagen"
                value={null}
                onChange={vi.fn()}
                hint="Laden Sie die erforderlichen Nachweise hoch."
                isMultifile
                maxFiles={3}
                extensions={['pdf']}
            />,
        );

        const input = getFileInput('Anlagen');
        expect(input).toHaveAccessibleName('Anlagen – optional');
        expect(input).toHaveAttribute('type', 'file');
        expect(input).toHaveAccessibleDescription(
            'Laden Sie die erforderlichen Nachweise hoch. 0 von max. 3 Dateien',
        );
        expect(input).toHaveAttribute('multiple');
        expect(getComputedStyle(container.querySelector('[data-file-upload-input-area]')!).minHeight)
            .toBe('64px');
        const uploadButton = screen.getByRole('button', {name: 'Dateien auswählen'});
        expect(uploadButton).not.toHaveClass('MuiButton-outlined');
        expect(uploadButton.querySelector('svg')).not.toBeNull();
        const prompt = screen.getByText('Dateien auswählen oder hier ablegen');
        const formats = screen.getByText('Erlaubte Formate: .pdf');
        expect(getComputedStyle(prompt).fontSize).toBe('16px');
        expect(getComputedStyle(formats).lineHeight).toBe('1.2');
        expect(getComputedStyle(formats).color).not.toBe(getComputedStyle(prompt).color);
        expect(getComputedStyle(prompt.parentElement!).gap).toBe('2px');
    });

    it('reports one externally associated error message', () => {
        render(
            <FileUploadComponent
                label="Nachweis"
                value={null}
                onChange={vi.fn()}
                error="Laden Sie einen Nachweis hoch."
                required
            />,
        );

        const input = getFileInput('Nachweis');
        expect(input).toHaveAccessibleName('Nachweis');
        expect(input).toBeRequired();
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Laden Sie einen Nachweis hoch.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('passes selected files to consumers', () => {
        const onChange = vi.fn();
        const file = new File(['content'], 'antrag.pdf', {type: 'application/pdf'});

        render(
            <FileUploadComponent
                label="Anlagen"
                value={null}
                onChange={onChange}
                extensions={['pdf']}
            />,
        );

        fireEvent.change(getFileInput('Anlagen'), {
            target: {files: [file]},
        });

        expect(onChange).toHaveBeenCalledWith([file]);
    });

    it('keeps a labelled field group when the file limit hides the input', () => {
        const file = new File(['content'], 'antrag.pdf', {type: 'application/pdf'});

        render(
            <FileUploadComponent
                label="Anlage"
                value={[file]}
                onChange={vi.fn()}
                hint="Es kann genau eine Datei hinterlegt werden."
            />,
        );

        expect(document.querySelector('input[type="file"]')).not.toBeInTheDocument();
        const group = screen.getByRole('group', {name: 'Anlage – optional'});
        expect(group).toHaveAccessibleDescription('Es kann genau eine Datei hinterlegt werden.');
        expect(screen.getByRole('button', {name: 'antrag.pdf entfernen'})).toBeInTheDocument();
        expect(getComputedStyle(document.querySelector('[data-file-upload-list-item]')!).minHeight)
            .toBe('50px');
    });

    it('exposes busy uploads as disabled and busy', () => {
        render(
            <FileUploadComponent
                label="Anlagen"
                value={null}
                onChange={vi.fn()}
                busy
            />,
        );

        const input = getFileInput('Anlagen');
        expect(input).toHaveAccessibleName('Anlagen – optional');
        expect(input).toBeDisabled();
        expect(input).toHaveAttribute('aria-busy', 'true');
        expect(input.closest('[data-form-field]')).toHaveAttribute('data-busy', 'true');
    });
});
