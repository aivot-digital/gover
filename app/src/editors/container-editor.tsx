import React, {useEffect, useState} from 'react';
import {type BaseEditorProps} from './base-editor';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type MarketplaceDetailModule} from '../models/entities/marketplace-detail-module';
import {ProsunaMarketplaceService} from '../services/prosuna-marketplace.service';
import {Button, Grid, Paper} from '@mui/material';
import {AlertComponent} from '../components/alert/alert-component';
import LinkOffOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LinkOff';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';
import {MarketplaceModuleInfoTable} from '../components/marketplace-module-info-table/marketplace-module-info-table';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectSystemConfigValue} from '../slices/system-config-slice';
import {SystemConfigKeys} from '../data/system-config-keys';
import {type AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function ContainerEditor(props: BaseEditorProps<GroupLayout>) {
    const [marketplaceModule, setMarketplaceModule] = useState<MarketplaceDetailModule>();
    const [confirmRemoveMarketplace, setConfirmRemoveMarketplace] = useState<() => void>();
    const marketplaceKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.prosuna.marketplaceKey));

    useEffect(() => {
        if (props.element.marketplaceLink != null && (marketplaceModule == null || marketplaceModule.id !== props.element.marketplaceLink.marketplaceId)) {
            ProsunaMarketplaceService
                .fetchModule(props.element.marketplaceLink.marketplaceId, marketplaceKey)
                .then((module) => {
                    setMarketplaceModule(module);
                })
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [props.element, marketplaceKey]);

    if (props.element.marketplaceLink == null) {
        return <></>;
    }

    if (marketplaceModule == null) {
        return <></>;
    }

    const handleRemoveMarketplaceInformation = (): void => {
        setConfirmRemoveMarketplace(() => () => {
            props.onPatch({
                marketplaceLink: null,
            });
            setMarketplaceModule(undefined);
        });
    };

    const onlyInputChild: AnyInputElement | null = props.element.children.length === 1 && isAnyInputElement(props.element.children[0]) ? props.element.children[0] : null;

    return (
        <>
            {
                onlyInputChild != null &&
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}>
                        <TextFieldComponent
                            value={onlyInputChild.destinationKey ?? undefined}
                            label="HTTP-Schnittstellen-Schlüssel"
                            onChange={(val) => {
                                props.onPatch({
                                    children: [
                                        {
                                            ...onlyInputChild,
                                            destinationKey: val,
                                        },
                                    ],
                                });
                            }}
                            hint="Dieser Schlüssel wird verwendet, wenn die Daten an eine HTTP-Schnittstelle gesendet werden."
                            disabled={!props.editable}
                        />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}>
                        <CheckboxFieldComponent
                            label="Pflichtangabe"
                            value={onlyInputChild.required ?? false}
                            onChange={(val) => {
                                props.onPatch({
                                    children: [
                                        {
                                            ...onlyInputChild,
                                            required: val,
                                        },
                                    ],
                                });
                            }}
                            disabled={!props.editable}
                        />
                    </Grid>
                </Grid>
            }
            <AlertComponent
                color="info"
                title="Marktplatz-Baustein"
            >
                Bei diesem Element handelt es sich um einen Marktplatz-Baustein.
                Es kann nicht bearbeitet werden.
                Für eine Bearbeitung müssen Sie die Verknüpfung zum Marktplatz-Baustein aufheben.
                Mehr Informationen hierzu finden Sie in der <a
                href="https://docs.prosuna.de"
                target="_blank"
                style={{color: 'inherit'}}
                rel="noreferrer noopener"
            >Dokumentation</a>.
            </AlertComponent>
            {props.editable &&
                <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<LinkOffOutlinedIcon/>}
                    onClick={handleRemoveMarketplaceInformation}
                >
                    Verknüpfung auflösen
                </Button>
            }
            <Paper
                sx={{
                    p: 2,
                    mt: 4,
                }}
            >
                <MarketplaceModuleInfoTable
                    module={marketplaceModule}
                    currentVersion={props.element.marketplaceLink.marketplaceVersion}
                />
            </Paper>
            <ConfirmDialog
                title="Verknüpfung auflösen"
                onConfirm={confirmRemoveMarketplace}
                onCancel={() => {
                    setConfirmRemoveMarketplace(undefined);
                }}
            >
                Soll die Verknüpfung wirklich aufgelöst werden?
                Bitte beachten Sie, dass die Verknüpfung nicht wiederhergestellt werden kann.
            </ConfirmDialog>
        </>
    );
};
