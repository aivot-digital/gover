import {FormEntity} from './form-entity';
import {FormVersionEntity} from './form-version-entity';

export interface FormExport {
    form: FormEntity;
    version: FormVersionEntity;
    build: {
        version: string;
        number: string;
        timestamp: string;
    };
    timestamp: string;
}