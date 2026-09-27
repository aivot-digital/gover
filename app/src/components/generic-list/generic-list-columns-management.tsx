import {Box, Button} from '@mui/material';
import {GridColumnsManagement, type GridColumnsManagementProps} from '@mui/x-data-grid';

declare module '@mui/x-data-grid' {
    interface ColumnsManagementPropsOverrides {
        onRestoreDefaults?: () => void;
    }
}

export function GenericListColumnsManagement({
    onRestoreDefaults,
    ...props
}: GridColumnsManagementProps & {
    onRestoreDefaults?: () => void;
}) {
    return (
        <>
            <GridColumnsManagement
                {...props}
                disableResetButton
            />
            <Box
                sx={{
                    px: 1.5,
                    pb: 1.5,
                }}
            >
                <Button onClick={onRestoreDefaults}>Standardspalten wiederherstellen</Button>
            </Box>
        </>
    );
}
