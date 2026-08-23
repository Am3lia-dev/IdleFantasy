package com.fantasyidler.repository

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fantasyidler.data.db.AppDatabase
import com.fantasyidler.data.model.SkillSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [34])
class SessionClockTrustTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sessionRepo: SessionRepository
    private lateinit var starter: QueuedSessionStarter
    private lateinit var workerStarter: WorkerQueuedSessionStarter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val json = Json { ignoreUnknownKeys = true }
        val gameData = GameDataRepository(context, json)
        sessionRepo = SessionRepository(db.skillSessionDao(), context, json, gameData)
        val boostRepo = BoostRepository(gameData)
        val dailyQuestRepo = DailyQuestRepository(gameData)
        val weeklyQuestRepo = WeeklyQuestRepository(gameData)
        val playerRepo = PlayerRepository(
            db.playerDao(),
            db.questProgressDao(),
            db.farmingPatchDao(),
            json,
            dailyQuestRepo,
            weeklyQuestRepo,
            BuffNotificationScheduler(context),
            gameData,
            boostRepo,
        )
        starter = QueuedSessionStarter(
            boostRepo,
            context,
            playerRepo,
            sessionRepo,
            TownRepository(gameData, playerRepo, QuestRepository(db.questProgressDao(), gameData), boostRepo),
            gameData,
            MercenaryRepository(playerRepo, gameData, dailyQuestRepo),
            json,
        )
        workerStarter = WorkerQueuedSessionStarter(boostRepo, playerRepo, sessionRepo, gameData, json)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun overdueMainSession(id: String, startElapsedMs: Long?): SkillSession {
        val now = System.currentTimeMillis()
        return SkillSession(
            sessionId = id,
            skillName = "mining",
            startedAt = now - 3_600_000,
            endsAt = now - 1000,
            activityKey = "iron_ore",
            startElapsedMs = startElapsedMs,
        )
    }

    private fun overdueWorkerSession(id: String, slot: Int, startElapsedMs: Long?): SkillSession {
        val now = System.currentTimeMillis()
        return SkillSession(
            sessionId = id,
            skillName = "mining",
            startedAt = now - 3_600_000,
            endsAt = now - 1000,
            activityKey = "iron_ore",
            isWorkerSession = true,
            workerSlot = slot,
            startElapsedMs = startElapsedMs,
        )
    }

    @Test
    fun `trusted overdue session completes via watchdog`() = runBlocking {
        sessionRepo.insertSession(overdueMainSession("trusted", SystemClock.elapsedRealtime() - 3_600_000))

        sessionRepo.completeOverdueSessions(starter)

        assertTrue(sessionRepo.getSession("trusted")!!.completed)
    }

    @Test
    fun `manipulated clock anchor keeps overdue session incomplete`() = runBlocking {
        sessionRepo.insertSession(overdueMainSession("faked", SystemClock.elapsedRealtime() - 1000))

        sessionRepo.completeOverdueSessions(starter)

        assertFalse(sessionRepo.getSession("faked")!!.completed)
    }

    @Test
    fun `legacy session without anchor completes via watchdog`() = runBlocking {
        sessionRepo.insertSession(overdueMainSession("legacy", null))

        sessionRepo.completeOverdueSessions(starter)

        assertTrue(sessionRepo.getSession("legacy")!!.completed)
    }

    @Test
    fun `trusted overdue worker session completes via watchdog`() = runBlocking {
        sessionRepo.insertSession(overdueWorkerSession("w_trusted", 1, SystemClock.elapsedRealtime() - 3_600_000))

        sessionRepo.completeOverdueSessions(starter, workerStarter)

        assertTrue(sessionRepo.getSession("w_trusted")!!.completed)
    }

    @Test
    fun `manipulated worker anchor keeps overdue worker session incomplete`() = runBlocking {
        sessionRepo.insertSession(overdueWorkerSession("w_faked", 1, SystemClock.elapsedRealtime() - 1000))

        sessionRepo.completeOverdueSessions(starter, workerStarter)

        assertFalse(sessionRepo.getSession("w_faked")!!.completed)
    }
}
