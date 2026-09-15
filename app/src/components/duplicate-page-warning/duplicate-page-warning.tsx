import {useEffect} from 'react';
import {useLocation, useMatches} from 'react-router-dom';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useDuplicatePageWarning} from '../../hooks/use-duplicate-page-warning';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../slices/shell-slice';
import {hasDuplicatePageWarningRouteHandle} from './duplicate-page-warning-route-handle';

const DUPLICATE_PAGE_WARNING_SNACKBAR_KEY = 'duplicate-page-warning';

export function DuplicatePageWarning() {
    const dispatch = useAppDispatch();
    const location = useLocation();
    const matches = useMatches();
    const duplicatePageWarningMatch = [...matches]
        .reverse()
        .find(match => hasDuplicatePageWarningRouteHandle(match.handle));
    const pageKey = duplicatePageWarningMatch == null ? null : duplicatePageWarningMatch.pathname + location.search;
    const hasDuplicatePageOpen = useDuplicatePageWarning(pageKey);

    useEffect(() => {
        if (hasDuplicatePageOpen) {
            dispatch(addSnackbarMessage({
                key: DUPLICATE_PAGE_WARNING_SNACKBAR_KEY,
                message: 'Diese Seite ist bereits in einem anderen Tab geöffnet. Änderungen können sich gegenseitig überschreiben.',
                severity: SnackbarSeverity.Warning,
                type: SnackbarType.Dismissable,
            }));
        } else {
            dispatch(removeSnackbarMessage(DUPLICATE_PAGE_WARNING_SNACKBAR_KEY));
        }
    }, [dispatch, hasDuplicatePageOpen]);

    useEffect(() => {
        return () => {
            dispatch(removeSnackbarMessage(DUPLICATE_PAGE_WARNING_SNACKBAR_KEY));
        };
    }, [dispatch]);

    return null;
}
