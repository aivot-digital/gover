import React from 'react';
import {Box, Button, Container, type SxProps, Typography, useTheme} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showDialog} from '../../../slices/app-slice';
import {PrivacyDialogId} from '../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../../dialogs/imprint-dialog/imprint-dialog';
import {Logo} from '../../../components/logo/logo';
import {ProsunaAttribution} from '../../../components/prosuna-attribution/prosuna-attribution';
import {ProcessInstanceStatusResponse} from "./customer-task-view-api-service";
import HelpOutlineOutlinedIcon from "@aivot/mui-material-symbols-400-n25-outlined/Help";
import {HelpDialogId} from "../../../dialogs/help-dialog/help.dialog";

const buttonStyle: SxProps = {
    color: 'text.primary',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.225rem',
};

interface CustomerListPageFooterProps {
    status: ProcessInstanceStatusResponse;
}

export function CustomerInstanceViewFooter(props: CustomerListPageFooterProps) {
    const {
        status,
    } = props;

    const theme = useTheme();
    const dispatch = useAppDispatch();
    const providerName = AppConfig.providerName.trim();


    return (
        <Box
            component="footer"
            role="contentinfo"
            sx={{
                boxShadow: 'inset 0px 10px 20px rgba(0, 0, 0, 0.06)',
                backgroundColor: 'background.paper',
            }}
        >
            <Container>
                <Box
                    sx={{
                        display: 'flex',
                        pt: 8,
                        pb: 10,
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        [theme.breakpoints.down('md')]: {
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                            pt: 4,
                            pb: 7,
                        },
                    }}
                >
                    <Logo
                        width={200}
                        height={100}
                    />

                    <Box
                        component="nav"
                        role="navigation"
                        aria-label="Rechtliche Informationen"
                        sx={{
                            [theme.breakpoints.down('md')]: {
                                ml: -2,
                            },
                        }}
                    >
                        <Box
                            sx={{
                                mb: 1,
                                display: 'flex',
                                justifyContent: 'flex-end',
                                flexWrap: 'wrap',
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                    justifyContent: 'flex-start',
                                },
                            }}
                        >
                            {
                                status.legalSupportDepartmentId != null &&
                                status.technicalSupportDepartmentId != null &&
                                <Button
                                    startIcon={
                                        <HelpOutlineOutlinedIcon/>
                                    }
                                    sx={buttonStyle}
                                    size="medium"
                                    onClick={() => dispatch(showDialog(HelpDialogId))}
                                >
                                    Hilfe
                                </Button>
                            }
                            {
                                status.privacyDepartmentId != null &&
                                <Button
                                    sx={buttonStyle}
                                    size="medium"
                                    onClick={() => dispatch(showDialog(PrivacyDialogId))}
                                >
                                    Datenschutz
                                </Button>
                            }

                            {
                                status.imprintDepartmentId != null &&
                                <Button
                                    sx={buttonStyle}
                                    size="medium"
                                    onClick={() => dispatch(showDialog(ImprintDialogId))}
                                >
                                    Impressum
                                </Button>
                            }
                        </Box>

                        <Box>
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{
                                    color: 'text.secondary',
                                    mt: 2,
                                    ml: 2.5,
                                    textAlign: 'left',
                                    [theme.breakpoints.up('md')]: {
                                        textAlign: 'right',
                                        mt: 0,
                                        mr: 1,
                                        ml: 0,
                                    },
                                }}
                            >
                                {
                                    providerName.length > 0 &&
                                    <>{providerName} &bull; </>
                                }
                                Alle Rechte vorbehalten.
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            </Container>

            <ProsunaAttribution placement="listing"/>
        </Box>
    );
}
