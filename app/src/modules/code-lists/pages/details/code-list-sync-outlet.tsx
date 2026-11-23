import React, {useEffect} from 'react';
import {Outlet, useParams} from 'react-router-dom';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {isApiError} from '../../../../models/api-error';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {CodeListsApiService} from '../../code-lists-api-service';
import {isCodeListSyncable} from '../../enums/code-list-source-type';
import {CodeListStatus} from '../../enums/code-list-status';
import {type CodeList} from '../../models/code-list';

const pollInterval = 1500;
const retryDelays = [3000, 6000, 12000];

export function CodeListSyncOutlet() {
    const {key} = useParams();
    const {item, setItem, isExistingItem, isNewItem, isBusy} = useGenericDetailsPageContext<CodeList, void>();
    const dispatch = useAppDispatch();

    useEffect(() => {
        if (isBusy || !isExistingItem || item == null || item.key !== key || !isCodeListSyncable(item.sourceType) ||
            (item.status !== CodeListStatus.SyncPending && item.status !== CodeListStatus.Syncing)) {
            return;
        }

        let active = true;
        let failures = 0;
        const api = new CodeListsApiService();
        const poll = async () => {
            try {
                const updated = await api.retrieve(item.key);
                if (!active) return;
                failures = 0;
                if (updated.status === item.status) {
                    timer = window.setTimeout(poll, pollInterval);
                    return;
                }
                // Only replace the server snapshot that initiated this read. A running save or a page
                // change may have supplied a newer one, and form drafts retain their own lock version.
                setItem(current => current === item ? updated : current);
            } catch (error) {
                if (!active) return;
                if (failures === 0) {
                    dispatch(showApiErrorSnackbar(error, 'Der Synchronisierungsstatus der Codeliste konnte nicht geladen werden.'));
                }
                const retryable = !isApiError(error) || error.status >= 500 || error.status === 408 || error.status === 429;
                const delay = retryDelays[failures++];
                if (retryable && delay != null) timer = window.setTimeout(poll, delay);
            }
        };
        let timer = window.setTimeout(poll, pollInterval);
        return () => {
            active = false;
            window.clearTimeout(timer);
        };
    }, [dispatch, isBusy, isExistingItem, item, key, setItem]);

    // During navigation the details context can still contain the previous list until the next GET returns.
    if (item == null || (isExistingItem && item.key !== key) || (isNewItem && item.id !== 0)) {
        return <GenericDetailsSkeleton/>;
    }
    return <Outlet key={key}/>;
}
