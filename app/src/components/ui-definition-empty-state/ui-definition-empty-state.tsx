import React from 'react';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Box, Button, Typography, useTheme} from '@mui/material';
import {alpha, keyframes} from '@mui/material/styles';

const fadeIn = keyframes`
    0% {
        opacity: 0;
    }
    100% {
        opacity: 1;
    }
`;

const guideEnter = keyframes`
    0% {
        opacity: 0;
        transform: scaleY(0.35);
    }
    100% {
        opacity: 1;
        transform: scaleY(1);
    }
`;

const enter = keyframes`
    0% {
        opacity: 0;
        transform: translateY(10px);
    }
    100% {
        opacity: 1;
        transform: translateY(0);
    }
`;

const sectionFloat = keyframes`
    0%, 100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-7px);
    }
`;

export type UiDefinitionEmptyStateTarget = 'section' | 'element';

interface UiDefinitionEmptyStateProps {
    target: UiDefinitionEmptyStateTarget;
    onAdd: () => void;
    disabled?: boolean;
}

function resolveCopy(target: UiDefinitionEmptyStateTarget) {
    if (target === 'section') {
        return {
            title: 'Ersten Abschnitt anlegen',
            description: 'Fügen Sie einen Abschnitt hinzu, damit Sie weitere Elemente verbauen und konfigurieren können.',
            actionLabel: 'Abschnitt hinzufügen',
        };
    }

    return {
        title: 'Erstes Element hinzufügen',
        description: 'Fügen Sie ein Element hinzu, damit Sie Daten anzeigen und erfassen können.',
        actionLabel: 'Element hinzufügen',
    };
}

export function UiDefinitionEmptyState(props: UiDefinitionEmptyStateProps) {
    const {
        target,
        onAdd,
        disabled = false,
    } = props;

    const theme = useTheme();
    const copy = resolveCopy(target);

    const handleAdd = () => {
        if (disabled) {
            return;
        }

        onAdd();
    };

    return (
        <Box
            sx={{
                minHeight: '44vh',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                px: 2,
                py: 5,
            }}
        >
            <Box
                sx={{
                    width: 'min(360px, calc(100vw - 72px))',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    textAlign: 'center',
                    opacity: 0,
                    animation: `${fadeIn} 320ms ease-out 400ms both`,
                    '@media (prefers-reduced-motion: reduce)': {
                        '&, & *': {
                            animation: 'none !important',
                            transition: 'none !important',
                        },
                    },
                }}
            >
                <Box
                    component="button"
                    type="button"
                    onClick={handleAdd}
                    disabled={disabled}
                    title={copy.actionLabel}
                    aria-label={copy.actionLabel}
                    sx={{
                        position: 'relative',
                        width: 84,
                        height: 76,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        opacity: 0,
                        animation: `${fadeIn} 320ms ease-out 520ms both`,
                        p: 0,
                        border: 0,
                        bgcolor: 'transparent',
                        appearance: 'none',
                        cursor: disabled ? 'default' : 'pointer',
                    }}
                >
                    <Box
                        sx={{
                            position: 'absolute',
                            width: 36,
                            height: 44,
                            top: 16,
                            left: 5,
                            borderRadius: '4px',
                            border: `1px solid ${alpha(theme.palette.text.primary, 0.08)}`,
                            background: alpha(theme.palette.background.paper, 0.76),
                            boxShadow: '0 6px 14px rgba(15, 23, 42, 0.05)',
                            transform: 'rotate(-10deg)',
                        }}
                    />
                    <Box
                        sx={{
                            position: 'absolute',
                            width: 38,
                            height: 46,
                            top: 9,
                            right: 5,
                            borderRadius: '6px',
                            border: `1px solid ${alpha(theme.palette.text.primary, 0.10)}`,
                            background: alpha(theme.palette.background.paper, 0.92),
                            boxShadow: '0 8px 16px rgba(15, 23, 42, 0.06)',
                            transform: 'rotate(8deg)',
                        }}
                    />
                    <Box
                        sx={{
                            position: 'relative',
                            width: 40,
                            height: 52,
                            borderRadius: '6px',
                            border: `2px dashed ${alpha(theme.palette.primary.main, 0.28)}`,
                            background: theme.palette.background.paper,
                            boxShadow: `0 8px 18px ${alpha(theme.palette.primary.main, 0.09)}`,
                            overflow: 'hidden',
                            animation: `${sectionFloat} 4.8s ease-in-out 1.1s infinite`,
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 0.625,
                                px: 0.875,
                                pt: 0.875,
                                pb: 1.375,
                            }}
                        >
                            <Box
                                sx={{
                                    width: '58%',
                                    height: 5,
                                    borderRadius: 999,
                                    bgcolor: alpha(theme.palette.text.primary, 0.14),
                                }}
                            />
                            <Box
                                sx={{
                                    width: '100%',
                                    height: 10,
                                    borderRadius: '7px',
                                    bgcolor: alpha(theme.palette.text.primary, 0.06),
                                }}
                            />
                            <Box
                                sx={{
                                    width: '100%',
                                    height: 10,
                                    borderRadius: '7px',
                                    bgcolor: alpha(theme.palette.text.primary, 0.06),
                                }}
                            />
                        </Box>
                    </Box>
                    <Box
                        sx={{
                            position: 'absolute',
                            right: 10,
                            bottom: 7,
                            width: 22,
                            height: 22,
                            borderRadius: '50%',
                            bgcolor: 'primary.main',
                            color: 'primary.contrastText',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: `0 6px 14px ${alpha(theme.palette.primary.main, 0.22)}`,
                        }}
                    >
                        <Add sx={{fontSize: 14}}/>
                    </Box>
                </Box>

                <Box
                    aria-hidden
                    sx={{
                        position: 'relative',
                        width: '1px',
                        height: 36,
                        my: '10px',
                        borderRadius: '999px',
                        background: `linear-gradient(180deg, ${alpha(theme.palette.primary.main, 0.28)} 0%, ${alpha(theme.palette.text.secondary, 0.30)} 52%, rgba(148, 163, 184, 0) 100%)`,
                        opacity: 0,
                        transformOrigin: 'top center',
                        transform: 'scaleY(0.35)',
                        animation: `${guideEnter} 360ms cubic-bezier(0.22, 1, 0.36, 1) 700ms both`,
                    }}
                />

                <Box
                    sx={{
                        opacity: 0,
                        animation: `${enter} 360ms ease-out 760ms both`,
                    }}
                >
                    <Typography
                        variant="caption"
                        component="span"
                        sx={{
                            display: 'inline-block',
                            color: 'text.secondary',
                            fontWeight: 700,
                            letterSpacing: '0.3px',
                            lineHeight: 1,
                            textTransform: 'uppercase',
                        }}
                    >
                        Struktur
                    </Typography>
                    <Typography
                        variant="h5"
                        component="h2"
                        sx={{
                            mt: 0.5,
                            maxWidth: 340,
                            color: 'text.primary',
                            lineHeight: 1.2,
                            whiteSpace: 'normal',
                        }}
                    >
                        {copy.title}
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{
                            mt: 1,
                            maxWidth: 310,
                            color: 'text.secondary',
                            lineHeight: 1.55,
                            whiteSpace: 'normal',
                        }}
                    >
                        {copy.description}
                    </Typography>
                </Box>

                <Box
                    sx={{
                        mt: '26px',
                        opacity: 0,
                        animation: `${enter} 360ms ease-out 920ms both`,
                    }}
                >
                    <Button
                        variant="contained"
                        startIcon={<Add sx={{fontSize: 18}}/>}
                        onClick={handleAdd}
                        disabled={disabled}
                        sx={{
                            minWidth: 0,
                            height: 46,
                            px: 2.75,
                            borderRadius: 1.5,
                            border: '1px solid transparent',
                            color: 'primary.contrastText',
                            bgcolor: 'primary.main',
                            boxShadow: '0px 4px 20px rgba(0, 0, 0, 0.1)',
                            textTransform: 'none',
                            fontWeight: 700,
                            fontSize: '0.95rem',
                            letterSpacing: 0,
                            whiteSpace: 'nowrap',
                            transition: 'transform 180ms ease, box-shadow 180ms ease, background-color 180ms ease',
                            '&:hover': {
                                bgcolor: 'primary.dark',
                                boxShadow: '0px 4px 20px rgba(0, 0, 0, 0.15)',
                            },
                        }}
                    >
                        {copy.actionLabel}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
