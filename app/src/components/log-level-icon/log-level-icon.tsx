import DebugIcon from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import InfoIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import WarningIcon from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import ErrorIcon from '@aivot/mui-material-symbols-400-n25-outlined/Report';
import {LogLevel} from '../../slices/logging-slice';

export function LogLevelIcon({level, active}: { level: LogLevel, active?: boolean }) {
    switch (level) {
        case LogLevel.Debug:
            return <DebugIcon color={active ? 'success' : undefined} />;
        case LogLevel.Info:
            return <InfoIcon color={active ? 'info' : undefined} />;
        case LogLevel.Warning:
            return <WarningIcon color={active ? 'warning' : undefined} />;
        case LogLevel.Error:
            return <ErrorIcon color={active ? 'error' : undefined} />;
        default:
            return <DebugIcon />;
    }
}