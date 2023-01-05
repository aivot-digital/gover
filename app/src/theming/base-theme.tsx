import {createTheme} from '@mui/material';
import {deDE} from '@mui/x-data-grid';
import {deDE as coreDeDE} from '@mui/material/locale';
import {
    faCircleCheck,
    faCircleExclamation,
    faCircleInfo,
    faTriangleExclamation
} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

const fontStackNoto = ['"Noto Sans"',
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

const fontStackRocGrotesk = ['"Roc Grotesk"',
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

const fontStackFranziska = ['"Franziska"',
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
    // ...Themes(Ministry.Innen),
    shape: {
        borderRadius: 0,
    },
    typography: {
        fontFamily: fontStackNoto,
        h1: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 700,
            fontSize: '1.75rem',
            lineHeight: '2rem'
        },
        h2: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        h3: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        h4: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        h5: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        h6: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        subtitle1: {
            fontFamily: fontStackRocGrotesk,
            fontWeight: 500,
        },
        body1: {
            fontFamily: fontStackNoto,
            color: '#444',
        },
        body2: {
            fontFamily: fontStackFranziska,
            color: '#444',
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
                    fontFamily: fontStackRocGrotesk,
                    alignItems: 'center',
                },
            },
            defaultProps: {
                disableElevation: true,
            }
        },
        MuiStepContent: {
            styleOverrides: {
                root: {
                    paddingTop: '1em',
                    paddingLeft: '45px',
                    marginLeft: '20px'
                },
            },
        },
        MuiStepConnector: {
            styleOverrides: {
                root: {
                    marginLeft: '20px',
                    display: 'none'
                },
            },
        },
        MuiStepLabel: {
            styleOverrides: {
                iconContainer: {
                    color: 'rgba(0, 0, 0, 0.4)'
                },
                label: {
                    fontFamily: fontStackRocGrotesk,
                    fontWeight: 500,
                    fontSize: '1.3125rem',
                    paddingTop: '4px',
                    marginLeft: '15px',
                    '&.Mui-completed': {
                        '.completed-step-suffix': {
                            display: 'inline-block',
                        }
                    },
                    '.completed-step-suffix': {
                        display: 'none',
                    },
                    color: 'rgba(0, 0, 0, 0.4)'
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
                    fontFamily: fontStackFranziska,
                    fontSize: '1rem',
                    padding: '14px 18px',
                },
            },
            defaultProps: {
                iconMapping: {
                    error: <FontAwesomeIcon icon={faCircleExclamation}/>,
                    info: <FontAwesomeIcon icon={faCircleInfo}/>,
                    success: <FontAwesomeIcon icon={faCircleCheck}/>,
                    warning: <FontAwesomeIcon icon={faTriangleExclamation}/>,
                },
            },
        },
        MuiAlertTitle: {
            styleOverrides: {
                root: {
                    fontFamily: fontStackRocGrotesk,
                    fontWeight: 500,
                    fontSize: '1.125rem',
                    marginBottom: '.125rem'
                },
            },
        },
        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    '&.Mui-disabled': {
                        backgroundColor: '#F8F8F8',
                        cursor: 'not-allowed'
                    },
                },
                input: {
                    '&.Mui-disabled': {
                        backgroundColor: '#F8F8F8',
                        cursor: 'not-allowed'
                    },
                },
            },
        },
        MuiFormControlLabel: {
            styleOverrides: {
                root: {
                    '&.Mui-disabled': {
                        cursor: 'not-allowed'
                    },
                },
            },
        },
    },
}, deDE, coreDeDE);
