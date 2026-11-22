import userEvent from '@testing-library/user-event';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {NoCodeEditorDialog} from './no-code-editor-dialog';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';

const api = vi.hoisted(() => {
    return {get: vi.fn()};
});
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../components/rich-text-input-component/rich-text-input-component', () => ({
    RichTextInputComponent: (props: {
        label: string;
        ariaLabel?: string;
        value?: string;
        onChange: (value: string) => void
    }) => <textarea aria-label={props.ariaLabel ?? props.label}
                    value={props.value ?? ''}
                    onChange={event => props.onChange(event.target.value)}/>,
}));
const root = generateElementWithDefaultValues(ElementType.FormLayout);
const props = {
    title: 'Bedingung',
    rootElement: root,
    returnType: NoCodeDataType.String,
    contextType: 'PROCESS' as const,
};
beforeEach(() => api.get.mockReset().mockResolvedValue([]));

describe('NoCodeEditorDialog', () => {
    it('closes an unchanged dialog with Escape from the reading controls', async () => {
        api.get.mockResolvedValue([{
            identifier: 'choice',
            packageName: 'core',
            label: 'Auswahl',
            abstractDescription: '',
            description: '',
            tags: [],
            humanReadableTemplate: '#0',
            signatures: [{
                returnType: NoCodeDataType.String,
                parameters: [{
                    type: NoCodeDataType.String,
                    label: 'Wert',
                    description: null,
                    options: [{label: 'Ja', value: 'ja'}],
                }],
            }],
        }]);
        const user = userEvent.setup();
        const onClose = vi.fn();
        const onApply = vi.fn();
        render(<NoCodeEditorDialog {...props}
                                   value={{type: 'NoCodeExpression', operatorIdentifier: 'choice', operands: [{type: 'NoCodeStaticValue', value: 'ja'}]}}
                                   onApply={onApply}
                                   onClose={onClose}/>);
        await user.click(await screen.findByRole('button', {name: 'Auswahl: zuklappen'}));
        await user.click(screen.getByRole('button', {name: 'Technische Details'}));
        await user.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalledOnce();
        expect(onApply).not.toHaveBeenCalled();
        expect(screen.queryByRole('dialog', {name: 'Änderungen verwerfen?'})).not.toBeInTheDocument();
    });

    it('scrolls the padded dialog content to the test area while keeping title and actions outside', async () => {
        render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'text',
        }}
                                   onApply={vi.fn()}
                                   onClose={vi.fn()}/>);
        const toggle = await screen.findByRole('button', {name: 'Testlauf'});
        const content = screen.getByRole('tree').closest<HTMLDivElement>('.MuiDialogContent-root')!;
        const panel = document.getElementById(toggle.getAttribute('aria-controls')!)!;
        content.scrollTo = vi.fn();
        content.scrollTop = 40;
        vi.spyOn(content, 'getBoundingClientRect').mockReturnValue({top: 100} as DOMRect);
        vi.spyOn(panel, 'getBoundingClientRect').mockReturnValue({top: 500} as DOMRect);
        fireEvent.click(toggle);
        expect(content.scrollTo).toHaveBeenCalledWith({
            top: 440,
            behavior: 'smooth',
        });
        expect(content).toContainElement(screen.getByRole('region', {name: 'Testlauf'}));
        expect(screen.getByRole('heading', {
            name: 'Testlauf',
            level: 3,
        })).toBeInTheDocument();
        expect(content).not.toContainElement(toggle);
        expect(content).not.toContainElement(screen.getByRole('button', {name: 'Übernehmen'}));
    });

    it.each([true, false])('allows removing an optional expression, but requires a configured expression when required=%s', async (required) => {
        const onApply = vi.fn();
        render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'text',
        }}
                                   required={required}
                                   onApply={onApply}
                                   onClose={vi.fn()}/>);
        await waitFor(() => expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'Wert „text“ löschen'}));
        await screen.findByRole('button', {name: 'Ausdruck hinzufügen'});
        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'Leerer Wert, Textwert'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        if (required) expect(onApply).not.toHaveBeenCalled();
        else {
            expect(onApply).toHaveBeenCalledWith(null);
            expect(screen.getByRole('treeitem')).not.toHaveAttribute('aria-invalid', 'true');
        }
    });

    it('allows an intentional empty value and preserves a draft across parent echoes', async () => {
        const onApply = vi.fn();
        const onClose = vi.fn();
        const {rerender} = render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'initial',
        }}
                                                      required
                                                      onApply={onApply}
                                                      onClose={onClose}/>);
        fireEvent.click(await screen.findByRole('button', {name: 'initial, Textwert'}));
        const input = await screen.findByRole('textbox', {name: /^Ausdruck/});
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.keyDown(input, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('textbox')).not.toBeInTheDocument());
        rerender(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'initial',
        }}
                                     required
                                     onApply={onApply}
                                     onClose={onClose}/>);
        await waitFor(() => expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(onApply).toHaveBeenCalledWith({
            type: 'NoCodeStaticValue',
            value: undefined,
        });
    });

    it('confirms discarding edits and never emits them on cancellation', async () => {
        const onApply = vi.fn();
        const onClose = vi.fn();
        render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'text',
        }}
                                   onApply={onApply}
                                   onClose={onClose}/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Wert „text“ löschen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        const confirmation = await screen.findByRole('dialog', {name: 'Änderungen verwerfen?'});
        expect(onClose).not.toHaveBeenCalled();
        fireEvent.click(within(confirmation).getByRole('button', {name: 'Änderungen verwerfen'}));
        expect(onClose).toHaveBeenCalledOnce();
        expect(onApply).not.toHaveBeenCalled();
    });

    it('blocks apply while metadata is unavailable and supports retry', async () => {
        const errorLog = vi.spyOn(console, 'error').mockImplementation(() => {
        });
        api.get.mockRejectedValueOnce(new Error('offline'));
        render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeStaticValue',
            value: 'text',
        }}
                                   onApply={vi.fn()}
                                   onClose={vi.fn()}/>);
        expect(await screen.findByText(/Die Funktionen konnten nicht geladen werden/)).toBeInTheDocument();
        expect(screen.queryByText('Unbekannte Funktion')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeDisabled();
        fireEvent.click(screen.getByRole('button', {name: 'Erneut laden'}));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled());
        errorLog.mockRestore();
    });

    it('blocks unknown operators after loading metadata', async () => {
        render(<NoCodeEditorDialog {...props} value={{
            type: 'NoCodeExpression',
            operatorIdentifier: 'missing',
            operands: [],
        }}
                                   onApply={vi.fn()}
                                   onClose={vi.fn()}/>);
        expect(await screen.findAllByText('Die ausgewählte Funktion ist nicht verfügbar.')).not.toHaveLength(0);
        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled();
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(screen.getByRole('treeitem')).toHaveFocus();
    });
});

it('keeps the parameters of an unavailable extension visible and does not emit an edited replacement on close', async () => {
    const onApply = vi.fn();
    const onClose = vi.fn();
    render(<NoCodeEditorDialog {...props} value={{
        type: 'NoCodeExpression',
        operatorIdentifier: 'extension:missing',
        operands: [
            {
                type: 'NoCodeStaticValue',
                value: 'stored value',
            },
            {
                type: 'NoCodeProcessDataReference',
                path: 'items[*].amount',
            },
        ],
    }}
                               onApply={onApply}
                               onClose={onClose}/>);
    expect(await screen.findAllByText('Die ausgewählte Funktion ist nicht verfügbar.')).not.toHaveLength(0);
    expect(screen.getAllByRole('treeitem')).toHaveLength(3);
    expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    expect(onApply).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
    expect(onClose).toHaveBeenCalledOnce();
    expect(onApply).not.toHaveBeenCalled();
});

it('uses a large dialog without maximization, retains history and validates only on apply', async () => {
    const onApply = vi.fn();
    render(<NoCodeEditorDialog {...props} value={{
        type: 'NoCodeStaticValue',
        value: 'text',
    }}
                               required
                               onApply={onApply}
                               onClose={vi.fn()}/>);
    fireEvent.click(await screen.findByRole('button', {name: 'Wert „text“ löschen'}));
    expect(screen.queryByText('Legen Sie einen Ausdruck fest.')).not.toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveClass('MuiDialog-paperWidthLg');
    expect(screen.queryByRole('button', {name: /Dialog maximieren|Dialog verkleinern/})).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Rückgängig'})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
    expect(screen.getByRole('button', {name: 'text, Textwert'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Wiederholen'}));
    expect(screen.getByRole('dialog')).not.toHaveClass('MuiDialog-paperFullScreen');
    expect(screen.getByRole('button', {name: 'Ausdruck hinzufügen'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    expect(onApply).not.toHaveBeenCalled();
    expect(screen.getByRole('treeitem')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('treeitem')).toHaveFocus();
});

it('applies notes and saved examples together with the expression, and discards them together', async () => {
    const onApply = vi.fn();
    const onClose = vi.fn();
    const original = {
        type: 'NoCodeStaticValue' as const,
        value: 'text',
    };
    render(<NoCodeEditorDialog {...props} value={original}
                               onApply={onApply}
                               onClose={onClose}/>);
    fireEvent.click(await screen.findByRole('button', {name: 'Notizen'}));
    fireEvent.change(await screen.findByRole('textbox', {name: 'Notizen zum Ausdruck'}), {target: {value: 'Fachliche Ausnahme'}});
    fireEvent.click(screen.getByRole('button', {name: 'Notizen schließen'}));
    await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Notizen zum Ausdruck'})).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.click(screen.getByRole('checkbox', {name: 'Testdaten mit dem Ausdruck speichern'}));
    await waitFor(() => expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled());
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    expect(onApply).toHaveBeenCalledWith({
        ...original,
        editorMetadata: {
            notes: 'Fachliche Ausnahme',
            testData: {
                version: 1,
                elementValues: {},
                sourceValues: {},
                visibility: {},
            },
        },
    });
    expect(original).not.toHaveProperty('editorMetadata');
    onApply.mockClear();
    fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
    const confirmation = await screen.findByRole('dialog', {name: 'Änderungen verwerfen?'});
    fireEvent.click(within(confirmation).getByRole('button', {name: 'Änderungen verwerfen'}));
    expect(onClose).toHaveBeenCalledOnce();
    expect(onApply).not.toHaveBeenCalled();
});

it('closes only the notes popover with Escape, restores focus and retains the draft', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const onApply = vi.fn();
    render(<NoCodeEditorDialog {...props} value={{
        type: 'NoCodeStaticValue',
        value: 'text',
    }}
                               onApply={onApply}
                               onClose={onClose}/>);
    await user.click(await screen.findByRole('button', {name: 'Notizen'}));
    const notes = await screen.findByRole('textbox', {name: 'Notizen zum Ausdruck'});
    await user.type(notes, 'Für Sonderfälle');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Notizen zum Ausdruck'})).not.toBeInTheDocument());
    expect(onClose).not.toHaveBeenCalled();
    expect(onApply).not.toHaveBeenCalled();
    const trigger = screen.getByRole('button', {name: 'Notizen anzeigen'});
    expect(trigger).toHaveFocus();
    await user.click(trigger);
    expect(await screen.findByRole('textbox', {name: 'Notizen zum Ausdruck'})).toHaveValue('Für Sonderfälle');
});
