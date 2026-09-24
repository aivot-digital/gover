import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {ProcessListActions} from './process-list-actions';
import {ProcessInstanceListEntry, ProcessTaskListEntry} from '../entities/process-list';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {Permission} from '../../../data/permissions/permission';
import {PermissionSet} from '../../permissions/models/permission-set';

const access = vi.hoisted(() => ({
    permissions: undefined as PermissionSet | undefined,
    dispatch: vi.fn(),
    confirm: vi.fn(),
}));
vi.mock('../../../hooks/use-app-selector', () => ({useAppSelector: () => access.permissions}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => access.dispatch}));
vi.mock('../../../providers/confirm-provider', () => ({useConfirm: () => access.confirm}));
const instance: ProcessInstanceListEntry = {
    id: 17,
    caseNumber: 'ABC-100',
    assignedFileNumbers: [],
    processId: 10,
    processVersion: 2,
    processName: 'Anmeldung',
    assignedUserId: null,
    assignedUserName: null,
    status: ProcessInstanceStatus.Failed,
    statusOverride: null,
    started: '',
    finished: null,
    test: false,
};
const allPermissions = [
    Permission.PROCESS_INSTANCE_REASSIGN,
    Permission.PROCESS_INSTANCE_EDIT_TASK,
    Permission.PROCESS_INSTANCE_UPDATE,
    Permission.PROCESS_INSTANCE_DELETE,
    Permission.PROCESS_DEFINITION_READ,
];
function grant(scope: 'system' | 'instance', id = 17) {
    access.permissions = {
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions:
            scope === 'instance'
                ? [
                      {
                          id: 'grant',
                          userId: 'me',
                          processInstanceId: id,
                          permissions: allPermissions,
                      },
                  ]
                : [],
        systemPermissions:
            scope === 'system'
                ? [
                      {
                          userId: 'me',
                          permissions: allPermissions,
                      },
                  ]
                : [],
    };
}
function showActions(item: ProcessInstanceListEntry | ProcessTaskListEntry = instance) {
    render(
        <MemoryRouter>
            <ProcessListActions
                item={item}
                onChanged={vi.fn()}
            />
        </MemoryRouter>,
    );
    fireEvent.click(screen.getByRole('button', {name: 'Weitere Aktionen'}));
}

describe('Process list actions', () => {
    beforeEach(() => {
        access.permissions = undefined;
    });
    it('keeps instance and task navigation available without offering unauthorized mutations or model access', () => {
        showActions();
        expect(screen.getByRole('menuitem', {name: 'Alle Aufgaben des Vorgangs'})).toHaveAttribute(
            'href',
            '/processes/10/versions/2/instances/17/tasks',
        );
        expect(screen.queryByRole('menuitem', {name: 'Vorgang zuweisen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Vorgang löschen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Im Prozessmodell ansehen'})).not.toBeInTheDocument();
    });
    it.each(['instance', 'system'] as const)('allows mutations with the matching %s grant', (scope) => {
        grant(scope);
        showActions();
        expect(screen.getByRole('menuitem', {name: 'Vorgang zuweisen'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Vorgang löschen'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Erneut starten'})).toBeInTheDocument();
        if (scope === 'system') {
            expect(screen.getByRole('menuitem', {name: 'Im Prozessmodell ansehen'})).toHaveAttribute(
                'href',
                '/processes/10/versions/2/?instanceId=17',
            );
        } else {
            expect(screen.queryByRole('menuitem', {name: 'Im Prozessmodell ansehen'})).not.toBeInTheDocument();
        }
    });
    it('does not use grants for another instance', () => {
        grant('instance', 18);
        showActions();
        expect(screen.queryByRole('menuitem', {name: 'Vorgang zuweisen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Erneut starten'})).not.toBeInTheDocument();
    });
    it('offers task assignment only while active and preserves the instance link', () => {
        grant('system');
        const task: ProcessTaskListEntry = {
            ...instance,
            id: 23,
            processInstanceId: 17,
            status: ProcessTaskStatus.Completed,
            taskName: 'Prüfen',
            taskType: null,
            description: null,
            deadline: null,
        };
        showActions(task);
        expect(screen.getByRole('menuitem', {name: 'Vorgang aufrufen'})).toHaveAttribute(
            'href',
            '/process-instances/17',
        );
        expect(screen.getByRole('menuitem', {name: 'Technische Aufgabendaten'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Aufgabe zuweisen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Erneut starten'})).not.toBeInTheDocument();
    });
});
