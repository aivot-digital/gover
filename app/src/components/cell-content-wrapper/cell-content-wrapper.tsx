import {ReactNode} from 'react';
import {Box, SxProps, Theme} from '@mui/material';

interface CellContentWrapperProps extends React.ComponentPropsWithoutRef<typeof Box> {
    children: ReactNode;
    sx?: SxProps<Theme>;
}

export function CellContentWrapper({children, sx, ...otherProps }: CellContentWrapperProps) {
    return (
        <Box
            {...otherProps}
            sx={[{
                display: 'flex',
                alignItems: 'center'
            }, { height: '100%' }, ...(Array.isArray(sx) ? sx : [sx])]}>
            {children}
        </Box>
    );
}
