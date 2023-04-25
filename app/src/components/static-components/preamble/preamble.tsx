import {Box, Grid} from '@mui/material';
import {ViewDispatcherComponent} from '../../view-dispatcher.component';
import {ElementType} from '../../../data/element-type/element-type';
import React from 'react';
import {useSelector} from 'react-redux';
import {selectLoadedApplication} from '../../../slices/app-slice';

interface PreambleProps {
    text: string;
}

export function Preamble(props: PreambleProps) {
    const introductionStep = useSelector(selectLoadedApplication)?.root.introductionStep;

    return (
        <>
            {
                (
                    introductionStep &&
                    introductionStep.initiativeName &&
                    introductionStep.initiativeLogoLink
                ) ?
                    <Grid
                        container
                        spacing={10}
                        justifyContent={'space-between'}
                    >
                        <Grid
                            item
                            xs={12}
                            md={8}
                            lg={7}
                        >
                            <ViewDispatcherComponent
                                element={{
                                    id: 'preambleText',
                                    type: ElementType.Richtext,
                                    content: props.text,
                                }}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            md={4}
                            lg={5}
                            sx={{pr: 6, textAlign: 'center'}}
                        >
                            {
                                introductionStep &&
                                introductionStep.initiativeName &&
                                introductionStep.initiativeLogoLink &&
                                <img
                                    src={introductionStep.initiativeLogoLink}
                                    alt={introductionStep.initiativeName}
                                    style={{maxWidth: '100%', marginTop: '-10px'}}
                                />
                            }
                        </Grid>
                    </Grid>
                    :
                    <Grid
                        container
                        spacing={10}
                        justifyContent={'space-between'}
                    >
                        <Grid
                            item
                            xs={12}
                        >
                            <Box sx={{maxWidth: '660px'}}>
                                <ViewDispatcherComponent
                                    element={{
                                        id: 'preambleText',
                                        type: ElementType.Richtext,
                                        content: props.text,
                                    }}
                                />
                            </Box>
                        </Grid>
                    </Grid>
            }
        </>
    );
}
