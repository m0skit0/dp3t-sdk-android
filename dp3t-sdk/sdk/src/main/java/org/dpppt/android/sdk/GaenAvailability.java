/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk;

public enum GaenAvailability {
	/**
	 * Ready to go
	 */
	GMS_AVAILABLE,
	/**
	 * Google Play Services need to be updated
	 */
	GMS_UPDATE_REQUIRED,
	/**
	 * Google Play Services are not available
	 */
	GMS_UNAVAILABLE,
	/**
	 * HMS Ready to go
	 */
	HMS_AVAILABLE,
	/**
	 * Huawei Play Services need to be updated
	 */
	HMS_UPDATE_REQUIRED,
	/**
	 * Huawei Play Services are not available
	 */
	HMS_UNAVAILABLE,

	/**
	 * Google and Huawei Play Services are both not available
	 */
	ALL_UNAVAILABLE
}
