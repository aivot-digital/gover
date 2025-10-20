import {
    Grid,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
    useTheme
} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseSummaryProps} from "../../summaries/base-summary";

// TODO: Value Type
export function TableFieldComponentSummary(props: BaseSummaryProps<TableFieldElement, any>) {
    const {
        model,
        value,
    } = props;

    const theme = useTheme();

    return (
        <>
            <Grid
                container
                sx={{
                    mt: 2,
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
                    size={4}>
                    <Typography
                        variant="body2"
                        sx={{
                            fontWeight: 'bold',
                        }}
                    >
                        {model.label}
                    </Typography>
                </Grid>
            </Grid>
            <TableContainer
                sx={{
                    mb: 3,
                    border: '1px solid #D4D4D4',
                    borderBottomWidth: "2px",
                }}
            >
                <Table size={"small"}>
                    <TableHead>
                        <TableRow>
                            {
                                (model.fields ?? []).map(field => (
                                    <TableCell key={field.label}>
                                        {field.label}
                                    </TableCell>
                                ))
                            }
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {
                            value && Array.isArray(value) && value.length > 0 ? value.map((value, index) => (
                                    <TableRow key={index}>
                                        {
                                            (model.fields ?? []).map(field => (
                                                <TableCell key={field.key ?? index.toString()} sx={{minWidth: '126px'}}>
                                                    {
                                                        field.datatype === 'number' ?
                                                            formatNumStringToGermanNum(value[field.key ?? ''], field.decimalPlaces) :
                                                            value[field.key ?? '']
                                                    }
                                                </TableCell>
                                            ))
                                        }
                                    </TableRow>
                                )) :
                                <TableRow>
                                    {
                                        (model.fields ?? []).map(field => (
                                            <TableCell key={field.label}>
                                                Keine Angaben
                                            </TableCell>
                                        ))
                                    }
                                </TableRow>
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </>
    );
}
