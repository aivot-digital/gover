import React from 'react';
import {type AnyElement} from '../../models/elements/any-element';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {DefaultTab} from '../element-editor-default-tab/default-tab';
import {type ElementEditorContentProps} from './element-editor-content-props';
import {DefaultTabs} from '../element-editor/default-tabs';
import {type BaseInputElement} from '../../models/elements/form/base-input-element';
import {StructureTab} from '../element-editor-structure-tab/structure-tab';
import {TestTab} from '../element-editor-test-tab/test-tab';
import {Box} from '@mui/material';
import {isRootElement} from '../../models/elements/root-element';
import {isForm} from '../../models/entities/form';
import {ApplicationPublishTab} from '../element-editor-application-publish-tab/application-publish-tab';
import {isGroupLayout} from '../../models/elements/form/layout/group-layout';
import {PresetPublishTab} from '../element-editor-preset-publish-tab/preset-publish-tab';
import {EditorDispatcher} from '../editor-dispatcher';
import {ElementEditorMetadataTab} from '../element-editor-metadata-tab/element-editor-metadata-tab';
import {VisibilityCodeTab} from '../element-editor-code-tab/visibility-code-tab';
import {OverrideCodeTab} from '../element-editor-code-tab/override-code-tab';
import {ValueCodeTab} from '../element-editor-code-tab/value-code-tab';
import {ValidationCodeTab} from '../element-editor-code-tab/validation-code-tab';


export function ElementEditorContent<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorContentProps<T, E>): JSX.Element | null {
    const {
        onChange,
        ...passProps
    } = props;

    switch (props.currentTab) {
        case DefaultTabs.properties:
            return (
                <DefaultTab
                    onChange={(element) => {
                        if (props.element.testProtocolSet?.professionalTest != null) {
                            if (props.element.testProtocolSet.technicalTest == null) {
                                element.testProtocolSet = undefined;
                            } else {
                                element.testProtocolSet = {
                                    technicalTest: props.element.testProtocolSet.technicalTest,
                                };
                            }
                        }
                        onChange(element);
                    }}
                    {...passProps}
                />
            );
        case DefaultTabs.visibility:
            return (
                <>
                    <VisibilityCodeTab
                        editable={props.editable}
                        parents={props.parents}
                        element={props.element}
                        onChange={(updatedElement) => {
                            if (props.element.testProtocolSet?.technicalTest != null) {
                                if (props.element.testProtocolSet.professionalTest == null) {
                                    updatedElement.testProtocolSet = undefined;
                                } else {
                                    updatedElement.testProtocolSet = {
                                        professionalTest: props.element.testProtocolSet.professionalTest,
                                    };
                                }
                            }
                            props.onChange(updatedElement as Partial<T>);
                        }}
                    />
                    {/*
                    <CodeTab
                        parents={props.parents}
                        key="visibility"
                        resultTitle="Sichtbarkeit festlegen"
                        resultHint="Dieses Element ist sichtbar, wenn die die main-Funktion des folgenden Codes den Wert true zurückgibt:"
                        element={props.element}
                        func={props.element.isVisible}
                        allowNoCode={true}
                        shouldReturnString={false}
                        onChange={(updatedFunc) => {
                            const updatedElement: Partial<AnyInputElement> = {
                                isVisible: updatedFunc,
                            };
                            if (props.element.testProtocolSet?.technicalTest != null) {
                                if (props.element.testProtocolSet.professionalTest == null) {
                                    updatedElement.testProtocolSet = undefined;
                                } else {
                                    updatedElement.testProtocolSet = {
                                        professionalTest: props.element.testProtocolSet.professionalTest,
                                    };
                                }
                            }
                            props.onChange(updatedElement as Partial<T>);
                        }}
                        editable={props.editable}
                    />
                    */}
                </>
            );
        case DefaultTabs.validation:
            return (
                <>
                    <ValidationCodeTab
                        editable={props.editable}
                        parents={props.parents}
                        element={props.element as BaseInputElement<any, any>}
                        onChange={(updatedElement) => {
                            if (props.element.testProtocolSet?.technicalTest != null) {
                                if (props.element.testProtocolSet.professionalTest == null) {
                                    updatedElement.testProtocolSet = undefined;
                                } else {
                                    updatedElement.testProtocolSet = {
                                        professionalTest: props.element.testProtocolSet.professionalTest,
                                    };
                                }
                            }
                            props.onChange(updatedElement as Partial<T>);
                        }}
                    />
                    {/*
                <CodeTab
                    parents={props.parents}
                    key="validate"
                    resultTitle="Validierung durchführen"
                    resultHint="Dieses Element ist valide, wenn die main-Funktion des folgenden Codes keinen String mit einem Validierungsproblem erzeugt, sondern null zurück gibt:"
                    element={props.element}
                    func={(props.element as BaseInputElement<any, any>).validate}
                    allowNoCode={true}
                    shouldReturnString={true}
                    onChange={(updatedFunc) => {
                        const updatedElement: Partial<AnyInputElement> = {
                            validate: updatedFunc,
                        };
                        if (props.element.testProtocolSet?.technicalTest != null) {
                            if (props.element.testProtocolSet.professionalTest == null) {
                                updatedElement.testProtocolSet = undefined;
                            } else {
                                updatedElement.testProtocolSet = {
                                    professionalTest: props.element.testProtocolSet.professionalTest,
                                };
                            }
                        }
                        props.onChange(updatedElement as Partial<T>);
                    }}
                    editable={props.editable}
                />
                */}
                </>
            );
        case DefaultTabs.value:
            return (
                <>
                    <ValueCodeTab
                        editable={props.editable}
                        parents={props.parents}
                        element={props.element as BaseInputElement<any, any>}
                        onChange={(updatedElement) => {
                            if (props.element.testProtocolSet?.technicalTest != null) {
                                if (props.element.testProtocolSet.professionalTest == null) {
                                    updatedElement.testProtocolSet = undefined;
                                } else {
                                    updatedElement.testProtocolSet = {
                                        professionalTest: props.element.testProtocolSet.professionalTest,
                                    };
                                }
                            }
                            props.onChange(updatedElement as Partial<T>);
                        }}
                    />
                    {/*
                <CodeTab
                    parents={props.parents}
                    key="value"
                    resultTitle="Dynamischen Wert bestimmen"
                    resultHint="Dieses Element bekommt den Rückgabewert der folgenden Funktion:"
                    element={props.element}
                    func={(props.element as BaseInputElement<any, any>).computeValue}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={(updatedFunc) => {
                        const updatedElement: Partial<AnyInputElement> = {
                            computeValue: updatedFunc,
                        };
                        if (props.element.testProtocolSet?.technicalTest != null) {
                            if (props.element.testProtocolSet.professionalTest == null) {
                                updatedElement.testProtocolSet = undefined;
                            } else {
                                updatedElement.testProtocolSet = {
                                    professionalTest: props.element.testProtocolSet.professionalTest,
                                };
                            }
                        }
                        props.onChange(updatedElement as Partial<T>);
                    }}
                    editable={props.editable}
                />
                */}
                </>
            );
        case DefaultTabs.patch:
            return (
                <>
                    <OverrideCodeTab
                        editable={props.editable}
                        parents={props.parents}
                        element={props.element}
                        onChange={(updatedElement) => {
                            if (props.element.testProtocolSet?.technicalTest != null) {
                                if (props.element.testProtocolSet.professionalTest == null) {
                                    updatedElement.testProtocolSet = undefined;
                                } else {
                                    updatedElement.testProtocolSet = {
                                        professionalTest: props.element.testProtocolSet.professionalTest,
                                    };
                                }
                            }
                            props.onChange(updatedElement as Partial<T>);
                        }}
                    />
                    {/*}
                <CodeTab
                    parents={props.parents}
                    key="patch"
                    resultTitle="Element aktualisieren"
                    resultHint="Dieses Element wird mit dem Rückgabewert der folgenden Funktion aktualisiert:"
                    element={props.element}
                    func={props.element.patchElement}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={(updatedFunc) => {
                        const updatedElement: Partial<AnyInputElement> = {
                            patchElement: updatedFunc,
                        };
                        if (props.element.testProtocolSet?.technicalTest != null) {
                            if (props.element.testProtocolSet.professionalTest == null) {
                                updatedElement.testProtocolSet = undefined;
                            } else {
                                updatedElement.testProtocolSet = {
                                    professionalTest: props.element.testProtocolSet.professionalTest,
                                };
                            }
                        }
                        props.onChange(updatedElement as Partial<T>);
                    }}
                    editable={props.editable}
                />
                */}
                </>
            );
        case DefaultTabs.structure:
            return (
                <StructureTab
                    elementModel={props.element}
                    onChange={(struct) => {
                        props.onChange(struct);
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.test:
            return (
                <TestTab
                    elementModel={props.element}
                    onPatch={(update) => {
                        props.onChange(update);
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.publish:
            return (
                <Box
                    sx={{
                        p: 4,
                    }}
                >
                    {
                        props.scope === 'application' &&
                        isRootElement(props.element) &&
                        isForm(props.entity) &&
                        /* @ts-expect-error */
                        <ApplicationPublishTab
                            {...props}
                        />
                    }

                    {
                        props.scope === 'preset' &&
                        isGroupLayout(props.element) &&
                        !isForm(props.entity) &&
                        /* @ts-expect-error */
                        <PresetPublishTab
                            {...props}
                        />
                    }
                </Box>
            );
        case DefaultTabs.metadata:
            return (
                <ElementEditorMetadataTab
                    elementModel={props.element}
                    onChange={props.onChange}
                    editable={props.editable}
                    entity={props.entity}
                />
            );
        default:
            if (props.additionalTabs.some((add) => props.currentTab === add.label)) {
                return (
                    <Box
                        sx={{
                            p: 4,
                        }}
                    >
                        <EditorDispatcher
                            props={props.element}
                            onPatch={props.onChange}
                            additionalTabIndex={props.additionalTabs.findIndex((add) => props.currentTab === add.label)}
                            entity={props.entity}
                            onPatchEntity={props.onChangeEntity}
                            editable={props.editable}
                        />
                    </Box>
                );
            }
            return null;
    }
}

