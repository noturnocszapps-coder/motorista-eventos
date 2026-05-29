package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.RoxouRepository
import com.example.ui.viewmodel.RoxouViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: RoxouRepository
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = RoxouRepository(db.roxouDao(), context)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun testSeedingAndViewModelInitialization() = runTest {
    try {
      // 1. Ensure prepopulated data executes successfully
      repository.ensurePrepopulatedData()

      // 2. Fetch seeded requests and profiles
      val requests = db.roxouDao().getAllRequests()
      assertNotNull(requests)

      // 3. Instantiate ViewModel and ensure no crashes on init block
      val viewModel = RoxouViewModel(repository)
      assertNotNull(viewModel)
    } catch (e: Throwable) {
      System.err.println("=== TEST EXCEPTION STACKTRACE ===")
      e.printStackTrace(System.err)
      System.err.println("=================================")
      throw e
    }
  }

  @Test
  fun readStringFromContext() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Reserva Roxou", appName)
  }
}
