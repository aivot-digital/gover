import React, {type ReactNode, useId} from 'react';
import {Accordion, AccordionDetails, AccordionSummary, Box, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';

interface ElementDescriptionContentProps {
    usage: string;
    settings?: string;
    children?: ReactNode;
    notes?: string;
    technicalDetails?: string;
}

function DescriptionSection({title, content}: {title: string; content?: string}) {
    if (!content?.trim()) {
        return null;
    }

    return <MarkdownContent markdown={`# ${title}\n\n${content}`} headingBaseLevel={4}/>;
}

export function ElementDescriptionContent({
    usage,
    settings,
    children,
    notes,
    technicalDetails,
}: ElementDescriptionContentProps) {
    const technicalId = useId();

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', gap: 2.5}}>
            <DescriptionSection title="Wofür verwenden Sie dieses Element?" content={usage}/>
            <DescriptionSection title="Welche Einstellungen sind wichtig?" content={settings}/>

            {children != null && <Box>
                <MarkdownContent markdown="# Wie sieht das Element aus?" headingBaseLevel={4}/>
                <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                    {children}
                </Box>
            </Box>}

            <DescriptionSection title="Was ist zu beachten?" content={notes}/>

            {technicalDetails?.trim() && <Accordion
                disableGutters
                elevation={0}
                slots={{heading: 'h4'}}
                slotProps={{transition: {unmountOnExit: true}}}
                sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    '&::before': {display: 'none'},
                }}
            >
                <AccordionSummary
                    id={`${technicalId}-heading`}
                    aria-controls={`${technicalId}-content`}
                    expandIcon={<KeyboardArrowDown/>}
                >
                    <Typography component="span" sx={{fontSize: '1.125rem', fontWeight: 600}}>
                        Technische Informationen
                    </Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <MarkdownContent markdown={technicalDetails}/>
                </AccordionDetails>
            </Accordion>}
        </Box>
    );
}
