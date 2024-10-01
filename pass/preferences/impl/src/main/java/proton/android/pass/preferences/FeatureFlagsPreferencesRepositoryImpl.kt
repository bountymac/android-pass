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

package proton.android.pass.preferences

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.proton.android.pass.preferences.BoolFlagPrefProto
import me.proton.android.pass.preferences.BooleanPrefProto
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository
import proton.android.pass.log.api.PassLogger
import proton.android.pass.preferences.FeatureFlag.ACCESS_KEY_V1
import proton.android.pass.preferences.FeatureFlag.ACCOUNT_SWITCH_V1
import proton.android.pass.preferences.FeatureFlag.AUTOFILL_DEBUG_MODE
import proton.android.pass.preferences.FeatureFlag.DIGITAL_ASSET_LINKS
import proton.android.pass.preferences.FeatureFlag.IDENTITY_V1
import proton.android.pass.preferences.FeatureFlag.SECURE_LINK_V1
import proton.android.pass.preferences.FeatureFlag.SECURITY_CENTER_V1
import proton.android.pass.preferences.FeatureFlag.SL_ALIASES_SYNC
import proton.android.pass.preferences.FeatureFlag.USERNAME_SPLIT
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("UNCHECKED_CAST")
@Singleton
class FeatureFlagsPreferencesRepositoryImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val featureFlagManager: FeatureFlagRepository,
    private val dataStore: DataStore<FeatureFlagsPreferences>
) : FeatureFlagsPreferencesRepository {

    override fun <T> get(featureFlag: FeatureFlag): Flow<T> = when (featureFlag) {
        AUTOFILL_DEBUG_MODE -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { autofillDebugModeEnabled.value }

        SECURITY_CENTER_V1 -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { securityCenterV1Enabled.value }

        IDENTITY_V1 -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { identityItemTypeEnabled.value }

        USERNAME_SPLIT -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { usernameSplitEnabled.value }

        ACCESS_KEY_V1 -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { accessKeyV1Enabled.value }

        SECURE_LINK_V1 -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { publicLinkV1Enabled.value }

        ACCOUNT_SWITCH_V1 -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { accountSwitchV1Enabled.value }

        SL_ALIASES_SYNC -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { simpleLoginAliasesSyncEnabled.value }

        DIGITAL_ASSET_LINKS -> getFeatureFlag(
            key = featureFlag.key,
            defaultValue = featureFlag.isEnabledDefault
        ) { digitalAssetLinkEnabled.value }
    }

    override fun <T> set(featureFlag: FeatureFlag, value: T?): Result<Unit> = when (featureFlag) {
        AUTOFILL_DEBUG_MODE -> setFeatureFlag {
            autofillDebugModeEnabled = boolFlagPrefProto(value)
        }

        SECURITY_CENTER_V1 -> setFeatureFlag {
            securityCenterV1Enabled = boolFlagPrefProto(value)
        }

        IDENTITY_V1 -> setFeatureFlag {
            identityItemTypeEnabled = boolFlagPrefProto(value)
        }

        USERNAME_SPLIT -> setFeatureFlag {
            usernameSplitEnabled = boolFlagPrefProto(value)
        }

        ACCESS_KEY_V1 -> setFeatureFlag {
            accessKeyV1Enabled = boolFlagPrefProto(value)
        }

        SECURE_LINK_V1 -> setFeatureFlag {
            publicLinkV1Enabled = boolFlagPrefProto(value)
        }

        ACCOUNT_SWITCH_V1 -> setFeatureFlag {
            accountSwitchV1Enabled = boolFlagPrefProto(value)
        }

        SL_ALIASES_SYNC -> setFeatureFlag {
            simpleLoginAliasesSyncEnabled = boolFlagPrefProto(value)
        }

        DIGITAL_ASSET_LINKS -> setFeatureFlag {
            digitalAssetLinkEnabled = boolFlagPrefProto(value)
        }
    }

    private fun <T> getFeatureFlag(
        key: String?,
        defaultValue: Boolean,
        prefGetter: FeatureFlagsPreferences.() -> BooleanPrefProto
    ): Flow<T> = if (key != null) {
        accountManager.getPrimaryUserId()
            .flatMapLatest { userId ->
                featureFlagManager.observe(
                    userId = userId,
                    featureId = FeatureId(id = key)
                )
            }
            .flatMapLatest { featureFlag ->
                dataStore.data
                    .catch { exception -> handleExceptions(exception) }
                    .map { preferences ->
                        fromBooleanPrefProto(
                            pref = prefGetter(preferences),
                            default = featureFlag?.value ?: defaultValue
                        ) as T
                    }
            }
    } else {
        dataStore.data
            .catch { exception -> handleExceptions(exception) }
            .map {
                fromBooleanPrefProto(prefGetter(it)) as T
            }
    }

    private fun setFeatureFlag(setter: FeatureFlagsPreferences.Builder.() -> Unit) = runCatching {
        runBlocking {
            dataStore.updateData { prefs ->
                val a = prefs.toBuilder()
                setter(a)
                a.build()
            }
        }
        return@runCatching
    }

    private fun <T> boolFlagPrefProto(value: T?): BoolFlagPrefProto {
        val builder = BoolFlagPrefProto.newBuilder()
        value?.let { builder.value = (it as Boolean).toBooleanPrefProto() }
        return builder.build()
    }

    private suspend fun FlowCollector<FeatureFlagsPreferences>.handleExceptions(exception: Throwable) {
        if (exception is IOException) {
            PassLogger.e("Cannot read preferences.", exception)
            emit(FeatureFlagsPreferences.getDefaultInstance())
        } else {
            throw exception
        }
    }

    override fun observeForAllUsers(featureFlag: FeatureFlag): Flow<Boolean> = accountManager.getAccounts()
        .flatMapLatest { accounts ->
            combine(
                accounts.map { account ->
                    observeIsFeatureEnabled(featureFlag, account.userId)
                }
            ) { areFeaturesEnabled ->
                areFeaturesEnabled.any { it }
            }
        }

    private fun observeIsFeatureEnabled(featureFlag: FeatureFlag, userId: UserId?): Flow<Boolean> =
        featureFlag.key?.let { featureFlagKey ->
            featureFlagManager.observe(
                userId = userId,
                featureId = FeatureId(id = featureFlagKey)
            ).flatMapLatest { remoteFeatureFlag ->
                dataStore.data
                    .catch { exception -> handleExceptions(exception) }
                    .map { preferences ->
                        fromBooleanPrefProto(
                            pref = getPrefProto(featureFlag, preferences),
                            default = remoteFeatureFlag?.value ?: featureFlag.isEnabledDefault
                        )
                    }
            }
        } ?: dataStore.data
            .catch { exception -> handleExceptions(exception) }
            .map { preferences -> fromBooleanPrefProto(getPrefProto(featureFlag, preferences)) }

    private fun getPrefProto(featureFlag: FeatureFlag, preferences: FeatureFlagsPreferences) = with(preferences) {
        when (featureFlag) {
            AUTOFILL_DEBUG_MODE -> autofillDebugModeEnabled
            SECURITY_CENTER_V1 -> securityCenterV1Enabled
            IDENTITY_V1 -> identityItemTypeEnabled
            USERNAME_SPLIT -> usernameSplitEnabled
            ACCESS_KEY_V1 -> accessKeyV1Enabled
            SECURE_LINK_V1 -> publicLinkV1Enabled
            ACCOUNT_SWITCH_V1 -> accountSwitchV1Enabled
            SL_ALIASES_SYNC -> simpleLoginAliasesSyncEnabled
            DIGITAL_ASSET_LINKS -> digitalAssetLinkEnabled
        }.value
    }
}
