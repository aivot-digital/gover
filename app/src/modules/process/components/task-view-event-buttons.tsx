import {Box, Button, Stack} from '@mui/material';
import type {
    TaskViewEvent,
    TaskViewEventAlignment,
    TaskViewEventColor,
    TaskViewEventVariant,
} from '../services/process-instance-task-api-service';

interface TaskViewEventButtonsProps {
    events: TaskViewEvent[];
    onEvent: (event: TaskViewEvent) => void | Promise<void>;
}

export function TaskViewEventButtons({events, onEvent}: TaskViewEventButtonsProps) {
    if (events.length === 0) {
        return null;
    }

    const leftAlignedEvents = events.filter((event) => getTaskViewEventAlignment(event) === 'left');
    const rightAlignedEvents = events.filter((event) => getTaskViewEventAlignment(event) === 'right');

    return (
        <Box
            sx={{
                mt: 4,
                display: 'flex',
                flexDirection: {
                    xs: 'column',
                    sm: 'row',
                },
                gap: 2,
                justifyContent: 'space-between',
                alignItems: {
                    sm: 'center',
                },
            }}
        >
            {
                leftAlignedEvents.length > 0 &&
                <TaskViewEventButtonGroup
                    events={leftAlignedEvents}
                    onEvent={onEvent}
                />
            }

            {
                rightAlignedEvents.length > 0 &&
                <TaskViewEventButtonGroup
                    events={rightAlignedEvents}
                    onEvent={onEvent}
                    rightAligned
                />
            }
        </Box>
    );
}

interface TaskViewEventButtonGroupProps extends TaskViewEventButtonsProps {
    rightAligned?: boolean;
}

function TaskViewEventButtonGroup({events, onEvent, rightAligned}: TaskViewEventButtonGroupProps) {
    return (
        <Stack
            direction={{
                xs: 'column',
                sm: 'row',
            }}
            spacing={2}
            sx={{
                width: {
                    xs: '100%',
                    sm: 'auto',
                },
                marginLeft: rightAligned ? {
                    sm: 'auto',
                } : undefined,
            }}
        >
            {
                events.map((event) => (
                    <Button
                        key={event.event}
                        variant={getTaskViewEventVariant(event)}
                        color={getTaskViewEventColor(event)}
                        onClick={() => {
                            void onEvent(event);
                        }}
                        sx={{
                            width: {
                                xs: '100%',
                                sm: 'auto',
                            },
                        }}
                    >
                        {event.label}
                    </Button>
                ))
            }
        </Stack>
    );
}

function getTaskViewEventVariant(event: TaskViewEvent): TaskViewEventVariant {
    if (event.variant === 'outlined' || event.variant === 'text') {
        return event.variant;
    }

    return 'contained';
}

function getTaskViewEventColor(event: TaskViewEvent): TaskViewEventColor {
    if (event.color === 'secondary' || event.color === 'error' || event.color === 'success') {
        return event.color;
    }

    return 'primary';
}

function getTaskViewEventAlignment(event: TaskViewEvent): TaskViewEventAlignment {
    if (event.alignment === 'right') {
        return event.alignment;
    }

    return 'left';
}
