import React from 'react';
import {act, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {Submitted} from './submitted';
import {
    CustomerTaskViewApiService,
    type ProcessInstanceStatusResponse,
} from '../../pages/customer-pages/customer-instance-view/customer-task-view-api-service';
import {ProcessInstanceStatus} from '../../modules/process/enums/process-instance-status';
import {ProcessTaskStatus} from '../../modules/process/enums/process-task-status';
import type {FormLayoutElement} from '../../models/elements/form-layout-element';
import type {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import type {ProcessEntity} from '../../modules/process/entities/process-entity';
import type {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

const dispatchMock = vi.hoisted(() => vi.fn());
const confettiPlayKeyMock = vi.hoisted(() => vi.fn());

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatchMock,
}));

vi.mock('../confetti/canvas-confetti-overlay', () => ({
    CanvasConfettiOverlay: (props: {playKey: number | null}) => {
        confettiPlayKeyMock(props.playKey);
        return null;
    },
    prosunaConfettiColors: [],
}));

describe('Submitted', () => {
    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('enables the PDF download when form processing completes without payment', async () => {
        vi.useFakeTimers();
        Object.defineProperty(window, 'matchMedia', {
            configurable: true,
            value: vi.fn().mockReturnValue({matches: false}),
        });

        const getInstanceStatus = vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockResolvedValueOnce(createStatusResponse(
                ProcessInstanceStatus.Running,
                ProcessTaskStatus.Running,
            ))
            .mockResolvedValueOnce(createStatusResponse(
                ProcessInstanceStatus.Running,
                ProcessTaskStatus.Completed,
            ));

        render(
            <Submitted
                startedProcessAccessKey="completed-process-access-key"
                paymentRequired={false}
                formElement={{children: []} as unknown as FormLayoutElement}
                node={{configuration: {formSlug: 'form'}} as unknown as ProcessNodeEntity}
                process={{slug: 'process'} as unknown as ProcessEntity}
                version={{processVersion: 1} as unknown as ProcessVersionEntity}
            />,
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('button', {name: 'PDF wird vorbereitet'})).toBeDisabled();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(1000);
        });

        expect(screen.getByRole('button', {name: 'Antrag als PDF herunterladen'})).toBeEnabled();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(3000);
        });
        expect(getInstanceStatus).toHaveBeenCalledTimes(2);
    });

    it('disables payment and PDF actions when process preparation fails', async () => {
        vi.useFakeTimers();
        Object.defineProperty(window, 'matchMedia', {
            configurable: true,
            value: vi.fn().mockReturnValue({matches: false}),
        });

        const getInstanceStatus = vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockResolvedValueOnce(createStatusResponse(ProcessInstanceStatus.Running))
            .mockResolvedValueOnce(createStatusResponse(
                ProcessInstanceStatus.Failed,
                ProcessTaskStatus.AwaitingPayment,
            ));

        render(
            <Submitted
                startedProcessAccessKey="process-access-key"
                paymentRequired
                formElement={{children: []} as unknown as FormLayoutElement}
                node={{configuration: {formSlug: 'form'}} as unknown as ProcessNodeEntity}
                process={{slug: 'process'} as unknown as ProcessEntity}
                version={{processVersion: 1} as unknown as ProcessVersionEntity}
            />,
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('heading', {name: 'Angaben erfolgreich übermittelt'})).toBeVisible();
        expect(confettiPlayKeyMock).toHaveBeenCalledWith(1);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(1000);
        });

        expect(screen.getByRole('heading', {name: 'Verarbeitung fehlgeschlagen'})).toBeVisible();
        expect(screen.getByText('Ihr Antrag konnte nicht verarbeitet werden')).toBeVisible();
        expect(screen.getByText(/Bitte wenden Sie sich an die zuständige Stelle/)).toBeVisible();
        expect(screen.getByRole('button', {name: 'Zahlung nicht verfügbar'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'PDF nicht verfügbar'})).toBeDisabled();
        expect(screen.queryByRole('link', {name: 'Zur Zahlung'})).not.toBeInTheDocument();
        expect(confettiPlayKeyMock).toHaveBeenLastCalledWith(null);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(3000);
        });
        expect(getInstanceStatus).toHaveBeenCalledTimes(2);
    });
});

function createStatusResponse(
    status: ProcessInstanceStatus,
    taskStatus?: ProcessTaskStatus,
): ProcessInstanceStatusResponse {
    return {
        title: 'Process',
        status,
        statusOverride: '',
        tasks: taskStatus == null ? [] : [{
            accessKey: 'task-access-key',
            status: taskStatus,
            statusOverride: '',
        }],
    };
}
