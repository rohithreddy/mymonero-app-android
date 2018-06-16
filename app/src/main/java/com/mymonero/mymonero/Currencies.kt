//
//  Currencies.kt
//  MyMonero
//
//  Copyright (c) 2014-2018, MyMonero.com
//
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification, are
//  permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice, this list of
//	conditions and the following disclaimer.
//
//  2. Redistributions in binary form must reproduce the above copyright notice, this list
//	of conditions and the following disclaimer in the documentation and/or other
//	materials provided with the distribution.
//
//  3. Neither the name of the copyright holder nor the names of its contributors may be
//	used to endorse or promote products derived from this software without specific
//	prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
//  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//  THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
//  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
//  STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
//  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

package com.mymonero.mymonero

import android.util.Log
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.round

typealias CcyConversion_Rate = Double
typealias CurrencySymbol = String
typealias CurrencyUID = String

enum class Currency(val rawValue: String)
{
	none(""),
	XMR("XMR"),
	USD("USD"),
	AUD("AUD"),
	BRL("BRL"),
	CAD("CAD"),
	CHF("CHF"),
	CNY("CNY"),
	EUR("EUR"),
	GBP("GBP"),
	HKD("HKD"),
	INR("INR"),
	JPY("JPY"),
	KRW("KRW"),
	MXN("MXN"),
	NOK("NOK"),
	NZD("NZD"),
	SEK("SEK"),
	SGD("SGD"),
	TRY("TRY"),
	RUB("RUB"),
	ZAR("ZAR");

	val currencySymbol: CurrencySymbol = this.rawValue

	val symbol: CurrencySymbol
		get() {
			if (this == Currency.none) {
				throw AssertionError(".none has no symbol")
			}
			return this.rawValue
		}
	val fullTitleDescription: String
		get() {
			throw AssertionError("Not implemented")
		}
	val uid: CurrencyUID
		get() {
			if (this == Currency.none) {
				throw AssertionError(".none has no symbol")
			}
			return this.rawValue
		}
	val hasAtomicUnits: Boolean
		get() = this == Currency.XMR
	val unitsForDisplay: Int
		get() {
			if (this == Currency.XMR) {
				return MoneroConstants.currency_unitPlaces
			}
			return 2
		}
	val lazy_allCurrencies: List<Currency> by lazy {
		listOf(Currency.XMR, Currency.USD, Currency.AUD, Currency.BRL, Currency.CAD, Currency.CHF,
			Currency.CNY, Currency.EUR, Currency.GBP, Currency.HKD, Currency.INR, Currency.JPY,
			Currency.KRW, Currency.MXN, Currency.NOK, Currency.NZD, Currency.SEK, Currency.SGD,
			Currency.TRY, Currency.RUB, Currency.ZAR)
	}
	val lazy_allCurrencySymbols: List<CurrencySymbol> by lazy {
		this.lazy_allCurrencies.map { it.symbol }
	}
	//
	fun nonAtomicCurrency_localized_formattedString(
		final_amountDouble: Double,
		decimalSeparator: String = Currency.decimalSeparator
	) : String {
		assert(this != Currency.XMR)
		if (final_amountDouble == 0.0) {
			return "0"
		}
		val naiveLocalizedString = MoneroAmountFormatters.localized_doubleFormatter.format(final_amountDouble)!!
		val components = naiveLocalizedString.split(decimalSeparator)
		val components_count = components.size
		if (components_count <= 0) {
			throw AssertionError("Unexpected 0 components while formatting nonatomic currency")
		}
		if (components_count == 1) {
			if (naiveLocalizedString.contains(decimalSeparator)) {
				throw AssertionError("Expected naiveLocalizedString.contains(decimalSeparator) == false")
			}
			return naiveLocalizedString + decimalSeparator + "00"
		}
		if (components_count != 2) {
			throw AssertionError("Expected components_count == 2")
		}
		val component_1 = components[0]
		val component_2 = components[1]
		val component_2_characters_count = component_2.count()
		if (component_2_characters_count > this.unitsForDisplay) {
			throw AssertionError("Expected component_2_characters_count <= this.unitsForDisplay")
		}
		val requiredNumberOfZeroes = this.unitsForDisplay - component_2_characters_count
		var rightSidePaddingZeroes = ""
		if (requiredNumberOfZeroes > 0) {
			for (i in 0 until requiredNumberOfZeroes) {
				rightSidePaddingZeroes += "0"
			}
		}
		return component_1 + decimalSeparator + component_2 + rightSidePaddingZeroes
	}
	fun displayUnitsRounded_amountInCurrency(moneroAmount: MoneroAmount): Double? {
		if (this == Currency.none) {
			throw AssertionError("Selected currency unexpectedly .none")
		}
		val moneroAmountDouble = DoubleFromMoneroAmount(moneroAmount)
		if (this == Currency.XMR) {
			return moneroAmountDouble
		}
		val xmrToCurrencyRate = CcyConversionRatesController.rateFromXMR_orNilIfNotReady(toCurrency = this)
		if (xmrToCurrencyRate == null) {
			return null
		}
		val raw_ccyConversionRateApplied_amount = moneroAmountDouble * xmrToCurrencyRate
		val roundingMultiplier = 10.0.pow(this.unitsForDisplay)
		val truncated_amount = round(roundingMultiplier * raw_ccyConversionRateApplied_amount) / roundingMultiplier
		//
		return truncated_amount
	}
	companion object {
		//
		val localeSymbols = (DecimalFormat.getInstance() as? DecimalFormat)?.decimalFormatSymbols
		val decimalSeparator = Character.toString(localeSymbols?.decimalSeparator ?: '.')
		//
		val ccyConversionRateCalculated_moneroAmountDouble_roundingPlaces = 4
		fun rounded_ccyConversionRateCalculated_moneroAmountDouble(
			userInputAmountDouble: Double,
			selectedCurrency: Currency
		) : Double? {
			if (selectedCurrency == Currency.none) {
				throw AssertionError("Selected currency unexpectedly .none")
			}
			val xmrToCurrencyRate = CcyConversionRatesController.rateFromXMR_orNilIfNotReady(toCurrency = selectedCurrency)
			if (xmrToCurrencyRate == null) {
				return null
			}
			val raw_ccyConversionRateApplied_amount = userInputAmountDouble / xmrToCurrencyRate
			val roundingMultiplier = 10.0.pow(ccyConversionRateCalculated_moneroAmountDouble_roundingPlaces)
			val truncated_amount = round(roundingMultiplier * raw_ccyConversionRateApplied_amount) / roundingMultiplier
			//
			return truncated_amount
		}
	}
}
object CcyConversionRatesController
{
	val didUpdateAvailabilityOfRates_fns = EventEmitter<CcyConversionRatesController, String?>()

	private val xmrToCurrencyRatesByCurrencyUID = mutableMapOf<CurrencyUID, CcyConversion_Rate>()

	init
	{
		this.setup()
	}
	private fun setup() {}

	fun isRateReady(currency: Currency) : Boolean {
		if (currency == Currency.none || currency == Currency.XMR) {
			throw AssertionError("Invalid 'currency' argument value")
		}
		return this.xmrToCurrencyRatesByCurrencyUID[currency.uid] != null
	}

	fun rateFromXMR_orNilIfNotReady(toCurrency: Currency) : CcyConversion_Rate? {
		if (toCurrency == Currency.none || toCurrency == Currency.XMR) {
			throw AssertionError("Invalid 'currency' argument value")
		}
		return this.xmrToCurrencyRatesByCurrencyUID[toCurrency.uid]
	}

	fun set(XMRToCurrencyRate: CcyConversion_Rate, forCurrency: Currency, isPartOfBatch: Boolean = false) : Boolean {
		val doNotNotify = isPartOfBatch
		val wasSetValueDifferent = XMRToCurrencyRate != this.xmrToCurrencyRatesByCurrencyUID[forCurrency.uid]
		this.xmrToCurrencyRatesByCurrencyUID[forCurrency.uid] = XMRToCurrencyRate
		if (doNotNotify != true) {
			this._notifyOf_updateTo_XMRToCurrencyRate()
		}
		return wasSetValueDifferent
	}

	fun ifBatched_notifyOf_set_XMRToCurrencyRate() {
		Log.d("CcyConversionRates", "Received updates: ${this.xmrToCurrencyRatesByCurrencyUID}")
		this._notifyOf_updateTo_XMRToCurrencyRate()
	}

	fun set_xmrToCcyRatesByCcy(xmrToCcyRatesByCcy: Map<Currency, Double>) {
		var wasAnyRateChanged = false
		for ((currency, rate) in xmrToCcyRatesByCcy) {
			val wasSetValueDifferent = this.set(
				XMRToCurrencyRate = rate,
				forCurrency = currency,
				isPartOfBatch = true
			)
			if (wasSetValueDifferent) {
				wasAnyRateChanged = true
			}
		}
		if (wasAnyRateChanged) {
			this.ifBatched_notifyOf_set_XMRToCurrencyRate()
		}
	}

	private fun _notifyOf_updateTo_XMRToCurrencyRate() {
		this.didUpdateAvailabilityOfRates_fns.invoke(this, null)
	}
}
