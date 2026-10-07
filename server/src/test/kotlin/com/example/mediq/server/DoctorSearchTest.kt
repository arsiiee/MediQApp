package com.example.mediq.server

import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Specialty
import com.example.mediq.server.db.DoctorStore
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers what `GET /doctors?search=` matches.
 *
 * ## Why this file exists
 *
 * The Doctors screen is labelled *"Search doctor name or specialty"*, and until
 * 2026-10-07 the `search` parameter matched `full_name` and nothing else — the
 * `LOWER(d.full_name) LIKE ?` clause in `DoctorStore.list`. A patient who typed
 * "dermatology" got zero results and no explanation, which reads as "this clinic
 * has no dermatologists" rather than "the search box ignored half of what I
 * typed". Verified against a running server: `?search=Rivera` returned 1,
 * `?search=dermatology` returned 0.
 *
 * Nothing caught it. The search reached the server, the server answered `200`,
 * and an empty list is a correct empty list. The client cannot tell "matched
 * nothing" from "cannot search that" — `RetrofitDoctorRepository` maps an
 * unreachable server to an empty page too — so the screen had no way to report
 * the difference, and `smoke.ps1` only asserts a status code on this route.
 *
 * ## The contract being pinned
 *
 * `search` is a case-insensitive **substring** match against:
 *
 * - `doctors.full_name`
 * - the specialty's **wire value** — `internal_medicine`, `ob_gynecology`
 * - the specialty's **display name** — `Internal Medicine`, `Ob-Gynecology`
 *
 * Both spellings are searchable because the patient types the display name
 * ("internal medicine", with a space) while the row stores the wire value
 * (`internal_medicine`, underscored), and neither derives from the other
 * mechanically — `ob_gynecology` becomes "Ob-Gynecology", not "Ob Gynecology".
 *
 * This is **additive**: every query that matched before still matches, and one
 * that matches a specialty now matches rows it did not. Nothing that used to
 * return a doctor stops returning one, which is what makes it safe without a new
 * endpoint or a wire change.
 *
 * ## What `search` deliberately does *not* match
 *
 * `building`. It has its own query parameter and its own "Filter by clinic
 * location" affordance, so widening `search` over it would make the two filters
 * silently overlap and leave the patient unable to tell which one they used.
 *
 * Note the message-last argument order: this is `kotlin.test`, not the
 * `org.junit.Assert` the `:app` tests use, where the message comes first.
 */
class DoctorSearchTest {

    private fun database(): Database = Database(
        ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:search_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            jwtSecret = "test-secret-not-used-for-anything-real-123456",
            jwtIssuer = "mediq-test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
            sessionTimeZone = "UTC",
        )
    )

    private fun insertDoctor(db: Database, fullName: String, specialty: Specialty, building: String) {
        val doctorId = UUID.randomUUID().toString()
        db.tx { c ->
            c.prepareStatement(
                "MERGE INTO specialties (id, display_name) KEY (id) VALUES (?, ?)"
            ).use { st ->
                st.setString(1, specialty.wireValue)
                st.setString(2, specialty.displayName)
                st.executeUpdate()
            }
            c.prepareStatement(
                """
                INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                     license_number, bio, building, floor, room, languages, created_at)
                VALUES (?, ?, ?, 5, 70000, 'TEST-SEARCH', 'bio', ?, '1F', '101', 'en', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, doctorId)
                st.setString(2, fullName)
                st.setString(3, specialty.wireValue)
                st.setString(4, building)
                st.setObject(5, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }
        }
    }

    /** Three doctors in three specialties, two of them in the same building. */
    private fun withFixture(block: (DoctorStore) -> Unit) {
        val db = database()
        try {
            insertDoctor(db, "Rivera", Specialty.INTERNAL_MEDICINE, "Main Building")
            insertDoctor(db, "Cruz", Specialty.DERMATOLOGY, "Annex")
            insertDoctor(db, "Santos", Specialty.OB_GYNECOLOGY, "Main Building")
            block(DoctorStore(db, slotDurationMinutes = 30))
        } finally {
            db.close()
        }
    }

    private fun names(store: DoctorStore, query: DoctorQuery): List<String> =
        store.list(query, limit = 20, offset = 0).first.map { it.fullName }

    private fun searchNames(store: DoctorStore, text: String): List<String> =
        names(store, DoctorQuery(searchText = text))

    // --- The regression this file was written for ----------------------------

    @Test
    fun `a specialty wire value finds the doctors in that specialty`() {
        withFixture { store ->
            assertEquals(
                listOf("Cruz"),
                searchNames(store, "dermatology"),
                "the screen promises 'name or specialty', so the wire value must match",
            )
        }
    }

    @Test
    fun `a specialty display name matches even though the row stores an underscored wire value`() {
        withFixture { store ->
            // A patient types the display name. `internal_medicine` in the row
            // cannot be found by "internal medicine" unless the underscore is
            // bridged, which is the whole reason this is a test.
            assertEquals(
                listOf("Rivera"),
                searchNames(store, "internal medicine"),
            )
        }
    }

    @Test
    fun `a hyphenated specialty display name matches its underscored wire value`() {
        withFixture { store ->
            // "ob_gynecology" -> "Ob-Gynecology" is not a mechanical substitution,
            // which is exactly why both spellings have to be searchable.
            assertEquals(listOf("Santos"), searchNames(store, "Ob-Gynecology"))
            assertEquals(listOf("Santos"), searchNames(store, "ob_gynecology"))
        }
    }

    @Test
    fun `a specialty search ignores case`() {
        withFixture { store ->
            assertEquals(listOf("Cruz"), searchNames(store, "DERMATOLOGY"))
            assertEquals(listOf("Cruz"), searchNames(store, "Dermatology"))
        }
    }

    @Test
    fun `a partial specialty word matches`() {
        withFixture { store ->
            assertEquals(
                listOf("Cruz"),
                searchNames(store, "derma"),
                "a patient who types 'derma' is mid-word, not asking for nothing",
            )
        }
    }

    // --- The additive guarantee ---------------------------------------------

    @Test
    fun `a name search still finds the doctor`() {
        withFixture { store ->
            // The guard that makes widening the predicate safe: every search that
            // worked before this change must return exactly what it returned
            // before, so no consumer can regress.
            assertEquals(listOf("Rivera"), searchNames(store, "Rivera"))
            assertEquals(listOf("Santos"), searchNames(store, "Santos"))
        }
    }

    @Test
    fun `a name search returns only the doctor whose name matched`() {
        withFixture { store ->
            // Santos is Ob-Gynecology. Searching her name must not be widened by
            // her specialty or building leaking in.
            assertEquals(
                listOf("Santos"),
                searchNames(store, "Santos"),
                "a name match must not pull in her specialty or building",
            )
        }
    }

    // --- What search must not swallow ---------------------------------------

    @Test
    fun `a search matching neither name nor specialty returns nothing`() {
        withFixture { store ->
            assertEquals(
                emptyList(),
                searchNames(store, "zzzz"),
                "an unmatched search returns an empty list, not the whole directory",
            )
        }
    }

    @Test
    fun `a search does not match the building`() {
        withFixture { store ->
            assertEquals(
                emptyList(),
                searchNames(store, "Annex"),
                "'building' has its own parameter; search must not overlap it",
            )
            assertEquals(
                listOf("Cruz"),
                names(store, DoctorQuery(building = "Annex")),
                "the dedicated building filter must still reach the same doctor",
            )
        }
    }
}
