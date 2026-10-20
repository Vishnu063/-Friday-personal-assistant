package com.friday.assistant

import android.content.Context
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ReferralsTest {
    private lateinit var context: Context
    private lateinit var book: Referrals
    private val connections = "First Name,Last Name,URL,Company,Position\nMira,Shah,https://www.linkedin.com/in/mira,Example,Engineer\nArun,Rao,https://www.linkedin.com/in/arun,Example,Recruiter\nमाया,शर्मा,https://www.linkedin.com/in/maya,Example,Engineer\n"
    @Before fun setup() { context = RuntimeEnvironment.getApplication(); context.deleteDatabase("referrals.db"); book = Referrals(context) }
    @After fun cleanup() { book.close(); context.deleteDatabase("referrals.db") }
    private fun fails(block: () -> Unit) { try { block(); fail("Expected validation to refuse the operation") } catch (_: IllegalArgumentException) {} }
    @Test fun incomingOutgoingBlankAndGroupMessagesExcludeParticipants() {
        book.importConnections(connections)
        book.importMessages("FROM,TO,CONTENT\nMira Shah,Me,Hi\nMe,Arun Rao,\nSomeone,\"Me; माया शर्मा\",Hello\n")
        assertEquals(3, book.contacts().count { it.prior })
        book.contacts().forEach { c -> fails { book.prepare(c.profile) } }
    }
    @Test fun profileMatchBlocksRenamedContactsAndReimportDoesNotForgetHistory() {
        book.importMessages("FROM,TO,SENDER PROFILE URL\nOld Name,Me,https://www.linkedin.com/in/MIRA/\n")
        book.importConnections(connections)
        assertTrue(book.contacts().first { it.name == "Mira Shah" }.prior)
        book.importMessages("FROM,TO,CONTENT\nUnrelated,Me,Hello\n")
        assertTrue(book.contacts().first { it.name == "Mira Shah" }.prior)
    }
    @Test fun unknownHistoryAndMalformedImportsNeverUnlockContacts() {
        book.importConnections(connections)
        fails { book.prepare(book.contacts().first().profile) }
        fails { book.importMessages("FROM,TO\nMira Shah,\n") }
        assertEquals(0, book.imported("messages"))
        fails { book.importConnections(connections + "Invalid,Row\n") }
        assertEquals(3, book.contacts().size)
        fails { book.importConnections(connections.replace("https://www.linkedin.com/in/mira", "https://evil.test/in/mira")) }
        assertEquals(3, book.contacts().size)
    }
    @Test fun ledgerSurvivesRefreshAndRestartAndRequiresPreparedState() {
        book.importConnections(connections); book.importMessages("FROM,TO\n")
        val profile = "https://www.linkedin.com/in/mira"
        fails { book.mark(profile, "sent") }
        book.prepare(profile); book.mark(profile, "sent")
        book.importConnections(connections); book.close(); book = Referrals(context)
        assertEquals("sent", book.contacts().first { it.profile == profile }.state)
        fails { book.prepare(profile) }
        val arun = "https://www.linkedin.com/in/arun"
        book.mark(arun, "blocked"); fails { book.prepare(arun) }
        book.clear(); assertTrue(book.contacts().isEmpty()); assertEquals(0, book.imported("messages"))
    }
    @Test fun duplicateNamesAreConservativelyExcluded() {
        book.importConnections(connections + "Mira,Shah,https://www.linkedin.com/in/mira-other,Other,Engineer\n")
        book.importMessages("FROM,TO\nMira Shah,Me\n")
        assertEquals(2, book.contacts().count { it.name == "Mira Shah" && it.prior })
    }
    @Test fun csvHandlesBomNotesQuotedMultilineAndEscapedQuotes() {
        val input = "\uFEFFNotes:\r\nExported data\r\nFROM,TO,CONTENT\r\nMira Shah,Me,\"A comma, a \\\"quote\\\"\nnext line\"\r\n".replace("\\\"", "\"\"")
        assertEquals(1, book.importMessages(input))
        fails { ReferralCsv.rows("FROM,TO\n\"unclosed,Me") }
        fails { ReferralCsv.rows("a,b\n\"value\"bad,other") }
    }
    @Test fun draftsUseOnlyEnteredFactsAndSafeProfileOrigins() {
        val draft = ReferralCsv.draft("Mira", "DevOps Engineer", "I work with AWS and Jenkins.", "Vishnu")
        assertTrue(draft.contains("Hi Mira")); assertTrue(draft.contains("I work with AWS and Jenkins."))
        assertFalse(draft.contains("4 years"))
        fails { ReferralCsv.draft("Mira", "DevOps Engineer", "", "Vishnu") }
        listOf("https://www.linkedin.com.evil/in/name", "javascript:alert(1)", "https://user@www.linkedin.com/in/name", "https://www.linkedin.com/in/name?token=bad").forEach { url -> fails { ReferralCsv.profile(url) } }
    }
}
