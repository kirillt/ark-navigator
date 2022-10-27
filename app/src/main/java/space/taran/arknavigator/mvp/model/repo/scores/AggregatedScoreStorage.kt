package space.taran.arknavigator.mvp.model.repo.scores

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.Score

class AggregatedScoreStorage(
    private val shards: Collection<ScoreStorage>
) : ScoreStorage {

    override fun contains(id: ResourceId) = shards
        .any { it.contains(id) }

    override fun countScores(): Int {
        var number = 0
        shards.forEach {
            number = it.countScores()
        }
        return number
    }

    override fun setScore(id: ResourceId, score: Score) {
        shards.forEach {
            if (it.contains(id))
                it.setScore(id, score)
        }
    }

    override fun getScore(id: ResourceId): Score {
        var score: Score = 0
        shards.forEach {
            if (it.contains(id))
                score = it.getScore(id)!!
        }
        return score
    }

    override suspend fun persist() {
        shards.forEach {
            it.persist()
        }
    }
}
