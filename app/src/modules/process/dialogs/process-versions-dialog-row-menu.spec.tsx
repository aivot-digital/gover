import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {type ProcessEntity} from '../entities/process-entity';
import {type ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus} from '../enums/process-status';
import {ProcessVersionsDialogRowMenu} from './process-versions-dialog-row-menu';

const mocks = vi.hoisted(() => ({permissions: new Set<string>()}));
vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: (_id: number, permission: string) => mocks.permissions.has(permission),
    useHasAnyDepartmentPermission: (permission: string) => mocks.permissions.has(permission),
}));

function renderMenu(status: ProcessStatus, draftedVersion: number | null) {
    render(
        <ProcessVersionsDialogRowMenu
            anchorEl={document.body}
            onClose={vi.fn()}
            process={{id: 7, draftedVersion} as ProcessEntity}
            processVersion={{processVersion: 1, status} as ProcessVersionEntity}
            onPublishVersion={vi.fn()}
            onRevokeVersion={vi.fn()}
            onReuseVersionAsDraft={vi.fn()}
            onReuseVersionAsNewProcess={vi.fn()}
            onExportVersion={vi.fn()}
            onDeleteVersion={vi.fn()}
        />,
    );
}

const item = (name: string) => screen.getByRole('menuitem', {name});

describe('ProcessVersionsDialogRowMenu', () => {
    beforeEach(() => {
        mocks.permissions = new Set();
    });

    it('leaves only export enabled for read-only users', () => {
        renderMenu(ProcessStatus.Drafted, 1);

        expect(item('Veröffentlichen')).toHaveAttribute('aria-disabled', 'true');
        expect(item('Als neuen Prozess kopieren')).toHaveAttribute('aria-disabled', 'true');
        expect(item('Entwurf löschen')).toHaveAttribute('aria-disabled', 'true');
        expect(item('Als Prozess exportieren')).not.toHaveAttribute('aria-disabled');
    });

    it('enables revoke with the publish permission and reuse with the update permission', () => {
        mocks.permissions = new Set([
            Permission.PROCESS_DEFINITION_PUBLISH_LOCAL,
            Permission.PROCESS_DEFINITION_UPDATE,
        ]);
        renderMenu(ProcessStatus.Published, null);

        expect(item('Zurückziehen')).not.toHaveAttribute('aria-disabled');
        expect(item('Als Entwurf verwenden')).not.toHaveAttribute('aria-disabled');
    });

    it('does not treat the update permission as permission to publish', () => {
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderMenu(ProcessStatus.Published, null);

        expect(item('Zurückziehen')).toHaveAttribute('aria-disabled', 'true');
    });
});
