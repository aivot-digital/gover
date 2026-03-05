import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from './shell-slice';
import {isApiError} from '../models/api-error';

export const showSuccessSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Success,
        type: SnackbarType.AutoHiding,
    });
};

export const showErrorSnackbar = (message: string, persist: boolean = false) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Error,
        type: persist ? SnackbarType.Dismissable : SnackbarType.AutoHiding,
    });
};

export const showApiErrorSnackbar = (error: any, defaultMessage: string, persist: boolean = false) => {
    if (isApiError(error) && error.displayableToUser) {
        return showErrorSnackbar(error.message, persist);
    }
    console.error(error);
    return showErrorSnackbar(defaultMessage, persist);
};

export const LOADING_TOAST_SNACKBAR_KEY = 'loading-toast';

export const showLoadingSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: LOADING_TOAST_SNACKBAR_KEY,
        message: message,
        severity: SnackbarSeverity.Info,
        type: SnackbarType.Loading,
    });
};

export const removeSnackbar = removeSnackbarMessage;

export const removeLoadingSnackbar = () => {
    return removeSnackbarMessage(LOADING_TOAST_SNACKBAR_KEY);
};
