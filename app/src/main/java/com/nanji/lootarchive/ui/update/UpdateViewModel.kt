package com.nanji.lootarchive.ui.update

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.BuildConfig
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.util.ApkDownloadManager
import com.nanji.lootarchive.util.UpdateChecker
import com.nanji.lootarchive.util.UpdateInfo
import com.nanji.lootarchive.util.isValidVersionName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * 「检查更新」页的状态机。
 *
 * 改前的检查/下载/安装三件事全在 AboutScreen 的 Composable 里，用 5 个
 * `remember { mutableStateOf }` 加 5 个弹窗表达。问题是：一旦屏幕被销毁
 * （旋转、退出页面），下载进度与「已发现新版本」这个事实就被一起丢掉，
 * 用户回来只看到一张干净的关于页，还得重新查一遍。
 *
 * 现在把它们搬进 ViewModel：
 *  - 检查结果（[UpdateUiState]）是「一次事实」，跨页面存活；
 *  - 下载过程（[DownloadUiState]）是「一次任务」，进度、安装器启动结果、
 *    失败原因都留在这里，页面只负责把状态画出来 —— 不再弹任何阻塞式弹窗。
 */
sealed interface UpdateUiState {
    /** 本次进程还没查过。 */
    data object Idle : UpdateUiState

    data object Checking : UpdateUiState

    /** 远端版本号没有高于本机。 */
    data class UpToDate(val checkedAt: Long) : UpdateUiState

    /** 远端有新版本。 */
    data class Available(val info: UpdateInfo, val checkedAt: Long) : UpdateUiState

    /**
     * 检查失败（网络不可达、远端 JSON 不合法……）。
     *
     * [message] 直接是可展示的中文短句：页面不再自己判断异常类型，
     * 免得同一个失败在弹窗里是一句、在列表里是另一句。
     */
    data class Failed(val message: String, val checkedAt: Long) : UpdateUiState
}

/**
 * 下载任务的瞬时状态。
 *
 * [installRequested] 为 true 表示 APK 已落盘、系统安装器已拉起 —— 这一格要留着，
 * 用户从安装器返回时页面应该还在说「正在打开系统安装器」，而不是退回「下载并安装」。
 */
data class DownloadUiState(
    val running: Boolean = false,
    val progress: ApkDownloadManager.Progress = ApkDownloadManager.Progress(),
    val installRequested: Boolean = false,
    val error: String? = null,
    val fileName: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadUiState())
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()

    /**
     * 最近一次「检查完成」的时间（成功或失败都算）。
     *
     * 单独存一份而不是从 [uiState] 里推算：检查进行中的那几秒 [UpdateUiState.Checking]
     * 里没有时间戳，卡片上的「最近检查」就会掉回「未检查」，看着像丢了一次记录。
     */
    private val _lastCheckedAt = MutableStateFlow<Long?>(null)
    val lastCheckedAt: StateFlow<Long?> = _lastCheckedAt.asStateFlow()

    /** 自动检查开关：直接跟着 DataStore 走，设置页与本页改的是同一份数据。 */
    val autoCheck: StateFlow<Boolean> = settingsRepository.autoCheckUpdate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val downloader = ApkDownloadManager(app)

    /**
     * 本次进程是否已经自动检查过。
     *
     * ViewModel 挂在 Activity 上（本 App 没有 NavHost，页面是 AnimatedContent 手写路由），
     * 会跨「进页面 / 退出页面」一直活着。所以只靠界面的 LaunchedEffect(Unit) 不够 ——
     * 每次回到本页它都会再跑一次，等于一进页面就发一次网络请求。
     */
    private var autoChecked = false

    /**
     * 应用启动时（或进入更新页时）按「自动检查更新」开关静默查一次。
     *
     * 只触发一次由两处共同保证：MainScreen 启动时调用一次，页面用
     * LaunchedEffect(Unit) 兜底；这里用 [autoChecked] 防止退出再进重复请求。
     * 已经查过（不是 [UpdateUiState.Idle]）就绝不覆盖用户的当前结果。
     */
    fun autoCheckOnce() {
        if (autoChecked) return
        autoChecked = true
        viewModelScope.launch {
            // autoCheck 的 StateFlow 初值是 true，但用户可能早就在设置里关掉了；
            // 读一次 DataStore 的真实值，避免「明明关了还在自动检查」。
            if (settingsRepository.autoCheckUpdate.first() && _uiState.value is UpdateUiState.Idle) {
                check()
            }
        }
    }

    fun check() {
        // 重复点击（或自动检查与手动点击撞在一起）直接忽略，别发两次请求
        if (_uiState.value is UpdateUiState.Checking) return
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = UpdateChecker.check(BuildConfig.VERSION_CODE)
            val checkedAt = System.currentTimeMillis()
            _lastCheckedAt.value = checkedAt
            _uiState.value = result.fold(
                onSuccess = { info ->
                    if (info == null) UpdateUiState.UpToDate(checkedAt)
                    else UpdateUiState.Available(info, checkedAt)
                },
                onFailure = { error -> UpdateUiState.Failed(checkErrorMessage(error), checkedAt) },
            )
        }
    }

    /**
     * 下载当前发现的新版本，并在下载完成后直接拉起系统安装器。
     *
     * 所有失败都在这里翻成中文短句写进 [DownloadUiState.error]，
     * 页面不再 try/catch —— 旧实现里 `checkError` / `downloadError` 两套弹窗
     * 各写一遍错误文案，最后两边说法还不一样。
     */
    fun download() {
        val available = _uiState.value as? UpdateUiState.Available ?: return
        if (_downloadState.value.running) return

        val url = available.info.apkDownloadUrl
        if (url.isBlank()) {
            _downloadState.value = DownloadUiState(error = "这个版本没有提供下载地址，请到 GitHub Release 手动下载")
            return
        }

        // 文件名要拼进外部文件目录的路径：版本号不合法就直接不下载，
        // 别让远端下发的字符串有机会带出 ../ 之类的路径片段。
        val versionName = available.info.versionName
        if (!isValidVersionName(versionName)) {
            _downloadState.value = DownloadUiState(error = "版本号格式异常，为避免写入非法路径已取消下载")
            return
        }

        val fileName = "LootArchive-v$versionName.apk"
        // 每次下载都从干净的状态起步：上一次的进度/错误不能跟着新任务一起显示
        _downloadState.value = DownloadUiState(running = true, fileName = fileName)

        viewModelScope.launch {
            val result = downloader.download(url, fileName) { progress ->
                _downloadState.update { it.copy(progress = progress) }
            }
            result.fold(
                onSuccess = { file ->
                    // download() 内部的 withContext(IO) 返回后已经回到 viewModelScope 的主线程；
                    // install() 会 startActivity，只能在主线程调。
                    val launched = downloader.install(file)
                    _downloadState.update {
                        it.copy(
                            running = false,
                            installRequested = launched,
                            error = if (launched) null else INSTALL_FAILED_MESSAGE,
                        )
                    }
                },
                onFailure = { error ->
                    _downloadState.update { it.copy(running = false, error = downloadErrorMessage(error)) }
                },
            )
        }
    }

    fun setAutoCheck(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoCheckUpdate(enabled) }
    }

    /** 用户点「忽略」时清掉错误：回到「下载并安装」的初始按钮，而不是一直挂着红字。 */
    fun clearDownloadError() {
        _downloadState.update { it.copy(error = null, installRequested = false) }
    }

    private companion object {
        /** 安装器拉不起来（常见于未开启「安装未知应用」权限）时给用户的说法。 */
        const val INSTALL_FAILED_MESSAGE = "无法启动系统安装器，请确认已允许拾物集安装未知来源应用"
    }
}

/**
 * 把检查失败的异常翻成一句能读的中文。
 *
 * 保留「校验失败」与「网络失败」的区分是有意的：前者说明远端 version.json 有问题、
 * 重试没有意义；后者用户可以自己换个网络再试。
 */
private fun checkErrorMessage(error: Throwable): String = when (error) {
    is UnknownHostException, is SocketTimeoutException -> "网络连接超时，请检查网络后重试"
    is IOException -> "无法连接到更新服务器，请检查网络后重试"
    else -> when {
        error.message?.contains("Invalid update metadata") == true ->
            "更新信息校验失败（版本号或下载地址不合法），已拒绝本次更新"
        error is org.json.JSONException -> "更新信息解析失败，远端数据格式可能有问题"
        else -> "检查失败：${error.message ?: "未知错误"}"
    }
}

/** 下载失败的异常翻译。域名白名单与 HTTP 状态码是两类完全不同的处置方式。 */
private fun downloadErrorMessage(error: Throwable): String = when (error) {
    is IllegalArgumentException -> "下载地址不在允许的域名内，已停止下载"
    is UnknownHostException, is SocketTimeoutException -> "下载超时，请检查网络后重试"
    is IOException -> "下载中断，请检查网络后重试"
    else -> "下载失败：${error.message ?: "未知错误"}"
}
