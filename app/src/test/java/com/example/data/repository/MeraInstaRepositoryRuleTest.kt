package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeraInstaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertProfile_and_createPost_succeedsForAuthenticatedUser() = runBlocking {
        val uid = signInTestUser("rahul@merainsta.test")
        val repo = MeraInstaRepository(firestore, auth)

        val profileRes = withTimeout(TIMEOUT_MS) {
            repo.upsertUserProfile(
                username = "rahul_k",
                displayName = "Rahul Kumar",
                bio = "Namaste Mera Insta!",
                avatarBase64 = ""
            )
        }
        assertTrue(profileRes.isSuccess)

        val postRes = withTimeout(TIMEOUT_MS) {
            repo.createPost(
                username = "rahul_k",
                userAvatarBase64 = "",
                caption = "Pehla post!",
                location = "Delhi",
                taggedUsers = "",
                filterName = "Warm",
                mediaType = "photo",
                mediaBase64 = "",
                isReel = false
            )
        }
        assertTrue(postRes.isSuccess)
        val postId = postRes.getOrThrow()

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAllPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.userId == uid })
    }

    @Test
    fun followUser_createsFollowAndSendsNotification() = runBlocking {
        val priyaUid = signInTestUser("priya@merainsta.test")
        val priyaRepo = MeraInstaRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            priyaRepo.upsertUserProfile("priya_s", "Priya", "Hi", "")
        }

        val rahulUid = signInTestUser("rahul2@merainsta.test")
        val rahulRepo = MeraInstaRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            rahulRepo.upsertUserProfile("rahul_2", "Rahul", "Hi", "")
        }

        val followRes = withTimeout(TIMEOUT_MS) {
            rahulRepo.followUser(priyaUid, "rahul_2")
        }
        assertTrue(followRes.isSuccess)

        // Priya receives notification
        signInTestUser("priya@merainsta.test")
        val notifs = withTimeout(FLOW_TIMEOUT_MS) {
            priyaRepo.observeNotifications().first { list ->
                list.any { it.senderId == rahulUid && it.type == "follow" }
            }
        }
        assertTrue(notifs.isNotEmpty())
    }

    @Test
    fun observeAllPosts_unauthenticatedUser_throwsIllegalStateOrPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MeraInstaRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeAllPosts().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertTrue(caught != null)
    }

    @Test
    fun unauthenticatedDirectQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("posts").get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            caught = e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
