import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Table, TableCell, TableContainer, TableHead, TableRow, Typography, TableBody, Grid} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form-elements/input-elements/table-field-element';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';

export function TableFieldComponentSummary({model, value}: BaseSummaryProps<TableFieldElement>) {
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
                                    <TableCell
                                        colSpan={(model.fields ?? []).length}
                                    >
                                        Keine Angabe
                                    </TableCell>
                                </TableRow>
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </>
    );
}
