import {Stack, type SxProps, Typography, type Theme} from '@mui/material';
import React, {type ReactNode} from 'react';
import EmptyStateIllustration from './empty-state-illustration.svg?react';

interface EmptyStateProps {
    title: ReactNode;
    description?: ReactNode;
    illustration?: ReactNode;
    actions?: ReactNode;
    sx?: SxProps<Theme>;
}

export function EmptyStateSection(props: EmptyStateProps): ReactNode {
    const {
        title,
        description,
        illustration,
        actions,
        sx,
    } = props;

    return (
        <Stack
            direction="column"
            sx={{
                gap: 2,
                alignItems: 'center',
                textAlign: 'center',
                maxWidth: 520,
                margin: '40px auto 48px auto',
                ...sx
            }}>
            {illustration ?? <EmptyStateIllustration />}

            <Typography
                variant="h2"
                component="h2"
            >
                {title}
            </Typography>

            {
                description != null &&
                <Typography sx={{
                    color: "text.secondary"
                }}>
                    {description}
                </Typography>
            }

            {
                actions != null &&
                <Stack
                    direction="row"
                    sx={{
                        gap: 2,
                        marginTop: 1.5,
                        flexWrap: 'wrap',
                        justifyContent: 'center'
                    }}>
                    {actions}
                </Stack>
            }
        </Stack>
    );
}
