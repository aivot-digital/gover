import {useEffect, useState} from 'react';
import {Box, Button, Stack} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {AssetSelector} from '../../modules/assets/components/asset-selector';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../slices/snackbar-slice';
import {HtmlTemplateInputValue} from '../../models/elements/form/input/html-template-input-element';
import {VStorageIndexItemWithAssetEntity} from '../../modules/storage/entities/storage-index-item-entity';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {isApiError} from '../../models/api-error';
import {HtmlTemplateInputComponentDialog} from './html-template-input-component-dialog';

interface HtmlTemplateInputComponentProps {
    label: string;
    hint?: string | null;
    error?: string | null;
    required?: boolean | null;
    disabled?: boolean;
    value?: HtmlTemplateInputValue | null;
    onChange: (value: HtmlTemplateInputValue | null) => void;
}


export function HtmlTemplateInputComponent(props: HtmlTemplateInputComponentProps) {
    const {
        label,
        hint,
        error,
        required,
        disabled,
        value,
        onChange,
    } = props;

    const dispatch = useAppDispatch();

    const [templateAsset, setTemplateAsset] = useState<VStorageIndexItemWithAssetEntity | null>(null);
    const [isLoadingTemplate, setIsLoadingTemplate] = useState(false);
    const [templateLoadError, setTemplateLoadError] = useState<string | null>(null);
    const [showEditorDialog, setShowEditorDialog] = useState(false);

    const assetKey = value?.assetKey ?? null;
    const slots = value?.slots ?? {};

    useEffect(() => {
        setTemplateLoadError(null);

        if (assetKey == null || isStringNullOrEmpty(assetKey)) {
            setTemplateAsset(null);
            return;
        }

        setIsLoadingTemplate(true);

        const abortController = new AbortController();
        new AssetsApiService()
            .retrieveByKey(assetKey, {
                abort: abortController.signal,
            })
            .then((asset) => {
                setTemplateAsset(asset);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'HTML-Vorlage konnte nicht geladen werden'));

                if (isApiError(err) && err.displayableToUser) {
                    setTemplateLoadError(err.message);
                } else {
                    setTemplateLoadError('Die HTML-Vorlage konnte nicht geladen werden.');
                }
            })
            .finally(() => {
                setIsLoadingTemplate(false);
            });

        return () => {
            abortController.abort();
        };
    }, [assetKey, showEditorDialog]);

    return (
        <Stack
            spacing={2}
            direction="row"
        >
            <AssetSelector
                label={label}
                hint={hint ?? undefined}
                error={error ?? templateLoadError ?? undefined}
                selectLabel="HTML-Vorlage auswählen"
                value={assetKey}
                onChange={(val) => {
                    onChange({
                        assetKey: val,
                        slots: value?.slots ?? {},
                    });
                }}
                disabled={disabled || isLoadingTemplate}
                required={required ?? undefined}
                mimetype="text/html"
                placeholder="Keine HTML-Vorlage ausgewählt"
            />

            <Box>
                <Button
                    variant="outlined"
                    startIcon={<Edit/>}
                    onClick={() => {
                        setShowEditorDialog(true);
                    }}
                    disabled={disabled || assetKey == null || isStringNullOrEmpty(assetKey) || isLoadingTemplate || templateLoadError != null}
                >
                    Anpassen
                </Button>
            </Box>

            <HtmlTemplateInputComponentDialog
                label={label}
                asset={templateAsset}
                slots={slots}
                onChangeSlots={(newSlots) => {
                    onChange({
                        assetKey: assetKey,
                        slots: newSlots,
                    });
                }}
                open={showEditorDialog}
                onClose={() => {
                    setShowEditorDialog(false);
                }}
            />
        </Stack>
    );
}
