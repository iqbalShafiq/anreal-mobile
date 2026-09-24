package co.ratmo.anreal.feature.chat.domain

import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test

class SkillsTest {

    @Test
    fun validate_rejects_bad_slug_and_mismatched_frontmatter() {
        val errors = validateSkillInput(
            name = "Bad Name",
            description = "Useful",
            bodyMd = "---\nname: other\ndescription: Useful\n---\nbody",
        )
        assertThat(errors.containsKey("name")).isTrue()
        assertThat(errors.containsKey("bodyMd")).isTrue()
    }

    @Test
    fun validate_accepts_matching_frontmatter_with_quotes() {
        val errors = validateSkillInput(
            name = "release-notes",
            description = "Draft notes",
            bodyMd = "---\nname: \"release-notes\"\ndescription: \"Draft notes\"\n---\n# body",
        )
        assertThat(errors.isEmpty()).isTrue()
    }

    @Test
    fun validate_rejects_missing_frontmatter() {
        val errors = validateSkillInput(name = "ok-name", description = "d", bodyMd = "# no fence")
        assertThat(errors.containsKey("bodyMd")).isTrue()
    }

    @Test
    fun status_defaults_active() {
        assertThat(Skill(id = "s1", name = "a", description = "d").status == SkillStatus.Active).isTrue()
        assertThat(Skill(id = "s1", name = "a", description = "d").isEnabled).isTrue()
    }
}
