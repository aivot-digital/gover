import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {MarkdownContent} from '../components/markdown-content/markdown-content';

export function RichTextInputSummary(props: BaseSummaryProps<RichTextInputElement, string>) {
    const theme = useTheme();
    const markdown = props.value?.trim() ?? '';

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid',
                borderBottomColor: 'divider',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {props.model.label}
                </Typography>
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                {markdown.length === 0 ? (
                    <Typography variant="body2">
                        Keine Angabe
                    </Typography>
                ) : (
                    // Rendered like the PDF of the submission, which prints link targets as well. Headings keep the
                    // size of the summary text, so a heading in an answer does not look like a heading of the summary.
                    <MarkdownContent
                        markdown={markdown}
                        revealLinkTargets
                        sx={{
                            typography: 'body2',
                            '& h1, & h2, & h3, & h4, & h5, & h6': {
                                fontSize: 'inherit',
                                fontWeight: 'bold',
                                my: 1.5,
                            },
                        }}
                    />
                )}
            </Grid>
        </Grid>
    );
}
