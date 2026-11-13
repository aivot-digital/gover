import {describe, expect, it} from 'vitest';
import {isLockVersionConflictError} from './api-error';

const conflict = {
    status: 409,
    message: 'Der Datensatz wurde zwischenzeitlich geändert. Laden Sie ihn bitte neu, bevor Sie Ihre Änderungen erneut speichern.',
    details: {reason: 'lock_version_conflict'},
    displayableToUser: true,
};

describe('isLockVersionConflictError', () => {
    it('detects a lock version conflict', () => {
        expect(isLockVersionConflictError(conflict)).toBe(true);
    });

    it.each([
        ['another conflict reason', {...conflict, details: {reason: 'assigned_tasks_lose_access'}}],
        ['a conflict without details', {...conflict, details: null}],
        ['another status', {...conflict, status: 400}],
        ['no api error', new Error('Netzwerkfehler')],
    ])('ignores %s', (_, error) => {
        expect(isLockVersionConflictError(error)).toBe(false);
    });
});
