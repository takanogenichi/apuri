package com.example.frenchquiz.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    answered: Int,
    correct: Int,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rate = if (answered > 0) correct * 100 / answered else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("おつかれさま！", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$correct / $answered 問 正解", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("正答率 $rate%", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    feedback(rate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("もう一度（不正解を復習）")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("ホームへ")
        }
    }
}

private fun feedback(rate: Int): String = when {
    rate >= 90 -> "素晴らしい！この調子で語彙と活用を盤石にしましょう。"
    rate >= 70 -> "good！間違えた問題を復習すればさらに伸びます。"
    rate >= 40 -> "あと一歩。『不正解のみ』で重点復習しましょう。"
    else -> "まずは解説をじっくり。繰り返せば必ず定着します。"
}
