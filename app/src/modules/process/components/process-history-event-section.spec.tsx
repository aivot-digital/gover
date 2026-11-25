import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {beforeEach, expect, it, vi} from 'vitest';
import {BaseTheme} from '../../../theming/base-theme';
import {ProcessHistoryEventScope} from '../enums/process-history-event-scope';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';
import {ProcessHistoryEventSection} from './process-history-event-section';

function events(scope: ProcessHistoryEventScope, number = 0, title = 'Eintrag') {
    return {
        content: [{
            ...new ProcessInstanceEventApiService().initialize(),
            id: number + 1,
            title,
            message: 'Details',
            timestamp: '2026-11-25T10:00:00Z',
            details: scope === ProcessHistoryEventScope.ManualInterventions ? {manualIntervention: true} : {},
        }],
        page: {
            number,
            size: 20,
            totalElements: 21,
            totalPages: 2,
        },
    };
}

beforeEach(() => vi.restoreAllMocks());

it.each([ProcessHistoryEventScope.ManualInterventions, ProcessHistoryEventScope.InstanceErrors] as const)(
    'paginates %s independently and keeps the previous entries if a later request fails', async scope => {
        const load = vi.spyOn(ProcessInstanceEventApiService.prototype, 'listHistory').mockResolvedValue(events(scope));
        render(<ThemeProvider theme={BaseTheme}><ProcessHistoryEventSection instanceId={17}
                                                                            scope={scope}
                                                                            refreshCounter={0}/></ThemeProvider>);
        expect(await screen.findByText('Eintrag')).toBeVisible();
        const title = scope === ProcessHistoryEventScope.ManualInterventions ? 'Manuelle Eingriffe' : 'Fehler auf Vorgangsebene';
        const pagination = within(screen.getByRole('navigation', {name: `Seiten: ${title}`}));
        load.mockRejectedValueOnce(new Error('Offline'));
        await userEvent.click(pagination.getByRole('button', {name: 'Nächste Seite'}));
        expect(await screen.findByText(/konnten nicht geladen werden/)).toBeVisible();
        expect(screen.getByText('Eintrag')).toBeVisible();
        load.mockResolvedValue(events(scope, 1, 'Weiterer Eintrag'));
        await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
        expect(await screen.findByText('Weiterer Eintrag')).toBeVisible();
        expect(screen.queryByText('Eintrag')).not.toBeInTheDocument();
        expect(load).toHaveBeenLastCalledWith(17, {
            scope,
            page: 1,
            size: 20,
        });
        expect(screen.getByText('21–21 von 21')).toBeVisible();
    },
);

it('clears stale results when the initial request fails and retries just that section', async () => {
    const load = vi.spyOn(ProcessInstanceEventApiService.prototype, 'listHistory').mockRejectedValueOnce(new Error('Offline'));
    render(<ThemeProvider theme={BaseTheme}><ProcessHistoryEventSection instanceId={17}
                                                                        scope={ProcessHistoryEventScope.ManualInterventions}
                                                                        refreshCounter={0}/></ThemeProvider>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Die manuellen Eingriffe konnten nicht geladen werden.');
    expect(screen.queryByText('Keine manuellen Eingriffe protokolliert.')).not.toBeInTheDocument();
    load.mockResolvedValue({
        content: [],
        page: {
            number: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
        },
    });
    await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
    expect(await screen.findByText('Keine manuellen Eingriffe protokolliert.')).toBeVisible();
});

it('ignores late responses after the section unmounts', async () => {
    let resolve!: (value: ReturnType<typeof events>) => void;
    vi.spyOn(ProcessInstanceEventApiService.prototype, 'listHistory').mockReturnValue(new Promise(value => {
        resolve = value;
    }));
    const {unmount} = render(<ProcessHistoryEventSection instanceId={17}
                                                         scope={ProcessHistoryEventScope.InstanceErrors}
                                                         refreshCounter={0}/>);
    unmount();
    resolve(events(ProcessHistoryEventScope.InstanceErrors));
    await waitFor(() => expect(screen.queryByText('Eintrag')).not.toBeInTheDocument());
});
