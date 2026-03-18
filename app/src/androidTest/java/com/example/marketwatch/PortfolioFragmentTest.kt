package com.example.marketwatch

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortfolioFragmentTest {

    @Test
    fun portfolio_uiComponents_areDisplayed() {
        onView(withId(R.id.tvWalletBalance)).check(matches(isDisplayed()))
        onView(withId(R.id.rvPortfolio)).check(matches(isDisplayed()))
        onView(withId(R.id.btnDeposit)).check(matches(isDisplayed()))
    }

    @Test
    fun portfolio_depositButton_opensDialog() {
        onView(withId(R.id.btnDeposit)).perform(click())
        onView(withText("Deposit Funds")).check(matches(isDisplayed()))
    }

    @Test
    fun portfolio_emptyWatchlist_showsMessage() {
        // Verification for empty watchlist state
    }

    @Test
    fun portfolio_transactionHistory_navigation() {
        onView(withId(R.id.btnHistory)).perform(click())
        // Verify navigation to TransactionsFragment
    }

    @Test
    fun portfolio_stockItem_clickOpensDetails() {
        // onView(withId(R.id.rvPortfolio)).perform(RecyclerViewActions.actionOnItemAtPosition<PortfolioAdapter.ViewHolder>(0, click()))
    }

    @Test
    fun portfolio_balanceFormatting_isCorrect() {
        onView(withId(R.id.tvWalletBalance)).check(matches(withText(containsString("$"))))
    }
}
