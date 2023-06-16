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

package proton.android.pass.featureaccount.impl

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import proton.android.pass.test.CallChecker
import proton.android.pass.test.HiltComponentActivity
import proton.android.pass.composecomponents.impl.R as CompR

@HiltAndroidTest
class AccountScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @get:Rule(order = 2)
    val intentsRule = IntentsRule()

    @Test
    fun accountScreenOnSignOutIsCalled() {
        val checker = CallChecker<Unit>()
        composeTestRule.setContent {
            AccountScreen(
                onNavigate = { if (it is AccountNavigation.SignOut) checker.call() }
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.getString(
                    R.string.account_sign_out_icon_content_description
                )
            )
            .performClick()
        assert(checker.isCalled)
    }

    @Test
    fun accountScreenOnBackIsCalled() {
        val checker = CallChecker<Unit>()
        composeTestRule.setContent {
            AccountScreen(
                onNavigate = { if (it is AccountNavigation.Back) checker.call() }
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.getString(CompR.string.navigate_back_icon_content_description)
            )
            .performClick()
        assert(checker.isCalled)
    }

    @Test
    fun accountScreenOnDeleteOpensWebsite() {
        composeTestRule.setContent {
            AccountScreen(
                onNavigate = {}
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.getString(R.string.account_delete_account_icon_content_description)
            )
            .performClick()
        intended(hasAction(Intent.ACTION_VIEW))
        intended(hasData("https://account.proton.me/u/0/pass/account-password"))
    }

    @Test
    fun accountScreenOnManageSubscription() {
        val checker = CallChecker<Unit>()
        composeTestRule.setContent {
            AccountScreen(
                onNavigate = { if (it is AccountNavigation.Subscription) checker.call() }
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.getString(R.string.manage_subscription_icon_content_description)
            )
            .performClick()
        assert(checker.isCalled)
    }
}
