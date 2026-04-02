import React, {type ReactNode} from 'react';
import {Panel} from '@xyflow/react';
import {Box, Button, Typography, useTheme} from '@mui/material';
import {alpha, lighten} from '@mui/material/styles';
import Add from '@mui/icons-material/Add';
import {ProcessNodeType} from '../../../../services/process-node-provider-api-service';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';

export const CANVAS_ADD_TRIGGER_BUTTON_HEIGHT = 46;

interface ProcessFlowEditorAddTriggerActionButtonProps {
    appearance?: 'hero' | 'inline';
    label: string;
    onClick: () => void;
}

export function ProcessFlowEditorAddTriggerActionButton(props: ProcessFlowEditorAddTriggerActionButtonProps): ReactNode {
    const {
        appearance = 'inline',
        label,
        onClick,
    } = props;
    const theme = useTheme();
    const isHero = appearance === 'hero';

    return (
        <Button
            variant={isHero ? 'contained' : 'text'}
            startIcon={<Add sx={{fontSize: 18}}/>}
            onClick={onClick}
            sx={{
                minWidth: 0,
                height: CANVAS_ADD_TRIGGER_BUTTON_HEIGHT,
                px: isHero ? 2.75 : 2.25,
                borderRadius: 1.5,
                border: isHero ? '1px solid transparent' : '2px dashed rgba(148, 163, 184, 0.5)',
                color: isHero ? '#ffffff' : 'rgba(148, 163, 184, 0.96)',
                bgcolor: isHero ? theme.palette.primary.main : 'transparent',
                background: isHero ? theme.palette.primary.main : 'transparent',
                boxShadow: isHero ? '0px 4px 20px rgba(0, 0, 0, 0.1)' : 'none',
                textTransform: 'none',
                fontWeight: 700,
                fontSize: '0.95rem',
                letterSpacing: 0,
                whiteSpace: 'nowrap',
                backdropFilter: isHero ? 'none' : 'blur(2px)',
                transition: 'transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background-color 180ms ease',
                '&:hover': {
                    bgcolor: isHero ? theme.palette.primary.dark : 'rgba(255, 255, 255, 0.88)',
                    background: isHero ? theme.palette.primary.dark : 'transparent',
                    borderColor: isHero ? 'transparent' : 'rgba(100, 116, 139, 0.62)',
                    boxShadow: isHero ? '0px 4px 20px rgba(0, 0, 0, 0.15)' : 'none',
                },
            }}
        >
            {label}
        </Button>
    );
}

interface ProcessFlowEditorEmptyStateProps {
    label: string;
    onAddTrigger: () => void;
    verticalOffset: number;
}

export function ProcessFlowEditorEmptyState(props: ProcessFlowEditorEmptyStateProps): ReactNode {
    const {
        label,
        onAddTrigger,
        verticalOffset,
    } = props;
    const triggerTypeStyle = ProviderTypeStyles[ProcessNodeType.Trigger];

    return (
        <Panel
            position="top-left"
            className="process-flow-editor-empty-state"
            style={{
                margin: 0,
                left: '50%',
                top: `calc(50% - ${verticalOffset}px)`,
                transform: 'translate(-50%, -50%)',
            }}
        >
            <Box
                className="process-flow-editor-empty-state-card"
                sx={{
                    '--process-flow-editor-empty-state-trigger-main': lighten(triggerTypeStyle.textColor, 0.14),
                    '--process-flow-editor-empty-state-trigger-soft': triggerTypeStyle.bgColor,
                    '--process-flow-editor-empty-state-trigger-ring-strong': alpha(triggerTypeStyle.textColor, 0.44),
                    '--process-flow-editor-empty-state-trigger-ring-soft': alpha(triggerTypeStyle.textColor, 0.24),
                } as React.CSSProperties}
            >
                <Box
                    className="process-flow-editor-empty-state-marker"
                    aria-hidden
                    onClick={onAddTrigger}
                    title={'Auslöser hinzufügen'}
                >
                    <Box className="process-flow-editor-empty-state-marker-ring process-flow-editor-empty-state-marker-ring-primary"/>
                    <Box className="process-flow-editor-empty-state-marker-ring process-flow-editor-empty-state-marker-ring-secondary"/>
                    <Box className="process-flow-editor-empty-state-marker-halo"/>
                    <Box className="process-flow-editor-empty-state-marker-core"/>
                </Box>
                <Box
                    className="process-flow-editor-empty-state-guide"
                    aria-hidden
                />
                <Box className="process-flow-editor-empty-state-text-block">
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
                        Startpunkt
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
                        Ersten Auslöser platzieren
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{
                            mt: 1,
                            maxWidth: 300,
                            color: 'text.secondary',
                            lineHeight: 1.55,
                            whiteSpace: 'normal',
                        }}
                    >
                        Wählen Sie den Auslöser, mit dem dieser Prozess beginnt.
                    </Typography>
                </Box>
                <Box className="process-flow-editor-empty-state-action">
                    <ProcessFlowEditorAddTriggerActionButton
                        appearance="hero"
                        label={label}
                        onClick={onAddTrigger}
                    />
                </Box>
            </Box>
        </Panel>
    );
}
