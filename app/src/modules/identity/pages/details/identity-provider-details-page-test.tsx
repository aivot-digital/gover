import {Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from '@mui/material';
import React, {useContext, useEffect, useState} from 'react';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {useSearchParams} from 'react-router-dom';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityResultState} from '../../enums/identity-result-state';
import {IdentityStateQueryParam} from '../../constants/identity-state-query-param';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {IdentityData} from '../../models/identity-data';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {ApiError, isApiError} from '../../../../models/api-error';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';

export function IdentityProviderDetailsPageTest() {
    const [urlSearchParams, _] = useSearchParams();
    const dispatch = useAppDispatch();
    const canTestIdentityProvider = useHasSystemPermission(Permission.IDENTITY_PROVIDER_UPDATE);

    const {
        item: identityProvider,
    } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    const [identityData, setIdentityData] = useState<IdentityData>();
    const [identityError, setIdentityError] = useState<string>();
    const [isStartingTest, setIsStartingTest] = useState(false);

    useEffect(() => {
        if (identityProvider == null) {
            return;
        }

        const stateStr = urlSearchParams.get(IdentityStateQueryParam);
        const state = stateStr != null ? parseInt(stateStr) : IdentityResultState.UnknownError;

        switch (state) {
            case IdentityResultState.Success:
                setIdentityError(undefined);
                IdentityProvidersApiService
                    .fetchIdentity(true, undefined)
                    .then((res) => {
                        if (res[identityProvider.key] != null) {
                            setIdentityData(res[identityProvider.key]);
                        } else {
                            const err: ApiError = {
                                message: 'Die Identität wurde nicht gesetzt',
                                details: `In der Map der Identitäten ist keine Identität mit dem Provider-Key ${identityProvider.key} vorhanden.`,
                                displayableToUser: true,
                                status: 404,
                            };
                            throw err;
                        }
                    })
                    .catch(err => {
                        console.error(err);
                        if (isApiError(err) && err.displayableToUser) {
                            setIdentityError(err.message);
                        } else {
                            setIdentityError('Beim Abruf der Identität ist ein Fehler aufgetreten');
                        }
                    });
                break;
            default:
            case IdentityResultState.UnknownError:
                setIdentityError('Unbekannter Fehler aufgetreten. Bitte versuchen Sie es erneut.');
                break;
        }
    }, [urlSearchParams, identityProvider]);

    const handleStartTest = async () => {
        if (identityProvider == null || !canTestIdentityProvider || isStartingTest) {
            return;
        }

        setIsStartingTest(true);
        try {
            const redirectUrl = await new IdentityProvidersApiService()
                .startTest(
                    identityProvider.key,
                    `${window.location.origin}${window.location.pathname}`,
                );
            window.location.href = redirectUrl;
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Test des Nutzerkontenanbieters konnte nicht gestartet werden.'));
            setIsStartingTest(false);
        }
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Test des Nutzerkontenanbieters
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Um die korrekte Funktion und Erscheinung eines Nutzerkontos sicherzustellen, können Sie hier einen Test durchführen.
                Nach einem erfolgreichen Test werden Sie auf diese Seite zurückgeleitet und können die Daten einsehen, die an Prosuna übermittelt wurden.
            </Typography>

            <Box
                sx={{
                    mt: 2,
                    mb: 2,
                }}
            >
                <DisabledTooltip
                    title={formatMissingPermissionTooltip(Permission.IDENTITY_PROVIDER_UPDATE)}
                    disabled={!canTestIdentityProvider}
                >
                    <Button
                        onClick={() => {
                            void handleStartTest();
                        }}
                        variant="contained"
                        startIcon={<ScienceOutlinedIcon />}
                        disabled={identityProvider == null || isStartingTest || !canTestIdentityProvider}
                    >
                        Authentifizierung testen
                    </Button>
                </DisabledTooltip>
            </Box>

            {
                identityData != null &&
                identityProvider != null &&
                <Box
                    sx={{
                        mt: 4,
                    }}
                >
                    <Typography
                        variant="h6"
                        sx={{
                            mb: 1,
                        }}
                    >
                        Testergebnisse
                    </Typography>

                    <Typography
                        sx={{
                            mb: 3,
                            maxWidth: 900,
                        }}
                    >
                        Hier sehen Sie die Daten, die von dem Nutzerkontenanbieter an Prosuna übermittelt wurden.
                        Bitte beachten Sie, dass nur die Attribute angezeigt werden, die auch in der Konfiguration des Anbieters zugewiesen worden sind.
                    </Typography>

                    {
                        identityError != null &&
                        <AlertComponent
                            color="error"
                            title="Fehler"
                            text={identityError}
                        />
                    }

                    <TableContainer sx={{border: '1px solid rgba(224, 224, 224, 1)', borderRadius: '4px', my: 2}}>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>
                                        <strong>Feld</strong>
                                    </TableCell>
                                    <TableCell>
                                        <strong>Wert</strong>
                                    </TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {
                                    identityProvider
                                        .attributes
                                        .map((field) => (
                                            <TableRow>
                                                <TableCell>{field.label}</TableCell>
                                                <TableCell>{identityData.attributes[field.keyInData] ?? <i>Kein Wert übergeben</i>}</TableCell>
                                            </TableRow>
                                        ))
                                }
                                {
                                    identityProvider
                                        .attributes
                                        .length === 0 &&
                                    <TableRow>
                                        <TableCell
                                            colSpan={2}
                                            sx={{
                                                color: 'text.secondary',
                                            }}
                                        >
                                            <i>Keine Attribute konfiguriert</i>
                                        </TableCell>
                                    </TableRow>
                                }
                            </TableBody>
                        </Table>
                    </TableContainer>

                    <Typography
                        variant="h6"
                        sx={{
                            mt: 3,
                            mb: 1,
                        }}
                    >
                        Original-Datensatz
                    </Typography>

                    <Typography
                        sx={{
                            mb: 3,
                            maxWidth: 900,
                        }}
                    >
                        Hier sehen Sie, im Gegensatz zu den obigen Testergebnissen, den vollständigen Datensatz, welcher vom Nutzerkontenanbieter an Prosuna übermittelt wurde.
                        Dieser kann auch Attribute enthalten, welche Sie in der Konfiguration des Nutzerkontenanbieters nicht zugewiesen haben.
                        Bitte beachten Sie, dass ausschließlich im Nutzerkontenanbieter zugewiesene Attribute auch innerhalb von Prosuna verwendbar sind.
                    </Typography>

                    <ExpandableCodeBlock
                        value={JSON.stringify(identityData.attributes, null, '\t')}
                        language="json"
                    />
                </Box>
            }
        </Box>
    );
}
