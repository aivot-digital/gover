import DebugIcon from '@mui/icons-material/BugReport';
import InfoIcon from '@mui/icons-material/Info';
import WarningIcon from '@mui/icons-material/Warning';
import ErrorIcon from '@mui/icons-material/Report';
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