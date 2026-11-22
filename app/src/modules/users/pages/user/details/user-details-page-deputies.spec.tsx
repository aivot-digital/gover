import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../../components/generic-details-page/generic-details-page-context';
import {type GenericListProps} from '../../../../../components/generic-list/generic-list-props';
import {type DateFieldComponentProps} from '../../../../../components/date-field/date-field-component-props';
import {parseLocalDateIso} from '../../../../../utils/temporal-utils';
import {type User} from '../../../models/user';
import {type VUserDeputyWithDetailsEntity} from '../../../entities/v-user-deputy-with-details-entity';
import {VUserDeputyWithDetailsApiService} from '../../../services/v-user-deputy-with-details-api-service';
import {UserDeputyApiService} from '../../../services/user-deputy-api-service';
import {UserDetailsPageDeputies} from './user-details-page-deputies';

const testState = vi.hoisted(() => ({
    dispatch: vi.fn(),
    refreshPermissions: vi.fn(),
}));

const user: User = {
    id: 'original-user',
    email: 'original@example.com',
    firstName: 'Alex',
    lastName: 'Original',
    fullName: 'Alex Original',
    enabled: true,
    verified: true,
    deletedInIdp: false,
    systemRoleId: null,
};

const deputy: VUserDeputyWithDetailsEntity = {
    ...new VUserDeputyWithDetailsApiService().initialize(),
    id: 7,
    originalUserId: user.id,
    originalUserFullName: user.fullName,
    deputyUserId: 'deputy-user',
    deputyUserFullName: 'Kim Vertretung',
    fromDate: parseLocalDateIso('2026-11-01'),
    untilDate: parseLocalDateIso('2026-11-30'),
    lockVersion: 3,
};

vi.mock('../../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
    useRefreshPermissionSet: () => testState.refreshPermissions,
}));

vi.mock('../../../../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(),
}));

vi.mock('../../../dialogs/select-user-dialog', () => ({
    SelectUserDialog: () => null,
}));

vi.mock('../../../../../components/generic-list/generic-list', () => ({
    GenericList: ({rowActions}: GenericListProps<VUserDeputyWithDetailsEntity>) => (
        <>
            {rowActions?.(deputy).map((action) => typeof action !== 'string' && 'onClick' in action && (
                <button key={action.tooltip} onClick={action.onClick} disabled={action.disabled}>
                    {action.tooltip}
                </button>
            ))}
        </>
    ),
}));

vi.mock('../../../../../components/date-field/date-field-component', () => ({
    DateFieldComponent: ({label, value, onChange}: DateFieldComponentProps) => (
        <label>
            {label}
            <input type="date" value={value ?? ''} onChange={(event) => onChange(event.target.value ? parseLocalDateIso(event.target.value) : null)}/>
        </label>
    ),
}));

function renderDeputyEditor() {
    render(
        <GenericDetailsPageContext.Provider value={{
            item: user,
            setItem: vi.fn(),
            setAdditionalData: vi.fn(),
            isBusy: false,
            setIsBusy: vi.fn(),
            refresh: vi.fn(),
            subscribeEvent: () => () => {},
            isEditable: true,
        }}>
            <UserDetailsPageDeputies/>
        </GenericDetailsPageContext.Provider>,
    );
    fireEvent.click(screen.getByRole('button', {name: 'Stellvertretung bearbeiten'}));
    fireEvent.change(screen.getByLabelText('Ende der Vertretung'), {target: {value: '2026-12-31'}});
}

describe('UserDetailsPageDeputies', () => {
    beforeEach(() => {
        testState.dispatch.mockReset();
        testState.refreshPermissions.mockReset();
    });

    it('offers a persistent page reload after an edit conflict and retains the edited draft', async () => {
        const message = 'Der Datensatz wurde zwischenzeitlich geändert. Laden Sie ihn bitte neu, bevor Sie Ihre Änderungen erneut speichern.';
        const update = vi.spyOn(UserDeputyApiService.prototype, 'update').mockRejectedValue({
            status: 409,
            message,
            details: {reason: 'lock_version_conflict'},
            displayableToUser: true,
        });
        renderDeputyEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({
                message: `${message} Beim Neuladen gehen nicht gespeicherte Änderungen auf dieser Seite verloren.`,
                severity: 'error',
                type: 'dismissable',
                action: 'reloadPage',
            }),
        })));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByLabelText('Ende der Vertretung')).toHaveValue('2026-12-31');
        expect(testState.refreshPermissions).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalledTimes(2));
        expect(update).toHaveBeenLastCalledWith(7, {
            id: 7,
            originalUserId: user.id,
            deputyUserId: deputy.deputyUserId,
            fromDate: deputy.fromDate,
            untilDate: '2026-12-31',
            lockVersion: 3,
        });
    });

    it('keeps another conflict as an ordinary error without a reload action', async () => {
        const message = 'Die Stellvertretung kann im gewählten Zeitraum nicht gespeichert werden.';
        vi.spyOn(UserDeputyApiService.prototype, 'update').mockRejectedValue({
            status: 409,
            message,
            details: null,
            displayableToUser: true,
        });
        renderDeputyEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message, severity: 'error', type: 'autoHiding'}),
        })));
        expect(testState.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({action: 'reloadPage'}),
        }));
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByLabelText('Ende der Vertretung')).toHaveValue('2026-12-31');
    });
});
