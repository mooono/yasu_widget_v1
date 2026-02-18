package com.example.yasuwidget.infrastructure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.yasuwidget.domain.model.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 位置情報の取得（FusedLocationProviderClient）
 * SYS-REQ-042: 取得失敗時はnullを返す（呼び出し側でキャッシュフォールバック）
 *
 * 更新アルゴリズム:
 * 1. lastLocation（システムキャッシュ）を取得し、鮮度を確認
 *    - LOCATION_MAX_AGE_MS 以内 → そのまま使用（高速・省電力）
 *    - 古い場合 → ステップ2へ
 * 2. getCurrentLocation で能動的にGPS/ネットワーク測位を実行
 *    - 成功 → 新しい位置を使用
 *    - 失敗 → ステップ3へ
 * 3. 古い lastLocation があればフォールバックとして使用（無いよりはマシ）
 * 4. すべて失敗 → null を返す（呼び出し側でキャッシュフォールバック）
 */
class LocationRepository(private val context: Context) {

    companion object {
        private const val TAG = "LocationRepository"
        /** lastLocationの鮮度閾値（2分）。更新間隔（約1分）の2倍。 */
        internal const val LOCATION_MAX_AGE_MS = 120_000L
    }

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * 現在地を取得する
     * @return GeoPoint または null（権限不足/取得失敗時）
     */
    suspend fun getCurrentLocation(): GeoPoint? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "位置情報の権限がありません")
            return null
        }

        return try {
            val location = getLastOrCurrentLocation()
            if (location != null) {
                Log.d(TAG, "位置取得成功: lat=${location.latitude}, lon=${location.longitude}")
            } else {
                Log.w(TAG, "位置取得結果がnullです")
            }
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during location fetch", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during location fetch", e)
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * lastLocation → getCurrentLocation の順で位置を取得する。
     *
     * lastLocation はシステムにキャッシュされた直近の位置を返すため、
     * 他アプリ（Google Maps等）が起動していなくても取得できる。
     * ただし鮮度が古い場合（LOCATION_MAX_AGE_MS超過）は最寄り駅の
     * 判定が遅れるため、getCurrentLocation で能動的に再取得する。
     * getCurrentLocation はGPSを能動的に起動して新しい位置を取得するが、
     * アクティブな位置プロバイダがない場合は null を返すことがある。
     */
    @Suppress("MissingPermission")
    private suspend fun getLastOrCurrentLocation(): Location? {
        // まず lastLocation を試行（高速・省電力）
        val lastLocation = getLastLocation()
        if (lastLocation != null && isFreshEnough(lastLocation)) {
            Log.d(TAG, "lastLocation取得成功（鮮度OK）")
            return lastLocation
        }

        // lastLocation が null または古い場合、getCurrentLocation で能動的に取得
        Log.d(TAG, "lastLocationが${if (lastLocation == null) "null" else "古い"}、getCurrentLocationを試行")
        val currentLocation = requestCurrentLocation()
        if (currentLocation != null) {
            return currentLocation
        }

        // getCurrentLocation も失敗した場合、古い lastLocation をフォールバックとして使用
        if (lastLocation != null) {
            Log.d(TAG, "getCurrentLocation失敗、古いlastLocationをフォールバック使用")
            return lastLocation
        }

        return null
    }

    /**
     * 位置情報が十分に新しいかを判定する
     */
    internal fun isFreshEnough(location: Location): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        return ageMs <= LOCATION_MAX_AGE_MS
    }

    @Suppress("MissingPermission")
    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    Log.w(TAG, "lastLocation取得失敗", it)
                    continuation.resume(null)
                }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                Log.w(TAG, "getCurrentLocation取得失敗", it)
                continuation.resume(null)
            }
        }
    }
}
