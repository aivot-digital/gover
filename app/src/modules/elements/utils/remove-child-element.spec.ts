import { removeChildElement } from './remove-child-element';
import { generateElementWithDefaultValues } from '../../../utils/generate-element-with-default-values';
import { ElementType } from '../../../data/element-type/element-type';
import { GroupLayout } from '../../../models/elements/form/layout/group-layout';
import { TextFieldElement } from '../../../models/elements/form/input/text-field-element';
import {AnyElementWithChildren} from '../../../models/elements/any-element-with-children';

describe('removeChildElement', () => {
    it('should remove the child element if it is a direct child', () => {
        const childElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        const updatedElement = removeChildElement(element, childElement);

        expect(updatedElement.children).not.toContain(childElement);
    });

    it('should remove the child element if it is a nested child', () => {
        const nestedChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const childElement = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        childElement.children.push(nestedChildElement);

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        const updatedElement = removeChildElement(element, nestedChildElement);

        expect((updatedElement.children[0] as AnyElementWithChildren).children).not.toContain(nestedChildElement);
    });

    it('should not modify the element if the child element is not present', () => {
        const childElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;

        const updatedElement = removeChildElement(element, childElement);

        expect(updatedElement).toEqual(element);
    });

    it('should not modify the element if the child element is not present in a nested child', () => {
        const nestedChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const childElement = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        childElement.children.push(nestedChildElement);

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        const otherChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const updatedElement = removeChildElement(element, otherChildElement);

        expect(updatedElement).toEqual(element);
    });
});