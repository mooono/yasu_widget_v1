package com.example.yasuwidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.yasuwidget.infrastructure.scheduler.UpdateScheduler
import com.example.yasuwidget.ui.theme.YasuWidgetTheme

class MainActivity : ComponentActivity() {

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val permissionGranted = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            permissionGranted.value = results.values.any { it }
            if (permissionGranted.value) {
                // バックグラウンド位置情報も要求（Android 10+）
                requestBackgroundLocationIfNeeded()
            }
        }

    private val requestBackgroundPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // バックグラウンド位置情報の結果（任意）
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionGranted.value = hasLocationPermission()

        // Widget更新スケジュールを開始
        UpdateScheduler(this).scheduleNextUpdate()

        setContent {
            YasuWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        hasPermission = permissionGranted.value,
                        onRequestPermission = { requestLocationPermission() }
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return locationPermissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(locationPermissions)
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgPerm = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            if (ContextCompat.checkSelfPermission(this, bgPerm) != PackageManager.PERMISSION_GRANTED) {
                requestBackgroundPermissionLauncher.launch(bgPerm)
            }
        }
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "YasuWidget",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "通勤・移動中に直近の発車時刻を確認できるWidgetです。",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ステップ1: 位置情報パーミッション
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "① 位置情報の許可",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (hasPermission) {
                    Text(text = "✅ 許可済み", fontSize = 14.sp)
                } else {
                    Text(
                        text = "Widgetが現在地に応じて表示を切り替えるため、位置情報へのアクセスが必要です。",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequestPermission) {
                        Text("位置情報を許可する")
                    }
                }
            }
        }

        // ステップ2: Widgetの追加手順
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "② Widgetをホーム画面に追加",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "1. ホーム画面の空きスペースを長押し", fontSize = 13.sp)
                Text(text = "2.「ウィジェット」を選択", fontSize = 13.sp)
                Text(text = "3.「YasuWidget」→「交通Widget」を選択", fontSize = 13.sp)
                Text(text = "4. ホーム画面に配置", fontSize = 13.sp)
            }
        }

        // ステップ3: 操作説明
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "③ Widgetの操作",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "🔄 右上の更新ボタン: 手動で即時更新", fontSize = 13.sp)
                Text(text = "◀ ▶ ボタン: 駅を切り替え（30分間保持）", fontSize = 13.sp)
                Text(text = "※ 約1分ごとに自動更新を試行します", fontSize = 13.sp)
            }
        }

        // 注意事項
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚠ 注意事項",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "・省電力モード時は更新間隔が長くなります\n" +
                           "・Widgetには最終更新時刻が常に表示されます\n" +
                           "・祝日は平日ダイヤとして扱います\n" +
                           "・電車時刻表はサンプルデータです",
                    fontSize = 12.sp
                )
            }
        }
    }
}