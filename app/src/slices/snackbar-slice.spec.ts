import {describe, expect, it} from 'vitest';
import {showApiErrorSnackbar} from './snackbar-slice';
import {SnackbarAction, SnackbarSeverity, SnackbarType} from './shell-slice';

const conflict = {
    status: 409,
    message: 'Der Datensatz wurde zwischenzeitlich geändert.',
    details: {reason: 'lock_version_conflict'},
    displayableToUser: true,
};

describe('showApiErrorSnackbar', () => {
    it('offers to reload the page for a lock version conflict and keeps the message until dismissed', () => {
        const action = showApiErrorSnackbar(conflict, 'Speichern fehlgeschlagen.');

        expect(action.payload).toMatchObject({
            message: 'Der Datensatz wurde zwischenzeitlich geändert. Beim Neuladen gehen nicht gespeicherte Änderungen auf dieser Seite verloren.',
            severity: SnackbarSeverity.Error,
            type: SnackbarType.Dismissable,
            action: SnackbarAction.ReloadPage,
        });
    });

    it('shows other conflicts without the reload action', () => {
        const action = showApiErrorSnackbar({...conflict, details: null}, 'Speichern fehlgeschlagen.');

        expect(action.payload).toMatchObject({message: conflict.message, severity: SnackbarSeverity.Error});
        expect(action.payload).not.toHaveProperty('action');
    });
});
