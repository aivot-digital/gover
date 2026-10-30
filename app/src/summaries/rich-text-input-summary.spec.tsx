import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {type RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {RichTextInputSummary} from './rich-text-input-summary';

const model: RichTextInputElement = {
    type: ElementType.RichTextInput,
    id: 'nachricht',
    name: null,
    testProtocolSet: null,
    visibility: null,
    override: null,
    metadata: null,
    weight: null,
    label: 'Ihre Nachricht',
    hint: null,
    required: null,
    disabled: null,
    technical: null,
    destinationKey: null,
    validation: null,
    value: null,
    reducedMode: null,
    maxCharacters: null,
};

function renderSummary(value: string | null) {
    render(
        <RichTextInputSummary
            model={model}
            value={value as string}
            authoredElementValues={{}}
            derivedData={createDerivedRuntimeElementData()}
        />,
    );
}

describe('RichTextInputSummary', () => {
    it('shows the answer formatted like the PDF of the submission', () => {
        renderSummary('## Anliegen\n\nBitte **prüfen** Sie:\n\n- [Antrag](https://example.org/antrag)\n- <u>Anlage</u>');

        expect(screen.getByRole('heading', {name: 'Anliegen'})).toBeInTheDocument();
        expect(screen.getByText('prüfen').tagName).toBe('STRONG');
        expect(screen.getAllByRole('listitem')).toHaveLength(2);
        // Raw HTML stays text, as in every other channel.
        expect(screen.getByText('<u>Anlage</u>')).toBeInTheDocument();
        expect(screen.queryByText(/\*\*/)).not.toBeInTheDocument();
    });

    // Answers may be shown to other people, so a link must not hide its target behind its text.
    it('shows the target of every link in the answer', () => {
        renderSummary('Bitte bestätigen Sie hier: [Zum Antrag](https://example.org/gefaelscht)\n[Datei](file:///etc/passwd)');

        expect(screen.getByText(/Zum Antrag \(https:\/\/example\.org\/gefaelscht\)/)).toBeInTheDocument();
        expect(screen.getByText(/Datei \(file:\/\/\/etc\/passwd\)/)).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it.each([null, '', ' \n '])('shows that no answer was given: %j', (value) => {
        renderSummary(value);

        expect(screen.getByText('Keine Angabe')).toBeInTheDocument();
    });
});
