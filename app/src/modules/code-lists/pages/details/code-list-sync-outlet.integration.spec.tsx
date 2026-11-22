import React, {useContext, useState} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {Link, MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeListSourceType} from '../../enums/code-list-source-type';
import {CodeListStatus} from '../../enums/code-list-status';
import {type CodeList} from '../../models/code-list';
import {CodeListDetailsPageIndex} from './code-list-details-page-index';
import {CodeListSyncOutlet} from './code-list-sync-outlet';

const dispatch = vi.hoisted(() => vi.fn());
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));
vi.mock('../../../../hooks/use-change-blocker', () => ({useChangeBlocker: () => ({dialog: null})}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => true}));

const original = {
    ...new CodeListsApiService().initialize(),
    key: 'test', id: 7, name: 'Testliste', sourceType: CodeListSourceType.XRepository,
    sourceRef: 'urn:test', status: CodeListStatus.SyncPending, lockVersion: 1,
};

function DetailEditor() {
    const defaultContext = useContext(GenericDetailsPageContext);
    const [item, setItem] = useState<CodeList>(original);
    const [isBusy, setIsBusy] = useState(false);
    return (
        <GenericDetailsPageContext.Provider value={{
            ...defaultContext, item, setItem, isBusy, setIsBusy, isExistingItem: true, isNewItem: false, isEditable: true,
        }}>
            <output aria-label="Synchronisierungsstatus">{item.status}</output>
            <MemoryRouter initialEntries={['/code-lists/test']}>
                <Link to="/code-lists/other">Zweite Codeliste</Link>
                <button onClick={() => setItem({...original, id: 8, key: 'other', name: 'Zweite Liste', status: CodeListStatus.Synced})}>
                    Zweite Liste laden
                </button>
                <Routes>
                    <Route path="/code-lists/:key" element={<CodeListSyncOutlet/>}>
                        <Route index element={<CodeListDetailsPageIndex/>}/>
                    </Route>
                </Routes>
            </MemoryRouter>
        </GenericDetailsPageContext.Provider>
    );
}

describe('code list synchronization in an open editor', () => {
    afterEach(() => {
        dispatch.mockClear();
        vi.restoreAllMocks();
        vi.useRealTimers();
    });

    it.each([false, true])('updates the server snapshot and preserves a draft started before completion: %s', async (editBeforeSync) => {
        vi.useFakeTimers();
        const completed = {...original, status: CodeListStatus.Synced, lockVersion: 2};
        const retrieve = vi.spyOn(CodeListsApiService.prototype, 'retrieve').mockResolvedValue(completed);
        const update = vi.spyOn(CodeListsApiService.prototype, 'update').mockResolvedValue(completed);
        render(<DetailEditor/>);
        const name = screen.getByRole('textbox', {name: 'Name'});
        const edit = () => fireEvent.change(name, {target: {value: 'Entwurf'}});
        if (editBeforeSync) edit();

        await act(async () => { await vi.advanceTimersByTimeAsync(2000); });

        expect(screen.getByLabelText('Synchronisierungsstatus')).toHaveTextContent(CodeListStatus.Synced);
        if (!editBeforeSync) edit();
        expect(name).toHaveValue('Entwurf');
        await act(async () => { fireEvent.click(screen.getByRole('button', {name: 'Speichern'})); });
        expect(update).toHaveBeenCalledTimes(1);
        expect(update.mock.calls[0][1]).toMatchObject({
            name: 'Entwurf', lockVersion: editBeforeSync ? 1 : 2,
        });
        await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
        expect(retrieve).toHaveBeenCalledTimes(1);
    });

    it('discards the previous draft when switching directly to another code list', () => {
        render(<DetailEditor/>);
        fireEvent.change(screen.getByRole('textbox', {name: 'Name'}), {target: {value: 'Entwurf der ersten Liste'}});
        fireEvent.click(screen.getByRole('link', {name: 'Zweite Codeliste'}));

        expect(screen.queryByRole('textbox', {name: 'Name'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Zweite Liste laden'}));
        expect(screen.getByRole('textbox', {name: 'Name'})).toHaveValue('Zweite Liste');
    });

    it('retries transient failures with increasing delays and only reports the first error', async () => {
        vi.useFakeTimers();
        const error = {status: 503, message: 'Vorübergehend nicht erreichbar', displayableToUser: true};
        const retrieve = vi.spyOn(CodeListsApiService.prototype, 'retrieve')
            .mockRejectedValueOnce(error)
            .mockRejectedValueOnce(error)
            .mockResolvedValue({...original, status: CodeListStatus.Synced, lockVersion: 2});
        render(<DetailEditor/>);

        await act(async () => { await vi.advanceTimersByTimeAsync(4400); });
        expect(retrieve).toHaveBeenCalledTimes(1);
        await act(async () => { await vi.advanceTimersByTimeAsync(6000); });
        expect(retrieve).toHaveBeenCalledTimes(2);
        await act(async () => { await vi.advanceTimersByTimeAsync(200); });
        expect(retrieve).toHaveBeenCalledTimes(3);
        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(screen.getByLabelText('Synchronisierungsstatus')).toHaveTextContent(CodeListStatus.Synced);
    });

    it.each([[403, 1], [503, 4]])('limits attempts after repeated HTTP %i responses to %i', async (status, attempts) => {
        vi.useFakeTimers();
        const retrieve = vi.spyOn(CodeListsApiService.prototype, 'retrieve')
            .mockRejectedValue({status, message: 'Fehler', displayableToUser: true});
        render(<DetailEditor/>);

        await act(async () => { await vi.advanceTimersByTimeAsync(60000); });
        expect(retrieve).toHaveBeenCalledTimes(attempts);
        expect(dispatch).toHaveBeenCalledTimes(1);
    });
});
