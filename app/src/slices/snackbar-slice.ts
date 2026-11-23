import {addSnackbarMessage, removeSnackbarMessage, SnackbarAction, SnackbarSeverity, SnackbarType} from './shell-slice';
import {isApiError, isLockVersionConflictError} from '../models/api-error';

export const showSuccessSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Success,
        type: SnackbarType.AutoHiding,
    });
};

export const showWarningSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message,
        severity: SnackbarSeverity.Warning,
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

/**
 * Shows a conflict with a newer saved state and offers to reload the page. Reloading is the one recovery that works on
 * every page: the edited copies of pages, dialogs and editors keep the outdated lock version until they are reloaded.
 */
export const showLockConflictSnackbar = (message: string) => {
    return addSnackbarMessage({
        key: new Date().getTime().toString(),
        message: `${message} Beim Neuladen gehen nicht gespeicherte Änderungen auf dieser Seite verloren.`,
        severity: SnackbarSeverity.Error,
        type: SnackbarType.Dismissable,
        action: SnackbarAction.ReloadPage,
    });
};

export const showApiErrorSnackbar = (error: any, defaultMessage: string, persist: boolean = false) => {
    if (isLockVersionConflictError(error)) {
        return showLockConflictSnackbar(error.message);
    }
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
