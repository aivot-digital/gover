import {Typography, useTheme} from '@mui/material';
import {HeadlineElement} from '../../models/elements/form/content/headline-element';
import {BaseViewProps} from "../../views/base-view";

export function HeadlineComponent({content, small}: {content: string, small: boolean}) {
    const theme = useTheme();
    return (
        <Typography
            variant={small ? 'h6' : 'subtitle1'}
            component={small ? 'h3' : 'h2'}
            sx={{
                color: small ? "#16191F" : theme.palette.primary.dark,
                fontSize: small ? "1rem" : "1.25rem",
                textTransform: small ? "uppercase" : "none",
                margin: '1.5em 0 0.5em 0',
            }}
        >
            {content}
        </Typography>
    );
}
