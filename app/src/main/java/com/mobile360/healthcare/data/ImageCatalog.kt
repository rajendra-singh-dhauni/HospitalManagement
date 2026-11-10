package com.mobile360.healthcare.data

import com.mobile360.healthcare.R

// Image variants of the same person: the stored portrait, a small avatar and alternate portraits.
object ImageCatalog {
    private val raoPortrait = R.drawable.a_headshot_portrait_of_dr_arun_rao_an_ex
    private val raoAvatar = R.drawable.small_avatar_of_dr_arun_rao_with_friendl
    private val raoFriendly = R.drawable.a_portrait_of_dr_arun_rao_a_friendly_mal
    private val raoCloseUp = R.drawable.close_up_professional_portrait_of_dr_aru
    private val karanPortrait = R.drawable.small_thumbnail_avatar_of_indian_male_ca

    // Alternate portraits of the same doctor, for larger hero placements.
    val raoPortraits: List<Int> = listOf(raoPortrait, raoFriendly, raoCloseUp)

    fun avatar(imageRes: Int): Int = when (imageRes) {
        raoPortrait -> raoAvatar
        else -> imageRes
    }

    val karan: Int get() = karanPortrait
}
