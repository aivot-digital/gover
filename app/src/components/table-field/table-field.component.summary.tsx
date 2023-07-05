import {Table, TableCell, TableContainer, TableHead, TableRow, Typography, TableBody, Grid} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseSummaryProps} from "../../summaries/base-summary";

// TODO: Value Type
export function TableFieldComponentSummary({model, value}: BaseSummaryProps<TableFieldElement, any>) {
    return (
        <>
            <Grid
                container
                sx={{
                    mt: 2,
                    borderBottom: "1px solid #D4D4D4",
                    py: 1
                }}
            >
                <Grid
                    item
                    xs={4}
                    sx={{textAlign: "right", pr: 5}}
                >
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
            <TableContainer sx={{mb: 3}}>
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
                                                <TableCell key={field.label}>
                                                    {
                                                        field.datatype === 'number' ?
                                                            formatNumStringToGermanNum(value[field.label], field.decimalPlaces) :
                                                            value[field.label]
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
