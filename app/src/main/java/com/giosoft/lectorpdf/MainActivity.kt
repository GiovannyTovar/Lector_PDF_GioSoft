package com.giosoft.lectorpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.giosoft.lectorpdf.ui.theme.LectorPdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LectorPdfTheme {
                Surface {
                    Text("Lector PDF GioSoft")
                }
            }
        }
    }
}
