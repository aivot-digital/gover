import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessAssignmentButton} from './process-assignment-button';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {Permission} from '../../../data/permissions/permission';

const access = vi.hoisted(() => ({
    allowed: true,
    check: vi.fn(),
}));
vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (...args: unknown[]) => {
        access.check(...args);
        return access.allowed;
    },
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

describe('ProcessAssignmentButton', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        access.allowed = true;
        access.check.mockClear();
    });

    it.each([false, true])('loads and saves the correct resource (task: %s)', async (isTask) => {
        const api = isTask ? ProcessInstanceTaskApiService.prototype : ProcessInstanceApiService.prototype;
        const options = vi.spyOn(api, 'assignmentOptions').mockResolvedValue([
            {
                id: 'kim',
                name: 'Kim Beispiel',
                email: null,
            },
        ]);
        const reassign = vi.spyOn(api, 'reassign').mockResolvedValue({} as never);
        const refreshed = vi.fn();
        render(
            <ProcessAssignmentButton
                instanceId={17}
                taskId={isTask ? 5 : undefined}
                taskStatus={ProcessTaskStatus.Running}
                instanceStatus={ProcessInstanceStatus.Running}
                assignedUserId="former"
                onAssigned={refreshed}
            />,
        );
        expect(options).not.toHaveBeenCalled();
        expect(access.check).toHaveBeenCalledWith(
            17,
            isTask ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_REASSIGN,
        );
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: isTask ? 'Aufgabe zuweisen' : 'Vorgang zuweisen'}));
        expect(screen.getByRole('dialog')).toHaveAccessibleDescription(
            isTask
                ? 'Wählen Sie, wer diese Aufgabe bearbeiten soll. Sie können Mitarbeiter:innen mit aktivem Konto auswählen, die den Vorgang einsehen und Aufgaben bearbeiten dürfen. Diese Rechte müssen auch ohne Stellvertretung bestehen.'
                : 'Wählen Sie, wer für diesen Vorgang zuständig sein soll. Aufgaben werden separat zugewiesen. Sie können Mitarbeiter:innen mit aktivem Konto auswählen, die den Vorgang auch ohne Stellvertretung einsehen dürfen.',
        );
        const input = screen.getByRole('combobox', {name: /Zugewiesen an/});
        await waitFor(() => expect(input).not.toBeDisabled());
        if (isTask) {
            expect(screen.queryByRole('button', {name: 'Zuweisung aufheben'})).not.toBeInTheDocument();
            expect(screen.getByRole('alert')).toHaveTextContent(
                'Die bisher zugewiesene Person steht nicht mehr zur Auswahl. Bitte wählen Sie eine andere Person aus.',
            );
        } else {
            expect(screen.getByRole('button', {name: 'Zuweisung aufheben'})).toBeEnabled();
        }
        await user.click(input);
        await user.click(await screen.findByRole('option', {name: 'Kim Beispiel'}));
        await user.click(screen.getByRole('button', {name: 'Zuweisung speichern'}));
        expect(options).toHaveBeenCalledWith(isTask ? 5 : 17);
        expect(reassign).toHaveBeenCalledWith(isTask ? 5 : 17, 'kim');
        expect(refreshed).toHaveBeenCalledOnce();
    });

    it('does not load candidates without permission or for completed tasks', () => {
        access.allowed = false;
        const options = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'assignmentOptions');
        const {rerender} = render(
            <ProcessAssignmentButton
                instanceId={17}
                taskId={5}
                taskStatus={ProcessTaskStatus.Running}
                assignedUserId={null}
                onAssigned={vi.fn()}
            />,
        );
        expect(screen.getByRole('button')).toBeDisabled();
        access.allowed = true;
        rerender(
            <ProcessAssignmentButton
                instanceId={17}
                taskId={5}
                taskStatus={ProcessTaskStatus.Completed}
                assignedUserId={null}
                onAssigned={vi.fn()}
            />,
        );
        expect(screen.getByRole('button')).toBeDisabled();
        expect(options).not.toHaveBeenCalled();
    });

    it.each([ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted])(
        'disables assignment for %s instances and explains why',
        async (status) => {
            const options = vi.spyOn(ProcessInstanceApiService.prototype, 'assignmentOptions');
            render(
                <ProcessAssignmentButton
                    instanceId={17}
                    instanceStatus={status}
                    assignedUserId="former"
                    onAssigned={vi.fn()}
                />,
            );
            const button = screen.getByRole('button', {name: 'Vorgang zuweisen'});
            expect(button).toBeDisabled();
            await userEvent.setup().hover(button.parentElement!);
            expect(await screen.findByRole('tooltip')).toHaveTextContent(
                'Die Zuweisung abgeschlossener oder abgebrochener Vorgänge kann nicht mehr geändert werden.',
            );
            expect(options).not.toHaveBeenCalled();
        },
    );

    it.each([false, true])('names the required permission in the disabled tooltip (task: %s)', async (isTask) => {
        access.allowed = false;
        render(
            <ProcessAssignmentButton
                instanceId={17}
                taskId={isTask ? 5 : undefined}
                taskStatus={ProcessTaskStatus.Running}
                assignedUserId={null}
                onAssigned={vi.fn()}
            />,
        );
        const button = screen.getByRole('button', {name: isTask ? 'Aufgabe zuweisen' : 'Vorgang zuweisen'});
        expect(button).toBeDisabled();
        await userEvent.setup().hover(button.parentElement!);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent(
            isTask ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_REASSIGN,
        );
    });
});
