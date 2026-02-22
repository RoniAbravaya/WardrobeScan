package com.wardrobescan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wardrobescan.app.ui.navigation.NavGraph
import com.wardrobescan.app.ui.theme.WardrobeScanTheme
import dagger.hilt.android.AndroidEntryPoint

import com.wardrobescan.app.ui.screen.PopupAnimationDemo

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WardrobeScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PopupAnimationDemo(onClose = {
                        setContent {
                            WardrobeScanTheme {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    NavGraph()
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}
