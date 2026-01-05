package com.example.smartexpensetracker.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.utils.CurrencyPreferences

class CurrencySelectionDialog : DialogFragment() {

    private var selectedCurrency: String = "USD"
    private var onCurrencySelected: ((String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selectedCurrency = CurrencyPreferences.getSelectedCurrency(requireContext())
        
        val currencies = CurrencyPreferences.getSupportedCurrencies()
        val currencyList = currencies.map { "${it.first} - ${it.second}" }.toTypedArray()
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Currency (${currencies.size} available)")
        
        // Find the index of the currently selected currency
        val selectedIndex = currencies.indexOfFirst { it.first == selectedCurrency }
        
        // Use setSingleChoiceItems for proper scrollable list
        builder.setSingleChoiceItems(currencyList, selectedIndex) { dialog, which ->
            selectedCurrency = currencies[which].first
            onCurrencySelected?.invoke(selectedCurrency)
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel", null)
        
        val dialog = builder.create()
        
        // Ensure the dialog list is scrollable
        dialog.setOnShowListener {
            dialog.listView?.let { listView ->
                // Scroll to selected item if exists
                if (selectedIndex >= 0) {
                    listView.setSelection(selectedIndex)
                }
            }
        }
        
        return dialog
    }
    
    fun setOnCurrencySelectedListener(listener: (String) -> Unit) {
        onCurrencySelected = listener
    }
    
    companion object {
        fun newInstance(onCurrencySelected: (String) -> Unit): CurrencySelectionDialog {
            val dialog = CurrencySelectionDialog()
            dialog.setOnCurrencySelectedListener(onCurrencySelected)
            return dialog
        }
    }
}
