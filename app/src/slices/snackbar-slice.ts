import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from './shell-slice';

export const showSuccessSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Success,
        type: SnackbarType.AutoHiding,
    });
};

export const showErrorSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Error,
        type: SnackbarType.AutoHiding,
    });
}

export const LOADING_TOAST_SNACKBAR_KEY = 'loading-toast';

export const showLoadingSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: LOADING_TOAST_SNACKBAR_KEY,
        message: message,
        severity: SnackbarSeverity.Info,
        type: SnackbarType.Loading,
    });
}

export const removeSnackbar = removeSnackbarMessage;

export const removeLoadingSnackbar = () => {
    return removeSnackbarMessage(LOADING_TOAST_SNACKBAR_KEY);
}
