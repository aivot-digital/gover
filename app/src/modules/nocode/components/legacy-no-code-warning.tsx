import {Alert, AlertTitle, Box, Button} from '@mui/material';
import {LEGACY_NO_CODE_NOTICE} from '../data/legacy-no-code-notice';

export function LegacyNoCodeWarning({onStart}: { onStart?: () => void }) {
    return <Alert severity="warning"
                  sx={{'& .MuiAlert-message': {minWidth: 0}}}>
        <AlertTitle>No-Code-Regel umstellen</AlertTitle>
        <Box sx={{maxWidth: 820}}>{LEGACY_NO_CODE_NOTICE}</Box>
        {onStart != null && <Button variant="outlined"
                                    color="inherit"
                                    sx={{mt: 1.5}}
                                    onClick={onStart}>
            Umstellung beginnen
        </Button>}
    </Alert>;
}
