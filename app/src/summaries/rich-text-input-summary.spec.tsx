import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
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

/** jsdom does not lay out content, so the collapsed answer reports the given heights. */
function mockAnswerHeight(scrollHeight: number, clientHeight: number) {
    vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(scrollHeight);
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(clientHeight);
}

afterEach(() => {
    vi.restoreAllMocks();
});

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

        expect(screen.getByRole('link', {name: 'https://example.org/gefaelscht'})).toHaveAttribute('href', 'https://example.org/gefaelscht');
        expect(screen.queryByRole('link', {name: 'Zum Antrag'})).not.toBeInTheDocument();
        expect(screen.getByText(/Datei \(file:\/\/\/etc\/passwd\)/)).toBeInTheDocument();
        expect(screen.getAllByRole('link')).toHaveLength(1);
    });

    it('keeps leading spaces that change the formatting, as in the PDF of the submission', () => {
        renderSummary('    **wörtlich**');

        expect(screen.getByText('**wörtlich**').closest('pre')).toBeInTheDocument();
        expect(screen.queryByText('wörtlich')).not.toBeInTheDocument();
    });

    it('collapses long answers and shows them completely on request', () => {
        mockAnswerHeight(300, 100);
        renderSummary('Erster Absatz\n\nZweiter Absatz');

        const toggle = screen.getByRole('button', {name: 'Text vollständig anzeigen'});
        const answer = document.getElementById(toggle.getAttribute('aria-controls')!);
        expect(answer).toHaveTextContent('Erster Absatz');
        expect(toggle).toHaveAttribute('aria-expanded', 'false');

        fireEvent.click(toggle);

        expect(screen.getByRole('button', {name: 'Weniger anzeigen'})).toHaveAttribute('aria-expanded', 'true');
    });

    it('shows short answers without a toggle', () => {
        mockAnswerHeight(100, 100);
        renderSummary('Kurze Antwort');

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('expands the answer when a hidden link receives keyboard focus', () => {
        mockAnswerHeight(300, 100);
        renderSummary('Erster Absatz\n\nSiehe https://example.org');
        const toggle = screen.getByRole('button', {name: 'Text vollständig anzeigen'});
        const answer = document.getElementById(toggle.getAttribute('aria-controls')!)!;
        const link = screen.getByRole('link', {name: 'https://example.org'});
        vi.spyOn(answer, 'getBoundingClientRect').mockReturnValue(new DOMRect(0, 0, 400, 100));
        vi.spyOn(link, 'getBoundingClientRect').mockReturnValue(new DOMRect(0, 200, 100, 20));

        fireEvent.focus(link);

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
    });

    it.each([null, '', ' \n '])('shows that no answer was given: %j', (value) => {
        renderSummary(value);

        expect(screen.getByText('Keine Angabe')).toBeInTheDocument();
    });
});
