import {ReactNode, useId} from 'react';
import {Accordion, AccordionDetails, AccordionSummary, Chip, Typography} from '@mui/material';
import ExpandMore from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';

export function LegacyNoCodeReference({children}: { children: ReactNode }) {
    const id = useId();
    return <Accordion defaultExpanded
                      disableGutters
                      elevation={0}
                      sx={{
                          border: 1,
                          borderColor: 'divider',
                          borderRadius: 1,
                          '&::before': {display: 'none'},
                      }}>
        <AccordionSummary id={`${id}-title`}
                          aria-controls={`${id}-content`}
                          expandIcon={<ExpandMore/>}
                          sx={{
                              '& .MuiAccordionSummary-content': {
                                  alignItems: 'center',
                                  gap: 1.5,
                                  flexWrap: 'wrap',
                              },
                          }}>
            <Typography component="span"
                        variant="subtitle1">Bisherige Regel</Typography>{' '}
            <Chip component="span"
                  label="Nur lesbar"
                  size="small"
                  variant="outlined"
                  sx={{color: 'text.secondary'}}/>
        </AccordionSummary>
        <AccordionDetails sx={{
            pt: 0,
            pb: 2,
        }}>{children}</AccordionDetails>
    </Accordion>;
}
