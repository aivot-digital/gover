import React from 'react';
import {Box, Button, Grid, Typography} from '@mui/material';
import {FunctionSelectorProps} from './function-selector-props';
import {FunctionTypeIcon} from '../../function-type-icon';
import {useAppSelector} from "../../../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../../data/system-config-keys";

export function FunctionSelector(props: FunctionSelectorProps) {
    const enableNewCodeEditors = useAppSelector(selectSystemConfigValue(SystemConfigKeys.experimentalFeatures.newCodeEditors));
    const options = [];

    if (props.allowNoCode) {
        options.push(
            {
                title: 'No-Code (Regelbasierte Konfiguration)',
                description: 'Diese Funktion erlaubt eine einfache, regelbasierte Konfiguration ohne Programmierung. Sie eignet sich besonders für grundlegende Entscheidungen, Berechnungen oder Validierungen, die auf den eingegebenen Formulardaten basieren. Diese Variante ist intuitiv und erfordert keine technischen Kenntnisse.',
                onSelect: props.onSelectNoCode,
                icon: FunctionTypeIcon['legacy-condition'],
            },
        );
    }
    if (props.allowExpression && enableNewCodeEditors) {
        options.push(
            {
                title: 'Erweiterter No-Code (Fortgeschrittene Regelgestaltung)',
                description: 'Diese Variante ermöglicht eine leistungsfähigere Regelverarbeitung mit erweiterten Bedingungen und komplexeren Berechnungen. Der Editor bietet mehr Flexibilität, setzt jedoch ein grundlegendes Verständnis von logischen Verknüpfungen und strukturierten Regeln voraus. Daher sind Vorkenntnisse im Umgang mit regelbasierten Systemen empfehlenswert.',
                onSelect: props.onSelectNoCodeExpression,
                icon: FunctionTypeIcon['expression'],
            },
        );
    }

    options.push(
        {
            title: 'Low-Code (Individuelle Logik durch eigene Skripte)',
            description: 'Diese Variante erlaubt es, eigene Skripte für Berechnungen, Validierungen und dynamische Anpassungen zu verwenden. Sie eignet sich besonders für spezifische Anforderungen, die mit reinen No-Code-Regeln nicht abgedeckt werden können. Basiskenntnisse in der Skriptgestaltung sind hilfreich.',
            onSelect: props.onSelectFunctionCode,
            icon: FunctionTypeIcon['legacy-code'],
        },
    );

    if(enableNewCodeEditors) {
        options.push(
            {
                title: 'Erweiterter Low-Code (Individuelle Logik mit Zugriff auf externe Datenquellen)',
                description: 'Bietet maximale Flexibilität durch individuelle Skripte und die Möglichkeit, externe Datenquellen wie APIs zu nutzen. Ideal für anspruchsvolle Prozesse, die eine Verbindung zu anderen Systemen erfordern. Diese Variante eignet sich für komplexe Automatisierungen und datengetriebene Entscheidungen.',
                onSelect: props.onSelectCloudCode,
                icon: FunctionTypeIcon['code'],
            },
        );
    }

    return (
        <Box>
            <Typography
                variant="subtitle1"
            >
                Wählen Sie einen geeigneten Typ für die Funktion aus
            </Typography>

            <Grid
                container
                spacing={2}
                sx={{
                    mt: 0,
                }}
            >
                {
                    options
                        .map((option, index) => (
                            <Grid
                                item
                                xs={props.fullWidth ? 12 : 6}
                                key={option.title}
                            >
                                <Box
                                    sx={{
                                        p: 2,
                                        border: '1px solid #e0e0e0',
                                        borderRadius: '4px',
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
                                        Funktion anlegen
                                    </Button>
                                </Box>
                            </Grid>
                        ))
                }
            </Grid>
        </Box>
    );
}
