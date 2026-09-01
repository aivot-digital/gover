import {fireEvent, render, screen, within} from '@testing-library/react';
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

describe('TestLab', () => {
    it('organizes components and system states into accessible tab panels', () => {
        render(<TestLab/>);

        expect(screen.getByRole('heading', {level: 1, name: 'Testlabor'})).toBeInTheDocument();

        const componentsTab = screen.getByRole('tab', {name: 'Komponenten'});
        const systemStatesTab = screen.getByRole('tab', {name: 'Systemzustände'});
        const componentsPanel = screen.getByRole('tabpanel', {name: 'Komponenten'});
        const tabList = screen.getByRole('tablist', {name: 'Bereiche des Testlabors'});
        const tabPaper = tabList.closest('.MuiPaper-root');

        expect(componentsTab).toHaveAttribute('aria-controls', 'test-lab-panel-components');
        expect(systemStatesTab).toHaveAttribute('aria-controls', 'test-lab-panel-system-states');
        expect(tabPaper).toContainElement(componentsPanel);
        expect(componentsPanel).toHaveTextContent('Feldkomponenten');

        fireEvent.click(systemStatesTab);

        const systemStatesPanel = screen.getByRole('tabpanel', {name: 'Systemzustände'});
        expect(systemStatesPanel).toBeVisible();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Shell-Fortschritt'})).toBeInTheDocument();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Fehlerseiten'})).toBeInTheDocument();
        expect(within(systemStatesPanel).getByRole('heading', {name: 'Benachrichtigungen'})).toBeInTheDocument();
    });
});
