import type {Metric} from 'web-vitals';

type ReportHandler = (metric: Metric) => void;

const reportWebVitals = (onPerfEntry?: ReportHandler) => {
    if (onPerfEntry && typeof onPerfEntry === 'function') {
        import('web-vitals').then(({ onCLS, onFCP, onLCP, onTTFB, onINP }) => {
            onCLS(onPerfEntry);
            onFCP(onPerfEntry);
            onLCP(onPerfEntry);
            onTTFB(onPerfEntry);
            onINP(onPerfEntry);
        });
    }
};

export default reportWebVitals;
