import {Api} from '../hooks/use-api';
import {NoCodeOperatorDetailsDTO} from '../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType} from '../data/no-code-data-type';
import {NoCodeExpression} from '../models/functions/no-code-expression';
import {DerivedRuntimeElementData} from '../models/element-data';

export class NoCodeApiService {
    private readonly api: Api;

    constructor(api: Api) {
        this.api = api;
    }

    public async getNoCodeOperators(filters?: {inputType?: NoCodeDataType, outputType?: NoCodeDataType, search?: string}): Promise<NoCodeOperatorDetailsDTO[]> {
        return this.api.get('no-code/operators', {
            queryParams: filters
        });
    }

    public async evaluateNoCode(expression: NoCodeExpression, data: DerivedRuntimeElementData): Promise<any> {
        return this.api.post('no-code/test', {
            expression: expression,
            elementData: data
        }, {});
    }
}
