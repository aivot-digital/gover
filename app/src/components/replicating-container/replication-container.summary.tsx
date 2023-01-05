import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Box, Chip, Grid, Typography} from '@mui/material';
import {
    ReplicatingContainerElement
} from '../../models/elements/form-elements/layout-elements/replicating-container-element';
import {flattenElements} from '../summary/summary.component.view';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import React from 'react';
import {faTurnDown} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {AnyElement} from '../../models/elements/any-element';

export function ReplicationContainerSummary({model, value}: BaseSummaryProps<ReplicatingContainerElement>) {
    let childModels: AnyElement[] = [];
    for (const child of model.children ?? []) {
        childModels = childModels.concat(flattenElements(child));
    }
    const values: string[] = value ?? [];
    return (
        <>
            <Grid
                container
                sx={{
                    mt: 2,
                    borderBottom: '1px solid #D4D4D4',
                    py: 1
                }}
            >
                <Grid
                    item
                    xs={4}
                    sx={{textAlign: 'right', pr: 5}}
                >
                    <Typography
                        variant="body2"
                        sx={{
                            fontWeight: 'bold',
                        }}
                    >
                        {model.label}
                    </Typography>
                </Grid>
            </Grid>

            {
                values.map((val, index) => (
                    <Box key={val}>
                        <Grid
                            container
                            sx={{
                                borderBottom: '1px solid #D4D4D4',
                                py: 1,
                            }}
                        >
                            <Grid
                                item
                                xs={4}
                                sx={{textAlign: 'right', pr: 5}}
                            >
                                <Typography
                                    variant="body2"
                                    sx={{
                                        fontWeight: 'bold',
                                    }}
                                >
                                    {
                                        (model.headlineTemplate ?? '').replace('#', (index + 1).toFixed())
                                    } <FontAwesomeIcon
                                    icon={faTurnDown}
                                    style={{marginLeft: '6px', transform: 'translateY(6px)'}}
                                />
                                </Typography>
                            </Grid>
                            <Grid
                                item
                                xs={8}
                            >
                                <Chip
                                    sx={{ml: -1}}
                                    size="small"
                                    label={'Datensatz'}
                                    variant="outlined"
                                />
                            </Grid>
                        </Grid>
                        {
                            childModels.map(child => (
                                <SummaryDispatcherComponent
                                    key={child.id}
                                    model={child}
                                    idPrefix={`${model.id}_${val}_`}
                                />
                            ))
                        }
                    </Box>
                ))
            }
        </>
    );
}
