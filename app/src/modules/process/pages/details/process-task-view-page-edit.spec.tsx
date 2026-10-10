import React, {type ComponentProps} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryRouter, Link, RouterProvider} from 'react-router-dom';
import {ElementType} from '../../../../data/element-type/element-type';
import {
    type AuthoredElementValues,
    literalAuthoredValue,
} from '../../../../models/element-data';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import type {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {
    ProcessInstanceTaskApiService,
    type TaskView,
} from '../../services/process-instance-task-api-service';
import {ProcessTaskViewPageEdit} from './process-task-view-page-edit';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {BaseApiService} from '../../../../services/base-api-service';
import type {ProcessInstanceAttachmentEntity} from '../../entities/process-instance-attachment-entity';
import {SnackbarSeverity} from '../../../../slices/shell-slice';

const testState = vi.hoisted(() => ({
    dispatch: vi.fn(),
    refresh: vi.fn(),
    nextValues: {} as AuthoredElementValues,
    item: {
        task: {
            id: 20,
            processInstanceId: 10,
            status: 'Running' as ProcessTaskStatus,
        },
        instance: null,
        process: null,
        node: null,
        provider: null,
    },
    attachment: {
        key: 'attachment-key',
        fileName: 'Rechnung.html',
    } as ProcessInstanceAttachmentEntity,
}));

vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({item: testState.item, refresh: testState.refresh}),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../../utils/with-delay', () => ({
    withDelay: <T,>(promise: Promise<T>) => promise,
}));

vi.mock('../../../elements/components/element-derivation-context', async () => {
    const {useOptionalProcessTaskViewAttachmentContext} = await vi.importActual<
        typeof import('./process-task-view-attachment-context')
    >('./process-task-view-attachment-context');

    function ViewAttachmentButton() {
        const attachmentContext = useOptionalProcessTaskViewAttachmentContext();
        return (
            <button
                type="button"
                onClick={() => void attachmentContext?.viewAttachment(testState.attachment)}
            >
                Anhang ansehen
            </button>
        );
    }

    return {ElementDerivationContext: (props: ComponentProps<typeof ElementDerivationContext>) => (
        <>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange(testState.nextValues)}
            >
                Werte ändern
            </button>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange(props.authoredElementValues)}
            >
                Unveränderte Werte melden
            </button>
            <ViewAttachmentButton/>
        </>
    )};
});

const layout = generateElementWithDefaultValues(ElementType.GroupLayout);

function createTaskView(data: AuthoredElementValues): TaskView {
    return {
        layout,
        data,
        events: [],
    };
}

async function renderPage(initialValues: AuthoredElementValues) {
    const getTaskView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getStaffTaskView')
        .mockResolvedValue(createTaskView(initialValues));
    const putTaskView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'putStaffTaskView')
        .mockResolvedValue(createTaskView(initialValues));
    const router = createMemoryRouter([
        {
            path: '/tasks/10/20/edit',
            element: (
                <>
                    <ProcessTaskViewPageEdit/>
                    <Link to="/target">Aufgabe verlassen</Link>
                </>
            ),
        },
        {
            path: '/target',
            element: <div>Zielseite</div>,
        },
    ], {
        initialEntries: ['/tasks/10/20/edit'],
    });

    render(<RouterProvider router={router}/>);
    await screen.findByRole('button', {name: 'Werte ändern'});

    expect(getTaskView).toHaveBeenCalledWith(10, 20);
    return {putTaskView};
}

describe('ProcessTaskViewPageEdit autosave', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        testState.dispatch.mockReset();
        testState.refresh.mockReset();
        testState.item.task.status = ProcessTaskStatus.Running;
        testState.nextValues = {};
        vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it.each([
        {
            description: 'the first row of a replicating list is removed',
            initialValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
            nextValues: {
                rows: literalAuthoredValue([
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
        },
        {
            description: 'the second row of a replicating list is removed',
            initialValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
            nextValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                ]),
            },
        },
        {
            description: 'an existing subject is cleared',
            initialValues: {
                subject: literalAuthoredValue('Existing subject'),
            },
            nextValues: {
                subject: literalAuthoredValue(null),
            },
        },
    ])('saves the first actual change when $description', async ({initialValues, nextValues}) => {
        const {putTaskView} = await renderPage(initialValues);
        testState.nextValues = nextValues;
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Werte ändern'}));

        expect(screen.getByText('Ungespeicherte Eingaben vorhanden')).toBeInTheDocument();
        expect(putTaskView).not.toHaveBeenCalled();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(putTaskView).toHaveBeenCalledOnce();
        expect(putTaskView).toHaveBeenCalledWith(10, 20, nextValues);
        expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
        expect(testState.refresh).toHaveBeenCalledOnce();
    });

    it('ignores an unchanged value notification', async () => {
        const initialValues = {
            subject: literalAuthoredValue('Existing subject'),
        };
        const {putTaskView} = await renderPage(initialValues);
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Unveränderte Werte melden'}));
        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(putTaskView).not.toHaveBeenCalled();
        expect(testState.refresh).not.toHaveBeenCalled();
        expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
    });

    it('refreshes an awaiting staff task after its first successful save', async () => {
        testState.item.task.status = ProcessTaskStatus.AwaitingStaff;
        const {putTaskView} = await renderPage({});
        testState.nextValues = {subject: literalAuthoredValue('Started')};
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Werte ändern'}));
        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(putTaskView).toHaveBeenCalledOnce();
        expect(testState.refresh).toHaveBeenCalledOnce();
    });

    it('flushes the first change before navigating away', async () => {
        const initialValues = {
            subject: literalAuthoredValue('Existing subject'),
        };
        const nextValues = {
            subject: literalAuthoredValue(null),
        };
        const {putTaskView} = await renderPage(initialValues);
        testState.nextValues = nextValues;
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Werte ändern'}));
        fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));

        await act(async () => {
            await Promise.resolve();
        });

        expect(putTaskView).toHaveBeenCalledOnce();
        expect(putTaskView).toHaveBeenCalledWith(10, 20, nextValues);
        expect(screen.getByText('Zielseite')).toBeInTheDocument();
        expect(screen.queryByText('Ungespeicherte Eingaben')).not.toBeInTheDocument();
    });
});

describe('ProcessTaskViewPageEdit attachment preview', () => {
    const createObjectURL = vi.fn();
    const revokeObjectURL = vi.fn();

    beforeEach(() => {
        vi.restoreAllMocks();
        testState.dispatch.mockReset();
        testState.item.task.status = ProcessTaskStatus.Running;
        createObjectURL.mockReset().mockReturnValue('blob:preview');
        revokeObjectURL.mockReset();
        Object.defineProperty(URL, 'createObjectURL', {configurable: true, value: createObjectURL});
        Object.defineProperty(URL, 'revokeObjectURL', {configurable: true, value: revokeObjectURL});
        vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    });

    function mockPreviewWindow() {
        const previewWindow = {
            opener: {},
            document: {title: '', body: {textContent: ''}},
            location: {replace: vi.fn()},
            close: vi.fn(),
        };
        vi.spyOn(window, 'open').mockReturnValue(previewWindow as unknown as Window);
        return previewWindow;
    }

    it('previews inert attachments with the checked media type', async () => {
        const previewWindow = mockPreviewWindow();
        vi.spyOn(BaseApiService.prototype, 'getBlob')
            .mockResolvedValue(new Blob(['%PDF'], {type: 'application/pdf'}));
        await renderPage({});

        fireEvent.click(screen.getByRole('button', {name: 'Anhang ansehen'}));

        await waitFor(() => expect(previewWindow.location.replace).toHaveBeenCalledWith('blob:preview'));
        expect(createObjectURL.mock.calls[0][0]).toHaveProperty('type', 'application/pdf');
        expect(previewWindow.close).not.toHaveBeenCalled();
    });

    it.each(['text/html', 'image/svg+xml', 'application/xml', 'application/octet-stream'])(
        'downloads %s attachments instead of opening them on the application origin',
        async (mediaType) => {
            const previewWindow = mockPreviewWindow();
            const getBlob = vi.spyOn(BaseApiService.prototype, 'getBlob')
                .mockResolvedValue(new Blob(['<script>alert(1)</script>'], {type: mediaType}));
            await renderPage({});

            fireEvent.click(screen.getByRole('button', {name: 'Anhang ansehen'}));

            await waitFor(() => expect(getBlob).toHaveBeenLastCalledWith(
                '/api/process-instance-attachments/attachment-key/file/?download=true',
            ));
            expect(previewWindow.close).toHaveBeenCalledOnce();
            expect(previewWindow.location.replace).not.toHaveBeenCalled();
            expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
                payload: expect.objectContaining({
                    message: 'Dieser Dateityp kann nicht in der Vorschau angezeigt werden. Der Anhang wird stattdessen heruntergeladen.',
                    severity: SnackbarSeverity.Warning,
                }),
            }));
        },
    );
});
