import React, {type ReactNode, useId} from 'react';
import {Accordion, AccordionDetails, AccordionSummary, Box, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';

interface TechnicalInformationSectionProps {
    children: ReactNode;
    headingBaseLevel?: 3 | 4;
}

export function TechnicalInformationSection({children, headingBaseLevel = 3}: TechnicalInformationSectionProps) {
    const sectionId = useId();

    return (
        <Box sx={{mt: 2}}>
            <Accordion
                disableGutters
                elevation={0}
                square
                slots={{heading: headingBaseLevel === 4 ? 'h4' : 'h3'}}
                slotProps={{transition: {unmountOnExit: true}}}
                sx={{
                    m: 0,
                    backgroundColor: 'transparent',
                    '&::before': {display: 'none'},
                }}
            >
                <AccordionSummary
                    id={`${sectionId}-heading`}
                    aria-controls={`${sectionId}-content`}
                    expandIcon={<KeyboardArrowDown/>}
                    sx={{
                        minHeight: 40,
                        px: 0,
                        '& .MuiAccordionSummary-content': {my: 0},
                    }}
                >
                    <Typography component="span" sx={{fontSize: '1rem', fontWeight: 600, lineHeight: 1.5}}>
                        Technische Informationen
                    </Typography>
                </AccordionSummary>
                <AccordionDetails sx={{p: 0}}>
                    {children}
                </AccordionDetails>
            </Accordion>
        </Box>
    );
}
