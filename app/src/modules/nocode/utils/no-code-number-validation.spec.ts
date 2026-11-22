import {describe, expect, it} from 'vitest';
import {NoCodeNumberConstraints} from '../../../models/dtos/no-code-operator-details-dto';
import {getNoCodeNumberValidationError} from './no-code-number-validation';

const constraints: NoCodeNumberConstraints = {
    integerOnly: true,
    minimum: -2147483648,
    maximum: 2147483647,
};

describe('getNoCodeNumberValidationError', () => {
    it.each(['0', '-1', ' 5 ', '+5', '5.0', '1e1', '10e-1', '.0', '1.', '-2147483648', '2147483647', 0, -5])(
        'accepts the evaluator’s supported whole-number input %j', (value) => {
            expect(getNoCodeNumberValidationError(value, 'Anzahl', constraints)).toBeUndefined();
        },
    );

    it.each([undefined, null, '', ' ', 'abc', '5,0', '1.5', '1e-1', '1.00000000000000000001', '0x10', true])(
        'explains invalid or non-integral input %j at the field', (value) => {
            expect(getNoCodeNumberValidationError(value, 'Anzahl', constraints))
                .toBe('Geben Sie für „Anzahl“ eine ganze Zahl ein.');
        },
    );

    it.each(['-2147483649', '2147483648', '1e1000', '-1e1000'])('explains the supported range only for invalid input %j', (value) => {
        expect(getNoCodeNumberValidationError(value, 'Anzahl', constraints))
            .toBe('Geben Sie für „Anzahl“ eine Zahl zwischen −2.147.483.648 und 2.147.483.647 ein.');
    });

    it('does not constrain other values unless the parameter declares constraints', () => {
        expect(getNoCodeNumberValidationError('1.5', 'Zahl')).toBeUndefined();
        expect(getNoCodeNumberValidationError(null, 'Wert', null)).toBeUndefined();
        expect(getNoCodeNumberValidationError('1.5', 'Zahl', {integerOnly: false})).toBeUndefined();
    });
});
