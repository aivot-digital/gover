import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';
import {Login} from './login';

describe('Login', () => {
    it('presents the staff login and preserves the requested location', () => {
        render(
            <MemoryRouter initialEntries={['/settings?tab=system']}>
                <Login/>
            </MemoryRouter>,
        );

        expect(screen.getByRole('heading', {
            level: 1,
            name: 'Willkommen bei Prosuna',
        })).toBeVisible();
        expect(screen.getByText(
            'Für diesen Bereich ist eine Anmeldung erforderlich. Melden Sie sich mit Ihrem ' +
            'persönlichen Mitarbeitenden-Konto an, um darauf zuzugreifen.',
        )).toBeVisible();
        expect(screen.getByText('Sie werden zum verwendeten Identitätsdienst weitergeleitet.')).toBeVisible();
        const prosunaLinks = screen.getAllByRole('link', {
            name: 'Mehr über Prosuna',
        });
        const [logoLink, footerLink] = prosunaLinks;

        expect(prosunaLinks).toHaveLength(2);
        prosunaLinks.forEach(link => {
            expect(link).toHaveAttribute('target', '_blank');
            expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        });
        expect(logoLink).toHaveAttribute(
            'href',
            'https://prosuna.de/?utm_source=prosuna_instance&utm_medium=referral' +
            '&utm_campaign=staff_login&utm_content=logo',
        );
        expect(logoLink).toHaveAttribute('title', 'Mehr über Prosuna – öffnet in einem neuen Tab');
        expect(footerLink).toHaveAttribute(
            'href',
            'https://prosuna.de/?utm_source=prosuna_instance&utm_medium=referral' +
            '&utm_campaign=staff_login&utm_content=footer_link',
        );
        expect(screen.getByRole('link', {
            name: 'Mit Mitarbeitenden-Konto anmelden',
        })).toHaveAttribute(
            'href',
            '/api/auth/login?app_uri=%2Fstaff%2Fsettings&app_state=%3Ftab%3Dsystem',
        );
    });
});
