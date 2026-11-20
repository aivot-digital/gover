import React from 'react';
import {Box, Button, Grid, Typography} from '@mui/material';
import {FunctionTypeIcon} from '../../function-type-icon';
import {ElementEditorSectionHeader} from '../../../element-editor-section-header/element-editor-section-header';

interface FunctionSelectorProps {
    onSelectFunctionCode: () => void;
    onSelectNoCode: () => void;
    onSelectCloudCode: () => void;
    onSelectNoCodeExpression: () => void;
    allowNoCode: boolean;
    allowExpression: boolean;
    fullWidth: boolean;
    mode?: 'create' | 'change';
}

export function FunctionSelector(props: FunctionSelectorProps) {
    const options = [];

    if (props.allowExpression) {
        options.push(
            {
                title: 'No-Code – Regeln und Berechnungen',
                description: 'Stellen Sie Bedingungen und Berechnungen aus Werten, Variablen und Operatoren zusammen, ohne Programmcode zu schreiben.',
                onSelect: props.onSelectNoCodeExpression,
                icon: FunctionTypeIcon['expression'],
            },
        );
    }

    options.push(
        {
            title: 'Low-Code – JavaScript',
            description: 'Schreiben Sie ein JavaScript-Skript für individuelle Logik. Dafür benötigen Sie Programmierkenntnisse.',
            onSelect: props.onSelectCloudCode,
            icon: FunctionTypeIcon['code'],
        },
    );

    return (
        <Box>
            {props.mode === 'change' ? <Typography sx={{mb: 2}}>
                Wählen Sie einen geeigneten Typ für die Funktion aus, die Sie umsetzen möchten.
            </Typography> : <ElementEditorSectionHeader
                title="No-/Low-Code-Funktion"
                variant="h5"
            >
                Wählen Sie einen geeigneten Typ für die Funktion aus, die Sie umsetzen möchten.
            </ElementEditorSectionHeader>}
            <Grid
                container
                spacing={2}
                sx={{
                    mt: 0,
                }}
            >
                {
                    options
                        .map((option) => (
                            <Grid
                                key={option.title}
                                size={props.fullWidth ? 12 : 6}
                            >
                                <Box
                                    component="section"
                                    aria-label={option.title}
                                    sx={{
                                        p: 2,
                                        border: 1,
                                        borderColor: 'divider',
                                        borderRadius: 1,
                                        height: '100%',
                                        display: 'flex',
                                        flexDirection: 'column',
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            mb: 1,
                                        }}
                                    >
                                        {option.icon}

                                        <Typography
                                            variant="subtitle1"
                                            component={props.mode === 'change' ? 'h3' : 'h6'}
                                            sx={{
                                                ml: 1,
                                            }}
                                        >
                                            {option.title}
                                        </Typography>
                                    </Box>

                                    <Typography
                                        variant="body2"
                                        sx={{
                                            mb: 2,
                                        }}
                                    >
                                        {option.description}
                                    </Typography>

                                    <Button
                                        sx={{
                                            mt: 'auto',
                                            ml: 'auto',
                                        }}
                                        onClick={option.onSelect}
                                        variant={'outlined'}
                                    >
                                        {props.mode === 'change' ? 'Typ auswählen' : 'Funktion anlegen'}
                                    </Button>
                                </Box>
                            </Grid>
                        ))
                }
            </Grid>
        </Box>
    );
}
