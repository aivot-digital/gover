import {Api} from '../hooks/use-api';
import {NoCodeOperatorDetailsDTO} from '../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType} from '../data/no-code-data-type';
import {NoCodeDryRunRequestDTO, NoCodeDryRunResultDTO} from '../models/dtos/no-code-dry-run-dto';

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

    /** Evaluates an expression with test data and returns the result of every operand, without saving anything. */
    public async dryRun(request: NoCodeDryRunRequestDTO): Promise<NoCodeDryRunResultDTO> {
        return this.api.post('no-code/dry-run/', request);
    }
}
