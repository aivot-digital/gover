import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import PublicOutlined from '@mui/icons-material/PublicOutlined';

export enum FormType {
    Public = 0,
    Internal = 1,
}

export const FormTypeLabels = {
    [FormType.Public]: 'Öffentlich',
    [FormType.Internal]: 'Intern',
};

export const FormTypeDescriptions = {
    [FormType.Public]: 'Öffentliche Formulare werden auf der Übersichtsseite angezeigt und können von Bürger:innen ausgefüllt werden.',
    [FormType.Internal]: 'Interne Formulare werden nicht auf der Übersichtsseite angezeigt, können aber über den Link geteilt werden.',
}

export const FormTypeIcons = {
    [FormType.Public]: <LockOutlinedIcon/>,
    [FormType.Internal]: <PublicOutlined/>,
};

export const FormTypes = [
    FormType.Public,
    FormType.Internal,
];