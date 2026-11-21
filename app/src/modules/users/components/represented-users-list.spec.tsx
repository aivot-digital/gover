import {cleanup, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {VUserDeputyWithDetailsApiService} from '../services/v-user-deputy-with-details-api-service';
import {RepresentedUsersList} from './represented-users-list';
import {parseLocalDateIso} from '../../../utils/temporal-utils';

vi.mock('../../../hooks/use-api', () => ({useApi: () => ({})}));
vi.mock('../../../utils/with-async-wrapper', () => ({
    withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main(),
}));

describe('RepresentedUsersList', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    afterEach(cleanup);

    it('lists only the relations in which the user is the deputy', async () => {
        const list = vi.spyOn(VUserDeputyWithDetailsApiService.prototype, 'list').mockResolvedValue({
            content: [{
                ...new VUserDeputyWithDetailsApiService().initialize(),
                id: 4,
                originalUserId: 'philipp',
                originalUserFullName: 'Philipp Schröder',
                deputyUserId: 'daniel',
                deputyUserFullName: 'Daniel Müller',
                fromDate: parseLocalDateIso('2026-11-02'),
                untilDate: null,
            }],
            page: {size: 1, number: 0, totalElements: 1, totalPages: 1},
        });

        render(
            <MemoryRouter>
                <RepresentedUsersList
                    deputyUserId="daniel"
                    emptyDescription="Keine Vertretungen."
                />
            </MemoryRouter>,
        );

        expect(await screen.findByText('Philipp Schröder')).toBeInTheDocument();
        expect(screen.queryByText('Daniel Müller')).not.toBeInTheDocument();
        expect(screen.getByText('Unbegrenzt')).toBeInTheDocument();
        expect(list).toHaveBeenCalledWith(0, 12, 'untilDate', 'ASC',
            expect.objectContaining({deputyUserId: 'daniel'}));
    });
});
