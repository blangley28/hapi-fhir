package ca.uhn.fhir.empi.rules.matcher;

/*-
 * #%L
 * HAPI FHIR - Enterprise Master Patient Index
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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.empi.util.CanonicalIdentifier;
import ca.uhn.fhir.empi.util.IdentifierUtil;
import org.hl7.fhir.instance.model.api.IBase;

public class IdentifierMatcher implements IEmpiFieldMatcher {
	/**
	 *
	 * @return true if the two fhir identifiers are the same.  If @param theIdentifierSystem is not null, then the
	 * matcher only returns true if the identifier systems also match this system.
	 * @throws UnsupportedOperationException if either Base is not an Identifier instance
	 */
	@Override
	public boolean matches(FhirContext theFhirContext, IBase theLeftBase, IBase theRightBase, boolean theExact, String theIdentifierSystem) {
		CanonicalIdentifier left = IdentifierUtil.identifierDtFromIdentifier(theLeftBase);
		if (theIdentifierSystem != null) {
			if (!theIdentifierSystem.equals(left.getSystemElement().getValueAsString())) {
				return false;
			}
		}
		CanonicalIdentifier right = IdentifierUtil.identifierDtFromIdentifier(theRightBase);
		return left.equals(right);
	}
}
