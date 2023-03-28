package proton.android.pass.data.impl.fakes

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.domain.entity.UserId
import proton.android.pass.common.api.LoadingResult
import proton.android.pass.data.api.repositories.RefreshSharesResult
import proton.android.pass.data.api.repositories.ShareRepository
import proton.pass.domain.Share
import proton.pass.domain.ShareId
import proton.pass.domain.entity.NewVault

class TestShareRepository : ShareRepository {

    private var createVaultResult: LoadingResult<Share> = LoadingResult.Loading
    private var refreshSharesResult: RefreshSharesResult =
        RefreshSharesResult(emptySet(), emptySet())
    private var observeSharesFlow = MutableSharedFlow<LoadingResult<List<Share>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
    )
    private var deleteVaultFlow = MutableSharedFlow<LoadingResult<Unit>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
    )
    private var getByIdResultFlow = MutableSharedFlow<LoadingResult<Share?>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
    )

    private var updateVaultResult: Result<Share> =
        Result.failure(IllegalStateException("UpdateVaultResult not set"))

    private var markAsPrimaryResult: Result<Share> =
        Result.failure(IllegalStateException("MarkAsPrimaryResult not set"))

    fun setCreateVaultResult(result: LoadingResult<Share>) {
        createVaultResult = result
    }

    fun setRefreshSharesResult(result: RefreshSharesResult) {
        refreshSharesResult = result
    }

    fun setDeleteVaultResult(result: LoadingResult<Unit>) {
        deleteVaultFlow.tryEmit(result)
    }

    fun setGetByIdResult(result: LoadingResult<Share?>) {
        getByIdResultFlow.tryEmit(result)
    }

    fun emitObserveShares(value: LoadingResult<List<Share>>) {
        observeSharesFlow.tryEmit(value)
    }

    fun setUpdateVaultResult(value: Result<Share>) {
        updateVaultResult = value
    }

    fun setMarkAsPrimaryResult(value: Result<Share>) {
        markAsPrimaryResult = value
    }

    override suspend fun createVault(userId: SessionUserId, vault: NewVault): LoadingResult<Share> =
        createVaultResult

    override suspend fun deleteVault(userId: UserId, shareId: ShareId): LoadingResult<Unit> =
        deleteVaultFlow.first()

    override suspend fun refreshShares(userId: UserId): RefreshSharesResult =
        refreshSharesResult

    override fun observeAllShares(userId: SessionUserId): Flow<LoadingResult<List<Share>>> =
        observeSharesFlow

    override suspend fun getById(userId: UserId, shareId: ShareId): LoadingResult<Share?> =
        getByIdResultFlow.first()

    override suspend fun updateVault(userId: UserId, shareId: ShareId, vault: NewVault): Share =
        updateVaultResult.getOrThrow()

    override suspend fun markAsPrimary(userId: UserId, shareId: ShareId): Share =
        markAsPrimaryResult.getOrThrow()
}
