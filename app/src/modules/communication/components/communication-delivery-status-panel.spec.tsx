import {act, cleanup, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {CommunicationDeliveryStatusPanel} from './communication-delivery-status-panel';
import {CommunicationDeliveryStatus, type CommunicationDeliveryView} from '../communication-deliveries-api-service';

const {forTask, forTest} = vi.hoisted(() => ({forTask: vi.fn(), forTest: vi.fn()}));
vi.mock('../communication-deliveries-api-service', async (importOriginal) => ({
    ...await importOriginal<typeof import('../communication-deliveries-api-service')>(),
    CommunicationDeliveriesApiService: class {
        forTask = forTask;
        forTest = forTest;
    },
}));

const pending: CommunicationDeliveryView = {
    id: 'delivery', status: CommunicationDeliveryStatus.Submitted, label: 'Wartet auf Zustellbestätigung',
    message: null, receipt: {submissionId: 'submission'}, updated: '2026-09-22T10:00:00Z', overdue: false, checking: true,
};

describe('CommunicationDeliveryStatusPanel', () => {
    beforeEach(() => { vi.clearAllMocks(); vi.useFakeTimers(); });
    afterEach(() => { cleanup(); vi.useRealTimers(); });

    it('shows rejection after a pending submission without sending another message', async () => {
        forTask.mockResolvedValueOnce(pending).mockResolvedValueOnce({
            ...pending, status: CommunicationDeliveryStatus.Rejected, label: 'Zustellung abgelehnt', checking: false,
            receipt: {submissionId: 'submission', eventId: 'event', problems: [{title: 'Schema violation', detail: 'Invalid metadata', instance: 'metadata'}]},
        });
        await act(async () => { render(<CommunicationDeliveryStatusPanel taskId={2}/>); });
        expect(screen.getByText('Wartet auf Zustellbestätigung')).toBeInTheDocument();
        expect(screen.queryByText('Zustellung bestätigt')).not.toBeInTheDocument();
        await act(async () => { await vi.advanceTimersByTimeAsync(10000); });
        expect(screen.getByText('Zustellung abgelehnt')).toBeInTheDocument();
        expect(screen.getByText('Schema violation: Invalid metadata')).toBeInTheDocument();
        expect(screen.getByText('Betroffener Bereich: metadata')).toBeInTheDocument();
        await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
        expect(forTask).toHaveBeenCalledTimes(2);
    });

    it('preserves the last known status when an update fails', async () => {
        forTest.mockResolvedValueOnce(pending).mockRejectedValueOnce(new Error('offline'));
        await act(async () => { render(<CommunicationDeliveryStatusPanel providerId={7} deliveryId="delivery"/>); });
        await act(async () => { await vi.advanceTimersByTimeAsync(10000); });
        expect(screen.getByText('Wartet auf Zustellbestätigung')).toBeInTheDocument();
        expect(screen.getByText(/Der aktuelle Versandstatus konnte nicht geladen werden/)).toBeInTheDocument();
        expect(forTest).toHaveBeenCalledWith(7, 'delivery');
    });

    it('cancels polling when unmounted', async () => {
        forTask.mockResolvedValue(pending);
        let view: ReturnType<typeof render>;
        await act(async () => { view = render(<CommunicationDeliveryStatusPanel taskId={2}/>); });
        view!.unmount();
        await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
        expect(forTask).toHaveBeenCalledTimes(1);
    });
});
