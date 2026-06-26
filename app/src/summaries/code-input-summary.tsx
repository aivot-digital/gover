import {Button, Grid, Typography, useTheme} from '@mui/material';
import {useMemo, useState} from 'react';
import {BaseSummaryProps} from './base-summary';
import {CodeInputElement} from '../models/elements/form/input/code-input-element';

function normalizeCodeValue(value: string | null | undefined): string {
    if (value == null || value.trim().length === 0) {
        return 'Keine Angabe';
    }

    return value;
}

export function CodeInputSummary(props: BaseSummaryProps<CodeInputElement, string>) {
    const theme = useTheme();
    const [expanded, setExpanded] = useState(false);

    const content = useMemo(() => normalizeCodeValue(props.value), [props.value]);
    const contentTooLong = content.length > 256;
    const visibleContent = contentTooLong && !expanded ? `${content.substring(0, 256)} …` : content;

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid #D4D4D4',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {props.model.label}
                </Typography>
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontFamily: 'monospace',
                        whiteSpace: 'pre-wrap',
                    }}
                >
                    {visibleContent}
                </Typography>

                {
                    contentTooLong &&
                    <Button
                        size="small"
                        sx={{mt: 1}}
                        onClick={() => setExpanded((current) => !current)}
                    >
                        {expanded ? 'Weniger anzeigen' : 'Code vollständig anzeigen'}
                    </Button>
                }
            </Grid>
        </Grid>
    );
}
