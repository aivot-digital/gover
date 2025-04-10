import {Api} from '../hooks/use-api';
import {NoCodeOperatorDetailsDTO} from '../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType} from '../data/no-code-data-type';
import {NoCodeExpression} from '../models/functions/no-code-expression';

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

    public async evaluateNoCode(expression: NoCodeExpression, data: any): Promise<any> {
        return this.api.post('no-code/evaluate', {
            expression: expression,
            data: data
        }, {});
    }
}