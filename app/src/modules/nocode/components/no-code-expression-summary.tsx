import {useLayoutEffect, useRef, useState} from 'react';
import {Box, Button, Typography} from '@mui/material';
import {NoCodeSummary} from '../utils/no-code-summary';

export function NoCodeExpressionSummary({summary, id}: {summary: NoCodeSummary; id: string}) {
    const textRef = useRef<HTMLParagraphElement>(null);
    const [expanded, setExpanded] = useState(false);
    const [overflows, setOverflows] = useState(false);
    const [showDetails, setShowDetails] = useState(false);
    const detailsId = `${id}-details`;

    useLayoutEffect(() => {
        const text = textRef.current;
        if (text == null) return;

        const measure = () => {
            // Measure the three-line preview even while the full sentence is visible.
            const {display, webkitLineClamp, webkitBoxOrient, overflow} = text.style;
            text.style.display = '-webkit-box';
            text.style.webkitLineClamp = '3';
            text.style.webkitBoxOrient = 'vertical';
            text.style.overflow = 'hidden';
            const hasOverflow = text.scrollHeight > text.clientHeight + 1;
            text.style.display = display;
            text.style.webkitLineClamp = webkitLineClamp;
            text.style.webkitBoxOrient = webkitBoxOrient;
            text.style.overflow = overflow;
            setOverflows(hasOverflow);
            if (!hasOverflow) setExpanded(false);
        };

        setExpanded(false);
        measure();
        const observer = typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(measure);
        observer?.observe(text);
        window.addEventListener('resize', measure);
        return () => {
            observer?.disconnect();
            window.removeEventListener('resize', measure);
        };
    }, [summary.text]);

    return <Box onKeyDown={event => {
                    if (['Enter', ' ', 'ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
                        event.stopPropagation();
                    }
                }}
                sx={{minWidth: 0}}>
        <Typography id={id}
                    ref={textRef}
                    variant="body2"
                    component="p"
                    sx={{
                        overflowWrap: 'anywhere',
                        whiteSpace: 'pre-wrap',
                        ...(!expanded && {
                            display: '-webkit-box',
                            WebkitLineClamp: 3,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                        }),
                    }}>{summary.text}</Typography>
        {(overflows || summary.details.length > 0) && <Box sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 0.5,
            mt: 0.5,
        }}>
            {overflows && <Button size="small"
                                  aria-expanded={expanded}
                                  aria-controls={id}
                                  onClick={() => setExpanded(current => !current)}>
                {expanded ? 'Weniger anzeigen' : 'Vollständig anzeigen'}
            </Button>}
            {summary.details.length > 0 && <Button size="small"
                                                   aria-expanded={showDetails}
                                                   aria-controls={detailsId}
                                                   onClick={() => setShowDetails(current => !current)}>
                Technische Details
            </Button>}
        </Box>}
        {summary.details.length > 0 && <Box id={detailsId}
                                           component="dl"
                                           hidden={!showDetails}
                                           sx={{
                                               m: 0,
                                               mt: 1,
                                               p: 1,
                                               borderRadius: 1,
                                               bgcolor: 'action.hover',
                                           }}>
            {summary.details.map((detail, index) => <Box key={index}
                                                       component="div"
                                                       sx={{'& + &': {mt: 1}}}>
                <Typography component="dt"
                            variant="body2"
                            sx={{overflowWrap: 'anywhere'}}>{detail.label}</Typography>
                <Typography component="dd"
                            variant="body2"
                            color="text.secondary"
                            sx={{
                                m: 0,
                                fontFamily: 'monospace',
                                overflowWrap: 'anywhere',
                                whiteSpace: 'pre-wrap',
                            }}>{detail.value}</Typography>
            </Box>)}
        </Box>}
    </Box>;
}
