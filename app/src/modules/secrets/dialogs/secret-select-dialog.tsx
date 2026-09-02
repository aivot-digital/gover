import {useEffect, useMemo, useState} from 'react';
import {SearchBaseDialog} from '../../../dialogs/search-base-dialog/search-base-dialog';
import {type Secret} from '../models/secret';
import {SecretsApiService} from '../secrets-api-service';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';

export interface SecretSelectDialogProps {
    id?: string;
    open: boolean;
    onClose: () => void;
    onSelect: (secret: Secret) => void;
}

export function SecretSelectDialog(props: SecretSelectDialogProps) {
    const {id, open, onClose, onSelect} = props;
    const api = useApi();
    const dispatch = useAppDispatch();
    const [secrets, setSecrets] = useState<Secret[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (!open) {
            return;
        }

        let active = true;
        setIsLoading(true);

        new SecretsApiService(api)
            .listAll()
            .then((response) => {
                if (active) {
                    setSecrets(response.content);
                }
            })
            .catch((error) => {
                if (active) {
                    setSecrets([]);
                    dispatch(showApiErrorSnackbar(error, 'Geheimnisse konnten nicht geladen werden.'));
                }
            })
            .finally(() => {
                if (active) {
                    setIsLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [api, dispatch, open]);

    const tabs = useMemo(() => [{
        title: 'Geheimnisse',
        options: secrets,
        onSelect: (secret: Secret) => {
            onSelect(secret);
            onClose();
        },
        searchPlaceholder: 'Geheimnis suchen',
        searchKeys: ['name', 'description'] as Array<keyof Secret>,
        primaryTextKey: 'name' as const,
        secondaryTextKey: 'description' as const,
        getId: 'key' as const,
        noOptionsMessage: isLoading
            ? 'Geheimnisse werden geladen ...'
            : 'Keine Geheimnisse verfügbar.',
        noSearchResultsMessage: 'Keine Geheimnisse gefunden, die zum Suchbegriff passen.',
    }], [isLoading, onClose, onSelect, secrets]);

    return (
        <SearchBaseDialog
            id={id}
            open={open}
            onClose={onClose}
            title="Geheimnis auswählen"
            tabs={tabs}
        />
    );
}
