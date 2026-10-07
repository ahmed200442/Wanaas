package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChatRoomRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createChatRoom_validPayload_createsDocumentAndReturnsMetadata(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = ChatRoomRepository(firestore)
    val customRoomId = "room_${UUID.randomUUID().toString().replace("-", "")}"

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createChatRoom(
        roomName = ROOM_NAME,
        creatorId = aliceUid,
        customRoomId = customRoomId
      )
    }
    assertTrue(createResult.isSuccess)
    val metadata = createResult.getOrThrow()
    assertEquals(customRoomId, metadata.roomId)
    assertEquals(ROOM_NAME, metadata.roomName)
    assertEquals(aliceUid, metadata.creatorId)
    assertNotNull(metadata.timestamp)
  }

  @Test
  fun saveMemberProfile_and_enterAndLeaveRoom_publishesJoinLeaveBannersAndActiveMembers(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = ChatRoomRepository(firestore)
    val customRoomId = "room_${UUID.randomUUID().toString().replace("-", "")}"

    val memberProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveMemberProfile(
        displayName = "أحمد المصري",
        avatarEmoji = "👑",
        roleBadge = "VIP عضو",
        supabaseSynced = true,
        userId = aliceUid
      )
    }
    assertTrue("saveMemberProfile failed: ${memberProfileResult.exceptionOrNull()}", memberProfileResult.isSuccess)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createChatRoom(
        roomName = ROOM_NAME,
        creatorId = aliceUid,
        customRoomId = customRoomId
      ).getOrThrow()
    }

    val joinEventResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.enterRoom(
        roomId = customRoomId,
        memberName = "أحمد المصري",
        avatarEmoji = "👑",
        roleBadge = "VIP عضو",
        supabaseLinked = true,
        userId = aliceUid
      )
    }
    assertTrue(joinEventResult.isSuccess)
    assertEquals("دخل العضو أحمد المصري", joinEventResult.getOrThrow().bannerText)

    val activeMembers = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeRoomActiveMembers(customRoomId).first { it.any { m -> m.userId == aliceUid } }
    }
    assertTrue(activeMembers.any { it.memberName == "أحمد المصري" })

    val leaveEventResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.leaveRoom(
        roomId = customRoomId,
        memberName = "أحمد المصري",
        avatarEmoji = "👑",
        userId = aliceUid
      )
    }
    assertTrue(leaveEventResult.isSuccess)
    assertEquals("خرج العضو أحمد المصري", leaveEventResult.getOrThrow().bannerText)
  }

  @Test
  fun createAnonymousFadfada_regularUserCannotReadIdentity_adminOwnerCanReadIdentity(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = ChatRoomRepository(firestore)
    val customPostId = "fadfada_${UUID.randomUUID().toString().replace("-", "")}"

    val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createAnonymousFadfadaPost(
        content = "فضفضة من القلب بدون اسم",
        moodTag = "💭 فضفضة عامة",
        authorName = "أحمد المصري",
        authorEmail = ALICE_EMAIL,
        authorId = aliceUid,
        customPostId = customPostId
      )
    }
    assertTrue("createAnonymousFadfadaPost failed: ${postResult.exceptionOrNull()}", postResult.isSuccess)

    // Regular member Bob can observe the anonymous post, but CANNOT read the author's name/email
    signInTestUser(BOB_EMAIL)
    val bobRepo = ChatRoomRepository(firestore)
    val bobPosts = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeAnonymousFadfadaPosts().first { it.any { p -> p.postId == customPostId } }
    }
    assertTrue(bobPosts.any { it.postId == customPostId && it.content == "فضفضة من القلب بدون اسم" })

    val bobIdentityAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getFadfadaAdminIdentity(customPostId)
    }
    assertTrue(bobIdentityAttempt.isFailure)
    val deniedEx = bobIdentityAttempt.exceptionOrNull() as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deniedEx?.code)

    // App Owner (hamadanagy1979@gmail.com) CAN read the author's real name and email
    signInTestUser(OWNER_ADMIN_EMAIL)
    val ownerRepo = ChatRoomRepository(firestore)
    val ownerIdentityResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.getFadfadaAdminIdentity(customPostId)
    }
    assertTrue("Admin owner identity fetch failed: ${ownerIdentityResult.exceptionOrNull()}", ownerIdentityResult.isSuccess)
    val identity = ownerIdentityResult.getOrThrow()
    assertNotNull(identity)
    assertEquals("أحمد المصري", identity?.authorName)
    assertEquals(ALICE_EMAIL, identity?.authorEmail)
  }

  @Test
  fun getChatRoomById_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = ChatRoomRepository(firestore)
    val customRoomId = "room_${UUID.randomUUID().toString().replace("-", "")}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createChatRoom(
        roomName = ROOM_NAME,
        creatorId = aliceUid,
        customRoomId = customRoomId
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = ChatRoomRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getChatRoomById(customRoomId)
    }
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull(exception)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }

  @Test
  fun observeChatRooms_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
    auth.signOut()
    val repository = ChatRoomRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeChatRooms("unauth_uid").first()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val OWNER_ADMIN_EMAIL = "hamadanagy1979@gmail.com"
    const val ROOM_NAME = "🌙 سهرة مصرية ونس"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
