package piuk.blockchain.android.ui.backup.completed

import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.PrefsUtil
import timber.log.Timber
import javax.inject.Inject

class BackupWalletCompletedPresenter @Inject constructor(
        private val transferFundsDataManager: TransferFundsDataManager,
        private val prefsUtil: PrefsUtil
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = prefsUtil.getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L)
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    internal fun checkTransferableFunds() {
        transferFundsDataManager.transferableFundTransactionListForDefaultAccount
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({ triple ->
                    if (!triple.left.isEmpty()) {
                        view.showTransferFundsPrompt()
                    }
                }, { Timber.e(it) })
    }

}
