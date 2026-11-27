import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {expect, it, vi} from 'vitest';
import {type EditorProps} from '@monaco-editor/react';
import {StructureTab} from './structure-tab';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementType} from '../../data/element-type/element-type';

const mocks = vi.hoisted(() => ({
    confirm: vi.fn(async () => true),
    editorProps: {} as EditorProps,
}));

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../providers/confirm-provider', () => ({useConfirm: () => mocks.confirm}));
vi.mock('../code-editor/monaco-editor', () => ({
    MonacoEditor: (props: EditorProps) => {
        mocks.editorProps = props;
        return null;
    },
}));

it('enables manual editing only after Monaco mounts while keeping the structure download available', async () => {
    const user = userEvent.setup();
    const element = {id: 'field', type: ElementType.Text, label: 'Name'} as AnyElement;
    const changed = {...element, label: 'Neuer Name'};
    const onChange = vi.fn();
    render(<StructureTab elementModel={element} editable onChange={onChange}/>);

    const toggle = screen.getByRole('switch', {name: 'Struktur manuell überschreiben'});
    expect(toggle).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Struktur anwenden'})).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Elementstruktur herunterladen'})).toBeEnabled();
    expect(mocks.confirm).not.toHaveBeenCalled();

    act(() => {
        mocks.editorProps.onMount?.({getValue: () => JSON.stringify(changed)} as never, {} as never);
    });
    expect(toggle).toBeEnabled();
    await user.click(toggle);
    expect(mocks.confirm).toHaveBeenCalledTimes(1);
    expect(toggle).toBeChecked();
    expect(mocks.editorProps.options?.readOnly).toBe(false);
    await user.click(screen.getByRole('button', {name: 'Struktur anwenden'}));
    expect(onChange).toHaveBeenCalledWith(changed);
    expect(toggle).not.toBeChecked();
});
