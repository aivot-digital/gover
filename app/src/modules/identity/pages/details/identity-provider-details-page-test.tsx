import {
    Box,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography
} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {useSearchParams} from 'react-router-dom';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityResultState} from '../../enums/identity-result-state';
import {IdentityStateQueryParam} from '../../constants/identity-state-query-param';
import {AlertComponent} from '../../../../components/alert/alert-component';
import Tooltip from "@mui/material/Tooltip";
import {IdentityData} from "../../models/identity-data";

export function IdentityProviderDetailsPageTest() {
    const [urlSearchParams, _] = useSearchParams();

    const {
        item: identityProvider,
    } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    const [identityData, setIdentityData] = useState<IdentityData>();
    const [identityError, setIdentityError] = useState<string>();

    const testLink = useMemo(() => {
        if (identityProvider == null) {
            return '#';
        }

        return IdentityProvidersApiService.createLink(identityProvider.key);
    }, [identityProvider]);

    useEffect(() => {
        const stateStr = urlSearchParams.get(IdentityStateQueryParam);
        const state = stateStr != null ? parseInt(stateStr) : IdentityResultState.UnknownError;

        switch (state) {
            case IdentityResultState.Success:
                setIdentityError(undefined);
                IdentityProvidersApiService
                    .fetchIdentity()
                    .then(setIdentityData)
                    .catch(err => {
                        console.error(err);
                    });
                break;
            default:
            case IdentityResultState.UnknownError:
                setIdentityError('Unbekannter Fehler aufgetreten. Bitte versuchen Sie es erneut.');
                break;
        }
    }, [urlSearchParams]);

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
                Nach einem erfolgreichen Test werden Sie auf diese Seite zurückgeleitet und können die Daten einsehen, die an Gover übermittelt wurden.
            </Typography>

            <Box
                sx={{
                    mt: 2,
                    mb: 2,
                }}
            >
                <Tooltip
                    title={
                        identityProvider?.isEnabled === false
                            ? 'Der Anbieter muss aktiviert sein, um die Authentifizierung zu testen.'
                            : ''
                    }
                    disableHoverListener={identityProvider?.isEnabled !== false}
                >
                    <span>
                        <Button
                            component="a"
                            href={testLink}
                            variant="contained"
                            startIcon={<ScienceOutlinedIcon />}
                            disabled={identityProvider == null || identityProvider?.isEnabled === false}
                        >
                            Authentifizierung testen
                        </Button>
                    </span>
                </Tooltip>
            </Box>

            {
                identityData != null &&
                identityProvider != null &&
                <Box
                    sx={{
                        mt: 4,
                    }}
                >
                    <Typography variant="h6" sx={{mb: 1}}>
                        Testergebnisse
                    </Typography>

                    <Typography sx={{mb: 3, maxWidth: 900}}>
                        Hier sehen Sie die Daten, die von dem Nutzerkontenanbieter an Gover übermittelt wurden. Bitte beachten Sie, dass nur die Attribute angezeigt werden, die auch in der Konfiguration des Anbieter zugewiesen worden sind.
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
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            }
        </Box>
    );
}