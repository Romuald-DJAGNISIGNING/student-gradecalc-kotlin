package com.romualdsigning.studentgradecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.romualdsigning.studentgradecalc.ui.screens.GradeCalculatorScreen
import com.romualdsigning.studentgradecalc.ui.theme.StudentGradeCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentGradeCalcTheme {
                GradeCalculatorScreen()
            }
        }
    }
}

