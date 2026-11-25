import {afterEach, describe, expect, it, vi} from 'vitest';
import {cleanup, render, screen} from '@testing-library/react';
import {ProcessAssignee} from './process-assignee';

vi.mock('../../../hooks/use-app-selector', () => ({useAppSelector: () => null}));
vi.mock('../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => false}));

describe('ProcessAssignee', () => {
    afterEach(cleanup);

    it('names the current deputies below the assignee', () => {
        render(<ProcessAssignee userId="philipp" deputies={[{id: 'daniel', name: 'Daniel Müller'}]} />);

        expect(screen.getByText('Zugewiesen · Name nicht verfügbar')).toBeInTheDocument();
        expect(screen.getByText('Vertreten durch Daniel Müller')).toBeInTheDocument();
    });

    it('shows no deputy line without an active deputy', () => {
        render(<ProcessAssignee userId="philipp" deputies={[]} />);

        expect(screen.getByText('Zugewiesen · Name nicht verfügbar')).toBeInTheDocument();
        expect(screen.queryByText(/Vertreten durch/)).not.toBeInTheDocument();
    });
});
