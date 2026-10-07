package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.FadfadaAnonymousScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatRoomsActionState
import com.google.firebase.Timestamp
import com.example.data.AnonymousFadfadaPost
import com.example.data.FadfadaAdminIdentity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FadfadaAnonymousScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun regularMember_seesAnonymousVentingWithoutAuthorNameOrEmail() {
        val samplePost = AnonymousFadfadaPost(
            postId = "post_101",
            authorId = "user_alice",
            content = "ربنا يفرجها علينا جميعاً",
            moodTag = "💭 فضفضة عامة",
            heartsCount = 3,
            timestamp = Timestamp.now()
        )
        val sampleIdentity = FadfadaAdminIdentity(
            postId = "post_101",
            authorId = "user_alice",
            authorName = "أحمد المصري",
            authorEmail = "alice@example.com",
            timestamp = Timestamp.now()
        )

        val state = ChatRoomsActionState(
            fadfadaPosts = listOf(samplePost),
            fadfadaAdminIdentities = mapOf("post_101" to sampleIdentity),
            isAdminOwner = false // Regular member
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                FadfadaAnonymousScreen(
                    currentUserId = "user_bob",
                    actionState = state,
                    isDarkMode = false,
                    onPublishFadfada = { _, _ -> },
                    onSendHeart = {},
                    onDeletePost = {},
                    onToggleAdminOwnerMode = {},
                    onBackToRooms = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fadfada_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fadfada_anonymous_label_post_101").assertExists()
        composeTestRule.onNodeWithText("ربنا يفرجها علينا جميعاً").assertExists()
        // Regular member must NOT see the admin identity box containing author name & email
        composeTestRule.onNodeWithTag("fadfada_admin_identity_box_post_101").assertDoesNotExist()
    }

    @Test
    fun adminAppOwner_seesAuthorRealNameAndEmailOnAnonymousFadfadaPost() {
        val samplePost = AnonymousFadfadaPost(
            postId = "post_102",
            authorId = "user_alice",
            content = "دعوة من القلب في ظهر الغيب",
            moodTag = "🌱 أمل وتفاؤل",
            heartsCount = 7,
            timestamp = Timestamp.now()
        )
        val sampleIdentity = FadfadaAdminIdentity(
            postId = "post_102",
            authorId = "user_alice",
            authorName = "أحمد المصري",
            authorEmail = "alice@example.com",
            timestamp = Timestamp.now()
        )

        val state = ChatRoomsActionState(
            fadfadaPosts = listOf(samplePost),
            fadfadaAdminIdentities = mapOf("post_102" to sampleIdentity),
            isAdminOwner = true // App Owner / Admin
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                FadfadaAnonymousScreen(
                    currentUserId = "owner_uid",
                    actionState = state,
                    isDarkMode = false,
                    onPublishFadfada = { _, _ -> },
                    onSendHeart = {},
                    onDeletePost = {},
                    onToggleAdminOwnerMode = {},
                    onBackToRooms = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fadfada_admin_identity_box_post_102").assertExists()
        composeTestRule.onNodeWithTag("fadfada_admin_author_name_post_102").assertExists()
        composeTestRule.onNodeWithTag("fadfada_admin_author_email_post_102").assertExists()
    }

    @Test
    fun publishNewFadfada_invokesCallbackWithContentAndMood() {
        var submittedContent = ""
        var submittedMood = ""

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                FadfadaAnonymousScreen(
                    currentUserId = "user_1",
                    actionState = ChatRoomsActionState(),
                    isDarkMode = false,
                    onPublishFadfada = { content, mood ->
                        submittedContent = content
                        submittedMood = mood
                    },
                    onSendHeart = {},
                    onDeletePost = {},
                    onToggleAdminOwnerMode = {},
                    onBackToRooms = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fadfada_content_input")
            .performTextInput("فضفضة تجريبية بدون اسم")
        composeTestRule.onNodeWithTag("publish_fadfada_button")
            .performClick()

        assertEquals("فضفضة تجريبية بدون اسم", submittedContent)
        assertEquals("💭 فضفضة عامة", submittedMood)
    }
}
