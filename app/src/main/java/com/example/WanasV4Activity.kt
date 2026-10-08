package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

/**
 * Compatibility entry point kept for the existing V4 shortcut.
 * The active room experience is now hosted by MainActivity with the native
 * RealVoiceRoomEngine. This screen avoids breaking the legacy navigation path.
 */
class WanasV4Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "وَنَس — التجربة الأصلية",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "الغرف الصوتية الحقيقية أصبحت Native ومتصلة مباشرة بمحرك الصوت.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Button(
                        onClick = { finish() },
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text("العودة إلى وَنَس")
                    }
                }
            }
        }
    }
}
