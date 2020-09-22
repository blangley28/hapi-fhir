package ca.uhn.fhir.jpa.dao.search;

/*-
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeDeclaredChildDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.dao.SearchBuilder;
import ca.uhn.fhir.jpa.dao.predicate.PredicateBuilderToken;
import ca.uhn.fhir.jpa.dao.predicate.SearchBuilderJoinEnum;
import ca.uhn.fhir.jpa.dao.predicate.SearchBuilderTokenModeEnum;
import ca.uhn.fhir.jpa.dao.predicate.SearchFilterParser;
import ca.uhn.fhir.jpa.dao.search.sql.TokenIndexTable;
import ca.uhn.fhir.jpa.model.entity.BaseResourceIndexedSearchParam;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.model.entity.ResourceIndexedSearchParamToken;
import ca.uhn.fhir.jpa.model.entity.ResourceLink;
import ca.uhn.fhir.jpa.searchparam.extractor.BaseSearchParamExtractor;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.base.composite.BaseCodingDt;
import ca.uhn.fhir.model.base.composite.BaseIdentifierDt;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.util.VersionIndependentConcept;
import com.google.common.collect.Sets;
import com.healthmarketscience.sqlbuilder.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Scope("prototype")
class PredicateBuilderToken2 extends BasePredicateBuilder2 implements IPredicateBuilder2 {
	private final PredicateBuilder2 myPredicateBuilder;
	@Autowired
	private ITermReadSvc myTerminologySvc;
	@Autowired
	private ModelConfig myModelConfig;

	PredicateBuilderToken2(SearchBuilder2 theSearchBuilder, PredicateBuilder2 thePredicateBuilder) {
		super(theSearchBuilder);
		myPredicateBuilder = thePredicateBuilder;
	}

	@Override
	public Condition addPredicate(String theResourceName,
											RuntimeSearchParam theSearchParam,
											List<? extends IQueryParameterType> theList,
											SearchFilterParser.CompareOperation theOperation,
											From<?, ResourceLink> theLinkJoin,
											RequestPartitionId theRequestPartitionId) {

		if (theList.get(0).getMissing() != null) {
			From<?, ResourceIndexedSearchParamToken> join = myQueryStack.createLinkJoin(theLinkJoin, SearchBuilderJoinEnum.TOKEN, theSearchParam.getName());
			addPredicateParamMissingForNonReference(theResourceName, theSearchParam.getName(), theList.get(0).getMissing(), join, theRequestPartitionId);
			return null;
		}

		List<Predicate> codePredicates = new ArrayList<>();

		List<IQueryParameterType> tokens = new ArrayList<>();
		for (IQueryParameterType nextOr : theList) {

			if (nextOr instanceof TokenParam) {
				TokenParam id = (TokenParam) nextOr;
				if (id.isText()) {

					// Check whether the :text modifier is actually enabled here
					boolean tokenTextIndexingEnabled = BaseSearchParamExtractor.tokenTextIndexingEnabledForSearchParam(myModelConfig, theSearchParam);
					if (!tokenTextIndexingEnabled) {
						String msg;
						if (myModelConfig.isSuppressStringIndexingInTokens()) {
							msg = myContext.getLocalizer().getMessage(PredicateBuilderToken.class, "textModifierDisabledForServer");
						}else{
							msg = myContext.getLocalizer().getMessage(PredicateBuilderToken.class, "textModifierDisabledForSearchParam");
						}
						throw new MethodNotAllowedException(msg);
					}

					myPredicateBuilder.addLinkPredicateString(theResourceName, theSearchParam, theList, theLinkJoin, theRequestPartitionId);
					break;
				}
			}

			tokens.add(nextOr);
		}

		if (tokens.isEmpty()) {
			return null;
		}

		TokenIndexTable join = getSqlBuilder().addTokenSelector();
		addPartitionIdPredicate(theRequestPartitionId, join, codePredicates);
		Condition predicate = createPredicateToken(tokens, theResourceName, theSearchParam, myCriteriaBuilder, join, theOperation, theRequestPartitionId);

		if (predicate != null) {
			getSqlBuilder().addPredicate(predicate);
		}

		return predicate;
	}

	public Condition createPredicateToken(Collection<IQueryParameterType> theParameters,
													  String theResourceName,
													  RuntimeSearchParam theSearchParam,
													  CriteriaBuilder theBuilder,
													  TokenIndexTable theFrom,
													  RequestPartitionId theRequestPartitionId) {
		return createPredicateToken(
			theParameters,
			theResourceName,
			theSearchParam,
			theBuilder,
			theFrom,
			null,
                theRequestPartitionId);
	}

	// FIXME: remove unneeded params
	private Condition createPredicateToken(Collection<IQueryParameterType> theParameters,
														String theResourceName,
														RuntimeSearchParam theSearchParam,
														CriteriaBuilder theBuilder,
														TokenIndexTable theFrom,
														SearchFilterParser.CompareOperation operation,
														RequestPartitionId theRequestPartitionId) {
		final List<VersionIndependentConcept> codes = new ArrayList<>();
		String paramName = theSearchParam.getName();

		TokenParamModifier modifier = null;
		for (IQueryParameterType nextParameter : theParameters) {

			String code;
			String system;
			if (nextParameter instanceof TokenParam) {
				TokenParam id = (TokenParam) nextParameter;
				system = id.getSystem();
				code = (id.getValue());
				modifier = id.getModifier();
			} else if (nextParameter instanceof BaseIdentifierDt) {
				BaseIdentifierDt id = (BaseIdentifierDt) nextParameter;
				system = id.getSystemElement().getValueAsString();
				code = (id.getValueElement().getValue());
			} else if (nextParameter instanceof BaseCodingDt) {
				BaseCodingDt id = (BaseCodingDt) nextParameter;
				system = id.getSystemElement().getValueAsString();
				code = (id.getCodeElement().getValue());
			} else if (nextParameter instanceof NumberParam) {
				NumberParam number = (NumberParam) nextParameter;
				system = null;
				code = number.getValueAsQueryToken(myContext);
			} else {
				throw new IllegalArgumentException("Invalid token type: " + nextParameter.getClass());
			}

			if (system != null && system.length() > ResourceIndexedSearchParamToken.MAX_LENGTH) {
				throw new InvalidRequestException(
					"Parameter[" + paramName + "] has system (" + system.length() + ") that is longer than maximum allowed (" + ResourceIndexedSearchParamToken.MAX_LENGTH + "): " + system);
			}

			if (code != null && code.length() > ResourceIndexedSearchParamToken.MAX_LENGTH) {
				throw new InvalidRequestException(
					"Parameter[" + paramName + "] has code (" + code.length() + ") that is longer than maximum allowed (" + ResourceIndexedSearchParamToken.MAX_LENGTH + "): " + code);
			}

			/*
			 * Process token modifiers (:in, :below, :above)
			 */

			if (modifier == TokenParamModifier.IN) {
				codes.addAll(myTerminologySvc.expandValueSet(null, code));
			} else if (modifier == TokenParamModifier.ABOVE) {
				system = determineSystemIfMissing(theSearchParam, code, system);
				validateHaveSystemAndCodeForToken(paramName, code, system);
				codes.addAll(myTerminologySvc.findCodesAbove(system, code));
			} else if (modifier == TokenParamModifier.BELOW) {
				system = determineSystemIfMissing(theSearchParam, code, system);
				validateHaveSystemAndCodeForToken(paramName, code, system);
				codes.addAll(myTerminologySvc.findCodesBelow(system, code));
			} else {
				codes.add(new VersionIndependentConcept(system, code));
			}

		}

		List<VersionIndependentConcept> sortedCodesList = codes
			.stream()
			.filter(t -> t.getCode() != null || t.getSystem() != null)
			.sorted()
			.distinct()
			.collect(Collectors.toList());

		if (codes.isEmpty()) {
			// This will never match anything
			getSqlBuilder().setMatchNothing();
			return null;
		}

		return theFrom.createPredicateOrList(theSearchParam.getName(), sortedCodesList);
	}

	private String determineSystemIfMissing(RuntimeSearchParam theSearchParam, String code, String theSystem) {
		String retVal = theSystem;
		if (retVal == null) {
			if (theSearchParam != null) {
				Set<String> valueSetUris = Sets.newHashSet();
				for (String nextPath : theSearchParam.getPathsSplit()) {
					BaseRuntimeChildDefinition def = myContext.newTerser().getDefinition(myResourceType, nextPath);
					if (def instanceof BaseRuntimeDeclaredChildDefinition) {
						String valueSet = ((BaseRuntimeDeclaredChildDefinition) def).getBindingValueSet();
						if (isNotBlank(valueSet)) {
							valueSetUris.add(valueSet);
						}
					}
				}
				if (valueSetUris.size() == 1) {
					String valueSet = valueSetUris.iterator().next();
					ValueSetExpansionOptions options = new ValueSetExpansionOptions()
						.setFailOnMissingCodeSystem(false);
					List<VersionIndependentConcept> candidateCodes = myTerminologySvc.expandValueSet(options, valueSet);
					for (VersionIndependentConcept nextCandidate : candidateCodes) {
						if (nextCandidate.getCode().equals(code)) {
							retVal = nextCandidate.getSystem();
							break;
						}
					}
				}
			}
		}
		return retVal;
	}

	private void validateHaveSystemAndCodeForToken(String theParamName, String theCode, String theSystem) {
		String systemDesc = defaultIfBlank(theSystem, "(missing)");
		String codeDesc = defaultIfBlank(theCode, "(missing)");
		if (isBlank(theCode)) {
			String msg = myContext.getLocalizer().getMessage(SearchBuilder.class, "invalidCodeMissingSystem", theParamName, systemDesc, codeDesc);
			throw new InvalidRequestException(msg);
		}
		if (isBlank(theSystem)) {
			String msg = myContext.getLocalizer().getMessage(SearchBuilder.class, "invalidCodeMissingCode", theParamName, systemDesc, codeDesc);
			throw new InvalidRequestException(msg);
		}
	}

	// FIXME: handle these and remove
	private Predicate addPredicate(String theResourceName, String theParamName, CriteriaBuilder theBuilder, From<?, ResourceIndexedSearchParamToken> theFrom, List<VersionIndependentConcept> theTokens, TokenParamModifier theModifier, SearchBuilderTokenModeEnum theTokenMode, RequestPartitionId theRequestPartitionId) {
		/*
		 * Note: A null system value means "match any system", but
		 * an empty-string system value means "match values that
		 * explicitly have no system".
		 */
		Expression<Long> hashField;
		List<Long> values;
		switch (theTokenMode) {
			case SYSTEM_ONLY:
				hashField = theFrom.get("myHashSystem").as(Long.class);
				values = theTokens
					.stream()
					.map(t -> ResourceIndexedSearchParamToken.calculateHashSystem(getPartitionSettings(), theRequestPartitionId, theResourceName, theParamName, t.getSystem()))
					.collect(Collectors.toList());
				break;
			case VALUE_ONLY:
				hashField = theFrom.get("myHashValue").as(Long.class);
				values = theTokens
					.stream()
					.map(t -> ResourceIndexedSearchParamToken.calculateHashValue(getPartitionSettings(), theRequestPartitionId, theResourceName, theParamName, t.getCode()))
					.collect(Collectors.toList());
				break;
			case SYSTEM_AND_VALUE:
			default:
				hashField = theFrom.get("myHashSystemAndValue").as(Long.class);
				values = theTokens
					.stream()
					.map(t -> ResourceIndexedSearchParamToken.calculateHashSystemAndValue(getPartitionSettings(), theRequestPartitionId, theResourceName, theParamName, t.getSystem(), t.getCode()))
					.collect(Collectors.toList());
				break;
		}

		/*
		 * Note: At one point we had an IF-ELSE here that did an equals if there was only 1 value, and an IN if there
		 * was more than 1. This caused a performance regression for some reason in Postgres though. So maybe simpler
		 * is better..
		 */
		Predicate predicate = hashField.in(values);

		if (theModifier == TokenParamModifier.NOT) {
			Predicate identityPredicate = theBuilder.equal(theFrom.get("myHashIdentity").as(Long.class), BaseResourceIndexedSearchParam.calculateHashIdentity(getPartitionSettings(), theRequestPartitionId, theResourceName, theParamName));
			Predicate disjunctionPredicate = theBuilder.not(predicate);
			predicate = theBuilder.and(identityPredicate, disjunctionPredicate);
		}
		return predicate;
	}

	private <T> Expression<Boolean> toEqualOrIsNullPredicate(Path<T> theExpression, T theCode) {
		if (theCode == null) {
			return myCriteriaBuilder.isNull(theExpression);
		}
		return myCriteriaBuilder.equal(theExpression, theCode);
	}
}
