/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.notifications.fakes

import proton.android.pass.domain.PendingInvite
import proton.android.pass.notifications.api.NotificationManager
import javax.inject.Inject

class TestNotificationManager @Inject constructor() : NotificationManager {

    private var hasNotificationPermission = true

    fun setHasNotificationPermission(value: Boolean) {
        hasNotificationPermission = value
    }

    override fun sendNotification() {
        // no op
    }

    override fun sendReceivedInviteNotification(pendingInvite: PendingInvite) = Unit

    override fun removeReceivedInviteNotification(pendingInvite: PendingInvite) = Unit

    override fun hasNotificationPermission(): Boolean = hasNotificationPermission

}
