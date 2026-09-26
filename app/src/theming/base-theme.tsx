import {createTheme} from '@mui/material';
import {gridClasses} from '@mui/x-data-grid';
import {deDE} from '@mui/x-data-grid/locales';
import {deDE as coreDeDE} from '@mui/material/locale';
import CheckCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import ReportOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Report';
import type {} from '@mui/x-data-grid/themeAugmentation';
import {getDisabledFieldBackground} from './field-state-colors';

const fontStackHeadlines = ['"Public Sans"',
    '-apple-system',
    'BlinkMacSystemFont',
    '"Segoe UI"',
    'Roboto',
    '"Helvetica Neue"',
    'Arial',
    'sans-serif',
    '"Apple Color Emoji"',
    '"Segoe UI Emoji"',
    '"Segoe UI Symbol"',
].join(',');

const fontStackBodyCopy = ['"Public Sans"',
    '-apple-system',
    'BlinkMacSystemFont',
    '"Segoe UI"',
    'Roboto',
    '"Helvetica Neue"',
    'Arial',
    'sans-serif',
    '"Apple Color Emoji"',
    '"Segoe UI Emoji"',
    '"Segoe UI Symbol"',
].join(',');

const fontStackAccentCopy = ['"Public Sans"',
    '-apple-system',
    'BlinkMacSystemFont',
    '"Segoe UI"',
    'Roboto',
    '"Helvetica Neue"',
    'Arial',
    'sans-serif',
    '"Apple Color Emoji"',
    '"Segoe UI Emoji"',
    '"Segoe UI Symbol"',
].join(',');

export const BaseTheme = createTheme({
    palette: {
        contrastThreshold: 4.5,
    },
    shape: {
        borderRadius: 4,
    },
    typography: {
        fontFamily: fontStackBodyCopy,
        h1: {
            fontFamily: fontStackHeadlines,
            fontWeight: 700,
            fontSize: '1.75rem',
        },
        h2: {
            fontFamily: fontStackHeadlines,
            fontWeight: 700,
            fontSize: '1.5rem',
        },
        h3: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1.375rem',
        },
        h4: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1.25rem',
        },
        h5: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1.125rem',
        },
        h6: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1rem',
        },
        subtitle1: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
        },
        body1: {
            fontFamily: fontStackBodyCopy,
        },
        body2: {
            fontFamily: fontStackAccentCopy,
            fontSize: '1rem',
            lineHeight: '1.5',
        },
    },
    components: {
        MuiTypography: {
            styleOverrides: {
                root: {
                    whiteSpace: 'break-spaces',
                },
            },
        },
        MuiIcon: {
            styleOverrides: {
                root: {
                    boxSizing: 'content-box',
                    padding: 3,
                    fontSize: '1.125rem',
                },
            },
        },
        MuiTooltip: {
            styleOverrides: {
                tooltip: {
                    fontSize: '0.8125rem',
                },
            },
        },
        MuiButton: {
            styleOverrides: {
                root: {
                    fontFamily: fontStackHeadlines,
                    alignItems: 'center',
                    textTransform: 'none',
                    fontSize: '0.9375rem',
                },
            },
            defaultProps: {
                disableElevation: true,
            },
        },
        MuiStepContent: {
            styleOverrides: {
                root: {
                    paddingTop: '1em',
                    paddingLeft: '45px',
                    marginLeft: '20px',
                },
            },
        },
        MuiStepConnector: {
            styleOverrides: {
                root: {
                    marginLeft: '20px',
                    display: 'none',
                },
            },
        },
        MuiStepLabel: {
            styleOverrides: {
                iconContainer: ({theme}) => ({
                    color: theme.palette.text.secondary,
                }),
                label: ({theme}) => ({
                    'fontFamily': fontStackHeadlines,
                    'fontWeight': 500,
                    'fontSize': '1.3125rem',
                    'paddingTop': '4px',
                    'marginLeft': '15px',
                    '&.Mui-completed': {
                        '.completed-step-suffix': {
                            display: 'inline-block',
                        },
                    },
                    '.completed-step-suffix': {
                        display: 'none',
                    },
                    'color': theme.palette.text.secondary,
                }),
            },
        },
        MuiTextField: {
            defaultProps: {
                fullWidth: true,
                margin: 'normal',
                variant: 'outlined',
            },
        },
        MuiFormControl: {
            defaultProps: {
                fullWidth: true,
                margin: 'normal',
                variant: 'outlined',
            },
        },
        MuiAlert: {
            styleOverrides: {
                root: {
                    fontFamily: fontStackAccentCopy,
                    fontSize: '1rem',
                    padding: '14px 18px',
                },
            },
            defaultProps: {
                iconMapping: {
                    error: <ErrorOutlineOutlinedIcon />,
                    info: <InfoOutlinedIcon />,
                    success: <CheckCircleOutlinedIcon />,
                    warning: <ReportOutlinedIcon />,
                },
            },
        },
        MuiAlertTitle: {
            styleOverrides: {
                root: {
                    fontFamily: fontStackHeadlines,
                    fontWeight: 500,
                    fontSize: '1.125rem',
                    marginBottom: '.125rem',
                },
            },
        },
        MuiOutlinedInput: {
            styleOverrides: {
                root: ({theme}) => ({
                    '&.Mui-disabled': {
                        backgroundColor: getDisabledFieldBackground(theme),
                        cursor: 'not-allowed',
                    },
                }),
                input: ({theme}) => ({
                    '&.Mui-disabled': {
                        WebkitTextFillColor: theme.palette.text.secondary,
                        cursor: 'not-allowed',
                    },
                }),
            },
        },
        MuiFormControlLabel: {
            styleOverrides: {
                root: {
                    '&.Mui-disabled': {
                        cursor: 'not-allowed',
                    },
                },
            },
        },
        MuiTableRow: {
            styleOverrides: {
                root: {
                    '&:last-child td': {
                        borderBottom: 0,
                    },
                },
            },
        },
        MuiDialogActions: {
            styleOverrides: {
                root: {
                    paddingBottom: 3 * 8,
                    paddingLeft: 3 * 8,
                    paddingRight: 3 * 8,
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    rowGap: '10px',
                },
            },
        },
        MuiDialog: {
            defaultProps: {
                slotProps: {
                    paper: {
                        elevation: 1,
                    },
                },
            },
        },
        MuiDrawer: {
            defaultProps: {
                elevation: 1,
            },
        },
        MuiTab: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
                    fontSize: '0.9375rem',
                },
            },
        },
        MuiDataGrid: {
            styleOverrides: {
                root: ({theme}) => ({
                    backgroundColor: `${theme.palette.background.paper} !important`,
                    borderBottom: 0,
                    // MUI X v9 paints the sort button with the header color. Transparent custom
                    // headers would otherwise composite that color twice and show a solid circle.
                    [`& .${gridClasses.columnHeader} .${gridClasses.sortButton}`]: {
                        backgroundColor: 'transparent',
                        '&:hover': {
                            backgroundColor: theme.palette.action.hover,
                        },
                    },
                }),
                row: ({theme}) => ({
                    '&:hover': {
                        backgroundColor: theme.palette.action.hover,
                    },
                }),
            },
        },
    },
}, deDE, coreDeDE);
