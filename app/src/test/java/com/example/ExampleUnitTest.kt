package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun tool_calculation_isCorrect() {
    val expr = "24 * 10 + 60"
    // Validate evaluation logic
    val parts = expr.split("+")
    val left = parts[0].trim().split("*")
    val result = (left[0].trim().toDouble() * left[1].trim().toDouble()) + parts[1].trim().toDouble()
    assertEquals(300.0, result, 0.001)
  }
}
