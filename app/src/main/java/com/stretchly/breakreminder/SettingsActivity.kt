package com.stretchly.breakreminder

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.stretchly.breakreminder.model.BreakConfig
import com.stretchly.breakreminder.util.BreakScheduler
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 设置界面Activity
 *
 * 参考Stretchly的Preferences界面设计，提供以下配置项：
 *
 * 小休息设置：
 * - 间隔时间（5/10/15/20/25/30分钟）
 * - 持续时间（10/15/20/25/30秒）
 *
 * 长休息设置：
 * - 间隔时间（15/20/25/30/45/60分钟）
 * - 持续时间（1/2/3/4/5分钟）
 * - 每N次小休息后触发
 *
 * 行为设置：
 * - 严格模式开关
 * - 声音提醒开关
 * - 振动提醒开关
 * - 自然休息检测开关
 *
 * 界面设置：
 * - 显示休息建议开关
 */
class SettingsActivity : AppCompatActivity() {

    // 偏好设置管理器
    private lateinit var prefsManager: PreferencesManager
    // 当前配置
    private lateinit var currentConfig: BreakConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 初始化偏好设置管理器
        prefsManager = PreferencesManager(this)
        currentConfig = prefsManager.getConfig()

        // 设置返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化所有设置项
        initMiniBreakSettings()
        initLongBreakSettings()
        initBehaviorSettings()
        initDisplaySettings()
        initResetButton()
    }

    // ==================== 小休息设置 ====================

    /**
     * 初始化小休息相关设置
     * 间隔时间和持续时间
     */
    private fun initMiniBreakSettings() {
        // 小休息间隔时间下拉框
        val spinnerMiniInterval = findViewById<Spinner>(R.id.spinnerMiniBreakInterval)
        val miniIntervalOptions = listOf("5分钟", "10分钟", "15分钟", "20分钟", "25分钟", "30分钟")
        val miniIntervalValues = listOf(
            5 * 60 * 1000L, 10 * 60 * 1000L, 15 * 60 * 1000L,
            20 * 60 * 1000L, 25 * 60 * 1000L, 30 * 60 * 1000L
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, miniIntervalOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMiniInterval.adapter = adapter

        // 设置当前选中值
        val currentIndex = miniIntervalValues.indexOf(currentConfig.miniBreakInterval)
        if (currentIndex >= 0) spinnerMiniInterval.setSelection(currentIndex)

        // 小休息持续时间下拉框
        val spinnerMiniDuration = findViewById<Spinner>(R.id.spinnerMiniBreakDuration)
        val miniDurationOptions = listOf("10秒", "15秒", "20秒", "25秒", "30秒")
        val miniDurationValues = listOf(
            10 * 1000L, 15 * 1000L, 20 * 1000L, 25 * 1000L, 30 * 1000L
        )
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, miniDurationOptions)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMiniDuration.adapter = durationAdapter

        val durationIndex = miniDurationValues.indexOf(currentConfig.miniBreakDuration)
        if (durationIndex >= 0) spinnerMiniDuration.setSelection(durationIndex)
    }

    // ==================== 长休息设置 ====================

    /**
     * 初始化长休息相关设置
     */
    private fun initLongBreakSettings() {
        // 长休息间隔时间下拉框
        val spinnerLongInterval = findViewById<Spinner>(R.id.spinnerLongBreakInterval)
        val longIntervalOptions = listOf("15分钟", "20分钟", "25分钟", "30分钟", "45分钟", "60分钟")
        val longIntervalValues = listOf(
            15 * 60 * 1000L, 20 * 60 * 1000L, 25 * 60 * 1000L,
            30 * 60 * 1000L, 45 * 60 * 1000L, 60 * 60 * 1000L
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, longIntervalOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLongInterval.adapter = adapter

        val currentIndex = longIntervalValues.indexOf(currentConfig.longBreakInterval)
        if (currentIndex >= 0) spinnerLongInterval.setSelection(currentIndex)

        // 长休息持续时间下拉框
        val spinnerLongDuration = findViewById<Spinner>(R.id.spinnerLongBreakDuration)
        val longDurationOptions = listOf("1分钟", "2分钟", "3分钟", "4分钟", "5分钟")
        val longDurationValues = listOf(
            1 * 60 * 1000L, 2 * 60 * 1000L, 3 * 60 * 1000L,
            4 * 60 * 1000L, 5 * 60 * 1000L
        )
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, longDurationOptions)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLongDuration.adapter = durationAdapter

        val durationIndex = longDurationValues.indexOf(currentConfig.longBreakDuration)
        if (durationIndex >= 0) spinnerLongDuration.setSelection(durationIndex)

        // 每N次小休息后触发长休息
        val spinnerLongCount = findViewById<Spinner>(R.id.spinnerLongBreakCount)
        val countOptions = listOf("2次", "3次", "4次", "5次")
        val countValues = listOf(2, 3, 4, 5)
        val countAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countOptions)
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLongCount.adapter = countAdapter

        val countIndex = countValues.indexOf(currentConfig.longBreakIntervalCount)
        if (countIndex >= 0) spinnerLongCount.setSelection(countIndex)
    }

    // ==================== 行为设置 ====================

    /**
     * 初始化行为相关开关设置
     */
    private fun initBehaviorSettings() {
        // 严格模式开关
        val switchStrict = findViewById<Switch>(R.id.switchStrictMode)
        switchStrict.isChecked = currentConfig.strictMode

        // 声音开关
        val switchSound = findViewById<Switch>(R.id.switchSound)
        switchSound.isChecked = currentConfig.soundEnabled

        // 振动开关
        val switchVibration = findViewById<Switch>(R.id.switchVibration)
        switchVibration.isChecked = currentConfig.vibrationEnabled

        // 自然休息检测开关
        val switchNatural = findViewById<Switch>(R.id.switchNaturalBreak)
        switchNatural.isChecked = currentConfig.naturalBreakEnabled
    }

    // ==================== 界面设置 ====================

    /**
     * 初始化界面显示相关设置
     */
    private fun initDisplaySettings() {
        // 显示休息建议开关
        val switchIdeas = findViewById<Switch>(R.id.switchBreakIdeas)
        switchIdeas.isChecked = currentConfig.showBreakIdeas
    }

    // ==================== 重置按钮 ====================

    /**
     * 初始化重置为默认设置按钮
     */
    private fun initResetButton() {
        val btnReset = findViewById<android.widget.Button>(R.id.btnResetDefaults)
        btnReset.setOnClickListener {
            prefsManager.resetToDefaults()
            currentConfig = prefsManager.getConfig()
            // 重新加载界面
            recreate()
        }
    }

    // ==================== 保存设置 ====================

    /**
     * 用户离开设置页面时保存配置
     * 如果服务正在运行，重新调度休息提醒
     */
    override fun onPause() {
        super.onPause()
        saveSettings()
    }

    /**
     * 保存所有设置到SharedPreferences
     */
    private fun saveSettings() {
        // 从界面读取小休息设置
        val miniIntervalValues = listOf(
            5 * 60 * 1000L, 10 * 60 * 1000L, 15 * 60 * 1000L,
            20 * 60 * 1000L, 25 * 60 * 1000L, 30 * 60 * 1000L
        )
        val miniDurationValues = listOf(
            10 * 1000L, 15 * 1000L, 20 * 1000L, 25 * 1000L, 30 * 1000L
        )
        val longIntervalValues = listOf(
            15 * 60 * 1000L, 20 * 60 * 1000L, 25 * 60 * 1000L,
            30 * 60 * 1000L, 45 * 60 * 1000L, 60 * 60 * 1000L
        )
        val longDurationValues = listOf(
            1 * 60 * 1000L, 2 * 60 * 1000L, 3 * 60 * 1000L,
            4 * 60 * 1000L, 5 * 60 * 1000L
        )
        val countValues = listOf(2, 3, 4, 5)

        // 构建新的配置对象
        val newConfig = BreakConfig(
            miniBreakInterval = miniIntervalValues.getOrNull(
                findViewById<Spinner>(R.id.spinnerMiniBreakInterval).selectedItemPosition
            ) ?: currentConfig.miniBreakInterval,
            miniBreakDuration = miniDurationValues.getOrNull(
                findViewById<Spinner>(R.id.spinnerMiniBreakDuration).selectedItemPosition
            ) ?: currentConfig.miniBreakDuration,
            longBreakInterval = longIntervalValues.getOrNull(
                findViewById<Spinner>(R.id.spinnerLongBreakInterval).selectedItemPosition
            ) ?: currentConfig.longBreakInterval,
            longBreakDuration = longDurationValues.getOrNull(
                findViewById<Spinner>(R.id.spinnerLongBreakDuration).selectedItemPosition
            ) ?: currentConfig.longBreakDuration,
            longBreakIntervalCount = countValues.getOrNull(
                findViewById<Spinner>(R.id.spinnerLongBreakCount).selectedItemPosition
            ) ?: currentConfig.longBreakIntervalCount,
            strictMode = findViewById<Switch>(R.id.switchStrictMode).isChecked,
            soundEnabled = findViewById<Switch>(R.id.switchSound).isChecked,
            vibrationEnabled = findViewById<Switch>(R.id.switchVibration).isChecked,
            naturalBreakEnabled = findViewById<Switch>(R.id.switchNaturalBreak).isChecked,
            showBreakIdeas = findViewById<Switch>(R.id.switchBreakIdeas).isChecked,
            miniBreakColor = currentConfig.miniBreakColor,
            longBreakColor = currentConfig.longBreakColor
        )

        // 保存配置
        prefsManager.saveConfig(newConfig)

        // 如果服务正在运行，重新调度
        if (prefsManager.isServiceEnabled()) {
            val scheduler = BreakScheduler(this)
            scheduler.scheduleNextBreak(newConfig, prefsManager.getMiniBreakCount())
        }
    }

    /**
     * 处理返回按钮
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
