import {describe, expect, it} from 'vitest';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {type AnyElement} from '../../models/elements/any-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type HeadlineElement} from '../../models/elements/form/content/headline-element';
import {type TextFieldElement} from '../../models/elements/form/input/text-field-element';
import {type ConfigLayoutElement} from '../../models/elements/form/layout/config-layout-element';
import {type StepperLayoutElement} from '../../models/elements/form/layout/stepper-layout-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {
    type ProcessNodeDefinitionMetadataReusableUiDefinition,
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
} from '../../modules/process/entities/process-node-definition-metadata';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {
    cloneReusableUiDefinitionForImport,
    createReusableUiDefinitionOptions,
    type ReusableUiDefinitionOption,
} from './reusable-ui-definition-utils';

describe('reusable UI definition utilities', () => {
    it('keeps partial definitions when at least one import mode is compatible', () => {
        const parent = createGroup('gp_target', []);
        const partialDefinition = createDefinition(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection,
            createGroup('gp_section', [
                createHeadline('hd_title'),
                createTextField('tx_value'),
            ]),
        );

        const bothModes = createReusableUiDefinitionOptions([partialDefinition], {
            parentType: parent.type,
            parentElement: parent,
            allParents: [parent],
            displayContext: ElementDisplayContext.CitizenFacing,
        });

        expect(bothModes).toHaveLength(1);
        expect(bothModes[0].groupDisabledReason).toBeUndefined();
        expect(bothModes[0].flatDisabledReason).toBeUndefined();

        const flatOnly = createReusableUiDefinitionOptions([partialDefinition], {
            parentType: parent.type,
            parentElement: parent,
            allParents: [parent],
            displayContext: ElementDisplayContext.CitizenFacing,
            limitElementTypes: [ElementType.Headline, ElementType.Text],
        });

        expect(flatOnly).toHaveLength(1);
        expect(flatOnly[0].groupDisabledReason).toContain('nicht freigegeben');
        expect(flatOnly[0].flatDisabledReason).toBeUndefined();
    });

    it('filters definitions when neither complete nor partial insertion is compatible', () => {
        const form = generateElementWithDefaultValues(ElementType.FormLayout) as AnyElement;
        const partialDefinition = createDefinition(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab,
            createGroup('gp_tab', [createHeadline('hd_tab'), createTextField('tx_tab')]),
        );

        expect(createReusableUiDefinitionOptions([partialDefinition], {
            parentType: form.type,
            parentElement: form,
            allParents: [form],
            displayContext: ElementDisplayContext.CitizenFacing,
        })).toEqual([]);
    });

    it('allows a complete stepper definition at a staff-facing configuration root', () => {
        const configLayout = {
            ...generateElementWithDefaultValues(ElementType.ConfigLayout),
            id: 'cf_root',
            type: ElementType.ConfigLayout,
            children: [],
        } as ConfigLayoutElement;
        const stepper = {
            id: 'sp_source',
            type: ElementType.StepperLayout,
            name: 'Bearbeitung',
            testProtocolSet: undefined,
            visibility: undefined,
            override: undefined,
            metadata: undefined,
            children: [],
        } as StepperLayoutElement;
        const definition = createDefinition(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition,
            stepper,
        );

        const options = createReusableUiDefinitionOptions([definition], {
            parentType: configLayout.type,
            parentElement: configLayout,
            allParents: [configLayout],
            displayContext: ElementDisplayContext.StaffFacing,
        });

        expect(options).toHaveLength(1);
        expect(options[0].partial).toBe(false);
    });

    it('clones the wrapper once before flattening and preserves cross-element references', () => {
        const referencedField = createTextField('tx_referenced');
        const referencingField = {
            ...createTextField('tx_referencing'),
            visibility: {
                type: 'ConditionSet',
                requirements: undefined,
                conditionSet: {
                    operator: ConditionSetOperator.All,
                    conditions: [{
                        reference: referencedField.id,
                    }],
                },
                noCode: undefined,
                javascriptCode: undefined,
                referencedIds: [referencedField.id],
            },
        } as TextFieldElement;
        const definition = createDefinition(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.StepperSection,
            createGroup('gp_section', [
                createHeadline('hd_section'),
                referencedField,
                referencingField,
            ]),
        );
        const option: ReusableUiDefinitionOption = {
            definition,
            partial: true,
        };

        const clonedChildren = cloneReusableUiDefinitionForImport(option, 'flat');

        expect(clonedChildren.map((element) => element.type)).toEqual([
            ElementType.Headline,
            ElementType.Text,
            ElementType.Text,
        ]);
        expect(clonedChildren.map((element) => element.id)).not.toContain(referencedField.id);
        expect(clonedChildren.map((element) => element.id)).not.toContain(referencingField.id);
        expect(clonedChildren[2].visibility?.conditionSet?.conditions?.[0].reference)
            .toBe(clonedChildren[1].id);
    });

    it('preserves the root container when cloning a complete stepper definition', () => {
        const sourceStep = {
            ...generateElementWithDefaultValues(ElementType.Step) as StepElement,
            id: 'st_source',
            children: [createTextField('tx_source')],
        };
        const sourceStepper: StepperLayoutElement = {
            id: 'sp_source',
            type: ElementType.StepperLayout,
            name: 'Bearbeitung',
            testProtocolSet: undefined,
            visibility: undefined,
            override: undefined,
            metadata: undefined,
            children: [sourceStep],
        };
        const option: ReusableUiDefinitionOption = {
            definition: createDefinition(
                ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition,
                sourceStepper,
            ),
            partial: false,
        };

        const imported = cloneReusableUiDefinitionForImport(option, 'complete');

        expect(imported).toHaveLength(1);
        expect(imported[0].type).toBe(ElementType.StepperLayout);
        expect(imported[0].id).not.toBe(sourceStepper.id);
        expect((imported[0] as StepperLayoutElement).children[0].id).not.toBe(sourceStep.id);
    });
});

function createDefinition(
    kind: ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
    uiDefinition: AnyElement,
): ProcessNodeDefinitionMetadataReusableUiDefinition {
    return {
        label: 'Wiederverwendbarer Bereich',
        subLabel: null,
        uiDefinition,
        origin: {
            id: 12,
            name: 'Prüfschritt',
        } as ProcessNodeDefinitionMetadataReusableUiDefinition['origin'],
        kind,
    };
}

function createGroup(id: string, children: AnyElement[]): GroupLayout {
    return {
        ...generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
        id,
        name: id,
        children: children as GroupLayout['children'],
        marketplaceLink: null,
    };
}

function createHeadline(id: string): HeadlineElement {
    return {
        ...generateElementWithDefaultValues(ElementType.Headline) as HeadlineElement,
        id,
        content: 'Überschrift',
    };
}

function createTextField(id: string): TextFieldElement {
    return {
        ...generateElementWithDefaultValues(ElementType.Text) as TextFieldElement,
        id,
    };
}
