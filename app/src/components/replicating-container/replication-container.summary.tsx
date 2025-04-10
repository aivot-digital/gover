import {Box, Chip, Grid, Typography, useTheme} from '@mui/material';
import {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {flattenElementsForSummary} from '../summary/summary.component.view';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import React from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {BaseSummaryProps} from '../../summaries/base-summary';
import SubdirectoryArrowLeftOutlinedIcon from '@mui/icons-material/SubdirectoryArrowLeftOutlined';
import {useAppSelector} from '../../hooks/use-app-selector';
import {resolveId} from '../../utils/id-utils';

export function ReplicationContainerSummary(props: BaseSummaryProps<ReplicatingContainerLayout, string[]>) {
    const visibilities = useAppSelector(state => state.app.visibilities);
    const prefixedId = resolveId(props.model.id, props.idPrefix);

    const values: string[] = props.value ?? [];

    const customerInput = props.customerInput != null ? props.customerInput : useAppSelector(state => state.app.inputs);

    const theme = useTheme();

    const makeChildModels = (val: string) => {
        let childModels: AnyElement[] = [];
        for (const child of props.model.children ?? []) {
            childModels = childModels.concat(flattenElementsForSummary(props.allElements, child, customerInput, visibilities, `${prefixedId}_${val}_`));
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
                        {props.model.label}
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
                    <Box
                        key={val}
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
                                    allElements={props.allElements}
                                    key={`${prefixedId}_${val}_${child.id}`}
                                    element={child}
                                    idPrefix={`${prefixedId}_${val}_`}
                                    showTechnical={props.showTechnical}
                                    customerInput={customerInput}
                                />
                            ))
                        }
                    </Box>
                ))
            }
        </>
    );
}
