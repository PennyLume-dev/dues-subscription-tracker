package com.dues.app

import com.dues.app.data.parseCatalog
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The same file is hosted for remote updates, so it must always parse. */
class CatalogParseTest {
    @Test fun bundledCatalogParses() {
        assertTrue(parseCatalog(File("src/main/assets/catalog.json").readText()).size >= 60)
    }

    @Test fun versionsCompareAsDates() {
        assertTrue("2026-10-01" > "2026-09-25")
        assertTrue("2026-09-25.2" > "2026-09-25")
    }
}
