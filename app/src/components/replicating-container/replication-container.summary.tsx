import {Box, Chip, Grid, Typography, useTheme} from '@mui/material';
import {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {flattenElementsForSummary} from '../summary/summary.component.view';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import React from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {BaseSummaryProps} from '../../summaries/base-summary';
import SubdirectoryArrowLeftOutlinedIcon from '@mui/icons-material/SubdirectoryArrowLeftOutlined';
import {useAppSelector} from '../../hooks/use-app-selector';

export function ReplicationContainerSummary({
                                                allElements,
                                                model,
                                                value,
                                                idPrefix,
                                                showTechnical,
                                            }: BaseSummaryProps<ReplicatingContainerLayout, string[]>) {
    const prefixedId = idPrefix != null ? (idPrefix + model.id) : model.id;

    const values: string[] = value ?? [];

    const customerInput = useAppSelector(state => state.app.inputs);

    const theme = useTheme();

    const makeChildModels = (val: string) => {
        let childModels: AnyElement[] = [];
        for (const child of model.children ?? []) {
            childModels = childModels.concat(flattenElementsForSummary(allElements, child, customerInput, `${prefixedId}_${val}_`));
        }
        return childModels;
    };

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
                    item
                    xs={12}
                    md={4}
                    sx={{
                        textAlign: 'left',
                        pr: 5,
                        [theme.breakpoints.up('md')]: {
                            textAlign: 'right',
                        },
                    }}
                >
                    <Typography
                        variant="body2"
                        sx={{
                            fontWeight: values.length === 0 ? 'normal' : 'bold',
                        }}
                    >
                        {model.label}
                    </Typography>
                </Grid>

                {
                    values.length === 0 &&
                    <Grid
                        item
                        xs={12}
                        md={8}
                    >
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
                    <Box key={val}
                         sx={{
                             border: '1px solid #D4D4D4',
                             mb: 2,
                             px: 2,
                             [theme.breakpoints.up('md')]: {
                                 px: 0,
                             },
                         }}>
                        <Grid
                            container
                            sx={{
                                borderBottom: '1px solid #D4D4D4',
                                py: 1,
                            }}
                        >
                            <Grid
                                item
                                xs={12}
                                md={4}
                                sx={{
                                    textAlign: 'left',
                                    pr: 5,
                                    [theme.breakpoints.up('md')]: {
                                        textAlign: 'right',
                                    },
                                }}
                            >
                                <Typography
                                    variant="body2"
                                    sx={{
                                        fontWeight: 'bold',
                                    }}
                                >
                                    {
                                        (model.headlineTemplate ?? '').replace('#', (index + 1).toFixed())
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
                                item
                                xs={12}
                                md={8}
                            >
                                <Chip
                                    sx={{ml: -1}}
                                    size="small"
                                    label="Datensatz"
                                    variant="outlined"
                                />
                            </Grid>
                        </Grid>
                        {
                            makeChildModels(val).map(child => (
                                <SummaryDispatcherComponent
                                    allElements={allElements}
                                    key={`${prefixedId}_${val}_${child.id}`}
                                    element={child}
                                    idPrefix={`${prefixedId}_${val}_`}
                                    showTechnical={showTechnical}
                                />
                            ))
                        }
                    </Box>
                ))
            }
        </>
    );
}
