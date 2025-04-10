import {createTheme} from '@mui/material';
import {deDE} from '@mui/x-data-grid/locales/deDE';
import {deDE as coreDeDE} from '@mui/material/locale';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';

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
            fontSize: '1.802rem',
            lineHeight: '2rem',
        },
        h2: {
            fontFamily: fontStackHeadlines,
            fontWeight: 700,
            fontSize: '1.602rem',
        },
        h3: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1.424rem',
        },
        h4: {
            fontFamily: fontStackHeadlines,
            fontWeight: 600,
            fontSize: '1.266rem',
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
                arrow: {
                    color: '#16191F',
                },
                tooltip: {
                    backgroundColor: '#16191F',
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
                iconContainer: {
                    color: 'rgba(0, 0, 0, 0.55)',
                },
                label: {
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
                    'color': 'rgba(0, 0, 0, 0.55)',
                },
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
                    error: <ErrorOutlineOutlinedIcon/>,
                    info: <InfoOutlinedIcon/>,
                    success: <CheckCircleOutlinedIcon/>,
                    warning: <ReportOutlinedIcon/>,
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
                root: {
                    '&.Mui-disabled': {
                        backgroundColor: '#F8F8F8',
                        cursor: 'not-allowed',
                    },
                },
                input: {
                    '&.Mui-disabled': {
                        backgroundColor: '#F8F8F8',
                        WebkitTextFillColor: 'rgba(0, 0, 0, 0.66)', // Make text more readable in disabled state
                        cursor: 'not-allowed',
                    },
                },
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
        MuiTab: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
                    fontSize: '0.9375rem',
                },
            },
        },
    },
}, deDE, coreDeDE);
