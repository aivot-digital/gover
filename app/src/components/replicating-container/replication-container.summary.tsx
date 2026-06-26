import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import {useTheme} from '@mui/material/styles';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import React from 'react';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import SubdirectoryArrowLeftOutlinedIcon from '@mui/icons-material/SubdirectoryArrowLeftOutlined';
import {type AuthoredElementValues} from '../../models/element-data';
import {resolveReplicatingContainerItemDerivedData} from '../../utils/element-data-utils';

export function ReplicationContainerSummary(props: BaseSummaryProps<ReplicatingContainerLayout, AuthoredElementValues[]>) {
    const {
        model,
        showTechnical,
        allowStepNavigation,
        authoredElementValues,
        derivedData,
        value,
    } = props;

    const {
        children,
    } = model;

    const values = (value ?? []) as AuthoredElementValues[];

    const theme = useTheme();

    return (
        <>
            <Grid
                container
                sx={{
                    mt: values.length === 0 ? 0 : 2,
                    borderBottom: values.length === 0 ? '1px solid #D4D4D4' : 'none',
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
                        md: 4
                    }}>
                    <Typography
                        variant="body2"
                        sx={{
                            fontWeight: values.length === 0 ? 'normal' : 'bold',
                        }}
                    >
                        {props.model.label}
                    </Typography>
                </Grid>

                {
                    values.length === 0 &&
                    <Grid
                        size={{
                            xs: 12,
                            md: 8
                        }}>
                        <Typography
                            variant="body2"
                        >
                            Keine Angaben
                        </Typography>
                    </Grid>
                }
            </Grid>
            {
                values.map((val, index) => (
                    <Box
                        key={`${model.id}-${index}`}
                        sx={{
                            border: '1px solid #D4D4D4',
                            mb: 2,
                            px: 2,
                            [theme.breakpoints.up('md')]: {
                                px: 0,
                            },
                        }}
                    >
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
                                    md: 4
                                }}>
                                <Typography
                                    variant="body2"
                                    sx={{
                                        fontWeight: 'bold',
                                    }}
                                >
                                    {
                                        (props.model.headlineTemplate ?? '').replace('#', (index + 1).toFixed())
                                    } <SubdirectoryArrowLeftOutlinedIcon
                                    sx={{
                                        marginLeft: '6px',
                                        fontSize: '1rem',
                                        transform: 'translateY(2px)',
                                    }}
                                />
                                </Typography>
                            </Grid>
                            <Grid
                                size={{
                                    xs: 12,
                                    md: 8
                                }}>
                                <Chip
                                    sx={{ml: -1}}
                                    size="small"
                                    label="Datensatz"
                                    variant="outlined"
                                />
                            </Grid>
                        </Grid>
                        {
                            (children ?? [])
                                .map(child => (
                                    <SummaryDispatcherComponent
                                        key={child.id}
                                        element={child}
                                        showTechnical={showTechnical}
                                        allowStepNavigation={allowStepNavigation}
                                        authoredElementValues={val}
                                        derivedData={resolveReplicatingContainerItemDerivedData(model, derivedData, index)}
                                    />
                                ))
                        }
                    </Box>
                ))
            }
        </>
    );
}
