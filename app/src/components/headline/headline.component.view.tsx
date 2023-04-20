import {Typography, useTheme} from '@mui/material';
import {HeadlineElement} from '../../models/elements/./form/./content/headline-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function HeadlineComponentView({element}: BaseViewProps<HeadlineElement, void>) {
    const theme = useTheme();
    return (
        <Typography
            variant={element.small ? 'h6' : 'subtitle1'}
            component={element.small ? 'h3' : 'h2'}
            sx={{
                color: element.small ? "#16191F" : theme.palette.primary.dark,
                fontSize: element.small ? "1rem" : "1.25rem",
                textTransform: element.small ? "uppercase" : "none",
                margin: '1.5em 0 0.5em 0',
            }}
        >
            {element.content ?? ''}
        </Typography>
    );
}
