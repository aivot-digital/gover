import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import type {TaskViewEvent} from '../services/process-instance-task-api-service';
import {TaskViewEventButtons} from './task-view-event-buttons';

describe('TaskViewEventButtons', () => {
    it('renders left events before right events and applies the configured button styles', () => {
        render(
            <TaskViewEventButtons
                events={[
                    createEvent('right', 'Rechts', {
                        alignment: 'right',
                        color: 'error',
                        variant: 'outlined',
                    }),
                    createEvent('left', 'Links'),
                    createEvent('success', 'Erfolg', {
                        color: 'success',
                        variant: 'text',
                    }),
                ]}
                onEvent={vi.fn()}
            />,
        );

        const buttons = screen.getAllByRole('button');
        expect(buttons.map((button) => button.textContent)).toEqual(['Links', 'Erfolg', 'Rechts']);
        expect(screen.getByRole('button', {name: 'Links'})).toHaveClass('MuiButton-contained', 'MuiButton-colorPrimary');
        expect(screen.getByRole('button', {name: 'Erfolg'})).toHaveClass('MuiButton-text', 'MuiButton-colorSuccess');
        expect(screen.getByRole('button', {name: 'Rechts'})).toHaveClass('MuiButton-outlined', 'MuiButton-colorError');
    });

    it('passes the complete event metadata to the click handler', async () => {
        const onEvent = vi.fn();
        const event = createEvent('submit', 'Daten einreichen');
        const user = userEvent.setup();

        render(
            <TaskViewEventButtons
                events={[event]}
                onEvent={onEvent}
            />,
        );

        await user.click(screen.getByRole('button', {name: 'Daten einreichen'}));

        expect(onEvent).toHaveBeenCalledOnce();
        expect(onEvent).toHaveBeenCalledWith(event);
    });

    it('renders nothing when no events are available', () => {
        const {container} = render(
            <TaskViewEventButtons
                events={[]}
                onEvent={vi.fn()}
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });
});

function createEvent(event: string, label: string, overrides?: Partial<TaskViewEvent>): TaskViewEvent {
    return {
        event,
        label,
        ...overrides,
    };
}
