/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * An abstraction for a commit from a repository.
 */
public interface IRepositoryCommit extends IRepositoryObject {

	/**
	 * Retrieves the {@link RevCommit}.
	 *
	 * @return the commit
	 */
	RevCommit getRevCommit();

	/**
	 * {@inheritDoc}
	 *
	 * @return the {@link ObjectId} of the {@link RevCommit}.
	 */
	@Override
	default ObjectId getObjectId() {
		RevCommit commit = getRevCommit();
		return commit == null ? null : commit.getId();
	}

}
