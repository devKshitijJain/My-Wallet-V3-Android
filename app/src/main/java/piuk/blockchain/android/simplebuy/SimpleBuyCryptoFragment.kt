package piuk.blockchain.android.simplebuy

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.blockchain.extensions.exhaustive
import com.blockchain.notifications.analytics.CurrencyChangedFromBuyForm
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.buyConfirmClicked
import com.blockchain.notifications.analytics.cryptoChanged
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.DecimalDigitsInputFilter
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Currency

class SimpleBuyCryptoFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    ChangeCurrencyHost {

    override val model: SimpleBuyModel by inject()

    private var lastState: SimpleBuyState? = null

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    override fun onBackPressed(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_buy_crypto_title)

        model.process(SimpleBuyIntent.FetchBuyLimits(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.ENTER_AMOUNT))
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FetchSupportedFiatCurrencies)
        analytics.logEvent(SimpleBuyAnalytics.BUY_FORM_SHOWN)
        input_amount.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                model.process(SimpleBuyIntent.EnteredAmount(s.toString()))
            }
        })

        btn_continue.setOnClickListener {
            model.process(SimpleBuyIntent.BuyButtonClicked)
            analytics.logEvent(buyConfirmClicked(
                lastState?.order?.amount?.valueMinor.toString(),
                lastState?.fiatCurrency ?: "")
            )
        }

        fiat_currency.setOnClickListener {
            showBottomSheet(
                FiatCurrencyChooserBottomSheet
                    .newInstance(lastState?.supportedFiatCurrencies ?: return@setOnClickListener)
            )
        }
    }

    override fun onFiatCurrencyChanged(fiatCurrency: String) {
        if (fiatCurrency == lastState?.fiatCurrency) return
        model.process(SimpleBuyIntent.FiatCurrencyUpdated(fiatCurrency))
        model.process(SimpleBuyIntent.FetchBuyLimits(fiatCurrency))
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(fiatCurrency))
        analytics.logEvent(CurrencyChangedFromBuyForm(fiatCurrency))
        input_amount.clearFocus()
    }

    override fun onCryptoCurrencyChanged(currency: CryptoCurrency) {
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(currency))
        input_amount.clearFocus()
        analytics.logEvent(cryptoChanged(currency))
    }

    override fun render(newState: SimpleBuyState) {
        lastState = newState

        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }
        fiat_currency_symbol.text =
            Currency.getInstance(newState.fiatCurrency).getSymbol(Locale.getDefault())
        fiat_currency.text = newState.fiatCurrency
        newState.selectedCryptoCurrency?.let {
            crypto_icon.setImageResource(it.drawableResFilled())
            crypto_text.setText(it.assetName())
            activity.setupToolbar(resources.getString(R.string.simple_buy_token, it.displayTicker))
        }
        arrow.visibleIf { newState.availableCryptoCurrencies.size > 1 }
        if (newState.maxAmount != null && newState.minAmount != null) {
            input_amount.filters =
                arrayOf(
                    DecimalDigitsInputFilter(
                        newState.maxIntegerDigitsForAmount(),
                        newState.maxDecimalDigitsForAmount()
                    )
                )
            up_to_amount.visible()
            up_to_amount.text =
                getString(
                    R.string.simple_buy_up_to_amount,
                    newState.maxAmount!!.formatOrSymbolForZero()
                )
        }

        newState.predefinedAmounts.takeIf {
            it.isNotEmpty() && newState.selectedCryptoCurrency != null
        }?.let { values ->
            predefined_amount_1.asPredefinedAmountText(values.getOrNull(0))
            predefined_amount_2.asPredefinedAmountText(values.getOrNull(1))
            predefined_amount_3.asPredefinedAmountText(values.getOrNull(2))
            predefined_amount_4.asPredefinedAmountText(values.getOrNull(3))
        } ?: kotlin.run {
            predefined_amount_1.gone()
            predefined_amount_2.gone()
            predefined_amount_3.gone()
            predefined_amount_4.gone()
        }

        btn_continue.isEnabled = newState.isAmountValid
        input_amount.isEnabled = newState.selectedCryptoCurrency != null

        error_icon.goneIf(newState.error == null)
        input_amount.backgroundTintList =
            ColorStateList.valueOf(
                resources.getColor(
                    if (newState.error != null)
                        R.color.red_600
                    else
                        R.color.blue_600
                )
            )

        newState.error?.let {
            handleError(it, newState)
        } ?: kotlin.run {
            error_action.gone()
        }

        if (input_amount.text.toString() != newState.enteredAmount) {
            input_amount.setText(newState.enteredAmount)
        }

        crypto_text.takeIf { newState.availableCryptoCurrencies.size > 1 }?.setOnClickListener {
            showBottomSheet(
                CryptoCurrencyChooserBottomSheet
                    .newInstance(newState.availableCryptoCurrencies)
            )
        }

        if (newState.confirmationActionRequested && newState.kycVerificationState != null) {
            when (newState.kycVerificationState) {
                // Kyc state unknown because error, or gold docs unsubmitted
                KycState.PENDING -> {
                    model.process(SimpleBuyIntent.ConfirmationHandled)
                    model.process(SimpleBuyIntent.KycStarted)
                    navigator().startKyc()
                    analytics.logEvent(SimpleBuyAnalytics.START_GOLD_FLOW)
                }
                // Awaiting results state
                KycState.IN_REVIEW,
                KycState.UNDECIDED -> {
                    navigator().goToKycVerificationScreen()
                }
                // Got results, kyc verification screen will show error
                KycState.VERIFIED_BUT_NOT_ELIGIBLE,
                KycState.FAILED -> {
                    navigator().goToKycVerificationScreen()
                }
                // We have done kyc and are verified
                KycState.VERIFIED_AND_ELIGIBLE -> {
                    navigator().goToCheckOutScreen()
                }
            }.exhaustive
        }
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    private fun handleError(error: InputError, state: SimpleBuyState) {
        when (error) {
            InputError.ABOVE_MAX -> {
                error_action.apply {
                    text = resources.getString(R.string.use_max)
                    visible()
                    setOnClickListener {
                        input_amount.setText(state.maxAmount?.asInputAmount() ?: "")
                        analytics.logEvent(SimpleBuyAnalytics.BUY_MAX_CLICKED)
                    }
                }
                up_to_amount.text = resources.getString(R.string.too_high)
            }
            InputError.BELOW_MIN -> {
                error_action.apply {
                    text = resources.getString(R.string.use_min)
                    visible()
                    setOnClickListener {
                        analytics.logEvent(SimpleBuyAnalytics.BUY_MIN_CLICKED)
                        input_amount.setText(state.minAmount?.asInputAmount() ?: "")
                    }
                }
                up_to_amount.text = resources.getString(R.string.too_low)
            }
        }
    }

    private fun FiatValue.asInputAmount(): String =
        this.toStringWithoutSymbol().withoutThousandsSeparator().withoutTrailingDecimalsZeros()

    private fun AppCompatTextView.asPredefinedAmountText(amount: FiatValue?) {
        amount?.let { amount ->
            text = amount.formatOrSymbolForZero().withoutTrailingDecimalsZeros()
            visible()
            setOnClickListener {
                input_amount.setText(amount.asInputAmount())
            }
        } ?: this.gone()
    }

    private fun String.withoutThousandsSeparator(): String =
        replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")

    private fun String.withoutTrailingDecimalsZeros(): String =
        replace("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00", "")

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        model.process(SimpleBuyIntent.ClearError)
    }
}

interface ChangeCurrencyHost : SimpleBuyScreen {
    fun onFiatCurrencyChanged(fiatCurrency: String)
    fun onCryptoCurrencyChanged(currency: CryptoCurrency)
}