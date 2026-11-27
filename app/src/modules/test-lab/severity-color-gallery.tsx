import {useMemo} from 'react';
import {
    Alert,
    Box,
    Button,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    ThemeProvider,
    Typography,
} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';
import {createAppTheme} from '../../theming/themes';

const severityPreviewModes = [
    {mode: 'light', label: 'Hell'},
    {mode: 'dark', label: 'Dunkel'},
] as const;

const severityExamples = [
    {color: 'error', label: 'Fehler'},
    {color: 'warning', label: 'Warnung'},
    {color: 'info', label: 'Information'},
    {color: 'success', label: 'Erfolg'},
] as const;

const alertVariants = [
    {variant: 'standard', label: 'Meldung'},
    {variant: 'outlined', label: 'Meldung mit Kontur'},
    {variant: 'filled', label: 'Gefüllte Meldung'},
] as const;

const buttonVariants = [
    {variant: 'text', label: 'Textschaltfläche'},
    {variant: 'outlined', label: 'Schaltfläche mit Kontur'},
    {variant: 'contained', label: 'Gefüllte Schaltfläche'},
] as const;

function SeverityColorTable({labelId}: {labelId: string}) {
    return (
        <TableContainer>
            <Table
                aria-labelledby={labelId}
                size="small"
                sx={{
                    minWidth: 640,
                    tableLayout: 'fixed',
                    '& tbody tr:last-child th': {borderBottom: 0},
                }}
            >
                <TableHead>
                    <TableRow>
                        <TableCell scope="col">Variante</TableCell>
                        {severityExamples.map(({color, label}) => (
                            <TableCell key={color} scope="col">{label}</TableCell>
                        ))}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {alertVariants.map(({variant, label}) => (
                        <TableRow key={variant}>
                            <TableCell component="th" scope="row">{label}</TableCell>
                            {severityExamples.map(({color, label: severityLabel}) => (
                                <TableCell key={color}>
                                    <Alert severity={color} variant={variant} role="presentation" sx={{px: 1.25, py: 0}}>
                                        {severityLabel}
                                    </Alert>
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                    {buttonVariants.map(({variant, label}) => (
                        <TableRow key={variant}>
                            <TableCell component="th" scope="row">{label}</TableCell>
                            {severityExamples.map(({color, label: severityLabel}) => (
                                <TableCell key={color}>
                                    <Button color={color} variant={variant}>
                                        {severityLabel}
                                    </Button>
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}

export function SeverityColorGallery() {
    const previews = useMemo(() => severityPreviewModes.map(({mode, label}) => ({
        label,
        id: `test-lab-severity-colors-${mode}`,
        theme: createAppTheme(AppConfig.systemTheme ?? undefined, BaseTheme, mode),
    })), []);

    return (
        <Box component="section" aria-labelledby="test-lab-severity-colors">
            <Typography id="test-lab-severity-colors" component="h3" variant="h6" sx={{mb: 1.5}}>
                Signalfarben
            </Typography>
            <Stack spacing={1.5}>
                {previews.map(({id, label, theme}) => (
                    <ThemeProvider key={id} theme={theme}>
                        <Box sx={{
                            bgcolor: 'background.paper',
                            color: 'text.primary',
                            border: 1,
                            borderColor: 'divider',
                            borderRadius: 1,
                            p: 1,
                        }}>
                            <Typography id={id} component="h4" variant="subtitle1" sx={{px: 2, mb: 0.5}}>
                                {label}
                            </Typography>
                            <SeverityColorTable labelId={id}/>
                        </Box>
                    </ThemeProvider>
                ))}
            </Stack>
        </Box>
    );
}
