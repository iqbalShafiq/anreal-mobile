package co.ratmo.anreal.core.domain.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class ResultTest {

    private enum class SampleError : Error {
        TOO_SHORT,
    }

    @Test
    fun map_transforms_success_data() {
        val result: Result<Int, SampleError> = Result.Success(2)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(Result.Success(6))
    }

    @Test
    fun map_preserves_error() {
        val result: Result<Int, SampleError> = Result.Error(SampleError.TOO_SHORT)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(Result.Error(SampleError.TOO_SHORT))
    }

    @Test
    fun onSuccess_runs_only_for_success() {
        var seen: Int? = null

        Result.Success(4).onSuccess { seen = it }
        Result.Error(SampleError.TOO_SHORT).onSuccess { seen = -1 }

        assertThat(seen).isEqualTo(4)
    }

    @Test
    fun onFailure_runs_only_for_error() {
        var seen: SampleError? = null

        Result.Success(4).onFailure { seen = it }
        val error = Result.Error(SampleError.TOO_SHORT).onFailure { seen = it }

        assertThat(seen).isEqualTo(SampleError.TOO_SHORT)
        assertThat(error).isInstanceOf(Result.Error::class)
    }

    @Test
    fun asEmptyResult_drops_success_payload() {
        val empty: EmptyResult<SampleError> = Result.Success("ok").asEmptyResult()

        assertThat(empty).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun mapError_transforms_error() {
        val mapped = Result.Error(SampleError.TOO_SHORT).mapError { OtherError.BAD }

        assertThat(mapped).isEqualTo(Result.Error(OtherError.BAD))
    }

    @Test
    fun mapError_preserves_success() {
        val mapped = Result.Success(3).mapError { OtherError.BAD }

        assertThat(mapped).isEqualTo(Result.Success(3))
    }

    @Test
    fun errorOrNull_returns_error_only() {
        assertThat(Result.Error(SampleError.TOO_SHORT).errorOrNull())
            .isEqualTo(SampleError.TOO_SHORT)
        assertThat(Result.Success(1).errorOrNull()).isEqualTo(null)
    }

    private enum class OtherError : Error {
        BAD,
    }
}
