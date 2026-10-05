import React from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
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
import {literalAuthoredValue} from '../../models/element-data';
import {FormTriggerApiService} from '../../modules/forms/services/form-trigger-api-service';

const dispatchMock = vi.hoisted(() => vi.fn());
const downloadBlobFileMock = vi.hoisted(() => vi.fn());

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatchMock,
}));

vi.mock('../../utils/download-utils', () => ({
    downloadBlobFile: downloadBlobFileMock,
}));

describe('Submitted', () => {
    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
        vi.restoreAllMocks();
        downloadBlobFileMock.mockClear();
    });

    it('enables the PDF download when form processing completes without payment', async () => {
        vi.useFakeTimers();

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
                node={{configuration: {formSlug: literalAuthoredValue('form')}} as unknown as ProcessNodeEntity}
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

    it.each([ProcessTaskStatus.Completed, ProcessTaskStatus.Failed])(
        'keeps PDF download available and payment disabled when first task is %s and process fails',
        async (taskStatus) => {
            vi.useFakeTimers();

            const getInstanceStatus = vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
                .mockResolvedValueOnce(createStatusResponse(ProcessInstanceStatus.Running))
                .mockResolvedValueOnce(createStatusResponse(
                    ProcessInstanceStatus.Failed,
                    taskStatus,
                ));
            const pdf = new Blob(['pdf'], {type: 'application/pdf'});
            const downloadSubmittedSummaryPdf = vi.spyOn(FormTriggerApiService.prototype, 'downloadSubmittedSummaryPdf')
                .mockResolvedValue(pdf);

            render(
                <Submitted
                    startedProcessAccessKey="process-access-key"
                    paymentRequired
                    formElement={{children: []} as unknown as FormLayoutElement}
                    node={{configuration: {formSlug: literalAuthoredValue('form')}} as unknown as ProcessNodeEntity}
                    process={{slug: 'process'} as unknown as ProcessEntity}
                    version={{processVersion: 1} as unknown as ProcessVersionEntity}
                />,
            );

            await act(async () => {
                await Promise.resolve();
            });

            expect(screen.getByRole('heading', {name: 'Angaben erfolgreich übermittelt'})).toBeVisible();

            await act(async () => {
                await vi.advanceTimersByTimeAsync(1000);
            });

            expect(screen.getByRole('heading', {name: 'Verarbeitung fehlgeschlagen'})).toBeVisible();
            expect(screen.getByText('Ihr Antrag konnte nicht verarbeitet werden')).toBeVisible();
            expect(screen.getByText(/Bitte wenden Sie sich an die zuständige Stelle/)).toBeVisible();
            expect(screen.getByRole('button', {name: 'Zahlung nicht verfügbar'})).toBeDisabled();
            fireEvent.click(screen.getByRole('button', {name: 'Antrag als PDF herunterladen'}));
            expect(screen.queryByRole('link', {name: 'Zur Zahlung'})).not.toBeInTheDocument();

            await act(async () => {
                await Promise.resolve();
            });
            expect(downloadSubmittedSummaryPdf).toHaveBeenCalledWith(
                'process',
                'form',
                'process-access-key',
                'task-access-key',
                1,
            );
            expect(downloadBlobFileMock).toHaveBeenCalledWith('Antrag.pdf', pdf);

            await act(async () => {
                await vi.advanceTimersByTimeAsync(3000);
            });
            expect(getInstanceStatus).toHaveBeenCalledTimes(2);
        },
    );

    it('keeps PDF download unavailable when the failed process has no first task', async () => {
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockResolvedValue(createStatusResponse(ProcessInstanceStatus.Failed));

        render(
            <Submitted
                startedProcessAccessKey="process-access-key"
                paymentRequired
                formElement={{children: []} as unknown as FormLayoutElement}
                node={{configuration: {formSlug: literalAuthoredValue('form')}} as unknown as ProcessNodeEntity}
                process={{slug: 'process'} as unknown as ProcessEntity}
                version={{processVersion: 1} as unknown as ProcessVersionEntity}
            />,
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('button', {name: 'PDF nicht verfügbar'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Zahlung nicht verfügbar'})).toBeDisabled();
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
        accessibilityDepartmentId: null,
        imprintDepartmentId: null,
        privacyDepartmentId: null,
        legalSupportDepartmentId: null,
        technicalSupportDepartmentId: null,
        theme: {
            primaryColor: '#733635',
            secondaryColor: '#A0C9CB',
            primaryColorDark: null,
            secondaryColorDark: null,
            logoUrl: '/logo.svg',
            logoUrlDark: '/logo-dark.svg',
            faviconUrl: '/favicon.svg',
        },
    };
}
