package de.aivot.GoverBackend.query.services;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.models.QuerySelect;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryEvaluationService {
    private final EntityManager entityManager;

    @Autowired
    public QueryEvaluationService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Object evaluate(QuerySelect querySelect) throws QueryValidationException {
        querySelect.validate();

        var query = querySelect.build();

        return entityManager
                .createNativeQuery(query)
                .getResultList();
    }
}
