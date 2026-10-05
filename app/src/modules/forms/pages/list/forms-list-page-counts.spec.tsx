import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter, useNavigate} from 'react-router-dom';
import {FormsListPage} from './forms-list-page';
import {FormTriggerApiService} from '../../services/form-trigger-api-service';

const mocks = vi.hoisted(() => ({api: {}, dispatch: vi.fn()}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => undefined}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../../../utils/with-async-wrapper', () => ({withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main()}));

function Navigation() {
    const navigate = useNavigate();
    return <button onClick={() => navigate('?filter=Drafted&search=Wohnen')}>Suche ändern</button>;
}

describe('Form overview counts', () => {
    afterEach(() => vi.restoreAllMocks());

    it('shows overview counts independently of search and refreshes on tab changes', async () => {
        const rows = vi.spyOn(FormTriggerApiService.prototype, 'listOverview').mockResolvedValue({
            content: [], page: {number: 0, size: 12, totalElements: 0, totalPages: 0},
        });
        const counts = vi.spyOn(FormTriggerApiService.prototype, 'overviewCounts')
            .mockResolvedValue({Published: 12, Drafted: 5});
        render(
            <MemoryRouter initialEntries={['/?search=Anmeldung']}>
                <FormsListPage />
                <Navigation />
            </MemoryRouter>,
        );
        expect(await screen.findByRole('tab', {name: 'Veröffentlicht 12'})).toBeInTheDocument();
        expect(counts).toHaveBeenCalledWith(expect.any(AbortSignal));
        fireEvent.click(screen.getByRole('tab', {name: 'In Bearbeitung 5'}));
        await waitFor(() => expect(rows).toHaveBeenLastCalledWith(0, 12, 'Drafted', 'Anmeldung', 'id', 'ASC'));
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(2));
        fireEvent.click(screen.getByRole('button', {name: 'Suche ändern'}));
        await waitFor(() => expect(rows).toHaveBeenLastCalledWith(0, 12, 'Drafted', 'Wohnen', 'id', 'ASC'));
        expect(counts).toHaveBeenCalledTimes(2);
    });
});
