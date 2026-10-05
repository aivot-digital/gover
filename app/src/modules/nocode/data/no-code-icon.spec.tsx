import {isValidElement} from 'react';
import {describe, expect, it} from 'vitest';
import DynamicForm from '@aivot/mui-material-symbols-400-n25-outlined/DynamicForm';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import {NoCodeIcon} from './no-code-icon';
import {ElementType} from '../../../data/element-type/element-type';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {FunctionTypeIcon} from '../../../components/element-editor-code-tab/function-type-icon';
import {OperandTypeIcon} from '../../../components/element-editor-code-tab/components/expression-editor/operand-type-icon';
import {KnownProviderIcons} from '../../process/data/known-provider-icons';

describe('NoCodeIcon', () => {
    it('shares the no-code icon between field types, function modes and process nodes', () => {
        expect(NoCodeIcon).toBe(DynamicForm);
        expect(getElementIconForType(ElementType.NoCodeInput)).toBe(NoCodeIcon);
        expect(KnownProviderIcons['no-code']).toBe(NoCodeIcon);
        for (const icon of [FunctionTypeIcon.expression, FunctionTypeIcon['legacy-condition']]) {
            expect(isValidElement(icon) && icon.type).toBe(NoCodeIcon);
        }
    });

    it('keeps Functions for expression operands inside the editor', () => {
        expect(isValidElement(OperandTypeIcon.exp) && OperandTypeIcon.exp.type).toBe(Functions);
    });

    it('keeps script icons distinct from no-code expressions', () => {
        expect(getElementIconForType(ElementType.CodeInput)).toBe(Code);
        for (const icon of [FunctionTypeIcon.code, FunctionTypeIcon['legacy-code']]) {
            expect(isValidElement(icon) && icon.type).toBe(Code);
        }
    });
});
