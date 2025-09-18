import {useEffect, useMemo, useState} from 'react';
import {BaseApiService} from '../../../services/base-api-service';
import {Page} from '../../../models/dtos/page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {SystemConfigDefinition} from '../models/system-config-definition';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {Collapse} from '../../../components/collapse/collapse';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {SystemConfigsApiService} from '../system-configs-api-service';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {useApi} from '../../../hooks/use-api';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {Theme} from '../../themes/models/theme';
import {SecretEntityResponseDTO} from '../../secrets/dtos/secret-entity-response-dto';
import {DepartmentResponseDTO} from '../../departments/dtos/department-response-dto';
import {ThemesApiService} from '../../themes/themes-api-service';
import {SecretsApiService} from '../../secrets/secrets-api-service';
import {DepartmentsApiService} from '../../departments/departments-api-service';

interface ConfigCategory {
    title: string;
    definitions: SystemConfigDefinition[];
}

interface AdditionalData {
    allThemes: Theme[];
    allSecrets: SecretEntityResponseDTO[];
    allDepartments: DepartmentResponseDTO[];
}

export function SystemConfigs() {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [systemConfigs, setSystemConfigs] = useState<Record<string, string | null | undefined>>();
    const [systemConfigDefinition, setSystemConfigDefinition] = useState<SystemConfigDefinition[]>();

    const [additionalData, setAdditionalData] = useState<AdditionalData>();

    useEffect(() => {
        dispatch(setLoadingMessage({
            blocking: false,
            message: 'Lade Systemeinstellungen...',
            estimatedTime: 600,
        }));

        Promise.all([
            new BaseApiService()
                .get<Page<SystemConfigDefinition>>('/api/system-config-definitions/'),
            new SystemConfigsApiService(api)
                .listAll(),
            new ThemesApiService(api)
                .listAll(),
            new SecretsApiService(api)
                .listAll(),
            new DepartmentsApiService(api)
                .listAll(),
        ])
            .then(([definitionResponse, configsResponse, themes, secrets, departments]) => {
                setSystemConfigDefinition(definitionResponse.content);

                const configMap: Record<string, string> = {};
                configsResponse.content.forEach(config => {
                    configMap[config.key] = config.value;
                });
                setSystemConfigs(configMap);

                setAdditionalData({
                    allThemes: themes.content,
                    allSecrets: secrets.content,
                    allDepartments: departments.content,
                })
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    }, []);

    const categories = useMemo(() => {
        if (systemConfigDefinition == null) {
            return undefined;
        }

        return systemConfigDefinition
            .reduce((categories, definition) => {
                let category = categories.find(c => c.title === definition.category);
                if (!category) {
                    category = {
                        title: definition.category,
                        definitions: [],
                    };
                    categories.push(category);
                }
                category.definitions.push(definition);
                return categories;
            }, [] as ConfigCategory[]);
    }, [systemConfigDefinition]);

    return (
        <PageWrapper
            title="Weitere Systemeinstellungen"
        >
            {
                (
                    systemConfigs == null ||
                    categories == null ||
                    additionalData == null
                ) && (
                    <LoadingPlaceholder />
                )
            }
            {
                systemConfigs != null &&
                categories != null &&
                additionalData != null &&
                categories.map(({title, definitions}) => (
                    <Collapse
                        key={title}
                        label={title}
                        openTooltip="Einstellungen anzeigen"
                        closeTooltip="Einstellungen verbergen"
                    >
                        {
                            definitions.map((def) => (
                                <ConfigDispatcher
                                    key={def.key}
                                    definition={def}
                                    value={systemConfigs[def.key]}
                                    onChange={(val) => {
                                        setSystemConfigs({
                                            ...systemConfigs,
                                            [def.key]: val,
                                        });
                                    }}
                                    additionalData={additionalData}
                                />
                            ))
                        }
                    </Collapse>
                ))
            }

        </PageWrapper>
    );
}

interface ConfigDispatcherProps {
    definition: SystemConfigDefinition;
    value: string | undefined | null;
    onChange: (newValue: string | undefined | null) => void;
    additionalData: AdditionalData;
}

function ConfigDispatcher(props: ConfigDispatcherProps) {
    const {
        definition,
        value,
        onChange,
        additionalData,
    } = props;

    switch (definition.type) {
        case 'TEXT':
            return (
                <TextFieldComponent
                    label={definition.label}
                    hint={definition.description}
                    value={value}
                    onChange={(newValue) => {
                        onChange(newValue);
                    }}
                />
            );
        case 'SECRET':
            return (
                <SelectFieldComponent
                    label={definition.label}
                    hint={definition.description}
                    value={value ?? undefined}
                    onChange={(newValue) => {
                        onChange(newValue);
                    }}
                    options={
                        additionalData.allSecrets.map((s) => ({
                            label: s.name,
                            value: s.key,
                        }))
                    }
                />
            );
        case 'THEME':
            return (
                <SelectFieldComponent
                    label={definition.label}
                    hint={definition.description}
                    value={value ?? undefined}
                    onChange={(newValue) => {
                        onChange(newValue);
                    }}
                    options={
                        additionalData.allThemes.map((t) => ({
                            label: t.name,
                            value: t.id.toString(),
                        }))
                    }
                />
            );
        case 'DEPARTMENT':
            return (
                <SelectFieldComponent
                    label={definition.label}
                    hint={definition.description}
                    value={value ?? undefined}
                    onChange={(newValue) => {
                        onChange(newValue);
                    }}
                    options={
                        additionalData.allDepartments.map((d) => ({
                            label: d.name,
                            value: d.id.toString(),
                        }))
                    }
                />
            );
        case 'FLAG':
            return (
                <CheckboxFieldComponent
                    label={definition.label}
                    hint={definition.description}
                    value={value === 'true'}
                    onChange={(newValue) => {
                        onChange(newValue ? 'true' : 'false');
                    }}
                />
            );
    }
}