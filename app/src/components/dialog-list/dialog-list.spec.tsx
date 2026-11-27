import {useState} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, expect, it, vi} from 'vitest';
import {DialogList} from './dialog-list';
import {ConfirmProvider} from '../../providers/confirm-provider';

function DraftEditor({item, onChange}: {item: string; onChange: (value: string) => void}) {
    const [draft, setDraft] = useState(item);
    return <input aria-label="Buchungsdaten" value={draft} onChange={(event) => {
        setDraft(event.target.value);
        onChange(event.target.value);
    }}/>;
}

afterEach(() => vi.useRealTimers());

it('starts a fresh draft when another item opens during the exit animation', () => {
    vi.useFakeTimers();
    const save = vi.fn();
    render(<ConfirmProvider><DialogList dialogTitle="Bearbeiten" items={['First', 'Second']}
        getId={(item) => item} title={(item) => item} dialogContentComponent={DraftEditor}
        onDialogSave={save} onDelete={vi.fn()}/></ConfirmProvider>);

    fireEvent.click(screen.getByText('First'));
    fireEvent.change(screen.getByRole('textbox', {name: 'Buchungsdaten'}), {target: {value: 'First edited'}});
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    fireEvent.click(screen.getByText('Second'));
    expect(screen.getByRole('textbox', {name: 'Buchungsdaten'})).toHaveValue('Second');
    act(() => vi.advanceTimersByTime(250));
    expect(screen.getByRole('textbox', {name: 'Buchungsdaten'})).toHaveValue('Second');
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    expect(save).toHaveBeenLastCalledWith('Second', 'Second');
});
