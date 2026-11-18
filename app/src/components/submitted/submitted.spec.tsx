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

    it('does not offer a status retry without an access key', async () => {
        const getInstanceStatus = vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus');
        render(<Submitted startedProcessAccessKey="  " paymentRequired formElement={{children: []} as unknown as FormLayoutElement}
            processSlug="process" formSlug="form" processVersion={1}/>);
        expect(await screen.findByText('Status nicht verfügbar')).toBeVisible();
        expect(screen.queryByRole('button', {name: 'Status erneut prüfen'})).not.toBeInTheDocument();
        expect(getInstanceStatus).not.toHaveBeenCalled();
    });

    it('does not claim payment success from a completed form task', async () => {
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockResolvedValue(createStatusResponse(ProcessInstanceStatus.Running, ProcessTaskStatus.Completed));
        render(<Submitted startedProcessAccessKey="access-key" paymentRequired formElement={{children: []} as unknown as FormLayoutElement}
            processSlug="process" formSlug="form" processVersion={1}/>);
        expect(await screen.findByText('Für diese Übermittlung ist keine weitere Zahlung erforderlich.')).toBeVisible();
        expect(screen.queryByText('Zahlung abgeschlossen')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', {name: 'Zur Zahlung'})).not.toBeInTheDocument();
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
                processSlug="process"
                formSlug="form"
                processVersion={1}
            />,
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('button', {name: 'PDF wird vorbereitet'})).toBeDisabled();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(1000);
        });

        expect(screen.getByRole('button', {name: 'Angaben als PDF herunterladen'})).toBeEnabled();

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
                    processSlug="process"
                    formSlug="form"
                    processVersion={1}
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
            if (taskStatus === ProcessTaskStatus.Completed) {
                expect(screen.queryByText('Ihre Angaben konnten nicht weiterverarbeitet werden')).not.toBeInTheDocument();
                expect(screen.queryByText(/Öffnen Sie „Hilfe“ am Ende der Seite\. Dort finden Sie die Kontaktinformationen\./)).not.toBeInTheDocument();
                expect(screen.getByText('Für diese Übermittlung ist keine weitere Zahlung erforderlich.')).toBeVisible();
                expect(screen.queryByText('Zahlung abgeschlossen')).not.toBeInTheDocument();
            } else {
                expect(screen.getByText('Ihre Angaben konnten nicht weiterverarbeitet werden')).toBeVisible();
                expect(screen.getByText(/Öffnen Sie „Hilfe“ am Ende der Seite\. Dort finden Sie die Kontaktinformationen\./)).toBeVisible();
                expect(screen.getByRole('button', {name: 'Zahlung nicht verfügbar'})).toBeDisabled();
            }
            fireEvent.click(screen.getByRole('button', {name: 'Angaben als PDF herunterladen'}));
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
            expect(downloadBlobFileMock).toHaveBeenCalledWith('Angaben.pdf', pdf);

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
                processSlug="process"
                formSlug="form"
                processVersion={1}
            />,
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('button', {name: 'PDF nicht verfügbar'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Zahlung nicht verfügbar'})).toBeDisabled();
    });
    it('shows a status error and retries without resubmitting the form', async () => {
        const getStatus = vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockRejectedValueOnce(new Error('offline'))
            .mockResolvedValueOnce(createStatusResponse(ProcessInstanceStatus.Running, ProcessTaskStatus.AwaitingPayment));
        const submit = vi.spyOn(FormTriggerApiService.prototype, 'submitForm');
        render(<Submitted startedProcessAccessKey="process-key" paymentRequired formElement={{children: []} as unknown as FormLayoutElement} processSlug="process" formSlug="form" processVersion={1}/>);
        expect(await screen.findByText('Status nicht verfügbar')).toBeVisible();
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Status erneut prüfen'}));
        expect(await screen.findByRole('link', {name: 'Zur Zahlung'})).toHaveAttribute('href', '/process/process-key/tasks/task-access-key');
        expect(getStatus).toHaveBeenCalledTimes(2);
        expect(submit).not.toHaveBeenCalled();
    });

    it('stops the loading indicators after a long preparation without claiming failure', async () => {
        vi.useFakeTimers();
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus')
            .mockResolvedValue(createStatusResponse(ProcessInstanceStatus.Running, ProcessTaskStatus.Running));
        render(<Submitted startedProcessAccessKey="process-key" paymentRequired formElement={{children: []} as unknown as FormLayoutElement} processSlug="process" formSlug="form" processVersion={1}/>);
        expect(screen.getByRole('button', {name: 'Zahlung wird vorbereitet'})).toBeDisabled();
        expect(screen.getAllByRole('progressbar', {hidden: true})).toHaveLength(2);
        await act(async () => { await vi.advanceTimersByTimeAsync(30_000); });
        expect(screen.getByText('Die Vorbereitung dauert länger')).toBeVisible();
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Verarbeitung fehlgeschlagen'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Status erneut prüfen'})).toBeEnabled();
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
