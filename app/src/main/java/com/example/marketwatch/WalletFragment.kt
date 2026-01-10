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
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WalletFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var balanceTextView: TextView
    private lateinit var adapter: TransactionAdapter
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var rvTransactions: RecyclerView
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
        shimmerContainer = view.findViewById(R.id.walletShimmerContainer)
        rvTransactions = view.findViewById(R.id.rvTransactions)
        val addFundsButton: MaterialButton = view.findViewById(R.id.addFundsButton)
        val withdrawFundsButton: MaterialButton = view.findViewById(R.id.withdrawFundsButton)

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
                if (!isAdded) return@addSnapshotListener
                
                if (e != null) {
                    Log.e("WalletFragment", "Error listening to user data", e)
                    return@addSnapshotListener
                }

                userCurrency = snapshot?.getString("currency") ?: "USD"
                currencySymbol = symbols[userCurrency] ?: "$"
                val balanceUsd = snapshot?.getDouble("balance") ?: 0.0
                
                fetchExchangeRate(userCurrency) { rate ->
                    exchangeRate = rate
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
                    val rate = if (response.isSuccessful) response.body()?.rates?.get(targetCurrency) ?: 1.0 else 1.0
                    onResult(rate)
                }
                override fun onFailure(call: Call<FrankfurterResponse>, t: Throwable) {
                    onResult(1.0)
                }
            })
    }

    private fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
        rvTransactions.visibility = View.GONE

        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE

                if (e != null) {
                    Log.e("WalletFragment", "Error loading transactions", e)
                    return@addSnapshotListener
                }

                transactionList.clear()
                snapshots?.forEach { doc ->
                    try {
                        val transaction = Transaction(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            symbol = doc.getString("symbol"),
                            amount = doc.getDouble("amount") ?: 0.0,
                            quantity = doc.getDouble("quantity"),
                            timestamp = doc.getTimestamp("timestamp")
                        )
                        transactionList.add(transaction)
                    } catch (ex: Exception) {
                        Log.e("WalletFragment", "Error parsing transaction", ex)
                    }
                }
                adapter.updateData(transactionList)
            }
    }

    private fun showFundsDialog(isAdding: Boolean) {
        val input = EditText(requireContext()).apply {
            hint = "0.00"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isAdding) "Deposit ($userCurrency)" else "Withdraw ($userCurrency)")
            .setMessage("Enter amount in $userCurrency:")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val amountText = input.text.toString()
                val amountValue = amountText.toDoubleOrNull() ?: 0.0
                if (amountValue > 0) {
                    val amountInUsd = amountValue / exchangeRate
                    executeFundsTransaction(if (isAdding) amountInUsd else -amountInUsd, if (isAdding) "DEPOSIT" else "WITHDRAW")
                } else {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeFundsTransaction(deltaUsd: Double, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        // שימוש בגישה ישירה ובטוחה יותר כדי להבטיח יצירת מסמך ורישום טרנזקציה
        userRef.get().addOnSuccessListener { snapshot ->
            val currentBalanceUsd = snapshot.getDouble("balance") ?: 0.0
            val newBalanceUsd = currentBalanceUsd + deltaUsd

            if (newBalanceUsd < 0) {
                Toast.makeText(context, "Insufficient funds", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // 1. עדכון היתרה (יוצר את המסמך/שדה אם חסר)
            userRef.set(hashMapOf("balance" to newBalanceUsd), SetOptions.merge())
                .addOnSuccessListener {
                    
                    // 2. הוספת הטרנזקציה להיסטוריה
                    val transactionData = hashMapOf(
                        "type" to type,
                        "amount" to Math.abs(deltaUsd),
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                    
                    userRef.collection("transactions").add(transactionData)
                        .addOnSuccessListener {
                            if (isAdded) Toast.makeText(context, "$type Successful!", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    if (isAdded) Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
