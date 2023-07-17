import {Box, Chip, Grid, Typography} from '@mui/material';
import {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {flattenElementsForSummary} from '../summary/summary.component.view';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import React from 'react';
import {faTurnDown} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {AnyElement} from '../../models/elements/any-element';
import {useSelector} from "react-redux";
import {selectCustomerInput} from "../../slices/customer-input-slice";
import {BaseSummaryProps} from "../../summaries/base-summary";

export function ReplicationContainerSummary({
                                                allElements,
                                                model,
                                                value,
                                                idPrefix,
                                            }: BaseSummaryProps<ReplicatingContainerLayout, string[]>) {
    const prefixedId = idPrefix != null ? (idPrefix + model.id) : model.id;

    const values: string[] = value ?? [];

    const customerInput = useSelector(selectCustomerInput);

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
                    mt: 2,
                    borderBottom: '1px solid #D4D4D4',
                    py: 1,
                }}
            >
                <Grid
                    item
                    xs={4}
                    sx={{
                        textAlign: 'right',
                        pr: 5,
                    }}
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
                                sx={{
                                    textAlign: 'right',
                                    pr: 5,
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
                                    } <FontAwesomeIcon
                                    icon={faTurnDown}
                                    style={{
                                        marginLeft: '6px',
                                        transform: 'translateY(6px)',
                                    }}
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
                                />
                            ))
                        }
                    </Box>
                ))
            }
        </>
    );
}
