import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {TextFieldEditor} from './text-field-editor';
import {ChipInputFieldEditor} from './chip-input-field-editor';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';

vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../modules/code-lists/code-lists-api-service', () => ({
    CodeListsApiService: class {
        async listAllOrdered() {
            return {content: [{key: 'places', name: 'Orte'}]};
        }
    },
}));

function SourceSwitchEditor({type, initialSuggestions}: {
    type: ElementType.Text | ElementType.ChipInput;
    initialSuggestions?: string[];
}) {
    const [optionsSource, setSource] = useState(OptionsSourceType.CodeList);
    const [suggestions, setSuggestions] = useState(initialSuggestions);
    const props = {
        editable: true, hasSummaryLayoutParent: false, scope: 'application' as const,
        onPatch: (patch: {optionsSource?: OptionsSourceType | null; suggestions?: string[] | null}) => {
            if (patch.optionsSource != null) setSource(patch.optionsSource);
            if ('suggestions' in patch) setSuggestions(patch.suggestions ?? []);
        },
    };
    const sourceProps = {optionsSource, suggestions, codeListKey: 'places'};
    return type === ElementType.Text
        ? <TextFieldEditor {...props} element={{...generateElementWithDefaultValues(ElementType.Text), ...sourceProps}}/>
        : <ChipInputFieldEditor {...props} element={{...generateElementWithDefaultValues(ElementType.ChipInput), ...sourceProps}}/>;
}

function renderEditor(type: ElementType.Text | ElementType.ChipInput, editable = true) {
    const onPatch = vi.fn();
    const props = {editable, onPatch, hasSummaryLayoutParent: false, scope: 'application' as const};
    const suggestions = ['Vorhanden'];
    render(type === ElementType.Text
        ? <TextFieldEditor {...props} element={{...generateElementWithDefaultValues(ElementType.Text), suggestions}}/>
        : <ChipInputFieldEditor {...props} element={{...generateElementWithDefaultValues(ElementType.ChipInput),
            suggestions, maxItems: 1, maxItemLength: 3, pasteSeparators: [',']}}/>);
    return {onPatch, input: screen.getByRole('combobox', {name: 'Vorschläge – optional'})};
}

describe.each([ElementType.Text, ElementType.ChipInput] as const)('suggestion editing for type %s', (type) => {
    it.each([
        {initialSuggestions: undefined},
        {initialSuggestions: []},
        {initialSuggestions: ['', '  ']},
    ])('keeps manual suggestions empty after switching from a codelist ($initialSuggestions)', async ({initialSuggestions}) => {
        const user = userEvent.setup();
        render(<SourceSwitchEditor type={type} initialSuggestions={initialSuggestions}/>);
        expect(await screen.findByText('Orte')).toBeVisible();
        await user.click(screen.getByRole('combobox', {name: 'Vorschläge definieren über'}));
        await user.click(screen.getByRole('option', {name: 'Manuelle Eingabe'}));
        expect(screen.getByRole('combobox', {name: 'Vorschläge – optional'})).toHaveValue('');
        expect(screen.queryAllByRole('button', {name: ''})).toHaveLength(0);
    });

    it('retains nonempty suggestions across source changes and saves only actual entries', async () => {
        const user = userEvent.setup();
        render(<SourceSwitchEditor type={type} initialSuggestions={['', 'Vorhanden', ' ']}/>);
        await screen.findByText('Orte');
        await user.click(screen.getByRole('combobox', {name: 'Vorschläge definieren über'}));
        await user.click(screen.getByRole('option', {name: 'Manuelle Eingabe'}));
        expect(screen.getByRole('button', {name: 'Vorhanden'})).toBeVisible();
        expect(screen.queryAllByRole('button', {name: ''})).toHaveLength(0);
        const input = screen.getByRole('combobox', {name: 'Vorschläge – optional'});
        await user.click(input);
        await user.keyboard('{Backspace}');
        expect(screen.queryByRole('button', {name: 'Vorhanden'})).not.toBeInTheDocument();
        expect(screen.queryAllByRole('button', {name: ''})).toHaveLength(0);
    });

    it('stores pasted lines as plain strings independently of the configured input rules', () => {
        const {onPatch, input} = renderEditor(type);
        expect(onPatch).not.toHaveBeenCalled();
        fireEvent.paste(input, {clipboardData: {getData: () => ' Bildung, Kultur \n Wohnen '}});
        expect(onPatch).toHaveBeenCalledExactlyOnceWith({suggestions: ['Vorhanden', 'Bildung, Kultur', 'Wohnen']});
    });

    it('persists an empty list when the last suggestion is removed', () => {
        const {onPatch, input} = renderEditor(type);
        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: 'Backspace'});
        expect(onPatch).toHaveBeenCalledExactlyOnceWith({suggestions: []});
    });

    it('prevents suggestion changes when the editor is disabled', () => {
        const {onPatch, input} = renderEditor(type, false);
        expect(input).toBeDisabled();
        fireEvent.paste(input, {clipboardData: {getData: () => 'Bildung\nWohnen'}});
        expect(onPatch).not.toHaveBeenCalled();
    });
});
