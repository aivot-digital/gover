import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceTaskStatusIcon} from './process-instance-task-status-icon';

describe('task status icon', () => {
    it.each([null, '', '   ', 'Wartet auf Antragstellende'])(
        'shows the full identity label without redundant custom status text (%s)', async statusOverride => {
            render(<ProcessInstanceTaskStatusIcon
                status={ProcessTaskStatus.AwaitingCustomer}
                statusOverride={statusOverride}
                assignedCustomerIdentityLabel=" Antragstellende "
            />);
            await userEvent.hover(screen.getByLabelText('Wartet auf Antragstellende'));
            expect(await screen.findByRole('tooltip')).toHaveTextContent(/^Wartet auf Antragstellende$/);
        },
    );

    it('explains the identity-based system status behind a custom status', async () => {
        const tooltip = 'Unterlagen angefordert (Systemstatus: Wartet auf Antragstellende)';
        render(<ProcessInstanceTaskStatusIcon
            status={ProcessTaskStatus.AwaitingCustomer}
            statusOverride=" Unterlagen angefordert "
            assignedCustomerIdentityLabel="Antragstellende"
        />);
        await userEvent.hover(screen.getByLabelText(tooltip));
        expect(await screen.findByRole('tooltip')).toHaveTextContent(tooltip);
    });

    it('keeps the generic tooltip without an assigned identity', async () => {
        render(<ProcessInstanceTaskStatusIcon status={ProcessTaskStatus.AwaitingCustomer} />);
        await userEvent.hover(screen.getByLabelText('Wartet auf Rückmeldung'));
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Wartet auf Rückmeldung');
    });
});
