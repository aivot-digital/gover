import React, {useEffect, useState} from 'react';
import {type BaseEditorProps} from './base-editor';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type StoreDetailModule} from '../models/entities/store-detail-module';
import {GoverStoreService} from '../services/gover-store.service';
import {Button, Paper} from '@mui/material';
import {AlertComponent} from '../components/alert/alert-component';
import LinkOffOutlinedIcon from '@mui/icons-material/LinkOffOutlined';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {StoreModuleInfoTable} from '../components/store-module-info-table/store-module-info-table';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectSystemConfigValue} from '../slices/system-config-slice';
import {SystemConfigKeys} from '../data/system-config-keys';
import {type AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function ContainerEditor(props: BaseEditorProps<GroupLayout, ElementTreeEntity>): JSX.Element {
    const [storeModule, setStoreModule] = useState<StoreDetailModule>();
    const [confirmRemoveStore, setConfirmRemoveStore] = useState<() => void>();
    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    useEffect(() => {
        if (props.element.storeLink != null && (storeModule == null || storeModule.id !== props.element.storeLink.storeId)) {
            GoverStoreService
                .fetchModule(props.element.storeLink.storeId, storeKey)
                .then((module) => {
                    setStoreModule(module);
                })
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [props.element, storeKey]);

    if (props.element.storeLink == null) {
        return <></>;
    }

    if (storeModule == null) {
        return <></>;
    }

    const handleRemoveStoreInformation = (): void => {
        setConfirmRemoveStore(() => () => {
            props.onPatch({
                storeLink: null,
            });
            setStoreModule(undefined);
        });
    };

    const onlyInputChild: AnyInputElement | null = props.element.children.length === 1 && isAnyInputElement(props.element.children[0]) ? props.element.children[0] : null;

    return (
        <>
            {
                onlyInputChild != null &&
                <>
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

                    <CheckboxFieldComponent
                        label="Pflichtangabe"
                        value={onlyInputChild.required}
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
                </>
            }

            <AlertComponent
                color="info"
                title="Store-Baustein"
            >
                Bei diesem Element handelt es sich um einen Store-Baustein.
                Es kann nicht bearbeitet werden.
                Für eine Bearbeitung müssen Sie die Verknüpfung zum Store-Baustein aufheben.
                Mehr Informationen hierzu finden Sie in der <a
                href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/store"
                target="_blank"
                rel="noreferrer noopener"
            >Dokumentation</a>.
            </AlertComponent>

            <Button
                fullWidth
                variant="outlined"
                startIcon={<LinkOffOutlinedIcon/>}
                onClick={handleRemoveStoreInformation}
            >
                Verknüpfung auflösen
            </Button>

            <Paper
                sx={{
                    p: 2,
                    mt: 4,
                }}
            >
                <StoreModuleInfoTable
                    module={storeModule}
                    currentVersion={props.element.storeLink.storeVersion}
                />
            </Paper>

            <ConfirmDialog
                title="Verknüpfung auflösen"
                onConfirm={confirmRemoveStore}
                onCancel={() => {
                    setConfirmRemoveStore(undefined);
                }}
            >
                Soll die Verknüpfung wirklich aufgelöst werden?
                Bitte beachten Sie, dass die Verknüpfung nicht wiederhergestellt werden kann.
            </ConfirmDialog>
        </>
    );
};
