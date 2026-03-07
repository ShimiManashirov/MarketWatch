# Market Watch 📈

Market Watch is a modern Android application designed for investors and traders who need a clean, focused, and powerful tool to monitor financial markets. It solves the problem of overly complex trading platforms by providing a streamlined interface for tracking assets and receiving critical price alerts.

## 🚀 Key Features

-   **User Management**: Secure Login and Registration using Firebase Authentication with automatic session recovery.
-   **Watchlist & Portfolio**: Add and remove stocks to your personal watchlist. Manage a virtual portfolio with real-time profit/loss calculations.
-   **Real-time Market Data**: Integration with the **Finnhub REST API** for up-to-the-minute stock quotes, company profiles, and financial news.
-   **Intelligent Price Alerts**: Set custom price targets and receive **Background Push Notifications** via WorkManager when your targets are hit.
-   **Community Feed**: A social feature where users can post thoughts, share market insights, and interact with others (Create, Read, Update, Delete posts with image support).
-   **Offline First**: Comprehensive local caching using **Room Database** ensures your portfolio and feed are accessible even without an internet connection.

## 🏗 Architecture

The app is built following modern Android architectural standards to ensure maintainability and testability:

-   **MVVM (Model-View-ViewModel)**: Decouples UI logic from business logic.
-   **Repository Pattern**: A clean data layer that abstracts the source of data (Firebase vs. Room vs. REST APIs).
-   **Single Activity Architecture**: Uses the **Navigation Component** with **SafeArgs** for type-safe screen transitions and standard animations.
-   **Reactive Programming**: Utilizes **Kotlin Coroutines and Flow** for asynchronous operations, ensuring a smooth, non-blocking UI.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **Database**: Room (Local), Firebase Firestore (Remote)
-   **Networking**: Retrofit & OkHttp
-   **Asynchronous Processing**: Kotlin Coroutines, WorkManager
-   **UI/UX**: Material 3 Design, Shimmer (Loading States), Swipe-to-Refresh
-   **Images**: Picasso
-   **Charts**: MPAndroidChart

## 🔧 Setup & Installation

1.  Clone the repository.
2.  Open the project in **Android Studio (Ladybug or newer)**.
3.  Ensure you have a valid `google-services.json` file in the `app/` directory.
4.  Sync Gradle and build the project.
5.  Run on an emulator or physical device (Min SDK 33 / Android 13+).

---
*Developed as part of the Mobile Development Course.*
