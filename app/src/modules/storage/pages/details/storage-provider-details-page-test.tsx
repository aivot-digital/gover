import {Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {useSearchParams} from 'react-router-dom';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import type {StorageProviderEntity} from '../../entities/storage-provider-entity';
import {IdentityData} from '../../../identity/models/identity-data';
import {IdentityIdQueryParam} from '../../../identity/constants/identity-id-query-param';
import {IdentityStateQueryParam} from '../../../identity/constants/identity-state-query-param';
import {IdentityResultState} from '../../../identity/enums/identity-result-state';
import {IdentityProvidersApiService} from '../../../identity/identity-providers-api-service';

export function StorageProviderDetailsPageTest() {
    const [urlSearchParams, _] = useSearchParams();

    const {
        item: storageProvider,
    } = useContext<GenericDetailsPageContextType<StorageProviderEntity, void>>(GenericDetailsPageContext);

    const [identityData, setIdentityData] = useState<IdentityData>();
    const [identityError, setIdentityError] = useState<string>();

    const testLink = useMemo(() => {
        if (storageProvider == null) {
            return '#';
        }

        return '#';
        //return IdentityProvidersApiService.createLink(identityProvider.key);
    }, [storageProvider]);

    useEffect(() => {
        const stateStr = urlSearchParams.get(IdentityStateQueryParam);
        const state = stateStr != null ? parseInt(stateStr) : IdentityResultState.UnknownError;
        const id = urlSearchParams.get(IdentityIdQueryParam);

        switch (state) {
            case IdentityResultState.Success:
                if (id == null) {
                    setIdentityError('Es wurde kein Nutzerkonto übergeben.');
                    break;
                }

                setIdentityError(undefined);
                IdentityProvidersApiService
                    .fetchIdentity(id)
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
                Test des Speicheranbieters
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Um die korrekte Funktion eines Speicheranbieters sicherzustellen, können Sie hier einen Test durchführen.
                Technisch wird ein Health-Check durchgeführt, bei welchem Gover eine Anfrage an den Speicheranbieter sendet und prüft, ob eine erfolgreiche Verbindung aufgebaut werden kann.
            </Typography>

            <Box
                sx={{
                    mt: 2,
                    mb: 2,
                }}
            >
                <Button
                    component="a"
                    href={testLink}
                    variant="contained"
                    startIcon={<ScienceOutlinedIcon />}
                    disabled={storageProvider == null}
                >
                    Speicheranbieter testen
                </Button>
            </Box>

            {
                identityData != null &&
                storageProvider != null &&
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
                        Hier sehen Sie die Daten, die von dem Nutzerkontenanbieter an Gover übermittelt wurden.
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
                        Hier sehen Sie, im Gegensatz zu den obigen Testergebnissen, den vollständigen Datensatz, welcher vom Nutzerkontenanbieter an Gover übermittelt wurde.
                        Dieser kann auch Attribute enthalten, welche Sie in der Konfiguration des Nutzerkontenanbieters nicht zugewiesen haben.
                        Bitte beachten Sie, dass ausschließlich im Nutzerkontenanbieter zugewiesene Attribute auch innerhalb von Gover verwendbar sind.
                    </Typography>

                    <ExpandableCodeBlock value={JSON.stringify(identityData.attributes, null, '\t')} />
                </Box>
            }
        </Box>
    );
}