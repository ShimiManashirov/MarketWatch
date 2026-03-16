package com.example.marketwatch

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Picasso
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class StockDetailsActivity : AppCompatActivity() {

    private val viewModel: StockDetailsViewModel by viewModels()
    
    private lateinit var symbol: String
    private lateinit var description: String
    
    private lateinit var favoriteStarButton: ImageButton
    private lateinit var priceAlertButton: ImageButton
    private lateinit var tvOwnedShares: TextView
    private lateinit var sellButton: MaterialButton
    private lateinit var stockLogo: ImageView
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var lineChart: LineChart
    private lateinit var priceTextView: TextView
    private lateinit var priceIlsTextView: TextView
    private lateinit var chipIndustry: Chip
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var contentView: NestedScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_details)

        symbol = intent.getStringExtra("symbol") ?: ""
        description = intent.getStringExtra("description") ?: ""

        if (symbol.isEmpty()) {
            finish()
            return
        }

        initViews()
        setupObservers()
        
        viewModel.observeStockStatus(symbol)
        viewModel.fetchData(symbol)
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.stockDetailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = symbol
        toolbar.setNavigationOnClickListener { finish() }

        shimmerLayout = findViewById(R.id.stockDetailsShimmer)
        contentView = findViewById(R.id.stockDetailsContent)
        favoriteStarButton = findViewById(R.id.favoriteStarButton)
        priceAlertButton = findViewById(R.id.priceAlertButton)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        sellButton = findViewById(R.id.sellStockButton)
        stockLogo = findViewById(R.id.ivStockLogo)
        newsRecyclerView = findViewById(R.id.rvStockNews)
        lineChart = findViewById(R.id.stockChart)
        priceTextView = findViewById(R.id.detailsPrice)
        priceIlsTextView = findViewById(R.id.detailsPriceIls)
        chipIndustry = findViewById(R.id.chipIndustry)

        findViewById<TextView>(R.id.detailsSymbolHeader).text = symbol
        findViewById<TextView>(R.id.detailsDescriptionHeader).text = description
        
        newsRecyclerView.layoutManager = LinearLayoutManager(this)
        newsRecyclerView.adapter = NewsAdapter(emptyList())
        
        findViewById<MaterialButton>(R.id.buyStockButton).setOnClickListener { /* Logic */ }
        sellButton.setOnClickListener { /* Logic */ }
        favoriteStarButton.setOnClickListener { viewModel.toggleFavorite(symbol, description) }

        shimmerLayout.startShimmer()
    }

    private fun setupObservers() {
        viewModel.stockStatus.observe(this) { status ->
            val isFavorite = status?.isFavorite ?: false
            val ownedQty = status?.quantity ?: 0.0
            favoriteStarButton.setImageResource(if (isFavorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off)
            
            if (ownedQty > 0) {
                tvOwnedShares.visibility = View.VISIBLE
                tvOwnedShares.text = "You own: ${String.format("%.2f", ownedQty)} shares"
                sellButton.visibility = View.VISIBLE
            } else {
                tvOwnedShares.visibility = View.GONE
                sellButton.visibility = View.GONE
            }
        }

        viewModel.quote.observe(this) { quote ->
            quote?.let {
                priceTextView.text = "$${String.format("%.2f", it.currentPrice)}"
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                contentView.visibility = View.VISIBLE
            }
        }

        viewModel.companyProfile.observe(this) { profile ->
            profile?.let {
                if (!it.logo.isNullOrEmpty()) Picasso.get().load(it.logo).into(stockLogo)
                if (!it.industry.isNullOrEmpty()) {
                    chipIndustry.text = it.industry
                    chipIndustry.visibility = View.VISIBLE
                }
            }
        }
    }
}
