import {Container, Paper, Stack} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {AuthoredElementValues} from '../../../models/element-data';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {ElementTree} from '../../../components/element-tree-2/element-tree';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {downloadObjectFile, downloadTextFile, uploadObjectFile} from '../../../utils/download-utils';
import {AnyElementWithChildren, isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {MIN_EDITOR_DRAWER_WIDTH_PX} from '../../../modules/process/pages/details/process-details-page';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {IconBadge} from '../../../components/icon-badge/icon-badge';
import FileJson from '@aivot/mui-material-symbols-400-n25-outlined/FileJson';
import ArrowDownward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowDownward';
import ArrowUpward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowUpward';
import Settings from '@aivot/mui-material-symbols-400-n25-outlined/Settings';
import {useElementEditorNavigation} from '../../../hooks/use-element-editor-navigation';
import {ConfigLayoutElement} from '../../../models/elements/form/layout/config-layout-element';

export function NodeConfigMaker() {
    const {
        navigateToElementEditor,
    } = useElementEditorNavigation();

    const [layout, setLayout] = useState<ConfigLayoutElement>(loadConfig);
    useEffect(() => {
        storeConfig(layout);
    }, [layout]);

    const [values, setValues] = useState<AuthoredElementValues>({});

    const handleDownloadConfig = () => {
        downloadObjectFile('config.json', layout);
    };

    const handleUploadConfig = () => {
        uploadObjectFile<ConfigLayoutElement>('application/json')
            .then(response => {
                if (response != null) {
                    setLayout(response);
                }
            });
    };

    const handleDownloadPOJO = () => {
        const pojo = createPOJO(layout);

        downloadTextFile('Pojo.java', pojo, 'text/plain');
    };

    return (
        <PageWrapper
            title="Node Config Maker"
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.dashboard}
                    title={`Node Config Maker - ${toSnakeCase(layout.name)}`}
                    actions={[
                        {
                            icon: <IconBadge
                                icon={<FileJson/>}
                                badgeIcon={<ArrowDownward/>}
                            />,
                            tooltip: 'Download Node Config JSON',
                            onClick: handleDownloadConfig,
                        },
                        {
                            icon: <IconBadge
                                icon={<FileJson/>}
                                badgeIcon={<ArrowUpward/>}
                            />,
                            tooltip: 'Upload Node Config JSON',
                            onClick: handleUploadConfig,
                        },
                        {
                            icon: <DataObject/>,
                            tooltip: 'Download Node Config POJO',
                            onClick: handleDownloadPOJO,
                        },
                        {
                            icon: <Settings/>,
                            tooltip: 'Einstellungen des Konfigurationslayouts',
                            onClick: () => {
                                navigateToElementEditor(layout.id);
                            },
                        },
                    ]}
                />

                <Stack
                    direction="row"
                    spacing={2}
                    sx={{
                        mt: 2,
                    }}
                >
                    <Paper
                        sx={{
                            width: MIN_EDITOR_DRAWER_WIDTH_PX,
                            p: 2,
                        }}
                    >
                        <ElementDerivationContext
                            element={layout}
                            authoredElementValues={values}
                            onAuthoredElementValuesChange={setValues}
                        />
                    </Paper>

                    <ElementTree
                        value={layout}
                        onChange={setLayout}
                        editable={true}
                        displayContext={ElementDisplayContext.StaffFacing}
                        allowElementIdEditing={true}
                    />
                </Stack>
            </Container>
        </PageWrapper>
    );
}

const basis: ConfigLayoutElement = {
    id: 'config',
    type: ElementType.ConfigLayout,
    children: [],
    metadata: null,
    override: null,
    weight: null,
    visibility: null,
    testProtocolSet: null,
    name: null,
};

function storeConfig(layout: ConfigLayoutElement) {
    localStorage.setItem('nodeConfigMakerLayout', JSON.stringify(layout));
}

function loadConfig(): ConfigLayoutElement {
    const stored = localStorage.getItem('nodeConfigMakerLayout');
    if (stored) {
        return JSON.parse(stored);
    }
    return basis as ConfigLayoutElement;
}

function createPOJO(layout: AnyElementWithChildren, indent: number = 0): string {
    const leftPad = ''.padStart(indent * 4, ' ');

    let layoutVarName = toSnakeCase(layout.name);

    const layoutAnnotation = layout.type === ElementType.ReplicatingContainer
        ? `@ReplicatingContainerLayoutElementElementPOJOBinding(id = ${layoutVarName})`
        : `@LayoutElementPOJOBinding(id = ${layoutVarName}, type = ElementType.${ElementTypeBackendName[layout.type]})`;

    const className = toCamelCase(layout.name, true);

    const lb = indent === 0
        ? [
            '// TODO: Fix package path',
            'package de.aivot.todo;',
            '',
            'import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;',
            'import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;',
            'import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;',
            'import de.aivot.gover.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;',
            'import de.aivot.gover.backend.enums.ElementType;',
            '',
            'import java.util.*;',
            '',
            layoutAnnotation,
            `public class ${className} {`,
            `    public static final String ${layoutVarName} = "${layout.id}";`,
            `    `,
        ]
        : [
            `public static final String ${layoutVarName} = "${layout.id}";`,
            layoutAnnotation,
            `public static class ${className} {`,
        ];

    if (layout.children != null) {
        for (const child of layout.children) {
            if (isReplicatingContainerLayout(child)) {
                const childContainerClassName = toCamelCase(child.name, true);
                const containerClass = createPOJO(child, indent + 1);
                lb.push(
                    containerClass,
                    `    public List<${childContainerClassName}> ${toCamelCase(child.name)};`,
                    `    `,
                );


            } else if (isAnyElementWithChildren(child)) {
                const childContainerClassName = toCamelCase(child.name, true);
                const containerClass = createPOJO(child, indent + 1);
                lb.push(
                    containerClass,
                    `    public ${childContainerClassName} ${toCamelCase(child.name)};`,
                    `    `,
                );
            } else if (isAnyInputElement(child)) {
                const lines: string[] = [];

                if ((child.type === ElementType.Select || child.type === ElementType.Radio || child.type === ElementType.MultiCheckbox) && child.options != null) {
                    lines.push(
                        ...child
                            .options
                            .map(option => {
                                const label = ((typeof option === 'string') ? option : option.label)
                                    .replace(/\s/g, '_')
                                    .replace(/[äÄ]/g, 'AE')
                                    .replace(/[öÖ]/g, 'OE')
                                    .replace(/[üÜ]/g, 'UE')
                                    .replace(/ß/g, 'SS')
                                    .replace(/[^a-zA-Z0-9_]/g, '')
                                    .toUpperCase();

                                const value = typeof option === 'string' ? option : option.value;

                                return `    public static final String ${toSnakeCase(child.name)}_OPT_${toSnakeCase(label)} = "${value}";`;
                            }),
                    );
                }

                const childFieldVarName = `${toSnakeCase(child.name)}_FIELD_ID`;

                lines.push(...[
                    `    public static final String ${childFieldVarName} = "${child.id}";`,
                    `    @InputElementPOJOBinding(id = ${childFieldVarName} , type = ElementType.${ElementTypeBackendName[child.type]}, properties = {})`,
                    `    public ${ElementTypeBackendValueClass[child.type]} ${child.id};`,
                    `    `,
                ]);

                lb.push(...lines);
            }
        }
    }

    lb.push('}');


    return lb
        .map(line => leftPad + line)
        .join('\n');
}

function toCamelCase(str: string | null | undefined, upper: boolean = false) {
    if (str == null) {
        return 'UNNAMED';
    }

    const isSnakeCase = str.includes('_');

    if (isSnakeCase) {
        return str.toLowerCase().split('_').map((word, index) => {
            if (index === 0) {
                if (upper) {
                    return word.charAt(0).toUpperCase() + word.slice(1);
                }
                return word;
            }
            return word.charAt(0).toUpperCase() + word.slice(1);
        }).join('');
    } else {
        if (upper) {
            return str.charAt(0).toUpperCase() + str.slice(1);
        }
        return str;
    }
}

function toSnakeCase(str: string | null | undefined) {
    if (str == null) {
        str = 'UNNAMED';
    }

    const isSnakeCase = str.includes('_');
    if (isSnakeCase) {
        return str.toUpperCase();
    } else {
        return str.replace(/([a-z])([A-Z])/g, '$1_$2').toUpperCase();
    }
}

const ElementTypeBackendName: Record<ElementType, string> = {
    [ElementType.FormLayout]: 'FormLayout',
    [ElementType.Step]: 'Step',
    [ElementType.Alert]: 'Alert',
    [ElementType.GroupLayout]: 'GroupLayout',
    [ElementType.Checkbox]: 'Checkbox',
    [ElementType.Date]: 'Date',
    [ElementType.Headline]: 'Headline',
    [ElementType.MultiCheckbox]: 'MultiCheckbox',
    [ElementType.Number]: 'Number',
    [ElementType.ReplicatingContainer]: 'ReplicatingContainerLayout',
    [ElementType.RichText]: 'RichText',
    [ElementType.Radio]: 'Radio',
    [ElementType.Select]: 'Select',
    [ElementType.Spacer]: 'Spacer',
    [ElementType.Table]: 'Table',
    [ElementType.Text]: 'Text',
    [ElementType.Time]: 'Time',
    [ElementType.IntroductionStep]: 'IntroductionStep',
    [ElementType.SubmitStep]: 'SubmitStep',
    [ElementType.SummaryStep]: 'SummaryStep',
    [ElementType.Image]: 'Image',
    [ElementType.SubmittedStep]: 'SubmittedStep',
    [ElementType.FileUpload]: 'FileUpload',
    [ElementType.DialogLayout]: 'DialogLayout',
    [ElementType.StepperLayout]: 'StepperLayout',
    [ElementType.ConfigLayout]: 'ConfigLayout',
    [ElementType.FunctionInput]: 'FunctionInput',
    [ElementType.CodeInput]: 'CodeInput',
    [ElementType.RichTextInput]: 'RichTextInput',
    [ElementType.UiDefinitionInput]: 'UiDefinitionInput',
    [ElementType.IdentityConfigElement]: 'IdentityConfig',
    [ElementType.TabLayout]: 'TabLayout',
    [ElementType.ChipInput]: 'ChipInput',
    [ElementType.DateTime]: 'DateTime',
    [ElementType.DateRange]: 'DateRange',
    [ElementType.TimeRange]: 'TimeRange',
    [ElementType.DateTimeRange]: 'DateTimeRange',
    [ElementType.MapPoint]: 'MapPoint',
    [ElementType.DomainAndUserSelect]: 'DomainAndUserSelect',
    [ElementType.AssignmentContext]: 'AssignmentContext',
    [ElementType.DataModelSelect]: 'DataModelSelect',
    [ElementType.DataObjectSelect]: 'DataObjectSelect',
    [ElementType.NoCodeInput]: 'NoCodeInput',
    [ElementType.SummaryLayout]: 'SummaryLayout',
    [ElementType.ProcessDataKeyInput]: 'ProcessDataKeyInput',
    [ElementType.ProcessAttachmentDisplay]: 'ProcessAttachmentDisplay',
    [ElementType.ProcessInstanceAttachmentSetSelect]: 'ProcessInstanceAttachmentSetSelect',
    [ElementType.ProcessIdentityIdInput]: 'ProcessIdentityIdInput',
    [ElementType.HtmlTemplateInput]: 'HtmlTemplateInputElement',
    [ElementType.StoragePathSelector]: 'StoragePathSelector',
};

const ElementTypeBackendValueClass: Record<ElementType, string | null> = {
    [ElementType.FormLayout]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.GroupLayout]: null,
    [ElementType.Checkbox]: 'Boolean',
    [ElementType.Date]: 'Instant',
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: 'List<String>',
    [ElementType.Number]: 'Number',
    [ElementType.ReplicatingContainer]: null,
    [ElementType.RichText]: null,
    [ElementType.Radio]: 'String',
    [ElementType.Select]: 'String',
    [ElementType.Spacer]: null,
    [ElementType.Table]: 'List<Map<String, Object>>',
    [ElementType.Text]: 'String',
    [ElementType.Time]: 'Instant',
    [ElementType.IntroductionStep]: 'Boolean',
    [ElementType.SubmitStep]: 'Object',
    [ElementType.SummaryStep]: null,
    [ElementType.Image]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: 'List<FileUploadInputElementItem>',
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: 'Object',
    [ElementType.CodeInput]: 'String',
    [ElementType.RichTextInput]: 'String',
    [ElementType.UiDefinitionInput]: 'BaseElement',
    [ElementType.IdentityConfigElement]: 'List<IdentityConfigElementSlot>',
    [ElementType.TabLayout]: null,
    [ElementType.ChipInput]: 'List<String>',
    [ElementType.DateTime]: 'Instant',
    [ElementType.DateRange]: 'RangeInputElementValue',
    [ElementType.TimeRange]: 'RangeInputElementValue',
    [ElementType.DateTimeRange]: 'RangeInputElementValue',
    [ElementType.MapPoint]: 'MapPointInputElementValue',
    [ElementType.DomainAndUserSelect]: 'List<DomainAndUserSelectInputElementValue>',
    [ElementType.AssignmentContext]: 'AssignmentContextInputElementValue',
    [ElementType.DataModelSelect]: 'String',
    [ElementType.DataObjectSelect]: 'String',
    [ElementType.NoCodeInput]: 'NoCodeInputElementItem',
    [ElementType.SummaryLayout]: null,
    [ElementType.ProcessDataKeyInput]: 'String',
    [ElementType.ProcessAttachmentDisplay]: null,
    [ElementType.ProcessInstanceAttachmentSetSelect]: 'List<String>',
    [ElementType.ProcessIdentityIdInput]: 'List<String>',
    [ElementType.HtmlTemplateInput]: 'HtmlTemplateInputElementValue',
    [ElementType.StoragePathSelector]: 'StoragePathSelectorInputElementValue',
};
