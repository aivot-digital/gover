import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {CodeEditor} from './code-editor';

const mocks = vi.hoisted(() => ({
    addExtraLib: vi.fn(),
    getTypes: vi.fn(),
    editor: {
        getValue: vi.fn(() => ''),
        setValue: vi.fn(),
        onDidBlurEditorText: vi.fn(() => ({dispose: vi.fn()})),
    },
}));

vi.mock('./monaco-editor', () => ({
    MonacoEditor: (props: { onMount?: (editor: typeof mocks.editor, monaco: unknown) => void }) => {
        props.onMount?.(mocks.editor, {
            languages: {},
            typescript: {
                javascriptDefaults: {
                    addExtraLib: mocks.addExtraLib,
                },
            },
        });

        return <div data-testid="monaco-editor"/>;
    },
}));

vi.mock('../../modules/javascript/javascript-api-service', () => ({
    JavascriptApiService: class {
        getTypes() {
            return mocks.getTypes();
        }
    },
}));

describe('CodeEditor', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mocks.getTypes.mockResolvedValue('declare const $: Record<string, unknown>;');
    });

    it('places actions in the provided header without leaving an empty toolbar above the editor', async () => {
        const onLookup = vi.fn();

        function Harness() {
            const [toolbar, setToolbar] = useState<HTMLDivElement | null>(null);
            return <>
                <div ref={setToolbar}
                     role="group"
                     aria-label="Funktionsaktionen"/>
                <CodeEditor value=""
                            onChange={vi.fn()}
                            toolbarContainer={toolbar}
                            actions={[{
                                label: 'Element-ID nachschlagen',
                                onClick: onLookup,
                            }]}/>
            </>;
        }

        render(<Harness/>);
        const action = within(screen.getByRole('group', {name: 'Funktionsaktionen'})).getByRole('button', {name: 'Element-ID nachschlagen'});
        expect(screen.getAllByRole('button', {name: 'Element-ID nachschlagen'})).toHaveLength(1);
        expect(screen.getByRole('group', {name: 'Code-Editor'}).previousElementSibling).toBeNull();
        fireEvent.click(action);
        expect(onLookup).toHaveBeenCalledOnce();
        await waitFor(() => expect(mocks.getTypes).toHaveBeenCalled());
    });

    it('registers JavaScript type hints through the Monaco top-level TypeScript API', async () => {
        render(
            <CodeEditor
                value=""
                onChange={vi.fn()}
                actions={[]}
                typeHints={[{
                    name: 'element',
                    content: 'declare const element: unknown;',
                }]}
            />,
        );

        expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
        expect(mocks.addExtraLib).toHaveBeenCalledWith(
            'declare const element: unknown;',
            '@types/element.d.ts',
        );
        await waitFor(() => expect(mocks.addExtraLib).toHaveBeenCalledWith(
            'declare const $: Record<string, unknown>;',
            '@types/global.d.ts',
        ));
    });

    it('keeps the editor available when global type hints cannot be loaded', async () => {
        mocks.getTypes.mockRejectedValue(new Error('unavailable'));

        render(
            <CodeEditor
                value=""
                onChange={vi.fn()}
                actions={[]}
            />,
        );

        await waitFor(() => expect(mocks.getTypes).toHaveBeenCalled());
        expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
    });
});
