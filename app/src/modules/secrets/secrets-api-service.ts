import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {SecretEntityRequestDTO} from './dtos/secret-entity-request-dto';
import {SecretEntityResponseDTO} from './dtos/secret-entity-response-dto';

interface SecretFilters {
    name: string;
}

export class SecretsApiService extends CrudApiService<SecretEntityRequestDTO, SecretEntityResponseDTO, SecretEntityResponseDTO, SecretEntityResponseDTO, SecretEntityResponseDTO, string, SecretFilters> {
    public constructor(api: Api) {
        super(api, 'secrets/');
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