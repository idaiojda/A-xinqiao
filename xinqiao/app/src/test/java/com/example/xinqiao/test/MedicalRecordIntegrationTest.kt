package com.example.xinqiao.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xinqiao.fragment.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 医疗记录重构集成测试
 * 验证所有新的现代片段都能正确实例化
 */
@RunWith(AndroidJUnit4::class)
class MedicalRecordIntegrationTest {

    @Test
    fun testModernFragmentsInstantiation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 测试所有新的现代片段都能正确创建
        val fragments = listOf(
            MedicalRecordOverviewFragmentNew::class.java,
            ConsultationRecordsFragmentNew::class.java,
            TestReportListFragmentNew::class.java,
            EmotionDiaryFragmentNew::class.java,
            HealthMetricsFragmentNew::class.java
        )
        
        fragments.forEach { fragmentClass ->
            try {
                val fragment = fragmentClass.newInstance()
                assertNotNull("片段 ${fragmentClass.simpleName} 应该能正确实例化", fragment)
                println("✅ ${fragmentClass.simpleName} 实例化成功")
            } catch (e: Exception) {
                fail("片段 ${fragmentClass.simpleName} 实例化失败: ${e.message}")
            }
        }
    }
    
    @Test
    fun testTitleBarResourcesExist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 测试标题栏资源是否存在
        val resources = context.resources
        
        try {
            // 测试渐变背景
            val gradientId = resources.getIdentifier("title_bar_gradient_modern", "drawable", context.packageName)
            assertTrue("标题栏渐变背景应该存在", gradientId > 0)
            
            // 测试现代布局
            val layoutId = resources.getIdentifier("activity_medical_record_ultra_modern", "layout", context.packageName)
            assertTrue("现代布局应该存在", layoutId > 0)
            
            println("✅ 标题栏资源验证成功")
        } catch (e: Exception) {
            fail("标题栏资源测试失败: ${e.message}")
        }
    }
}