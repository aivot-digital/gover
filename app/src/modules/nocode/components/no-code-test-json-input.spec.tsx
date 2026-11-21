import {useEffect, useState} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {EditorProps} from '@monaco-editor/react';
import {NoCodeTestJsonInput} from './no-code-test-json-input';

const engine = vi.hoisted(() => ({
    format: vi.fn(),
    focus: vi.fn(),
    dispose: vi.fn(),
    blur: undefined as (() => void) | undefined,
    props: undefined as EditorProps | undefined,
}));
vi.mock('../../../components/code-editor/monaco-editor', () => ({
    MonacoEditor: (props: EditorProps) => {
        engine.props = props;
        useEffect(() => {
            props.onMount?.({
                focus: engine.focus,
                onDidBlurEditorText: (callback: () => void) => {
                    engine.blur = callback;
                    return {dispose: engine.dispose};
                },
                getAction: (name: string) => name === 'editor.action.formatDocument' ? {run: engine.format} : null,
            } as never, {} as never);
        }, []);
        return <textarea aria-label={props.options?.ariaLabel}
                         value={props.value ?? ''}
                         onChange={event => props.onChange?.(event.target.value, {} as never)}
                         onBlur={() => engine.blur?.()}/>;
    },
}));

beforeEach(() => {
    vi.clearAllMocks();
    engine.format.mockResolvedValue(undefined);
    engine.blur = undefined;
});

const base = {
    ariaLabel: 'Vorgangsdaten (JSON)',
    value: '{"name":"Anna"}',
    onChange: vi.fn(),
    onBlur: vi.fn(),
};

function Harness(props: Omit<Parameters<typeof NoCodeTestJsonInput>[0], 'toolbarContainer'>) {
    const [toolbar, setToolbar] = useState<HTMLElement | null>(null);
    return <>
        <div role="toolbar"
             aria-label="JSON-Aktionen"
             ref={setToolbar}/>
        <NoCodeTestJsonInput {...props} toolbarContainer={toolbar}/></>;
}

describe('NoCodeTestJsonInput', () => {
    it('uses the JSON editor without eager validation and preserves incomplete input', () => {
        const onChange = vi.fn();
        const onBlur = vi.fn();
        render(<Harness {...base} onChange={onChange}
                        onBlur={onBlur}/>);
        expect(engine.props).toMatchObject({
            language: 'json',
            className: 'nokey',
            options: {
                minimap: {enabled: false},
                renderValidationDecorations: 'off',
                wordWrap: 'on',
            },
        });
        const input = screen.getByRole('textbox', {name: base.ariaLabel});
        fireEvent.change(input, {target: {value: '{"name":'}});
        expect(onChange).toHaveBeenLastCalledWith('{"name":');
        expect(onBlur).not.toHaveBeenCalled();
        fireEvent.blur(input);
        expect(onBlur).toHaveBeenCalledOnce();
    });

    it('connects errors and reference hints and forwards validation focus into Monaco', () => {
        render(<><span id="reference">$.person</span><Harness {...base} ariaDescribedBy="reference"
                                                              error="Prüfen Sie das JSON."/></>);
        const group = screen.getByRole('group', {name: base.ariaLabel});
        expect(group).toHaveAttribute('aria-invalid', 'true');
        expect(group).toHaveAccessibleDescription('Prüfen Sie das JSON. $.person');
        expect(engine.props?.options?.renderValidationDecorations).toBe('on');
        fireEvent.focus(group);
        expect(engine.focus).toHaveBeenCalledOnce();
    });

    it('formats through Monaco without parsing and rewriting numeric values itself', async () => {
        const onChange = vi.fn();
        render(<Harness {...base} value='{"id":12345678901234567890}'
                        onChange={onChange}/>);
        fireEvent.click(within(screen.getByRole('toolbar', {name: 'JSON-Aktionen'})).getByRole('button', {name: 'JSON formatieren'}));
        await waitFor(() => expect(engine.format).toHaveBeenCalledOnce());
        expect(onChange).not.toHaveBeenCalled();
        act(() => engine.props?.onChange?.('{\n  "id": 12345678901234567890\n}', {} as never));
        expect(onChange).toHaveBeenLastCalledWith('{\n  "id": 12345678901234567890\n}');
    });

    it('keeps editing available after a formatting failure and cleans up the blur listener', async () => {
        engine.format.mockRejectedValueOnce(new Error('worker unavailable'));
        const latestBlur = vi.fn();
        const view = render(<Harness {...base}/>);
        fireEvent.click(within(screen.getByRole('toolbar', {name: 'JSON-Aktionen'})).getByRole('button', {name: 'JSON formatieren'}));
        expect(await screen.findByRole('alert')).toHaveTextContent('Das JSON konnte nicht formatiert werden.');
        expect(screen.getByRole('textbox', {name: base.ariaLabel})).toHaveValue(base.value);
        view.rerender(<Harness {...base} onBlur={latestBlur}/>);
        fireEvent.blur(screen.getByRole('textbox', {name: base.ariaLabel}));
        expect(latestBlur).toHaveBeenCalledOnce();
        const formatButton = screen.getByRole('button', {name: 'JSON formatieren'});
        view.unmount();
        expect(formatButton).not.toBeInTheDocument();
        expect(engine.dispose).toHaveBeenCalledOnce();
    });
});
