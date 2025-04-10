import {hasChildElement} from './has-child-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {TextFieldElement} from '../../../models/elements/form/input/text-field-element';

describe('hasChildElement', () => {
    it('should return true if the child element is a direct child', () => {
        const childElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        expect(hasChildElement(element, childElement)).toBe(true);
    });

    it('should return true if the child element is a nested child', () => {
        const nestedChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const childElement = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        childElement.children.push(nestedChildElement);

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        expect(hasChildElement(element, nestedChildElement)).toBe(true);
    });

    it('should return false if the child element is not present', () => {
        const childElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;

        expect(hasChildElement(element, childElement)).toBe(false);
    });

    it('should return false if the child element is not present in a nested child', () => {
        const nestedChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        const childElement = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        childElement.children.push(nestedChildElement);

        const element = generateElementWithDefaultValues(ElementType.Container) as GroupLayout;
        element.children.push(childElement);

        const otherChildElement = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        expect(hasChildElement(element, otherChildElement)).toBe(false);
    });
});