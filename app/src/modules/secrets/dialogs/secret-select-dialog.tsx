import {useEffect, useState} from 'react';
import {Box, CircularProgress} from '@mui/material';
import Key from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import {SearchBaseDialog} from '../../../dialogs/search-base-dialog/search-base-dialog';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {SecretsApiService} from '../secrets-api-service';
import {type Secret} from '../models/secret';

export interface SecretSelectDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (secret: Secret) => void;
}

export function SecretSelectDialog(props: SecretSelectDialogProps) {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [secrets, setSecrets] = useState<Secret[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadFailed, setLoadFailed] = useState(false);

    useEffect(() => {
        let active = true;

        if (!props.open) {
            return () => {
                active = false;
            };
        }

        setSecrets([]);
        setLoading(true);
        setLoadFailed(false);

        void new SecretsApiService(api)
            .listAllOrdered('name', 'ASC')
            .then((page) => {
                if (active) {
                    setSecrets(page.content);
                }
            })
            .catch((error) => {
                if (active) {
                    setLoadFailed(true);
                    dispatch(showApiErrorSnackbar(error, 'Geheimnisse konnten nicht geladen werden.'));
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [api, dispatch, props.open]);

    return (
        <SearchBaseDialog
            open={props.open}
            onClose={props.onClose}
            title="Geheimnis auswählen"
            tabs={[
                {
                    title: 'Geheimnisse',
                    options: secrets,
                    onSelect: (secret) => {
                        props.onSelect(secret);
                        props.onClose();
                    },
                    searchPlaceholder: 'Geheimnis suchen',
                    searchKeys: ['name', 'description'],
                    primaryTextKey: 'name',
                    secondaryTextKey: 'description',
                    getId: 'key',
                    getIcon: () => <Key/>,
                    noOptionsMessage: loading ? (
                        <Box sx={{display: 'flex', justifyContent: 'center'}}>
                            <CircularProgress size={24}/>
                        </Box>
                    ) : loadFailed ? (
                        'Geheimnisse konnten nicht geladen werden.'
                    ) : (
                        'Keine Geheimnisse verfügbar.'
                    ),
                    noSearchResultsMessage: 'Keine passenden Geheimnisse gefunden.',
                },
            ]}
        />
    );
}
