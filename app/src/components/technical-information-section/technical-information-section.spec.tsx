import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';
import {TechnicalInformationSection} from './technical-information-section';

describe('TechnicalInformationSection', () => {
    it('starts collapsed beneath a level-three heading without mounting the technical content', () => {
        render(<TechnicalInformationSection>Interne Kennung</TechnicalInformationSection>);

        const heading = screen.getByRole('heading', {name: 'Technische Informationen', level: 3});
        expect(within(heading).getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region')).not.toBeInTheDocument();
        expect(screen.queryByText('Interne Kennung')).not.toBeInTheDocument();
    });

    it('opens from the label and toggles with Space and Enter while preserving focus and region linkage', async () => {
        const user = userEvent.setup();
        render(<TechnicalInformationSection>Interne Kennung</TechnicalInformationSection>);
        const toggle = screen.getByRole('button', {name: 'Technische Informationen'});

        await user.click(screen.getByText('Technische Informationen'));

        const region = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(toggle).toHaveAttribute('aria-controls', region.id);
        expect(region).toHaveAttribute('aria-labelledby', toggle.id);
        expect(region).toHaveTextContent('Interne Kennung');
        expect(toggle).toHaveFocus();

        await user.keyboard(' ');

        expect(toggle).toHaveAttribute('aria-expanded', 'false');
        await waitFor(() => expect(screen.queryByRole('region')).not.toBeInTheDocument());
        expect(toggle).toHaveFocus();

        await user.keyboard('{Enter}');

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('region', {name: 'Technische Informationen'})).toHaveAttribute('id', region.id);
    });

    it('uses the configured heading level and unique control targets for multiple sections', async () => {
        const user = userEvent.setup();
        render(<>
            <TechnicalInformationSection>Erste Kennung</TechnicalInformationSection>
            <TechnicalInformationSection headingBaseLevel={4}>Zweite Kennung</TechnicalInformationSection>
        </>);
        const first = within(screen.getByRole('heading', {name: 'Technische Informationen', level: 3}))
            .getByRole('button');
        const second = within(screen.getByRole('heading', {name: 'Technische Informationen', level: 4}))
            .getByRole('button');

        expect(first.id).not.toBe(second.id);
        expect(first.getAttribute('aria-controls')).not.toBe(second.getAttribute('aria-controls'));

        await user.click(second);

        const region = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(region).toHaveTextContent('Zweite Kennung');
        expect(region).toHaveAttribute('aria-labelledby', second.id);
        expect(first).toHaveAttribute('aria-expanded', 'false');
    });

    it('unmounts closed content so temporary input values start fresh when reopened', async () => {
        const user = userEvent.setup();
        render(<TechnicalInformationSection>
            <input aria-label="Testwert" defaultValue=""/>
        </TechnicalInformationSection>);
        const toggle = screen.getByRole('button', {name: 'Technische Informationen'});
        await user.click(toggle);
        await user.type(screen.getByRole('textbox', {name: 'Testwert'}), 'Zwischenwert');

        await user.click(toggle);
        await waitFor(() => expect(screen.queryByRole('textbox')).not.toBeInTheDocument());
        await user.click(toggle);

        expect(screen.getByRole('textbox', {name: 'Testwert'})).toHaveValue('');
    });
});
