import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {CodeListStatus} from '../enums/code-list-status';
import {CodeListSourceType} from '../enums/code-list-source-type';
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
                columnMappings={[]}
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

    it('warns only for a previously mapped source column after a successful synchronization', () => {
        const column = {key: 'name', name: 'Land', index: null};
        const renderChip = (status: CodeListStatus, formerSourceColumn: string | null) => (
            <CodeListStatusChip
                sourceType={CodeListSourceType.Asset}
                status={status}
                columnMappings={[{...column, formerSourceColumn}]}
            />
        );
        const {rerender} = render(renderChip(CodeListStatus.Synced, 'country'));
        expect(screen.getByText('Synchronisiert mit Warnung')).toBeInTheDocument();

        rerender(renderChip(CodeListStatus.SyncFailed, 'country'));
        expect(screen.getByText('Synchronisierung fehlgeschlagen')).toBeInTheDocument();

        rerender(renderChip(CodeListStatus.Synced, null));
        expect(screen.getByText('Synchronisiert')).toBeInTheDocument();
    });
});
