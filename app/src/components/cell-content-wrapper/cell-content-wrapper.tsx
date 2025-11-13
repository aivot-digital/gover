import {ReactNode} from 'react';
import {Box, SxProps, Theme} from '@mui/material';

interface CellContentWrapperProps extends React.ComponentPropsWithoutRef<typeof Box> {
    children: ReactNode;
    sx?: SxProps<Theme>;
}

export function CellContentWrapper({children, sx, ...otherProps }: CellContentWrapperProps) {
    return (
        <Box
            display={'flex'}
            alignItems={'center'}
            sx={[
                { height: '100%' },
                ...(Array.isArray(sx) ? sx : [sx]),
            ]}
            {...otherProps}
        >
            {children}
        </Box>
    );
}
