import {StatusTableProps} from './status-table-props';
import {Box, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from '@mui/material';
import React from 'react';

export function StatusTable(props: StatusTableProps) {
    return (
        <Box
            sx={{
                mt: 4,
            }}
        >
            <Typography
                variant="h6"
                sx={{
                    mb: 2,
                }}
            >
                {props.label}
            </Typography>

            <TableContainer>
                <Table
                    sx={{
                        tableLayout: 'fixed',
                    }}
                >
                    <TableBody>
                        {
                            props.items.map(item => (
                                <TableRow key={item.label}>
                                    <TableCell scope="th">
                                        {item.label}
                                    </TableCell>
                                    <TableCell>
                                        <Box
                                            display="flex"
                                            alignItems="center"
                                        >
                                            {item.icon}
                                            <Box sx={{ml: 1}}>
                                                {item.children}
                                            </Box>
                                        </Box>
                                    </TableCell>
                                </TableRow>
                            ))
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}