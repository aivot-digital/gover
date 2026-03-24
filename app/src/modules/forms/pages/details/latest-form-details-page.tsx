import {useNavigate, useParams} from 'react-router-dom';
import {useEffect} from 'react';
import {FormVersionApiService} from '../../services/form-version-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {setErrorMessage} from '../../../../slices/shell-slice';
import {isApiError} from '../../../../models/api-error';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';

export function LatestFormDetailsPage() {
    const formId = useParams().id;
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    useEffect(() => {
        if (formId == null) {
            dispatch(setErrorMessage({
                status: 404,
                message: 'Das angeforderte Formular wurde nicht gefunden.',
            }));
            return;
        }

        const formIdNum = parseInt(formId);
        if (isNaN(formIdNum)) {
            dispatch(setErrorMessage({
                status: 404,
                message: 'Das angeforderte Formular wurde nicht gefunden.',
            }));
            return;
        }

        new FormVersionApiService()
            .retrieve({
                formId: formIdNum,
                version: 'latest',
            })
            .then(res => {
                navigate(`/forms/${res.formId}/${res.version}`, {
                    replace: true,
                });
            })
            .catch((err) => {
                if (isApiError(err)) {
                    if (err.status === 403) {
                        dispatch(setErrorMessage({
                            status: 403,
                            message: err.displayableToUser ? err.message : 'Sie verfügen nicht über die notwendigen Rechte, um dieses Formular anzuzeigen.',
                        }));
                    } else if (err.status === 404) {
                        dispatch(setErrorMessage({
                            status: 404,
                            message: err.displayableToUser ? err.message : 'Das angeforderte Formular wurde nicht gefunden.',
                        }));
                    } else {
                        dispatch(setErrorMessage({
                            status: err.status,
                            message: err.displayableToUser ? err.message : 'Das Formular konnte nicht geladen werden.',
                        }));
                    }
                } else {
                    dispatch(setErrorMessage({
                        status: 500,
                        message: 'Das Formular konnte nicht geladen werden.',
                    }));
                }
                console.error(err);
            });
    }, [dispatch, formId, navigate]);

    return <LoadingPlaceholder />;
}
