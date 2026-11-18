import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {FileList} from './file-list';

describe('FileList', () => {
    it('keeps complete names accessible while truncating their visual display', () => {
        const name = 'Ein sehr langer Dateiname '.repeat(10) + '.pdf';
        render(<FileList items={[{key: 'file', name}]}/>);
        expect(screen.getByTitle(name)).toHaveStyle({overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'});
        expect(screen.getByRole('listitem')).toHaveTextContent(name);
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows optional metadata and disables only the unavailable action', () => {
        const remove = vi.fn();
        const download = vi.fn();
        render(<FileList items={[{
            key: 'file', name: 'Nachweis.pdf', secondaryText: '24 KB',
            actions: [
                {label: 'Entfernen', icon: <span/>, onClick: remove, disabled: true},
                {label: 'Herunterladen', icon: <span/>, onClick: download},
            ],
        }]}/>);
        expect(screen.getByText('24 KB')).toBeVisible();
        expect(screen.getByRole('button', {name: 'Entfernen'})).toBeDisabled();
        fireEvent.click(screen.getByRole('button', {name: 'Entfernen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Herunterladen'}));
        expect(remove).not.toHaveBeenCalled();
        expect(download).toHaveBeenCalledOnce();
    });
});
