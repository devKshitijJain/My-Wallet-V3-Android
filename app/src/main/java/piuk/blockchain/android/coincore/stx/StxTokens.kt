package piuk.blockchain.android.coincore.stx

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.stx.STXAccount
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

internal class StxTokens(
    rxBus: RxBus,
    private val payloadManager: PayloadManager,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.STX

    override fun init(): Completable =
        Completable.complete()
            .andThen(Completable.defer { loadAccounts() })
            .andThen(Completable.defer { initActivities() })
            .doOnComplete { Timber.d("Coincore: Init STX Complete") }
            .doOnError { Timber.d("Coincore: Init STX Failed") }

    private fun loadAccounts(): Completable =
        Completable.complete()

    private fun initActivities(): Completable =
        Completable.complete()

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(getDefaultStxAccountRef())

    override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.just(getStxAccount())

    override fun accounts(filter: Set<AssetFilter>): Single<CryptoAccountGroup> {
        TODO("not implemented")
    }

    override fun receiveAddress(): Single<String> {
        TODO("not implemented")
    }

    private fun getDefaultStxAccountRef(): AccountReference {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: throw IllegalStateException("Wallet not available")

        return hdWallets[0].stxAccount.toAccountReference()
    }

    private fun getStxAccount(): CryptoSingleAccount {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: throw IllegalStateException("Wallet not available")

        return StxCryptoAccount(
            label = "STX Account",
            address = hdWallets[0].stxAccount.bitcoinSerializedBase58Address
        )
    }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> {
        TODO("not implemented")
    }

    override fun noncustodialBalance(): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRate(epochWhen: Long): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> {
        TODO("not implemented")
    }

    // No supported actions at this time
    override val noncustodialActions = emptySet<AssetAction>()
    override val custodialActions = emptySet<AssetAction>()

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> {
        TODO("not implemented")
    }
}

private fun STXAccount.toAccountReference(): AccountReference.Stx =
    AccountReference.Stx(
        _label = "STX Account",
        address = bitcoinSerializedBase58Address
    )
