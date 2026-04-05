import React, {type ReactNode} from 'react';
import {Box, Typography} from '@mui/material';
import {AlertComponent} from '../../../../components/alert/alert-component';

export function ProcessTaskViewPageCommunication(): ReactNode {
    return (
        <Box
            sx={{
                pt: 1,
            }}
        >
            <Typography variant="h5">
                Kommunikation
            </Typography>

            <AlertComponent
                title="Kommunikation ist vorgesehen"
                text="Der Kommunikationsverlauf für Aufgaben ist bereits in der Seitenstruktur eingeplant, bleibt aber bis zur fachlichen Umsetzung deaktiviert."
                color="info"
                sx={{mt: 2}}
            />
        </Box>
    );
}
