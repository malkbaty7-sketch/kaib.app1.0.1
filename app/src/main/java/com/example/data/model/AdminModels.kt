package com.example.data.model

data class AdminPermissions(
    val accessUserData: Boolean = false,       // 1- منح الحساب الوصول الى بيانات المستخدمين
    val deleteCode: Boolean = false,           // 2- امكانية حذف كود
    val editCode: Boolean = false,             // 3- امكانية تعديل كود
    val replaceCode: Boolean = false,          // 4- امكانية استبدال كود
    val addImageOrShape: Boolean = false,      // 5- امكانية اضافة صورة او شكل
    val addTextToApp: Boolean = false,         // 6- امكانية اضافة نصوص الى التطبيق
    val monitorActivity: Boolean = false       // 7- امكانية مراقبة نشاط التطبيق او نشاط المستخدمين داخل التطبيق
)

data class AdminUser(
    val id: String = java.util.UUID.randomUUID().toString(),
    val username: String,
    val userType: String, // "مدير", "مسؤول"
    val permissions: AdminPermissions
)

data class AppAnalyticsData(
    val totalUsers: Int = 14280,
    val totalDownloads: Int = 38450,
    val totalBooksUploaded: Int = 9260,
    val totalBooksCreated: Int = 5820,
    val totalProjectsCompleted: Int = 4130,
    val avgDailyHoursPersonal: Float = 2.4f,
    val avgDailyHoursGroup: Float = 6.8f,
    val peakUsageTime: String = "8:00 مساءً - 11:00 مساءً",
    val activeSessionsToday: Int = 1890
)

data class AppUserProfile(
    val id: String = "usr_001",
    val displayName: String = "الكاتب المبدع",
    val username: String = "kateeb_author",
    val email: String = "author@kateeb.app",
    val phone: String = "+966 50 123 4567",
    val avatarIconId: Int = 0,
    val isVip: Boolean = false,
    val publishedWorks: List<PublishedWork> = listOf(
        PublishedWork(
            id = "w1",
            title = "أسرار الفكر والبحث العلمي",
            excerpt = "دراسة تحليلية معمقة في مناهج التفكير المعاصر واستخلاص النتائج الأكاديمية.",
            isPublic = true,
            publishDate = System.currentTimeMillis() - 86400000L * 3
        ),
        PublishedWork(
            id = "w2",
            title = "مسودة رواية: أطياف المساء",
            excerpt = "فصول أولية حول مغامرة في مدينة أثرية قديمة.",
            isPublic = false,
            publishDate = System.currentTimeMillis() - 86400000L * 1
        )
    )
)

data class PublishedWork(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val excerpt: String,
    val isPublic: Boolean = true,
    val publishDate: Long = System.currentTimeMillis()
)

data class SharedProjectPermissions(
    val allowReuseTemplate: Boolean = true,    // 1- السماح للمستخدمين باعادة استخدام القالب
    val allowEditCopy: Boolean = true,         // 2- السماح للمستخدمين بالتعديل على النسخة التي قمت بمشاركتها
    val allowDownload: Boolean = true,         // 3- السماح للمستخدمين بالتحميل
    val allowRepublish: Boolean = false        // 4- امكانية اعادة نشر هذا الملف من قبل الاخرين
)

data class SupportTicket(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val senderName: String,
    val senderContact: String,
    val message: String,
    val date: Long = System.currentTimeMillis(),
    val status: String = "قيد المعالجة"
)

data class ParentalControlConfig(
    val isEnabled: Boolean = false,
    val pinCode: String = "1234",
    val filterMatureContent: Boolean = true
)
