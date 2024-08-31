import {StatusTableProps} from './status-table-props';
import {Box, Card, Table, TableBody, TableCell, TableContainer, TableRow, Typography, useMediaQuery, useTheme} from '@mui/material';
import React from 'react';

export function StatusTable(props: StatusTableProps) {
    const theme = useTheme();
    const lgAndUp = useMediaQuery(theme.breakpoints.up('lg'));

    return (
        <Box
            sx={{
                mt: 4,
                ...props.sx,
            }}
        >
            <Box
                display="flex"
                alignItems="center"
            >
                <Typography
                    variant={props.labelVariant ?? 'subtitle1'}
                    sx={{
                        mr: 1,
                        ...props.labelSx,
                    }}
                >
                    {props.label}
                </Typography>

                {props.labelIcon}
            </Box>

            {
                props.description != null &&
                <Typography
                    sx={props.descriptionSx}
                >
                    {props.description}
                </Typography>
            }

            <Card
                sx={props.cardSx}
                variant={props.cardVariant}
            >
                <TableContainer>
                    <Table
                        sx={{
                            tableLayout: 'fixed',
                        }}
                    >
                        <TableBody>
                            {
                                props.items.map(item => (
                                    <React.Fragment key={item.label}>
                                        <TableRow>
                                            <TableCell
                                                scope="th"
                                                width={lgAndUp ? '25%' : '50%'}
                                                sx={item.subItems != null && item.subItems.length > 0 ? {
                                                    borderBottom: 'none',
                                                    pb: 0,
                                                    verticalAlign: item.alignTop ? 'top' : undefined,
                                                } : {
                                                    verticalAlign: item.alignTop ? 'top' : undefined,
                                                }}
                                            >
                                                {item.label}
                                            </TableCell>
                                            <TableCell
                                                sx={item.subItems != null && item.subItems.length > 0 ? {
                                                    borderBottom: 'none',
                                                    pb: 0,
                                                } : undefined}
                                            >
                                                <Box
                                                    display="flex"
                                                    alignItems={item.alignTop ? 'flex-start' : 'center'}
                                                >
                                                    {item.icon}
                                                    <Box sx={{ml: 1}}>
                                                        {item.children}
                                                    </Box>
                                                </Box>
                                            </TableCell>
                                        </TableRow>

                                        {
                                            item.subItems != null &&
                                            item.subItems.length > 0 &&
                                            item.subItems.map((subItem, index, arr) => (
                                                <TableRow key={subItem.label}>
                                                    <TableCell
                                                        sx={{
                                                            pt: 0,
                                                            pb: index < arr.length - 1 ? 0 : undefined,
                                                            borderBottom: index < arr.length - 1 ? 'none' : undefined,
                                                        }}
                                                    />
                                                    <TableCell
                                                        sx={{
                                                            pt: 0,
                                                            pb: index < arr.length - 1 ? 0 : undefined,
                                                            borderBottom: index < arr.length - 1 ? 'none' : undefined,
                                                        }}
                                                    >
                                                        <Box
                                                            display="flex"
                                                            alignItems="flex-start"
                                                            sx={{
                                                                pl: 1,
                                                            }}
                                                        >
                                                            {subItem.icon}
                                                            <Box sx={{ml: 1}}>
                                                                {subItem.children}
                                                            </Box>
                                                        </Box>
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                        }
                                    </React.Fragment>
                                ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            </Card>
        </Box>
    );
}