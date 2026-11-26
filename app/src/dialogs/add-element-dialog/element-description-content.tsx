import React, {type ReactNode} from 'react';
import {Box} from '@mui/material';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';
import {TechnicalInformationSection} from '../../components/technical-information-section/technical-information-section';

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
    return (
        <Box>
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
            </Box>

            {technicalDetails?.trim() && <TechnicalInformationSection headingBaseLevel={4}>
                <MarkdownContent markdown={technicalDetails} headingBaseLevel={4}/>
            </TechnicalInformationSection>}
        </Box>
    );
}
