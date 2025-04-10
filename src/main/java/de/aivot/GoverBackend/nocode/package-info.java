/**
 * This module contains all general logic to execute create and evaluate no code expressions.
 * It is used to calculate visibilities, values, validations and other dynamic values in the backend.
 * <br/>
 * <strong>Usage:</strong>
 * Instantiate a new {@link de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService} and call the evaluate method with the no code expression.
 * You can provide an array of {@link de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider} to the service, which will provide operators for the evaluation.
 * <br/>
 * <strong>Writing operator providers:</strong>
 * An operator is a class implementing the {@link de.aivot.GoverBackend.nocode.models.NoCodeOperator} interface.
 * The operator is then registered in a class implementing the {@link de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider} interface.
 * The operator provider should have a unique package name, so that the service can find it.
 */
package de.aivot.GoverBackend.nocode;