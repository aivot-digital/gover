import {AppInfo} from '../app-info';

export function getAppVersionLabel(): string {
    return AppInfo.version === '@buildVersion' ? '5.x (DEV)' : AppInfo.version;
}

export function getAboutProsunaLabel(): string {
    return `Über Prosuna v${getAppVersionLabel()}`;
}
