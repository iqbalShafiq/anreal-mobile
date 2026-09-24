package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource

class StubSkillsDataSource : SkillsRemoteDataSource {
    override suspend fun listSkills(): Result<List<Skill>, DataError.Network> = Result.Success(emptyList())

    override suspend fun getSkill(id: String): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun deleteSkill(id: String): Result<Unit, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))
}
