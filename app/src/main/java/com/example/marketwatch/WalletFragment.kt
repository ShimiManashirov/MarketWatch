package com.example.marketwatch

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WalletFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var balanceTextView: TextView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()
    
    private var userCurrency = "USD"
    private var currencySymbol = "$"
    private var exchangeRate = 1.0 

    private val symbols = mapOf("USD" to "$", "EUR" to "€", "ILS" to "₪", "GBP" to "£", "JPY" to "¥")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallet, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        balanceTextView = view.findViewById(R.id.walletBalanceTextView)
        val addFundsButton: MaterialButton = view.findViewById(R.id.addFundsButton)
        val withdrawFundsButton: MaterialButton = view.findViewById(R.id.withdrawFundsButton)
        val rvTransactions: RecyclerView = view.findViewById(R.id.rvTransactions)

        rvTransactions.layoutManager = LinearLayoutManager(context)
        adapter = TransactionAdapter(transactionList, userCurrency, currencySymbol, exchangeRate)
        rvTransactions.adapter = adapter

        loadUserData()
        loadTransactions()

        addFundsButton.setOnClickListener { showFundsDialog(true) }
        withdrawFundsButton.setOnClickListener { showFundsDialog(false) }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                userCurrency = snapshot.getString("currency") ?: "USD"
                currencySymbol = symbols[userCurrency] ?: "$"
                
                fetchExchangeRate(userCurrency) { rate ->
                    exchangeRate = rate
                    val balanceUsd = snapshot.getDouble("balance") ?: 0.0
                    val convertedBalance = balanceUsd * exchangeRate
                    
                    balanceTextView.text = "$currencySymbol${String.format("%.2f", convertedBalance)}"
                    adapter.updateCurrency(userCurrency, currencySymbol, exchangeRate)
                }
            }
    }

    private fun fetchExchangeRate(targetCurrency: String, onResult: (Double) -> Unit) {
        if (targetCurrency == "USD") {
            onResult(1.0)
            return
        }

        FrankfurterApiClient.apiService.getLatestRates("USD")
            .enqueue(object : Callback<FrankfurterResponse> {
                override fun onResponse(call: Call<FrankfurterResponse>, response: Response<FrankfurterResponse>) {
                    if (response.isSuccessful) {
                        val rate = response.body()?.rates?.get(targetCurrency) ?: 1.0
                        onResult(rate)
                    } else {
                        onResult(1.0) // Fallback
                    }
                }

                override fun onFailure(call: Call<FrankfurterResponse>, t: Throwable) {
                    onResult(1.0) // Fallback
                }
            })
    }

    private fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || !isAdded) return@addSnapshotListener

                transactionList.clear()
                for (doc in snapshots) {
                    val transaction = doc.toObject(Transaction::class.java).copy(id = doc.id)
                    transactionList.add(transaction)
                }
                adapter.updateData(transactionList)
            }
    }

    private fun showFundsDialog(isAdding: Boolean) {
        val input = EditText(requireContext()).apply {
            hint = "Amount in $userCurrency"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isAdding) "Deposit ($userCurrency)" else "Withdraw ($userCurrency)")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val amountText = input.text.toString()
                if (amountText.isNotEmpty()) {
                    val amountInUserCurrency = amountText.toDouble()
                    val amountInUsd = amountInUserCurrency / exchangeRate
                    executeFundsTransaction(if (isAdding) amountInUsd else -amountInUsd, if (isAdding) "DEPOSIT" else "WITHDRAW")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeFundsTransaction(deltaUsd: Double, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalanceUsd = snapshot.getDouble("balance") ?: 0.0
            val newBalanceUsd = currentBalanceUsd + deltaUsd

            if (newBalanceUsd < 0) throw Exception("Insufficient funds")

            transaction.update(userRef, "balance", newBalanceUsd)

            val transactionRef = userRef.collection("transactions").document()
            transaction.set(transactionRef, hashMapOf(
                "type" to type,
                "amount" to Math.abs(deltaUsd),
                "timestamp" to com.google.firebase.Timestamp.now()
            ))
        }.addOnSuccessListener {
            Toast.makeText(context, "Transaction successful", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
