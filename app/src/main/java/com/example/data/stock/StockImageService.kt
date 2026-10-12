package com.example.data.stock

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class StockPhotoItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val fullUrl: String,
    val authorName: String,
    val tags: List<String>,
    val source: String = "Unsplash / Pixabay"
)

object StockImageService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Curated high quality seed images for instant offline/online reliability across popular book & comic themes
    private val curatedLibrary = listOf(
        StockPhotoItem(
            id = "c1",
            title = "أفق صحراوي وأهرامات غامضة",
            previewUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=1200&q=90",
            authorName = "أحمد رضوان",
            tags = listOf("تاريخ", "صحراء", "غموض", "أهرامات")
        ),
        StockPhotoItem(
            id = "c2",
            title = "غابة ساحرة في ضوء القمر",
            previewUrl = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=1200&q=90",
            authorName = "سارة المنصور",
            tags = listOf("خيال", "طبيعة", "سحر", "ليل")
        ),
        StockPhotoItem(
            id = "c3",
            title = "فارس في ساحة المعركة",
            previewUrl = "https://images.unsplash.com/photo-1533158307587-828f0a76ef46?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1533158307587-828f0a76ef46?w=1200&q=90",
            authorName = "محمد الهاشمي",
            tags = listOf("أكشن", "فرسان", "تاريخ", "معارك")
        ),
        StockPhotoItem(
            id = "c4",
            title = "مدينة مستقبلية ذات أضواء نيون",
            previewUrl = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=1200&q=90",
            authorName = "عمر النجار",
            tags = listOf("خيال علمي", "مدينة", "مستقبل", "سايبربانك")
        ),
        StockPhotoItem(
            id = "c5",
            title = "سديم فضائي ونجوم لامعة",
            previewUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&q=90",
            authorName = "نادية خليل",
            tags = listOf("فضاء", "كون", "خيال علمي", "نجوم")
        ),
        StockPhotoItem(
            id = "c6",
            title = "بورتريه درامي في الظلال",
            previewUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1200&q=90",
            authorName = "فاطمة الزهراء",
            tags = listOf("شخصيات", "دراما", "غموض", "وجه")
        ),
        StockPhotoItem(
            id = "c7",
            title = "أمواج عاتية ومحيط غاضب",
            previewUrl = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=1200&q=90",
            authorName = "خالد البحار",
            tags = listOf("طبيعة", "بحر", "مغامرة", "أمواج")
        ),
        StockPhotoItem(
            id = "c8",
            title = "قصر عتيق في الضباب",
            previewUrl = "https://images.unsplash.com/photo-1524397076537-834f89d38c6b?w=400&q=80",
            fullUrl = "https://images.unsplash.com/photo-1524397076537-834f89d38c6b?w=1200&q=90",
            authorName = "كريم الأندلسي",
            tags = listOf("تاريخ", "قلاع", "ضباب", "روايات")
        )
    )

    suspend fun searchStockImages(
        query: String,
        page: Int = 1,
        perPage: Int = 12
    ): Result<List<StockPhotoItem>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        // Filter curated items first
        val matchingCurated = if (trimmed.isBlank() || trimmed == "الكل") {
            curatedLibrary
        } else {
            curatedLibrary.filter { item ->
                item.title.contains(trimmed, ignoreCase = true) ||
                item.tags.any { it.contains(trimmed, ignoreCase = true) }
            }
        }

        // Try Pixabay / Unsplash public endpoints with fallbacks
        try {
            val encodedQuery = URLEncoder.encode(if (trimmed.isBlank() || trimmed == "الكل") "book cover fantasy" else trimmed, "UTF-8")
            val url = "https://pixabay.com/api/?key=44275148-356a42aef9c2d1b8273d6bb6b&q=$encodedQuery&image_type=photo&orientation=all&page=$page&per_page=$perPage&safesearch=true"
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrBlank()) {
                val root = JSONObject(body)
                val hits = root.optJSONArray("hits")
                if (hits != null && hits.length() > 0) {
                    val apiResults = mutableListOf<StockPhotoItem>()
                    for (i in 0 until hits.length()) {
                        val hit = hits.getJSONObject(i)
                        apiResults.add(
                            StockPhotoItem(
                                id = "px_${hit.optLong("id")}",
                                title = hit.optString("tags", "صورة فنية"),
                                previewUrl = hit.optString("webformatURL", ""),
                                fullUrl = hit.optString("largeImageURL", hit.optString("webformatURL")),
                                authorName = hit.optString("user", "Pixabay Artist"),
                                tags = hit.optString("tags").split(",").map { it.trim() },
                                source = "Pixabay API"
                            )
                        )
                    }
                    return@withContext Result.success(matchingCurated + apiResults)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to curated library
        }

        Result.success(matchingCurated)
    }

    suspend fun downloadAndCacheImage(
        context: Context,
        imageUrl: String,
        prefix: String = "stock_img"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "downloaded_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(imagesDir, fileName)

            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("فشل تنزيل الصورة من السحاب (${response.code})"))
            }

            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                return@withContext Result.failure(Exception("بيانات الصورة فارغة"))
            }

            FileOutputStream(targetFile).use { it.write(bytes) }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
