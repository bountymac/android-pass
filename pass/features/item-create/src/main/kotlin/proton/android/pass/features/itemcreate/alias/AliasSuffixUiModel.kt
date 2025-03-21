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

package proton.android.pass.features.itemcreate.alias

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import proton.android.pass.domain.AliasSuffix

@Parcelize
data class AliasSuffixUiModel(
    val suffix: String,
    val signedSuffix: String,
    val isCustom: Boolean,
    val isPremium: Boolean,
    val domain: String
) : Parcelable {

    constructor(aliasSuffix: AliasSuffix) : this(
        suffix = aliasSuffix.suffix,
        signedSuffix = aliasSuffix.signedSuffix,
        isCustom = aliasSuffix.isCustom,
        isPremium = aliasSuffix.isPremium,
        domain = aliasSuffix.domain
    )

    fun toDomain(): AliasSuffix = AliasSuffix(
        suffix = suffix,
        signedSuffix = signedSuffix,
        isCustom = isCustom,
        isPremium = isPremium,
        domain = domain
    )
}
