import {StatusTableProps} from './status-table-props';
import {
    Box,
    Button,
    Card,
    Collapse,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import React, {useState} from 'react';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import KeyboardArrowUp from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowUp';

export function StatusTable(props: StatusTableProps) {
    const theme = useTheme();
    const lgAndUp = useMediaQuery(theme.breakpoints.up('lg'));
    const [
        expandedItems,
        setExpandedItems,
    ] = useState<Record<string, boolean>>({});

    const isExpanded = (item: StatusTableProps['items'][number]): boolean => {
        return expandedItems[item.label] ?? item.detailsDefaultExpanded ?? false;
    };

    const toggleDetails = (item: StatusTableProps['items'][number]): void => {
        setExpandedItems((prev) => ({
            ...prev,
            [item.label]: !(prev[item.label] ?? item.detailsDefaultExpanded ?? false),
        }));
    };

    return (
        <Box
            sx={{
                mt: 4,
                ...props.sx,
            }}
        >
            {
                props.description != null &&
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
            }

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
                        size={props.dense ? 'small' : 'medium'}
                    >
                        <TableBody>
                            {
                                props.items.map(item => {
                                    const itemHasSubItems = item.subItems != null && item.subItems.length > 0;
                                    const itemHasDetails = item.details != null;
                                    const itemIsExpanded = isExpanded(item);
                                    const labelVerticalAlign = item.alignTop ? 'top' : 'middle';
                                    return (
                                        <React.Fragment key={item.label}>
                                            <TableRow>
                                                <TableCell
                                                    scope="th"
                                                    width={lgAndUp ? '25%' : '50%'}
                                                    sx={itemHasSubItems ? {
                                                        borderBottom: 'none',
                                                        pb: 0,
                                                        verticalAlign: labelVerticalAlign,
                                                    } : {
                                                        verticalAlign: labelVerticalAlign,
                                                    }}
                                                >
                                                    {item.label}
                                                </TableCell>
                                                <TableCell
                                                    sx={itemHasSubItems ? {
                                                        borderBottom: 'none',
                                                        pb: 0,
                                                    } : undefined}
                                                >
                                                    <Box
                                                        display="flex"
                                                        alignItems={item.alignTop ? 'flex-start' : 'center'}
                                                    >
                                                        {item.icon}
                                                        <Box sx={{ml: item.icon != null ? 1.5 : undefined}}>
                                                            {item.children}
                                                        </Box>
                                                    </Box>
                                                    {
                                                        itemHasDetails &&
                                                        <Box
                                                            sx={{
                                                                mt: 1,
                                                                ml: item.indentDetails === true && item.icon != null ? 4.5 : undefined,
                                                            }}
                                                        >
                                                            <Button
                                                                size="small"
                                                                variant="text"
                                                                endIcon={itemIsExpanded ? <KeyboardArrowUp/> : <KeyboardArrowDown/>}
                                                                onClick={() => toggleDetails(item)}
                                                                sx={{
                                                                    minWidth: 0,
                                                                    px: 0,
                                                                    py: 0.25,
                                                                    justifyContent: 'flex-start',
                                                                    textTransform: 'none',
                                                                    fontWeight: 400,
                                                                    color: 'text.secondary',
                                                                    '&:hover': {
                                                                        backgroundColor: 'transparent',
                                                                        color: 'primary.main',
                                                                        textDecoration: 'underline',
                                                                    },
                                                                    '& .MuiButton-endIcon': {
                                                                        ml: 0.5,
                                                                    },
                                                                }}
                                                            >
                                                                {
                                                                    itemIsExpanded ?
                                                                        (item.detailsExpandedLabel ?? 'Details ausblenden') :
                                                                        (item.detailsLabel ?? 'Details anzeigen')
                                                                }
                                                            </Button>
                                                            <Collapse in={itemIsExpanded}>
                                                                <Box sx={{mt: 1}}>
                                                                    {item.details}
                                                                </Box>
                                                            </Collapse>
                                                        </Box>
                                                    }
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
                                    );
                                })
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            </Card>
        </Box>
    );
}
