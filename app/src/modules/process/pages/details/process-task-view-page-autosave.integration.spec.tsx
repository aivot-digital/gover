import React, {type ComponentProps, type PropsWithChildren} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../../data/element-type/element-type';
import {Permission} from '../../../../data/permissions/permission';
import {createDerivedRuntimeElementData, getLiteralElementValue, literalAuthoredValue} from '../../../../models/element-data';
import type {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {setPermissions, setUser, userReducer} from '../../../../slices/user-slice';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import type {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import type {ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessInstanceAttachmentApiService} from '../../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../../services/process-instance-attachment-set-api-service';
import type {PermissionSet} from '../../../permissions/models/permission-set';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceTaskApiService, type TaskView} from '../../services/process-instance-task-api-service';
import {ProcessTaskViewPage, type ProcessTaskDetailsPageItem} from './process-task-view-page';
import {ProcessTaskViewPageEdit} from './process-task-view-page-edit';
import {ProcessTaskViewPageIndex} from './process-task-view-page-index';

vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: PropsWithChildren) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../../search/hooks/use-record-recent-search-item', () => ({useRecordRecentSearchItem: () => {}}));
vi.mock('../../../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../../../utils/with-delay', () => ({
    withDelay: <T,>(promise: Promise<T>) => promise,
}));
vi.mock('../../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: (props: ComponentProps<typeof ElementDerivationContext>) => (
        <input
            aria-label="Betreff"
            value={getLiteralElementValue<string>(props.authoredElementValues, 'subject') ?? ''}
            onChange={(event) => props.onAuthoredElementValuesChange({
                ...props.authoredElementValues,
                subject: literalAuthoredValue(event.target.value),
            })}
        />
    ),
}));

const firstSave = '2026-10-07T06:25:00Z';
const secondSave = '2026-10-07T06:27:00Z';

async function renderPage(
    initialStatus = ProcessTaskStatus.AwaitingStaff,
    initialView: Partial<TaskView> = {},
    instance: ProcessInstanceEntity | null = null,
) {
    const permissions: PermissionSet = {
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions: [],
        systemPermissions: [{
            userId: 'user',
            permissions: [Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_INSTANCE_EDIT_TASK],
        }],
    };
    const store = configureStore({reducer: {user: userReducer}});
    store.dispatch(setUser({
        id: 'user',
        email: 'user@example.org',
        firstName: 'Test',
        lastName: 'User',
        fullName: 'Test User',
        enabled: true,
        verified: true,
        deletedInIdp: false,
        systemRoleId: null,
    }));
    store.dispatch(setPermissions(permissions));
    vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(permissions);

    let details: ProcessTaskDetailsPageItem = {
        task: {
            ...new ProcessInstanceTaskApiService().initialize(),
            id: 20,
            processInstanceId: 10,
            status: initialStatus,
            updated: '2026-10-07T06:20:00Z',
        },
        instance,
        process: null,
        node: {name: 'Prüfung', description: null},
        provider: null,
    };
    const view: TaskView = {
        layout: generateElementWithDefaultValues(ElementType.GroupLayout),
        data: {},
        events: [],
        ...initialView,
    };
    const getView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getStaffTaskView').mockResolvedValue(view);
    const getDetails = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'retrieveDetails')
        .mockImplementation(async () => details);
    let saveCount = 0;
    const putView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'putStaffTaskView')
        .mockImplementation(async (_, __, data) => {
            details = {
                ...details,
                task: {
                    ...details.task,
                    status: ProcessTaskStatus.InProgress,
                    updated: saveCount++ === 0 ? firstSave : secondSave,
                },
            };
            return {...view, data};
        });
    const router = createMemoryRouter([{
        path: '/tasks/:instanceId/:taskId',
        element: <ProcessTaskViewPage />,
        children: [
            {index: true, element: <ProcessTaskViewPageIndex />},
            {path: 'edit', element: <ProcessTaskViewPageEdit />},
        ],
    }], {initialEntries: ['/tasks/10/20/edit']});

    render(<Provider store={store}><RouterProvider router={router} /></Provider>);
    await screen.findByRole('textbox', {name: 'Betreff'});
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-07T12:00:00Z'));

    return {getDetails, getView, putView, getSavedDetails: () => details};
}

async function changeAndSave(subject: string) {
    fireEvent.change(screen.getByRole('textbox', {name: 'Betreff'}), {target: {value: subject}});
    await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
    });
    expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
}

async function showGeneralInformation() {
    await act(async () => {
        fireEvent.click(screen.getByRole('tab', {name: 'Allgemeine Informationen'}));
    });
}

describe('Task timestamps after autosave', () => {
    beforeEach(() => {
        vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('shows the server timestamp of the second save after switching tabs', async () => {
        const {putView} = await renderPage();
        await changeAndSave('First draft');
        await changeAndSave('Second draft');
        await showGeneralInformation();

        expect(putView).toHaveBeenCalledTimes(2);
        expect(screen.getByRole('row', {name: /Zuletzt aktualisiert/})).toHaveTextContent('07.10.2026 um 08:27 Uhr');
        expect(screen.getByRole('row', {name: /Aufgabenstatus/})).toHaveTextContent('In Bearbeitung');
    });

    it('updates the timestamp when switching tabs flushes pending changes', async () => {
        const {putView} = await renderPage();
        await changeAndSave('First draft');
        fireEvent.change(screen.getByRole('textbox', {name: 'Betreff'}), {target: {value: 'Second draft'}});
        await showGeneralInformation();

        expect(putView).toHaveBeenLastCalledWith(10, 20, {subject: literalAuthoredValue('Second draft')}, []);
        expect(screen.getByRole('row', {name: /Zuletzt aktualisiert/})).toHaveTextContent('07.10.2026 um 08:27 Uhr');
    });

    it('preserves newer inputs while task details are being refreshed', async () => {
        const {getDetails, getView, putView, getSavedDetails} = await renderPage(ProcessTaskStatus.InProgress);
        const pendingDetails = Promise.withResolvers<ProcessTaskDetailsPageItem>();
        getDetails.mockImplementationOnce(() => pendingDetails.promise);
        await changeAndSave('First draft');
        fireEvent.change(screen.getByRole('textbox', {name: 'Betreff'}), {target: {value: 'Second draft'}});

        const savedDetails = getSavedDetails();
        await act(async () => {
            pendingDetails.resolve(savedDetails);
        });

        expect(screen.getByRole('textbox', {name: 'Betreff'})).toHaveValue('Second draft');
        expect(screen.getByText('Ungespeicherte Eingaben vorhanden')).toBeInTheDocument();
        expect(getView).toHaveBeenCalledOnce();
        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });
        expect(putView).toHaveBeenLastCalledWith(10, 20, {subject: literalAuthoredValue('Second draft')}, []);
        await showGeneralInformation();
        expect(screen.getByRole('row', {name: /Zuletzt aktualisiert/})).toHaveTextContent('07.10.2026 um 08:27 Uhr');
    });

    it('references the stored attachments after a save stored the uploaded files', async () => {
        const layout = generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout;
        layout.children = [{...generateElementWithDefaultValues(ElementType.FileUpload), id: 'upload'}];
        const uploadedFile = {name: 'nachweis.pdf', size: 3, uri: 'blob:nachweis'};
        const storedFile = {name: 'nachweis.pdf', size: 3, uri: 'process-instance-attachment:stored'};
        const {putView} = await renderPage(ProcessTaskStatus.InProgress, {
            layout,
            data: {upload: literalAuthoredValue([uploadedFile])},
        });
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'deriveStaffTaskView')
            .mockResolvedValue(createDerivedRuntimeElementData());
        putView.mockImplementation(async (_, __, data) => ({
            layout,
            data: {...data, upload: literalAuthoredValue([storedFile])},
            events: [],
        }));

        await changeAndSave('First draft');
        await changeAndSave('Second draft');

        expect(putView).toHaveBeenNthCalledWith(1, 10, 20, {
            upload: literalAuthoredValue([uploadedFile]),
            subject: literalAuthoredValue('First draft'),
        }, [uploadedFile]);
        expect(putView).toHaveBeenNthCalledWith(2, 10, 20, {
            upload: literalAuthoredValue([storedFile]),
            subject: literalAuthoredValue('Second draft'),
        }, []);
    });

    it('reloads the attachments of the instance only after saves that change the files', async () => {
        const layout = generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout;
        layout.children = [{...generateElementWithDefaultValues(ElementType.FileUpload), id: 'upload'}];
        const uploadedFile = {name: 'nachweis.pdf', size: 3, uri: 'blob:nachweis'};
        const storedFile = {name: 'nachweis.pdf', size: 3, uri: 'process-instance-attachment:stored'};
        const emptyPage = {content: [], totalElements: 0, totalPages: 0, number: 0, size: 0};
        const listAttachments = vi.spyOn(ProcessInstanceAttachmentApiService.prototype, 'listAll')
            .mockResolvedValue(emptyPage as never);
        vi.spyOn(ProcessInstanceAttachmentSetApiService.prototype, 'listAll').mockResolvedValue(emptyPage as never);
        const {putView} = await renderPage(ProcessTaskStatus.InProgress, {
            layout,
            data: {upload: literalAuthoredValue([uploadedFile])},
        }, {id: 10} as ProcessInstanceEntity);
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'deriveStaffTaskView')
            .mockResolvedValue(createDerivedRuntimeElementData());
        putView.mockImplementation(async (_, __, data) => ({
            layout,
            data: {...data, upload: literalAuthoredValue([storedFile])},
            events: [],
        }));
        expect(listAttachments).toHaveBeenCalledTimes(1);

        await changeAndSave('First draft');
        expect(listAttachments).toHaveBeenCalledTimes(2);

        await changeAndSave('Second draft');
        expect(listAttachments).toHaveBeenCalledTimes(2);
    });
});
