import {useState} from 'react';
import {VisuallyHidden} from 'react-aria-components';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {Box, Button, Dialog, DialogActions, DialogContent, Tooltip, Typography} from '@mui/material';
import {NoCodeDryRunResultDTO, NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {formatNoCodeValue, noCodeResultType} from '../utils/format-no-code-value';
import {NoCodeDataTypeLabels} from '../../../data/no-code-data-type';

export function NoCodeTestResult({
                                     result,
                                     id,
                                     compact = false,
                                 }: { result: NoCodeDryRunResultDTO; id?: string; compact?: boolean }) {
    const [details, setDetails] = useState(false);
    const evaluated = result.status === NoCodeDryRunStatus.Evaluated;
    const text = evaluated ? formatNoCodeValue(result.value, result.valueType) : `${result.status === NoCodeDryRunStatus.Failed ? 'Fehler' : 'Nicht ausgewertet'}: ${result.message ?? 'Kein Ergebnis verfügbar.'}`;
    const typeLabel = result.valueType == null ? noCodeResultType(result.value) : NoCodeDataTypeLabels[result.valueType];
    const complex = evaluated && result.value != null && typeof result.value === 'object';
    const expandable = evaluated && (text.length > (compact ? 60 : 160) || complex || text.includes('\n'));
    const short = expandable ? `${text.slice(0, compact ? 59 : 159)}${text.length > (compact ? 59 : 159) ? '…' : ''}` : text;
    return <Box sx={compact ? {
        display: 'inline-flex',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 0.5,
        ml: 0.75,
        pl: 1,
        minWidth: 0,
        maxWidth: '100%',
        borderLeft: 1,
        borderColor: 'divider',
    } : undefined}
                onKeyDown={(event) => {
                    if (details || event.target instanceof HTMLElement && event.target.closest('button')) event.stopPropagation();
                }}
                onClick={(event) => event.stopPropagation()}>
        {compact && <Tooltip arrow
                             title="Testergebnis"><Science aria-hidden
                                                           sx={{
                                                               fontSize: 16,
                                                               color: 'warning.main',
                                                               flexShrink: 0,
                                                           }}/></Tooltip>}
        <Typography id={id}
                    variant={compact ? 'body2' : 'subtitle1'}
                    color={result.status === NoCodeDryRunStatus.Failed ? 'error' : compact ? 'text.secondary' : 'text.primary'}
                    sx={{
                        overflowWrap: 'anywhere',
                        whiteSpace: 'pre-wrap', ...(!compact && {
                            p: 1.5,
                            bgcolor: 'background.default',
                            borderRadius: 1,
                            fontWeight: evaluated ? 500 : undefined,
                        }),
                    }}>
            {compact && evaluated &&
                <VisuallyHidden elementType="span">Testergebnis:</VisuallyHidden>}{compact && evaluated ? ' ' : ''}{short}
        </Typography>
        {!compact && evaluated && <Typography variant="caption"
                                              color="text.secondary"
                                              sx={{
                                                  display: 'block',
                                                  mt: 0.5,
                                              }}>Datentyp: {typeLabel}</Typography>}
        {expandable && <Button size="small"
                               onClick={() => setDetails(true)}
                               aria-haspopup="dialog">{compact ? 'Ergebnis ansehen' : 'Gesamtergebnis ansehen'}</Button>}
        {details && <Dialog open
                            fullWidth
                            maxWidth="md"
                            onClose={() => setDetails(false)}>
            <DialogTitleWithClose onClose={() => setDetails(false)}>Testergebnis</DialogTitleWithClose>
            <DialogContent>
                {evaluated && <Typography>Datentyp: {typeLabel}</Typography>}
                <Box component="pre"
                     tabIndex={0}
                     aria-label="Vollständiges Testergebnis"
                     sx={{
                         whiteSpace: 'pre-wrap',
                         overflowWrap: 'anywhere',
                         maxHeight: '60vh',
                         overflow: 'auto',
                     }}>
                    {complex ? JSON.stringify(result.value, null, 2) : evaluated && typeof result.value === 'string' ? result.value : text}
                </Box>
            </DialogContent>
            <DialogActions><Button onClick={() => setDetails(false)}>Schließen</Button></DialogActions>
        </Dialog>}
    </Box>;
}
