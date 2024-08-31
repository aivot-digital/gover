import {Box, Button, Dialog, DialogActions, DialogContent, Typography, useTheme} from '@mui/material';
import {getUrlWithoutQuery} from '../../utils/location-utils';
import React, {useEffect} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {ApiService} from '../../services/api-service';
import {AuthDataDto} from '../../models/dtos/auth-data-dto';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {hydrateCustomerInput, hydrateDisabled, selectCustomerInputValue, setHasLoadedSavedCustomerInput} from '../../slices/app-slice';
import {CustomerInputService} from '../../services/customer-input-service';
import {CustomerInput} from '../../models/customer-input';
import {IdInputProps} from './id-input-props';
import {IdCustomerData} from './id-customer-data';
import {Idp, IdpMappingSource} from '../../data/idp';
import {useSearchParams} from 'react-router-dom';
import {DialogTitleWithClose} from "../dialog-title-with-close/dialog-title-with-close";
import {transformBundIdAttribute} from '../../data/bund-id-attributes';
import {transformBayernIdAttribute} from '../../data/bayern-id-attributes';
import {transformShIdAttribute} from '../../data/sh-id-attributes';
import {transformMukAttribute} from '../../data/muk-attributes';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export const CodeQueryKey = 'code';
export const IdpQueryKey = 'idp';

export const IdCustomerDataKey = '__id_data__';

export function IdInput(props: IdInputProps): JSX.Element {
    const dispatch = useAppDispatch();
    const [searchParams, setSearchParams] = useSearchParams();
    const value: IdCustomerData | undefined = useAppSelector(selectCustomerInputValue(IdCustomerDataKey));

    const redirectUri = getUrlWithoutQuery() + '?' + new URLSearchParams({
        [IdpQueryKey]: props.idpQueryId,
    }).toString();

    useEffect(() => {
        const api = new ApiService();

        const searchParams = new URLSearchParams(window.location.search);
        const idp = searchParams.get(IdpQueryKey);
        const code = searchParams.get(CodeQueryKey);

        if (idp === props.idpQueryId && code != null && code.length > 0) {
            api
                .postFormUrlEncoded<AuthDataDto>(`${props.host}/realms/${props.realm}/protocol/openid-connect/token`, {
                    grant_type: 'authorization_code',
                    client_id: props.client,
                    code: code,
                    redirect_uri: redirectUri,
                })
                .then((authData) => {
                    return api.get<Record<string, any>>(`${props.host}/realms/${props.realm}/protocol/openid-connect/userinfo`, {
                        Authorization: `Bearer ${authData.access_token}`,
                    }).then(userInfo => ({
                        authData: authData,
                        userInfo: userInfo,
                    }));
                })
                .then(({authData, userInfo}) => {
                    const customerData: IdCustomerData = {
                        authData: authData,
                        userInfo: userInfo,
                        idp: idp,
                    };
                    let customerInput: CustomerInput = {
                        [IdCustomerDataKey]: customerData,
                    };
                    const disabledElements: Record<string, boolean> = {};

                    customerInput = {
                        ...customerInput,
                        ...CustomerInputService.loadCustomerInputState(props.form),
                    };

                    for (const elem of props.allElements) {
                        if (elem.metadata != null) {
                            const mapping = elem.metadata[IdpMappingSource[props.idpQueryId]];
                            if (mapping != null) {
                                let elemValue = userInfo[mapping];
                                if (elemValue != null && isStringNotNullOrEmpty(elemValue)) {
                                    if (idp === Idp.BundId) {
                                        elemValue = transformBundIdAttribute(mapping, elemValue)
                                    } else if (idp === Idp.BayernId) {
                                        elemValue = transformBayernIdAttribute(mapping, elemValue)
                                    } else if (idp === Idp.ShId) {
                                        elemValue = transformShIdAttribute(mapping, elemValue)
                                    } else if (idp === Idp.Muk) {
                                        elemValue = transformMukAttribute(mapping, elemValue)
                                    }
                                    customerInput[elem.id] = elemValue;
                                    disabledElements[elem.id] = true;
                                }
                            }
                        }
                    }

                    dispatch(hydrateCustomerInput(customerInput));
                    dispatch(hydrateDisabled(disabledElements));
                    dispatch(setHasLoadedSavedCustomerInput(true));

                    const idpLoginHeadline = document.getElementById("idp-login");
                    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

                    if(idpLoginHeadline){
                        idpLoginHeadline.scrollIntoView({
                            block: 'start',
                            behavior: prefersReducedMotion ? 'auto' : 'smooth',
                        });
                    }
                    setOpenSuccessDialog(true);
                })
                .catch((error) => {
                    console.error(error);
                });
        }
    }, []);

    const isSuccessful = value != null && value.idp === props.idpQueryId;
    const theme = useTheme();
    const successColor = theme.palette.success.main; // Greift auf die Haupt-"success"-Farbe zu
    const successColorWithOpacity = `rgba(${parseInt(successColor.slice(1, 3), 16)}, ${parseInt(successColor.slice(3, 5), 16)}, ${parseInt(successColor.slice(5, 7), 16)}, 0.04)`;
    const [openSuccessDialog, setOpenSuccessDialog] = React.useState(false);

    const handleClose = () => {
        setOpenSuccessDialog(false);
    };

    return (
        <>
            <Button
                variant="outlined"
                color={
                    isSuccessful
                        ? 'success'
                        : 'primary'
                }
                fullWidth
                component={isSuccessful ? 'div' : 'a'}
                href={isSuccessful ? undefined : `${props.host}/realms/${props.realm}/protocol/openid-connect/auth?${new URLSearchParams({
                    client_id: props.client,
                    redirect_uri: redirectUri,
                    response_type: 'code',
                    login: 'true',
                    scope: 'openid profile email' + (props.getScope != null ? (' ' + props.getScope(props.form)) : ''),
                    kc_idp_hint: props.broker,
                    backURL: getUrlWithoutQuery(),
                }).toString()}`}
                sx={{
                    textTransform: 'none',
                    p: 1.5,
                    mt: 2,
                    backgroundColor: isSuccessful ? successColorWithOpacity : "inherit",
                    justifyContent: "start",
                    flexDirection: {xs: "column", md: "row"},
                }}
                disabled={!isSuccessful && value != null}
            >
                <Box sx={{
                    opacity: (!isSuccessful && value != null) ? 0.6 : 1,
                    width: {md: 210},
                    flexShrink: {md: 0},
                    pr: {md: 4},
                    mr: {md: 4},
                    textAlign: {md: "center"},
                    borderRight: {md: "1px solid #bdbdbd"},
                    display: "flex",
                    justifyContent: "center",
                }}>
                    {props.icon}
                </Box>
                <Typography
                    color="inherit"
                    sx={{
                        mt: {xs: 1, md: 0},
                        maxWidth: {xs: 420, md: "100%"},
                        textAlign: {xs: "center", md: "left"},
                    }}
                >
                    {isSuccessful ? props.successMessage : props.callToAction}
                </Typography>
            </Button>
            <Dialog onClose={handleClose} open={openSuccessDialog} maxWidth={"xs"}>
                <DialogTitleWithClose
                    onClose={handleClose}
                    closeTooltip="Schließen"
                >
                    Authentifizierung erfolgreich
                </DialogTitleWithClose>
                <DialogContent className={"content-without-margin-on-childs"}>
                    <p>{props.successMessage} Die Daten aus Ihrem Konto wurden automatisch in den Antrag übernommen.</p>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleClose}
                        variant='contained'
                    >
                        Mit Antrag fortfahren
                    </Button>
                </DialogActions>

            </Dialog>
        </>
    );
}