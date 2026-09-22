import {beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen, waitFor, fireEvent} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, useLocation, useNavigate} from 'react-router-dom';
import {ProcessTaskList} from './process-task-list';
import {ProcessInstanceListPage} from './process-instance-page';
import {ProcessListApiService} from '../../services/process-list-api-service';

const mocks = vi.hoisted(() => ({
    api: {},
    dispatch: vi.fn(),
}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => undefined}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => children,
}));
vi.mock('../../../../utils/with-async-wrapper', () => ({
    withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main(),
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../components/process-list-actions', () => ({ProcessListActions: () => null}));
const page = {
    content: [],
    page: {
        number: 0,
        size: 12,
        totalElements: 0,
        totalPages: 0,
    },
};
function Navigation() {
    const location = useLocation();
    const navigate = useNavigate();
    return (
        <>
            <output aria-label="URL">{location.search}</output>
            <button onClick={() => navigate(-1)}>Zurück</button>
        </>
    );
}

describe('Process lists', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        vi.spyOn(ProcessListApiService.prototype, 'options').mockResolvedValue({
            processes: [
                {
                    value: '10',
                    label: 'Anmeldung',
                },
            ],
            assignees: [
                {
                    value: 'person',
                    label: 'Kim Beispiel',
                },
            ],
        });
    });

    it('defaults to my open tasks and applies assignment changes atomically with a page reset', async () => {
        const fetch = vi.spyOn(ProcessListApiService.prototype, 'tasks').mockResolvedValue(page);
        render(
            <MemoryRouter initialEntries={['/?page=4&search=AZ-100']}>
                <ProcessTaskList />
                <Navigation />
            </MemoryRouter>,
        );
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(
                3,
                12,
                'deadline',
                'ASC',
                expect.objectContaining({
                    assignee: 'mine',
                    view: 'open',
                    search: 'AZ-100',
                }),
            ),
        );
        const user = userEvent.setup();
        await user.click(screen.getByRole('combobox', {name: 'Zugewiesen an'}));
        await user.click(await screen.findByRole('option', {name: 'Alle Mitarbeiter:innen'}));
        await waitFor(() =>
            expect(fetch).toHaveBeenLastCalledWith(
                0,
                12,
                'deadline',
                'ASC',
                expect.objectContaining({
                    assignee: 'all',
                    view: 'open',
                    search: 'AZ-100',
                }),
            ),
        );
        expect(fetch.mock.calls.filter((call) => call[4].assignee === 'all').every((call) => call[0] === 0)).toBe(true);
        fireEvent.click(screen.getByRole('tab', {name: 'Überfällige Aufgaben'}));
        await waitFor(() =>
            expect(fetch).toHaveBeenLastCalledWith(
                0,
                12,
                'deadline',
                'ASC',
                expect.objectContaining({
                    assignee: 'all',
                    view: 'overdue',
                }),
            ),
        );
        fireEvent.click(screen.getByRole('button', {name: 'Zurück'}));
        await waitFor(() =>
            expect(fetch).toHaveBeenLastCalledWith(0, 12, 'deadline', 'ASC', expect.objectContaining({view: 'open'})),
        );
    });

    it('shows all tasks and assignments in an instance, preserving its scope', async () => {
        const fetch = vi.spyOn(ProcessListApiService.prototype, 'tasks').mockResolvedValue(page);
        render(
            <MemoryRouter>
                <ProcessTaskList instanceId={17} />
            </MemoryRouter>,
        );
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(
                0,
                12,
                'started',
                'DESC',
                expect.objectContaining({
                    instanceId: 17,
                    assignee: 'all',
                    view: 'all',
                }),
            ),
        );
        expect(ProcessListApiService.prototype.options).toHaveBeenCalledWith(true, 17);
        expect(screen.getAllByRole('columnheader')[0]).toHaveAttribute('data-field', 'icon');
        expect(screen.queryByRole('combobox', {name: 'Prozess'})).not.toBeInTheDocument();
    });

    it('defaults to active instances and clears a version restriction when changing process', async () => {
        const fetch = vi.spyOn(ProcessListApiService.prototype, 'instances').mockResolvedValue(page);
        render(
            <MemoryRouter initialEntries={['/?processId=10&processVersion=2&page=3']}>
                <ProcessInstanceListPage />
                <Navigation />
            </MemoryRouter>,
        );
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(
                2,
                12,
                'started',
                'DESC',
                expect.objectContaining({
                    view: 'active',
                    assignee: 'all',
                    processId: 10,
                    processVersion: 2,
                }),
            ),
        );
        const user = userEvent.setup();
        await user.click(screen.getByRole('combobox', {name: 'Prozess'}));
        await user.click(await screen.findByRole('option', {name: 'Alle Prozesse'}));
        await waitFor(() =>
            expect(fetch).toHaveBeenLastCalledWith(
                0,
                12,
                'started',
                'DESC',
                expect.objectContaining({
                    processId: undefined,
                    processVersion: undefined,
                }),
            ),
        );
        expect(screen.getByLabelText('URL')).not.toHaveTextContent('processVersion');
        expect(screen.getByRole('button', {name: 'Spalten'})).toBeInTheDocument();
        expect(screen.getAllByRole('columnheader')[0]).toHaveAttribute('data-field', 'icon');
    });
});
