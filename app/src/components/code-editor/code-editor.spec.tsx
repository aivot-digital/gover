import {render, screen, waitFor} from '@testing-library/react';
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
    MonacoEditor: (props: {onMount?: (editor: typeof mocks.editor, monaco: unknown) => void}) => {
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

    it('registers JavaScript type hints through the Monaco top-level TypeScript API', async () => {
        render(
            <CodeEditor
                value=""
                onChange={vi.fn()}
                actions={[]}
                typeHints={[{name: 'element', content: 'declare const element: unknown;'}]}
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
