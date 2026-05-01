package com.example.attit.utils

import com.example.attit.R

object AvatarUtils {
    // 1. The Master List of all your avatars
    // We map a "String ID" (for the database) to a "Drawable Resource" (for the screen)
    val avatars = listOf(
        "icon1" to R.drawable.icon1,
        "icon2" to R.drawable.icon2,
        "icon3" to R.drawable.icon3,
        "icon4" to R.drawable.icon4,
        "icon5" to R.drawable.icon5,
        "icon6" to R.drawable.icon6,
        "icon7" to R.drawable.icon7,
        "icon8" to R.drawable.icon8,
        "icon9" to R.drawable.icon9,
        "icon10" to R.drawable.icon10,
        "icon11" to R.drawable.icon11,
        "icon12" to R.drawable.icon12,
        "icon13" to R.drawable.icon13,
        "icon14" to R.drawable.icon14
    )

    // 2. Helper to get the image resource from a string ID
    fun getAvatarResId(avatarId: String?): Int? {
        if (avatarId == null) return null
        return avatars.find { it.first == avatarId }?.second
    }
}