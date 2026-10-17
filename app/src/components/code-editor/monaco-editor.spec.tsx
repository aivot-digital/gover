import {useEffect} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import type {EditorProps} from '@monaco-editor/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {MonacoEditor} from './monaco-editor';
import {loadMonacoEditor} from './monaco-editor-loader';

vi.mock('./monaco-editor-loader', async (importOriginal) => ({
    ...await importOriginal<typeof import('./monaco-editor-loader')>(),
    loadMonacoEditor: vi.fn(),
}));

const mounted = vi.fn();
function EditorImplementation(props: EditorProps) {
    useEffect(() => { mounted(); }, []);
    return <textarea aria-label="Code" value={props.value} readOnly={props.options?.readOnly}
                     onChange={(event) => props.onChange?.(event.target.value, {} as never)}/>;
}

describe('MonacoEditor', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.mocked(loadMonacoEditor).mockReset();
    });
    afterEach(() => { vi.useRealTimers(); });

    it('uses the latest props after loading and keeps the mounted editor on updates', async () => {
        let finish!: (value: Awaited<ReturnType<typeof loadMonacoEditor>>) => void;
        vi.mocked(loadMonacoEditor).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
        const onChange = vi.fn();
        const {rerender} = render(<MonacoEditor value="initial" options={{readOnly: false}} onChange={onChange}/>);
        expect(screen.getByRole('status')).toHaveTextContent('Code-Editor wird geladen');
        rerender(<MonacoEditor value="updated while loading" options={{readOnly: true}} onChange={onChange}/>);
        await act(async () => { finish({MonacoEditorImplementation: EditorImplementation as never}); });
        await act(async () => { await vi.advanceTimersByTimeAsync(499); });
        expect(screen.getByRole('status')).toBeVisible();
        expect(mounted).not.toHaveBeenCalled();
        await act(async () => { await vi.advanceTimersByTimeAsync(1); });

        const editor = screen.getByRole('textbox', {name: 'Code'});
        expect(editor).toHaveValue('updated while loading');
        expect(editor).toHaveAttribute('readonly');
        expect(onChange).not.toHaveBeenCalled();
        rerender(<MonacoEditor value="updated after loading" options={{readOnly: false}} onChange={onChange}/>);
        expect(screen.getByRole('textbox', {name: 'Code'})).toBe(editor);
        expect(editor).toHaveValue('updated after loading');
        expect(editor).not.toHaveAttribute('readonly');
        fireEvent.change(editor, {target: {value: 'new input'}});
        expect(onChange).toHaveBeenCalledWith('new input', expect.anything());
        expect(mounted).toHaveBeenCalledTimes(1);
        expect(loadMonacoEditor).toHaveBeenCalledTimes(1);
    });

    it('handles a failed download locally and retries without changing the value', async () => {
        vi.mocked(loadMonacoEditor).mockRejectedValueOnce(new Error('missing chunk'))
            .mockResolvedValueOnce({MonacoEditorImplementation: EditorImplementation as never});
        const onChange = vi.fn();
        render(<><input aria-label="Other input" defaultValue="unsaved"/>
            <MonacoEditor value="existing code" onChange={onChange}/></>);
        await act(async () => { await vi.advanceTimersByTimeAsync(499); });
        expect(screen.getByRole('status')).toBeVisible();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        await act(async () => { await vi.advanceTimersByTimeAsync(1); });
        expect(screen.getByRole('alert')).toHaveTextContent('Der Code-Editor konnte nicht geladen werden.');
        expect(screen.getByRole('textbox', {name: 'Other input'})).toHaveValue('unsaved');
        fireEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
        await act(async () => { await vi.advanceTimersByTimeAsync(499); });
        expect(screen.getByRole('status')).toBeVisible();
        await act(async () => { await vi.advanceTimersByTimeAsync(1); });
        expect(screen.getByRole('textbox', {name: 'Code'})).toHaveValue('existing code');
        expect(onChange).not.toHaveBeenCalled();
        expect(loadMonacoEditor).toHaveBeenCalledTimes(2);
    });

    it('does not mount an editor if its view closes while the download is pending', async () => {
        let finish!: (value: Awaited<ReturnType<typeof loadMonacoEditor>>) => void;
        vi.mocked(loadMonacoEditor).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
        const {unmount} = render(<MonacoEditor value="existing code"/>);
        unmount();
        await act(async () => { finish({MonacoEditorImplementation: EditorImplementation as never}); });
        await act(async () => { await vi.advanceTimersByTimeAsync(500); });
        expect(mounted).not.toHaveBeenCalled();
    });

    it('does not add a further delay after a slow download', async () => {
        let finish!: (value: Awaited<ReturnType<typeof loadMonacoEditor>>) => void;
        vi.mocked(loadMonacoEditor).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
        render(<MonacoEditor value="existing code"/>);
        await act(async () => { await vi.advanceTimersByTimeAsync(800); });
        expect(screen.getByRole('status')).toBeVisible();
        await act(async () => { finish({MonacoEditorImplementation: EditorImplementation as never}); });
        expect(screen.getByRole('textbox', {name: 'Code'})).toHaveValue('existing code');
    });

    it('preserves inputs and explains manual recovery when a stylesheet failure cannot be retried safely', async () => {
        vi.mocked(loadMonacoEditor).mockRejectedValue(new Error('Unable to preload CSS for /assets/editor-Ab12_-34.css'));
        render(<><input aria-label="Other input" defaultValue="unsaved"/><MonacoEditor value="existing code"/></>);
        await act(async () => { await vi.advanceTimersByTimeAsync(500); });
        expect(screen.getByRole('alert')).toHaveTextContent('Bitte sichern Sie Ihre Eingaben und laden Sie anschließend die Seite neu.');
        expect(screen.queryByRole('button', {name: 'Erneut versuchen'})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Other input'})).toHaveValue('unsaved');
        expect(mounted).not.toHaveBeenCalled();
    });
});
