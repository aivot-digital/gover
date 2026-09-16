import {SecretEntityRequestDTO} from './dtos/secret-entity-request-dto';
import {SecretEntityResponseDTO} from './dtos/secret-entity-response-dto';
import {BaseCrudApiService} from "../../services/base-crud-api-service";

interface SecretFilters {
    name: string;
}

export class SecretsApiService extends BaseCrudApiService<
    SecretEntityRequestDTO,
    SecretEntityResponseDTO,
    SecretEntityResponseDTO,
    SecretEntityResponseDTO,
    string,
    SecretFilters,
    keyof SecretEntityResponseDTO
> {
    public constructor() {
        super('secrets/');
    }

    public initialize(): SecretEntityResponseDTO {
        return {
            key: '',
            name: '',
            description: '',
            value: '',
        };
    }
}