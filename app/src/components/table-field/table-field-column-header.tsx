import {Box} from '@mui/material';

interface TableFieldColumnHeaderProps {
    label: string;
    optional?: boolean;
}

export function TableFieldColumnHeader(props: TableFieldColumnHeaderProps) {
    return (
        <Box
            component="span"
            sx={{
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
            }}
        >
            {props.label}
            {props.optional && (
                <>
                    {' '}
                    <Box
                        component="span"
                        sx={{
                            fontWeight: 400,
                            ml: 0.75,
                            opacity: 0.72,
                        }}
                    >
                        – optional
                    </Box>
                </>
            )}
        </Box>
    );
}
