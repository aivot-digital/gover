import React from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {createMemoryRouter, Link, RouterProvider} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../../data/element-type/element-type';
import {type AuthoredElementValues, createDerivedRuntimeElementData, getLiteralElementValue, literalAuthoredValue} from '../../../../models/element-data';
import {SelectFieldPresentation} from '../../../../models/elements/form/input/select-field-presentation';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceAttachmentApiService} from '../../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../../services/process-instance-attachment-set-api-service';
import {ProcessInstanceTaskApiService, type TaskView} from '../../services/process-instance-task-api-service';
import {ProcessTaskViewPageEdit} from './process-task-view-page-edit';

vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => true}));
vi.mock('../../../../hooks/use-app-dispatch', () => {
    const dispatch = vi.fn();
    return {useAppDispatch: () => dispatch};
});
vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => {
    const context = {
        item: {
            task: {id: 20, processInstanceId: 10, status: 'AwaitingStaff'},
            instance: {id: 10},
            provider: {key: 'de.aivot.core.assign_instance', componentKey: 'assign_instance', name: 'Vorgang zuweisen'},
        },
        refresh: vi.fn(),
    };
    return {useGenericDetailsPageContext: () => context};
});
vi.mock('../../../../utils/with-delay', () => ({withDelay: <T,>(promise: Promise<T>) => promise}));

const removed = {value: '4869086b-5edd-4c6b-9c64-2eb081044ef1', label: 'Maria Muster'};
const eligible = {value: '0bfcdf45-494f-4f24-b1ad-04819cdea617', label: 'Alex Beispiel'};
const fieldId = 'assignedUserId';
const unavailableDetails = {code: 'manual_assignment_recipient_unavailable', fieldId, rejectedValue: removed.value};

function view(options: typeof removed[], data: AuthoredElementValues): TaskView {
    return {
        layout: {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'instance-assignment-task-view',
            children: [{
                ...generateElementWithDefaultValues(ElementType.Select),
                id: fieldId, label: 'Zuständige Person', required: true,
                disabled: options.length === 0, presentation: SelectFieldPresentation.Combobox, options,
            }],
        },
        data,
        events: options.length ? [{label: 'Vorgang zuweisen', event: 'assign'}] : [],
        removableAttachmentKeys: [],
    };
}

async function openTask(options: typeof removed[], data: AuthoredElementValues = {}) {
    const api = ProcessInstanceTaskApiService.prototype;
    const attachments = {content: [], page: {size: 0, number: 0, totalElements: 0, totalPages: 0}};
    vi.spyOn(ProcessInstanceAttachmentApiService.prototype, 'listAll').mockResolvedValue(attachments);
    vi.spyOn(ProcessInstanceAttachmentSetApiService.prototype, 'listAll').mockResolvedValue(attachments);
    const get = vi.spyOn(api, 'getStaffTaskView').mockResolvedValue(view(options, data));
    const put = vi.spyOn(api, 'putStaffTaskView').mockImplementation(async (_, __, draft) => view(options, draft));
    vi.spyOn(api, 'deriveStaffTaskView').mockImplementation(async (_, __, draft) => {
        return createDerivedRuntimeElementData({effectiveValues: {[fieldId]: getLiteralElementValue(draft, fieldId)}});
    });
    vi.spyOn(api, 'retrieve').mockResolvedValue({...new ProcessInstanceTaskApiService().initialize(), status: ProcessTaskStatus.Completed});
    const router = createMemoryRouter([
        {path: '/', element: <><ProcessTaskViewPageEdit/><Link to="/tasks">Aufgabe verlassen</Link></>},
        {path: '/tasks', element: <h1>Aufgabenliste</h1>},
    ]);
    render(<RouterProvider router={router}/>);
    const input = await screen.findByRole('combobox', {name: /Zuständige Person/});
    return {input, get, put};
}

function choose(input: HTMLElement, person: typeof removed) {
    fireEvent.keyDown(input, {key: 'ArrowDown'});
    fireEvent.click(screen.getByRole('option', {name: person.label}));
}

async function advanceAutosave() {
    await act(async () => { await vi.advanceTimersByTimeAsync(2000); });
}

describe('Stale assignment recipients in staff tasks', () => {
    afterEach(() => { vi.useRealTimers(); });
    it.each([true, false])('clears the rejected recipient after submission (alternative available: %s)', async (hasAlternative) => {
        const {input, get, put} = await openTask(hasAlternative ? [removed, eligible] : [removed]);
        choose(input, removed);
        const fresh = view(hasAlternative ? [eligible] : [], {});
        get.mockResolvedValue(fresh);
        put.mockRejectedValueOnce({
            status: 400, message: 'Die ausgewählte Person steht nicht mehr zur Verfügung.', displayableToUser: true,
            details: hasAlternative
                ? createDerivedRuntimeElementData({elementStates: {[fieldId]: {error: 'Auswahl nicht mehr verfügbar', errorDetails: unavailableDetails}}})
                : unavailableDetails,
        });
        fireEvent.click(screen.getByRole('button', {name: 'Vorgang zuweisen'}));

        const message = 'Maria Muster kann diesem Vorgang nicht mehr zugewiesen werden. ' +
            (hasAlternative ? 'Bitte wählen Sie eine andere Person aus.' : 'Derzeit steht keine andere Person zur Auswahl.');
        await waitFor(() => expect(screen.getByRole('combobox')).toHaveAccessibleDescription(expect.stringContaining(message)));
        expect(screen.getByRole('combobox')).toHaveValue('');
        expect(document.body).not.toHaveTextContent(removed.value);
        expect(put).toHaveBeenCalledTimes(1);

        if (hasAlternative) {
            choose(screen.getByRole('combobox'), eligible);
            expect(screen.queryAllByText(message)).toHaveLength(0);
            put.mockResolvedValueOnce(view([eligible], {}));
            fireEvent.click(screen.getByRole('button', {name: 'Vorgang zuweisen'}));
            expect(await screen.findByRole('heading', {name: 'Aufgabenliste'})).toBeVisible();
            expect(put).toHaveBeenLastCalledWith(10, 20, {[fieldId]: literalAuthoredValue(eligible.value)}, [], 'assign');
        } else {
            expect(screen.getByRole('combobox')).toBeDisabled();
            expect(screen.queryByRole('button', {name: 'Vorgang zuweisen'})).not.toBeInTheDocument();
            fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));
            expect(await screen.findByRole('heading', {name: 'Aufgabenliste'})).toBeVisible();
            expect(put).toHaveBeenCalledTimes(1);
        }
    });

    it('saves the removal when leaving after an already stored recipient was rejected', async () => {
        const saved = {[fieldId]: literalAuthoredValue(removed.value)};
        const {get, put} = await openTask([removed, eligible], saved);
        get.mockResolvedValue(view([eligible], saved));
        put.mockRejectedValueOnce({status: 400, message: 'Auswahl nicht mehr verfügbar', displayableToUser: true, details: unavailableDetails})
            .mockResolvedValue(view([eligible], {}));
        fireEvent.click(screen.getByRole('button', {name: 'Vorgang zuweisen'}));
        await waitFor(() => expect(screen.getByRole('combobox')).toHaveAccessibleDescription(/Maria Muster kann/));
        fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));
        expect(await screen.findByRole('heading', {name: 'Aufgabenliste'})).toBeVisible();
        expect(put).toHaveBeenCalledTimes(2);
        expect(put).toHaveBeenLastCalledWith(10, 20, {}, []);
    });

    it.each([true, false])('lets staff leave when only cleanup of a revoked saved recipient is pending (online: %s)', async (online) => {
        const saved = {[fieldId]: literalAuthoredValue(removed.value)};
        const {get, put} = await openTask([removed, eligible], saved);
        get.mockResolvedValue(view([eligible], saved));
        put.mockRejectedValueOnce({status: 400, message: 'Auswahl nicht mehr verfügbar', displayableToUser: true, details: unavailableDetails})
            .mockRejectedValue({status: 503, message: 'Zwischenspeichern fehlgeschlagen', displayableToUser: false});
        fireEvent.click(screen.getByRole('button', {name: 'Vorgang zuweisen'}));
        await waitFor(() => expect(screen.getByRole('combobox')).toHaveAccessibleDescription(/Maria Muster kann/));
        vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(online);
        fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));
        expect(await screen.findByRole('heading', {name: 'Aufgabenliste'})).toBeVisible();
        expect(put).toHaveBeenCalledTimes(online ? 2 : 1);
    });

    it('removes an unavailable recipient from a reopened draft without displaying its ID', async () => {
        const {input, put} = await openTask([eligible], {[fieldId]: literalAuthoredValue(removed.value)});
        await waitFor(() => expect(input).toHaveAccessibleDescription(/Die ausgewählte Person kann diesem Vorgang nicht mehr zugewiesen werden/));
        expect(input).toHaveValue('');
        expect(document.body).not.toHaveTextContent(removed.value);
        await waitFor(() => expect(put).toHaveBeenCalled(), {timeout: 3500});
        const [, , saved, , event] = put.mock.calls.at(-1)!;
        expect(saved).not.toHaveProperty(fieldId);
        expect(event).toBeUndefined();
    });

    it.each([true, false])('shows the last displayed name after autosave (removal already persisted: %s)', async (alreadyRemoved) => {
        const {input, put} = await openTask([removed, eligible]);
        // A grant can also be withdrawn between persisting the draft and constructing the response layout.
        const saved = alreadyRemoved ? {} : {[fieldId]: literalAuthoredValue(removed.value)};
        put.mockResolvedValueOnce(view([eligible], saved));
        put.mockResolvedValue(view([eligible], {}));
        vi.useFakeTimers();
        choose(input, removed);
        await advanceAutosave();
        expect(screen.getByRole('combobox')).toHaveAccessibleDescription(/Maria Muster kann diesem Vorgang nicht mehr zugewiesen werden/);
        expect(screen.getByRole('combobox')).toHaveValue('');
        await advanceAutosave();
        expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
        expect(put).toHaveBeenCalledTimes(alreadyRemoved ? 1 : 2);
    });

    it('preserves a newer choice when an in-flight autosave removes the previously selected person', async () => {
        const {input, put} = await openTask([removed, eligible]);
        const pending = Promise.withResolvers<TaskView>();
        put.mockImplementationOnce(() => pending.promise);
        vi.useFakeTimers();
        choose(input, removed);
        await advanceAutosave();
        expect(put).toHaveBeenCalledTimes(1);
        choose(input, eligible);
        await act(async () => pending.resolve(view([eligible], {})));
        expect(screen.getByRole('combobox')).toHaveValue(eligible.label);
        expect(screen.queryByText(/Maria Muster kann/)).not.toBeInTheDocument();
        await advanceAutosave();
        expect(put).toHaveBeenLastCalledWith(10, 20, {[fieldId]: literalAuthoredValue(eligible.value)}, []);
    });

    it('leaves after a pending autosave without sending the removed person again', async () => {
        const {input, put} = await openTask([removed, eligible]);
        const pending = Promise.withResolvers<TaskView>();
        put.mockImplementationOnce(() => pending.promise);
        vi.useFakeTimers();
        choose(input, removed);
        await advanceAutosave();
        expect(put).toHaveBeenCalledTimes(1);
        vi.useRealTimers();
        fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        await act(async () => pending.resolve(view([eligible], {})));
        expect(await screen.findByRole('heading', {name: 'Aufgabenliste'})).toBeVisible();
        expect(put).toHaveBeenCalledTimes(1);
    });
});
