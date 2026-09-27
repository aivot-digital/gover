import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {CodeListStatus} from '../enums/code-list-status';
import {CodeListStatusChip} from './code-list-status-chip';

describe('CodeListStatusChip', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('should render the rounded relative synchronization time', () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-08-07T10:30:00Z'));

        render(
            <CodeListStatusChip
                status={CodeListStatus.Synced}
                lastSync="2026-07-18T22:00:00Z"
            />,
        );

        const label = screen.getByText('Synchronisiert vor 20 Tagen');
        expect(label).toBeInTheDocument();
        expect(label.closest('.MuiChip-root')).toHaveAttribute(
            'title',
            'Zuletzt synchronisiert: 19.07.2026 – 00:00:00 Uhr',
        );
    });
});
