import {fireEvent, render, screen, within} from '@testing-library/react';
import {alertClasses} from '@mui/material';
import {rgbToHex} from '@mui/material/styles';
import {describe, expect, it, vi} from 'vitest';
import {TestLab} from './test-lab';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
}));

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../components/generic-page-header/generic-page-header', () => ({
    GenericPageHeader: ({title}: {title: string}) => <h1>{title}</h1>,
}));

vi.mock('./field-layout-gallery', () => ({
    FieldLayoutGallery: () => <div>Feldkomponenten</div>,
}));

vi.mock('./input-mode-prototype', () => ({
    InputModePrototype: () => <div>Dynamische Eingabemodi</div>,
}));

describe('TestLab', () => {
    it('organizes components and system states into accessible tab panels', () => {
        render(<TestLab/>);

        expect(screen.getByRole('heading', {level: 1, name: 'Testlabor'})).toBeInTheDocument();

        const componentsTab = screen.getByRole('tab', {name: 'Komponenten'});
        const inputModesTab = screen.getByRole('tab', {name: 'Eingabemodi'});
        const systemStatesTab = screen.getByRole('tab', {name: 'Systemzustände'});
        const componentsPanel = screen.getByRole('tabpanel', {name: 'Komponenten'});
        const tabList = screen.getByRole('tablist', {name: 'Bereiche des Testlabors'});
        const tabPaper = tabList.closest('.MuiPaper-root');

        expect(componentsTab).toHaveAttribute('aria-controls', 'test-lab-panel-components');
        expect(inputModesTab).toHaveAttribute('aria-controls', 'test-lab-panel-input-modes');
        expect(systemStatesTab).toHaveAttribute('aria-controls', 'test-lab-panel-system-states');
        expect(tabPaper).toContainElement(componentsPanel);
        expect(tabPaper).toHaveStyle({minWidth: 0, maxWidth: '100%'});
        expect(componentsPanel).toHaveTextContent('Feldkomponenten');

        fireEvent.click(inputModesTab);

        const inputModesPanel = screen.getByRole('tabpanel', {name: 'Eingabemodi'});
        expect(inputModesPanel).toBeVisible();
        expect(inputModesPanel).toHaveTextContent('Dynamische Eingabemodi');

        fireEvent.click(systemStatesTab);

        const systemStatesPanel = screen.getByRole('tabpanel', {name: 'Systemzustände'});
        expect(systemStatesPanel).toBeVisible();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Shell-Fortschritt'})).toBeInTheDocument();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Fehlerseiten'})).toBeInTheDocument();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Benachrichtigungen'})).toBeInTheDocument();
    });

    it('shows the active signal colors and their component variants in both modes', () => {
        render(<TestLab/>);

        fireEvent.click(screen.getByRole('tab', {name: 'Systemzustände'}));
        const panel = screen.getByRole('tabpanel', {name: 'Systemzustände'});
        const gallery = within(panel).getByRole('region', {name: 'Signalfarben'});

        for (const [name, warningFill] of [['Hell', '#ffca79'], ['Dunkel', '#dfb354']]) {
            const table = within(gallery).getByRole('table', {name});
            expect(within(table).getAllByRole('columnheader').map((header) => header.textContent))
                .toEqual(['Variante', 'Fehler', 'Warnung', 'Information', 'Erfolg']);
            expect(within(table).getAllByRole('rowheader').map((header) => header.textContent))
                .toEqual([
                    'Meldung',
                    'Meldung mit Kontur',
                    'Gefüllte Meldung',
                    'Textschaltfläche',
                    'Schaltfläche mit Kontur',
                    'Gefüllte Schaltfläche',
                ]);
            expect(within(table).getAllByRole('button', {name: 'Warnung'})).toHaveLength(3);

            const filledAlerts = within(table).getByRole('row', {name: /^Gefüllte Meldung/});
            const warning = within(filledAlerts).getByRole('cell', {name: 'Warnung'});
            const alert = warning.querySelector(`.${alertClasses.filled}`)!;
            expect(rgbToHex(getComputedStyle(alert).backgroundColor)).toBe(warningFill);
        }

        expect(within(gallery).queryByRole('alert')).not.toBeInTheDocument();
    });
});
