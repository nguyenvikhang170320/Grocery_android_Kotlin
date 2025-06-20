package com.example.grocery.thumucquantrong

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val vietnamLocale = Locale("vi", "VN")

    val formatter: DecimalFormat = (NumberFormat.getCurrencyInstance(vietnamLocale) as DecimalFormat).apply {
        val symbols = DecimalFormatSymbols(vietnamLocale)
        symbols.currencySymbol = "đ"
        this.decimalFormatSymbols = symbols
        this.maximumFractionDigits = 0
        this.minimumFractionDigits = 0
    }

    fun format(amount: Double?): String {
        return if (amount != null) {
            formatter.format(amount)
        } else {
            formatter.format(0.0)
        }
    }


}