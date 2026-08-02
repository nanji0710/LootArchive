package com.nanji.lootarchive.util

/** UI 反馈时长常量 — 集中管理各类自动消失计时，避免散落魔法数字 */
object Feedback {
    /** 成就解锁弹窗自动消失 */
    const val ACHIEVEMENT_DISMISS = 2500L
    /** 通用操作反馈（Toast/消息）自动消失 */
    const val MESSAGE_DISMISS = 2000L
    /** 相机对焦提示消失 */
    const val CAMERA_FOCUS_DISMISS = 900L
    /** 备份操作反馈 */
    const val BACKUP_DISMISS = 3000L
}
