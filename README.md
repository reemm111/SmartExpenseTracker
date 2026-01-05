# Smart Expense Tracker

### A Personal Finance Assistant for Students and Young Professionals

Smart Expense Tracker is an Android application developed using **Kotlin** that helps users record, categorize, and analyze their daily income and expenses. The application is designed with a focus on **simplicity, efficiency, and clarity**, reducing the friction commonly associated with manual expense tracking.

The app is built using **MVVM architecture** and **Room Database**, providing real-time balance updates, category-based spending summaries, and intelligent budget alerts. All core functionality works **offline**, ensuring reliability and privacy.


## Motivation and Problem Statement

Many existing expense tracking applications require multiple steps and extensive manual input to record a single transaction. This complexity often leads to user frustration and inconsistent usage, particularly among students and young professionals.

Smart Expense Tracker addresses this issue by:

* Minimizing required user input
* Providing smart category suggestions
* Offering clear visual feedback on spending habits
* Storing all data locally for fast access and offline use


## Key Features

### Transaction Management

* Add, edit, and delete income and expense transactions
* Real-time balance calculation (total income, total expenses, net balance)
* Swipe-to-delete gesture using `ItemTouchHelper`
* Undo option for accidental deletions
* Search and filter transactions by type, category, or date


### Smart Category Suggestions

To reduce manual effort, the application automatically suggests a category based on the entered transaction amount.

**Example rules for expenses:**

* $0 – $5 → Snacks
* $5 – $15 → Food
* $15 – $30 → Transportation
* $30 – $100 → Shopping
* $100 – $500 → Utilities
* $500+ → Rent

Users can manually change the suggested category if needed.

    
### Local Data Persistence

* All transactions are stored locally using Room Database
* Fully functional offline
* Data persists across application restarts


### Category Summary and Analytics

* Category-wise expense breakdown
* Percentage of total spending per category
* Color-coded visual indicators
* Categories sorted by highest spending


### Budget Management and Alerts

* Overall monthly budget configuration
* Optional category-specific budgets
* Automatic budget alerts:

  * Warning when spending reaches 80%
  * Critical alert when spending reaches or exceeds 100%
* Alerts displayed via Snackbars and optional system Notifications


## Android Concepts and Technologies Used

 Concept            Purpose                             
-------------------------------------------------                          
 Kotlin             Primary programming language        
 MVVM Architecture  Separation of concerns              
 Room Database      Local data persistence              
 LiveData           Lifecycle-aware UI updates          
 RecyclerView       Displaying transaction lists        
 ViewModel          Business logic and state management 
 Kotlin Coroutines  Asynchronous operations             
 ItemTouchHelper    Swipe gestures                      
 Notifications      Budget alerts                       
 SharedPreferences  User settings storage               


## Architecture Overview (MVVM)

UI (Activity / Fragment)
        ↓
    ViewModel
        ↓
    Repository
        ↓
Room Database (DAO)

MVVM pattern --> improves maintainability, simplifies lifecycle handling, and ensures a clean separation between UI and business logic.

## Application Data Flow

### Adding a Transaction

1. User enters transaction details
2. Smart category suggestion is applied
3. ViewModel validates the input
4. Repository inserts data through DAO
5. Room updates the database
6. LiveData notifies observers
7. UI updates automatically
8. Balance and budget checks are recalculated


## Error Handling and Stability

* Input validation on all user entries
* Safe coroutine usage to prevent UI blocking
* Graceful handling of empty states
* Stable behavior during configuration changes
* No dependency on internet connectivity for core features


## Stretch Goals and Extended Features
In addition to the original project proposal, several enhancements were implemented during development to improve usability and functionality.

### Implemented Beyond the Original Proposal

* **Multi-Currency Support**

  * Support for more than 160 currencies
  * Real-time exchange rate retrieval with intelligent caching
  * Offline functionality using cached exchange rates
  * Persistent currency selection across sessions

* **PDF Export**

  * Export complete transaction history to a formatted PDF
  * Includes income and expense summary and category breakdown
  * Allows sharing via email or other supported applications

* **Transaction Editing**

  * Existing transactions can be selected and modified
  * Changes reflected immediately using LiveData

* **Search and Filtering**

  * Real-time search functionality
  * Combined filters for transaction type and category

* **Enhanced Budget System**

  * Category-specific budgets in addition to overall budget
  * Distinct visual and notification-based alerts for warnings and limits

* **UI and UX Improvements**

  * Material Design 3 components
  * Empty-state views for improved feedback
  * Improved input validation and error messaging

## Setup Instructions

### Prerequisites

* Android Studio (Arctic Fox or newer)
* Android SDK 24 or higher
* Kotlin 1.9+

### Installation

1. Clone the repository:
   git clone https://github.com/reemm111/SmartExpenseTracker.git
   
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the application on an emulator or physical device (API 24+)


## Project Structure

app/
 ├── data/
 │   ├── dao/
 │   ├── entity/
 │   └── repository/
 ├── ui/
 │   ├── main/
 │   ├── addtransaction/
 │   ├── summary/
 │   └── budget/
 ├── viewmodel/
 └── MainActivity.kt


## Team Members

* Jessy Karamaoun
* Reem Karim

## License

This project was developed for educational purposes as part of the Android Development course (Semester 5).

