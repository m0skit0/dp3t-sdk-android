/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.TracingStatus.ErrorState;

public class SyncErrorState {

	private static SyncErrorState instance;

	private ErrorState syncError = null;
	private long syncErrorGracePeriod = 24 * 60 * 60 * 1000L;
	private long errorNotificationGracePeriod = 5 * 60 * 1000L;

	private SyncErrorState() { }

	public static synchronized SyncErrorState getInstance() {
		if (instance == null) {
			instance = new SyncErrorState();
		}
		return instance;
	}

	public void setSyncError(ErrorState syncError) {
		this.syncError = syncError;
	}

	public ErrorState getSyncError() {
		return syncError;
	}

	public void setSyncErrorGracePeriod(long durationMillis) {
		this.syncErrorGracePeriod = durationMillis;
	}

	public long getSyncErrorGracePeriod() {
		return syncErrorGracePeriod;
	}

	public void setErrorNotificationGracePeriod(long durationMillis) {
		this.errorNotificationGracePeriod = durationMillis;
	}

	public long getErrorNotificationGracePeriod() {
		return errorNotificationGracePeriod;
	}

}
