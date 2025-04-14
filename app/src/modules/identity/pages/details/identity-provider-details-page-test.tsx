import {Box, Button, Table, TableBody, TableCell, TableHead, TableRow, Typography} from '@mui/material';
import {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {useSearchParams} from 'react-router-dom';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';

const SuccessSearchParam = 'success';

export function IdentityProviderDetailsPageTest() {
    const [urlSearchParams, _] = useSearchParams();

    const {
        item: identityProvider,
    } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    const [identityData, setIdentityData] = useState<any>();

    const testLink = useMemo(() => {
        if (identityProvider == null) {
            return '#';
        }

        const redirectLink = new URL(window.location.href);
        redirectLink.searchParams.append(SuccessSearchParam, 'true');

        return ''; // TODO: IdentityProvidersApiService.createLink(identityProvider.key, redirectLink.toString());
    }, [identityProvider]);

    useEffect(() => {
        if (urlSearchParams.has(SuccessSearchParam)) {
            /* TODO:
            IdentityProvidersApiService
                .fetchIdentity()
                .then(setIdentityData)
                .catch(err => {
                    console.error(err);
                });

             */
        }
    }, [urlSearchParams]);

    return (
        <Box>
            <Typography variant="body1">
                Sie können hier einen Test durchführen, um zu sehen, wie die Anmeldung für Ihre Benutzer aussehen wird.
                Nach einer erfolgreichen Test werden Sie auf diese Seite zurückgeleitet und können die Daten einsehen, die an Gover übermittelt wurden.
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
                    disabled={identityProvider == null}
                >
                    Authentifizierung testen
                </Button>
            </Box>

            {
                identityData != null &&
                identityProvider != null &&
                <Box
                    sx={{
                        mt: 4,
                    }}
                >
                    <Typography variant="h6">
                        Testergebnisse
                    </Typography>

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
                                            <TableCell>{identityData[field.keyInData] ?? <i>Kein Wert übergeben</i>}</TableCell>
                                        </TableRow>
                                    ))
                            }
                        </TableBody>
                    </Table>
                </Box>
            }
        </Box>
    );
}