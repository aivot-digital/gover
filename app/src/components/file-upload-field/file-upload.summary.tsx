import {Grid, Typography, useTheme} from '@mui/material';
import {FileUploadElement, FileUploadElementItem} from "../../models/elements/form/input/file-upload-element";
import {humanizeFileSize} from "../../utils/huminization-utils";
import {BaseSummaryProps} from "../../summaries/base-summary";

export function FileUploadSummary({
                                      model,
                                      value,
                                  }: BaseSummaryProps<FileUploadElement, FileUploadElementItem[]>) {
    const theme = useTheme();

    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
                py: 1,
            }}
        >
            <Grid
                item
                xs={12}
                md={4}
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
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
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                item
                xs={12}
                md={8}
            >
                <Typography
                    variant="body2"
                >
                    {
                        value != null && value.length > 0 ? (value as FileUploadElementItem[]).map(item => `${item.name} (${humanizeFileSize(item.size)})`).join(', ') : 'Keine Anlagen hinzugefügt'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
