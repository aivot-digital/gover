import {useCallback} from 'react';
import {addSnackbarMessage, SnackbarSeverity, SnackbarType} from '../slices/shell-slice';
import {useAppDispatch} from './use-app-dispatch';

export function useNotImplemented() {
    const dispatch = useAppDispatch();

    return useCallback(() => {
        dispatch(addSnackbarMessage({
            type: SnackbarType.AutoHiding,
            severity: SnackbarSeverity.Info,
            key: 'not-implemented-' + new Date().getTime(),
            message: 'Diese Funktion ist noch nicht implementiert.',
        }));
    }, []);
}