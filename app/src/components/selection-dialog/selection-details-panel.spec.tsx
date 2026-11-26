import {type ComponentProps} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {SelectionDetailsPanel} from './selection-details-panel';
import {TechnicalInformationSection} from '../technical-information-section/technical-information-section';

const props: ComponentProps<typeof SelectionDetailsPanel> = {
    contentKey: 'first',
    icon: null,
    label: 'Element',
    title: 'Erstes Element',
    primaryActionLabel: 'Hinzufügen',
    primaryActionIcon: null,
    onPrimaryAction: vi.fn(),
    onClose: vi.fn(),
};

function detailsBody(): HTMLElement {
    return screen.getByRole('button', {name: 'Details schließen'})
        .parentElement!.previousElementSibling as HTMLElement;
}

function details(overrides: Partial<ComponentProps<typeof SelectionDetailsPanel>> = {}) {
    return <SelectionDetailsPanel {...props} {...overrides}>
        <TechnicalInformationSection headingBaseLevel={4}>
            Technische Angaben zum Element
        </TechnicalInformationSection>
    </SelectionDetailsPanel>;
}

describe('SelectionDetailsPanel', () => {
    it('starts each selection at the top with collapsed technical information, including when returning', async () => {
        const user = userEvent.setup();
        const {rerender} = render(details());
        detailsBody().scrollTop = 320;
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(screen.getByRole('region', {name: 'Technische Informationen'})).toBeVisible();

        rerender(details({contentKey: 'second', title: 'Zweites Element'}));

        expect(detailsBody().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();
        detailsBody().scrollTop = 180;

        rerender(details());

        expect(detailsBody().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
    });

    it('preserves reading position and expanded content when the same selection receives new data or actions', async () => {
        const user = userEvent.setup();
        const {rerender} = render(details());
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        detailsBody().scrollTop = 240;

        rerender(details({primaryActionLabel: 'Übernehmen', description: 'Aktualisierte Zusammenfassung'}));

        expect(detailsBody().scrollTop).toBe(240);
        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeVisible();
        expect(screen.getByText('Aktualisierte Zusammenfassung')).toBeVisible();
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('region', {name: 'Technische Informationen'})).toBeVisible();
    });

    it('retains focus on an action outside the reading area when the selected item changes', () => {
        const {rerender} = render(details());
        const closeButton = screen.getByRole('button', {name: 'Details schließen'});
        closeButton.focus();

        rerender(details({contentKey: 'second', title: 'Zweites Element'}));

        expect(closeButton).toHaveFocus();
        expect(screen.getByRole('heading', {name: 'Zweites Element', level: 3})).toBeVisible();
    });
});
