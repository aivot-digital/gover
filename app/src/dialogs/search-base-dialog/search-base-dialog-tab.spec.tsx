import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {SearchBaseDialogTab} from './search-base-dialog-tab';

interface TestOption {
    id: string;
    name: string;
}

describe('SearchBaseDialogTab', () => {
    it('exposes the relationship and state of the details action', () => {
        const options: TestOption[] = [{id: 'alpha', name: 'Alpha'}];

        render(
            <SearchBaseDialogTab
                title="Eintrag wählen"
                options={options}
                onSelect={vi.fn()}
                searchPlaceholder="Einträge suchen"
                searchKeys={['name']}
                primaryTextKey="name"
                getId="id"
                detailsBuilder={(option) => <div>Details für {option.name}</div>}
            />,
        );

        // Direct label queries avoid a jsdom CSS-resolution bug triggered by MUI's Grid styles.
        const showDetails = screen.getByLabelText('Alpha: Details anzeigen');
        expect(showDetails).toHaveAttribute('aria-expanded', 'false');
        expect(showDetails).not.toHaveAttribute('aria-controls');

        fireEvent.click(showDetails);

        const hideDetails = screen.getByLabelText('Alpha: Details schließen');
        const detailsId = hideDetails.getAttribute('aria-controls');
        expect(hideDetails).toHaveAttribute('aria-expanded', 'true');
        expect(detailsId).not.toBeNull();
        expect(document.getElementById(detailsId as string)).toHaveTextContent('Details für Alpha');
    });
});
