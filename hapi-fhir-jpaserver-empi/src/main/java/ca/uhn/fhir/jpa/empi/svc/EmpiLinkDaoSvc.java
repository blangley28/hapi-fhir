package ca.uhn.fhir.jpa.empi.svc;

/*-
 * #%L
 * HAPI FHIR JPA Server - Enterprise Master Patient Index
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

import ca.uhn.fhir.empi.api.EmpiLinkSourceEnum;
import ca.uhn.fhir.empi.api.EmpiMatchResultEnum;
import ca.uhn.fhir.empi.model.EmpiMessages;
import ca.uhn.fhir.jpa.dao.data.IEmpiLinkDao;
import ca.uhn.fhir.jpa.entity.EmpiLink;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmpiLinkDaoSvc {
	private static final Logger ourLog = LoggerFactory.getLogger(EmpiLinkDaoSvc.class);

	@Autowired
	private IEmpiLinkDao myEmpiLinkDao;
	@Autowired
	private ResourceTableHelper myResourceTableHelper;

	public void createOrUpdateLinkEntity(IBaseResource thePerson, IBaseResource theResource, EmpiMatchResultEnum theMatchResult, EmpiLinkSourceEnum theLinkSource, EmpiMessages theEmpiMessages) {
		Long personPid = myResourceTableHelper.getPidOrNull(thePerson);
		Long resourcePid = myResourceTableHelper.getPidOrNull(theResource);

		EmpiLink empiLink = getOrCreateEmpiLinkByPersonPidAndTargetPid(personPid, resourcePid);

		empiLink.setLinkSource(theLinkSource);
		empiLink.setMatchResult(theMatchResult);
		String message = String.format("Creating EmpiLink from %s to %s -> %s", thePerson.getIdElement().toUnqualifiedVersionless(), theResource.getIdElement().toUnqualifiedVersionless(), theMatchResult);
		theEmpiMessages.addMessage(message);
		ourLog.debug(message);
		myEmpiLinkDao.save(empiLink);
	}



	@NotNull
	public EmpiLink getOrCreateEmpiLinkByPersonPidAndTargetPid(Long thePersonPid, Long theResourcePid) {
		EmpiLink existing = getLinkByPersonPidAndTargetPid(thePersonPid, theResourcePid);
		if (existing != null) {
			return existing;
		} else {
			EmpiLink empiLink = new EmpiLink();
			empiLink.setPersonPid(thePersonPid);
			empiLink.setTargetPid(theResourcePid);
			return empiLink;
		}
	}

	public EmpiLink getLinkByPersonPidAndTargetPid(Long thePersonPid, Long theTargetPid) {

		if (theTargetPid == null || thePersonPid == null) {
			return null;
		}
		EmpiLink link = new EmpiLink();
		link.setTargetPid(theTargetPid);
		link.setPersonPid(thePersonPid);
		Example<EmpiLink> example = Example.of(link);
		return myEmpiLinkDao.findOne(example).orElse(null);
	}

	public List<EmpiLink> getEmpiLinksByTargetPidAndMatchResult(Long theTargetPid, EmpiMatchResultEnum theMatchResult) {
		EmpiLink exampleLink = new EmpiLink();
		exampleLink.setTargetPid(theTargetPid);
		exampleLink.setMatchResult(theMatchResult);
		Example<EmpiLink> example = Example.of(exampleLink);
		return myEmpiLinkDao.findAll(example);
	}

	public Optional<EmpiLink> getMatchedLinkForTargetPid(Long theTargetPid) {
		EmpiLink exampleLink = new EmpiLink();
		exampleLink.setTargetPid(theTargetPid);
		exampleLink.setMatchResult(EmpiMatchResultEnum.MATCH);
		Example<EmpiLink> example = Example.of(exampleLink);
		return myEmpiLinkDao.findOne(example);

	}

	public Optional<EmpiLink> getEmpiLinksByPersonPidTargetPidAndMatchResult(Long thePersonPid, Long theTargetPid, EmpiMatchResultEnum theMatchResult) {
		EmpiLink exampleLink = new EmpiLink();
		exampleLink.setPersonPid(thePersonPid);
		exampleLink.setTargetPid(theTargetPid);
		exampleLink.setMatchResult(theMatchResult);
		Example<EmpiLink> example = Example.of(exampleLink);
		return myEmpiLinkDao.findOne(example);
	}

	/**
	 * Get all {@link EmpiLink} which have {@link EmpiMatchResultEnum#POSSIBLE_DUPLICATE} as their match result.
	 *
	 * @return A list of EmpiLinks that hold potential duplicate persons.
	 */
    public List<EmpiLink> getPossibleDuplicates() {
		 EmpiLink exampleLink = new EmpiLink();
		 exampleLink.setMatchResult(EmpiMatchResultEnum.POSSIBLE_DUPLICATE);
		 Example<EmpiLink> example = Example.of(exampleLink);
		 return myEmpiLinkDao.findAll(example);
	 }

}