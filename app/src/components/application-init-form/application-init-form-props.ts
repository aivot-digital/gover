import {Application} from '../../models/application';
import {RootElement} from '../../models/elements/root-element';

export interface ApplicationInitFormProps {
    application: Application;
    onChange: (app: Application) => void;
    errors: ApplicationInitFormPropsErrors;
}

export type ApplicationInitFormPropsErrors = {
    [key in keyof (Application & RootElement)]?: string;
};
