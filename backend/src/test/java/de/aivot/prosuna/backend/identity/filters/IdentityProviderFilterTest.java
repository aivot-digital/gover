package de.aivot.prosuna.backend.identity.filters;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityProviderFilterTest {
    @Test
    @SuppressWarnings("unchecked")
    void communicationProviderFilterUsesCorrelatedExistsSubquery() {
        Root<IdentityProviderEntity> identityProviderRoot = mock();
        CriteriaQuery<?> query = mock();
        CriteriaBuilder criteriaBuilder = mock();
        Subquery<Integer> bindingSubquery = mock();
        Root<CommunicationProviderBindingEntity> bindingRoot = mock();
        Path<Integer> bindingId = mock();
        Path<Object> bindingIdentityProviderKey = mock();
        Path<Object> identityProviderKey = mock();
        Path<Object> bindingCommunicationProviderId = mock();
        Predicate matchingIdentityProvider = mock();
        Predicate matchingCommunicationProvider = mock();
        Predicate exists = mock();
        Predicate result = mock();

        when(query.subquery(Integer.class)).thenReturn(bindingSubquery);
        when(bindingSubquery.from(CommunicationProviderBindingEntity.class)).thenReturn(bindingRoot);
        when(bindingRoot.<Integer>get("id")).thenReturn(bindingId);
        when(bindingRoot.get("identityProviderKey")).thenReturn(bindingIdentityProviderKey);
        when(identityProviderRoot.get("key")).thenReturn(identityProviderKey);
        when(bindingRoot.get("communicationProviderId")).thenReturn(bindingCommunicationProviderId);
        when(bindingSubquery.select(bindingId)).thenReturn(bindingSubquery);
        when(criteriaBuilder.equal(bindingIdentityProviderKey, identityProviderKey)).thenReturn(matchingIdentityProvider);
        when(criteriaBuilder.equal(bindingCommunicationProviderId, 7)).thenReturn(matchingCommunicationProvider);
        when(bindingSubquery.where(matchingIdentityProvider, matchingCommunicationProvider)).thenReturn(bindingSubquery);
        when(criteriaBuilder.exists(bindingSubquery)).thenReturn(exists);
        when(criteriaBuilder.and(exists)).thenReturn(result);

        var specification = IdentityProviderFilter.create()
                .setCommunicationProviderId(7)
                .build();

        assertSame(result, specification.toPredicate(identityProviderRoot, query, criteriaBuilder));
        verify(criteriaBuilder).exists(bindingSubquery);
        verify(criteriaBuilder).and(exists);
    }
}
